# Complete Payment Flow: Client → DB/Redis → Kafka → DB/Redis → Client

## Gateway Redis keys (4 types)

| Key | When created | Typical values | Purpose |
|-----|--------------|----------------|---------|
| `idempotency:{key}` | Step 2 | `PROCESSING` → `COMPLETED` (or `FAILED` on validation error) | Same idempotency key cannot create two payments |
| `balance:{userId}` | Step 4 (sender); refreshed step 18 | e.g. `10000.0` (always a **number**, never `FAILED`) | Cached balance for fast validation / post-settlement sync |
| `tx:processed:{transactionId}` | Step 7 | `RESERVED` → `COMPLETED` | Same transaction id cannot be used twice |
| `tx:status:{transactionId}` | **Step 18 only** (not at create) | `COMPLETED` or `FAILED` | Cached **final payment outcome** after ledger feedback |

Steps 1–11 do **not** write `tx:status:{transactionId}`. That key appears only when gateway consumes `payment.completed` or `payment.failed`.

---

## Step-by-step flow

1. **Client POST** (+ `Idempotency-Key` header)

2. **`idempotency:{key}` = PROCESSING** (amount, currency, sender, receiver from body; no `transactionId` yet)

3. **Ledger HTTP GET** `.../accounts/{id}/balance?currency=INR` (sender + receiver existence checks)

4. **`balance:{senderId}`** = balance from ledger (not payment amount)

5. **Check** `amount ≤ balance` (read from Redis)

6. **UUID** created

7. **`tx:processed:{transactionId}` = RESERVED**

8. **DB save** PENDING (gateway Postgres)

9. **DB transaction commits** (payment row = `PENDING`)

10. **`afterCommit`** (same request, before HTTP response returns):
    - `idempotency:{key}` = **COMPLETED** (state + `transactionId` + amount, currency, sender, receiver) — basis: **Postgres commit succeeded** (idempotency “create finished”, not money settled)
    - `tx:processed:{transactionId}` = **COMPLETED** (was `RESERVED`)
    - **Kafka publish** → topic `payment.initiated` (`PaymentInitiatedEvent`)
    - **`tx:status:{transactionId}`** → **not created yet**

11. **HTTP 201** response to client
    - body: `transactionId`, status **PENDING**, `idempotencyKey`, amount, currency, sender, receiver
    - (`idempotency:{key}` is **not** updated again after this)

12. **Kafka consume** `payment.initiated` (ledger-service)
    - `PaymentInitiatedKafkaListener.onPaymentInitiated()`

13. **Settle in ledger DB**
    - `PaymentSettlementProcessor.settlePayment()`
    - lock accounts, debit sender, credit receiver (on success)
    - ledger transaction row → **COMPLETED** or **FAILED** (or **SKIPPED** if not PENDING)

14. **Ledger Redis refresh** (ledger’s own cache, not gateway)
    - `AccountBalanceRedisUpdater.syncAfterSettlement()`
    - `balance:{userId}` on **ledger** Redis (if not SKIPPED)

15. **Kafka publish outcome**
    - `payment.completed` **OR** `payment.failed`

16. **Kafka consume** `payment.completed` OR `payment.failed` (gateway)
    - `PaymentResultKafkaListener.onPaymentCompleted()` / `onPaymentFailed()`

17–18. **Gateway update on settlement feedback** (`PaymentStatusUpdateService`) — **DB first, then Redis**
    - **17. Postgres first** — `updateStatusIfPending()` → `COMPLETED` or `FAILED` (only if row is still `PENDING`)
    - **18. Redis second** — `PaymentRedisCacheUpdater`:
      - **`tx:status:{transactionId}` = COMPLETED or FAILED** ← only key that stores outcome text (`FAILED` / `COMPLETED`)
      - `balance:{senderId}` refreshed via `LedgerBalanceClient` (+ `balance:{receiverId}` on **success** only)
      - `idempotency:{key}` → **NOT changed** (stays COMPLETED)
      - `tx:processed:{transactionId}` → **NOT changed** (stays COMPLETED)
    - **Note:** `@Transactional` covers Postgres only. If DB fails, Redis is skipped. If DB succeeds and Redis fails, DB stays updated.

---

## Request-Response Lifecycle

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          PHASE 1: CLIENT REQUEST                            │
└─────────────────────────────────────────────────────────────────────────────┘

CLIENT
  │
  └─→ POST /v1/payments
      Headers: Idempotency-Key: "abc-123"
      Body: {senderId, receiverId, amount, currency}
            │
            ├─→ PaymentController.create(idempotencyKey, request)
            │
            └─→ PaymentService.createPayment(idempotencyKey, command)


┌─────────────────────────────────────────────────────────────────────────────┐
│                    PHASE 2: REDIS LOCK + VALIDATION                         │
└─────────────────────────────────────────────────────────────────────────────┘

    PaymentIdempotencyService.requireNewPaymentLock(idempotencyKey, command)
         │
         ├─→ PaymentIdempotencyCache.lockIdempotencyKeyOrReturnExisting()
         │    │
         │    └─→ Redis.setIfAbsent("idempotency:abc-123", PROCESSING, 24hrs)
         │        ✓ Outcome: PROCEED (acquired lock)
         │
         └─→ Validate payment rules
             Execute business validators
             Reserve transactionId ("uuid-123")

    ✓ Redis State:
      Key: "idempotency:abc-123"
      Value: {
        state: PROCESSING,
        transactionId: null,
        senderId, receiverId, amount, currency
      }


┌─────────────────────────────────────────────────────────────────────────────┐
│                       PHASE 3: DB PERSISTENCE                               │
└─────────────────────────────────────────────────────────────────────────────┘

    PendingPaymentService.savePendingPaymentAndPublishEvent(...)
         │
         ├─→ paymentRepository.savePending(transactionId, idempotencyKey, command)
         │    │
         │    └─→ INSERT INTO transactions (
         │           transactionId="uuid-123",
         │           idempotencyKey="abc-123",
         │           senderId, receiverId, amount, currency,
         │           status=PENDING,
         │           createdAt
         │        )
         │        ✓ Unique constraints on transactionId & idempotencyKey
         │
         ├─→ Create PaymentResponse DTO
        │           state: READY,  ← Release lock, transactionId registered
         │
         └─→ Register TransactionSynchronization callbacks

    ✓ DB State:
      transactions table:
      │ transactionId │ idempotencyKey │ status  │ createdAt │
      │ uuid-123      │ abc-123        │ PENDING │ 2026-... │


┌─────────────────────────────────────────────────────────────────────────────┐
│                    PHASE 4: DB COMMIT ✓ (Critical)                          │
└─────────────────────────────────────────────────────────────────────────────┘

    Spring Transaction commits to PostgreSQL
         │
         ├─→ INSERT confirmed
         ├─→ Unique constraint indexes updated
         └─→ afterCommit() callbacks triggered

        state: READY,
┌─────────────────────────────────────────────────────────────────────────────┐
│              PHASE 5: POST-COMMIT - REDIS & KAFKA PUBLISH                   │
└─────────────────────────────────────────────────────────────────────────────┘

    TransactionSynchronization.afterCommit() {
         │
         ├─→ idempotencyCache.saveCompletedIdempotencyState(idempotencyKey, transactionId, command)
         │    │
         │    └─→ Redis.set("idempotency:abc-123", {
         │           state: COMPLETED,
         │           transactionId: "uuid-123",
         │           senderId, receiverId, amount, currency
         │        }, TTL=24hrs)
         │
         ├─→ paymentEventProducer.publishPaymentInitiated(response)
         │    │
         │    └─→ Kafka Topic: "payment.initiated"
         │        Message: {
         │          transactionId: "uuid-123",
         │          senderId, receiverId, amount, currency,
         │          timestamp
         │        }
         │
         └─→ Return PaymentResponse to CLIENT (status: PENDING)

    ✓ Redis State UPDATED:
      Key: "idempotency:abc-123"
      Value: {
        state: COMPLETED,
        transactionId: "uuid-123",  ← NOW POPULATED
        senderId, receiverId, amount, currency
      }

    ✓ Kafka Producer:
      Publishes PaymentInitiated event to "payment.initiated" topic


┌─────────────────────────────────────────────────────────────────────────────┐
│                 CLIENT RECEIVES RESPONSE (Immediate)                        │
└─────────────────────────────────────────────────────────────────────────────┘

    Response: {
      "transactionId": "uuid-123",
      "idempotencyKey": "abc-123",
      "status": "PENDING",  ← ⚠️ NOT YET COMPLETED
      "senderId": ...,
      "receiverId": ...,
      "amount": ...,
      "currency": ...,
      "createdAt": "2026-05-24T..."
    }

    ✓ Client receives 201 Created
    ✓ Client can now poll: GET /v1/payments/uuid-123


┌─────────────────────────────────────────────────────────────────────────────┐
│                   PHASE 6: KAFKA PROCESSING (Async)                         │
└─────────────────────────────────────────────────────────────────────────────┘

    Ledger Service processes PaymentInitiated event
         │
         ├─→ Debit sender account
         ├─→ Credit receiver account
         ├─→ Update ledger balances
         │
         └─→ Publish PaymentCompletedEvent OR PaymentFailedEvent
             to Kafka topics:
               ✓ "payment.completed" topic  OR
               ✗ "payment.failed" topic


┌─────────────────────────────────────────────────────────────────────────────┐
│                  PHASE 7: KAFKA CONSUMER FEEDBACK                           │
└─────────────────────────────────────────────────────────────────────────────┘

    PaymentResultKafkaListener.onPaymentCompleted(PaymentCompletedEvent) {
         │
         └─→ PaymentStatusUpdateService.applyPaymentCompleted(event)
              │
              ├─→ @Transactional method
              │
              ├─→ paymentRepository.updateStatusIfPending(
              │    transactionId="uuid-123",
              │    status=COMPLETED
              │   )
              │    │
              │    └─→ UPDATE transactions
              │        SET status='COMPLETED'
              │        WHERE transactionId='uuid-123'
              │        AND status='PENDING'  ← Prevents double updates
              │
              └─→ PaymentRedisCacheUpdater.updateRedisAfterPaymentCompleted(event)
                  │
                  ├─→ paymentStatusCache.writeStatus(
                  │    transactionId="uuid-123",
                  │    status=COMPLETED
                  │   )
                  │    │
                  │    └─→ Redis.set(
                  │           "tx:status:uuid-123",
                  │           "COMPLETED",
                  │           TTL=24hrs
                  │        )
                  │
                  ├─→ refreshBalanceFromLedger(senderId, currency)
                  │    └─→ Redis.set(balance cache for sender)
                  │
                  └─→ refreshBalanceFromLedger(receiverId, currency)
                      └─→ Redis.set(balance cache for receiver)
    }

    ✓ DB State UPDATED:
      transactions table:
      │ transactionId │ idempotencyKey │ status    │ createdAt │
      │ uuid-123      │ abc-123        │ COMPLETED │ 2026-... │

    ✓ Redis State UPDATED:
      Key: "tx:status:uuid-123"
      Value: "COMPLETED"

      Balance caches updated for sender & receiver


┌─────────────────────────────────────────────────────────────────────────────┐
│                   PHASE 8: CLIENT POLLS FOR STATUS                          │
└─────────────────────────────────────────────────────────────────────────────┘

    CLIENT
      │
      └─→ GET /v1/payments/uuid-123
          │
          ├─→ PaymentController.getByTransactionId(transactionId)
          │
          └─→ PaymentService.getPaymentStatus("uuid-123")
              │
              └─→ PaymentRepository.findByTransactionId("uuid-123")
                  SELECT * FROM transactions WHERE transactionId='uuid-123'
                  status → "COMPLETED" (or still PENDING if settlement lagging)

    RESPONSE:
    {
      "transactionId": "uuid-123",
      "idempotencyKey": "abc-123",
      "status": "COMPLETED",  ← ✓ UPDATED from PENDING
      "senderId": ...,
      "receiverId": ...,
      "amount": ...,
      "currency": ...,
      "createdAt": "2026-05-24T..."
    }

    ✓ Client sees final status: COMPLETED


┌─────────────────────────────────────────────────────────────────────────────┐
│                           SUMMARY TIMELINE                                  │
└─────────────────────────────────────────────────────────────────────────────┘

CLARIFICATION: TWO SEPARATE REDIS OPERATIONS
═══════════════════════════════════════════════════════════════════════════════

Flow 5 vs Flow 12 are NOT redundant - they update DIFFERENT Redis keys:

FLOW 5 (Post-Commit) → Idempotency Lock
  Key: "idempotency:abc-123"
  Purpose: Prevent duplicate DB inserts if client retries
  State: PROCESSING → READY (releases lock, registers transactionId)
  When: IMMEDIATELY after DB commit (before payment is processed)

FLOW 12 (Kafka Confirmation) → Transaction Status Cache
  Key: "tx:status:uuid-123"
  Purpose: Cache final transaction status for fast queries
  State: null → COMPLETED (confirms payment actually succeeded)
  When: AFTER Kafka event confirms payment completed

Why both?
  ✓ Flow 5 prevents duplicate inserts (idempotency safety)
  ✓ Flow 12 caches final result (query performance)
  ✓ Idempotency cache persists 24 hours (protects against retries)
  ✓ Status cache can have different TTL (optimized for queries)

═══════════════════════════════════════════════════════════════════════════════

┌─────────────────────────────────────────────────────────────────────────────┐
│                           SUMMARY TIMELINE                                  │
└─────────────────────────────────────────────────────────────────────────────┘

T=0ms:    Client sends POST /v1/payments
          ├─→ Redis idempotency lock acquired (PROCESSING)
          ├─→ DB insert (PENDING)
          └─→ Client receives response (status: PENDING)
T=2ms:    After DB commit (in same transaction)
          ├─→ Redis idempotency lock released (READY with transactionId)
          ├─→ PaymentInitiated event published to Kafka
          └─ Transaction status cache STILL EMPTY

T=1-50ms: Kafka processes PaymentInitiated event
          ├─→ Ledger service credits/debits accounts
          └─→ Publishes PaymentCompletedEvent to Kafka

T=50-100ms: Gateway service receives PaymentCompletedEvent
           ├─→ DB updated to COMPLETED (final confirmation)
           ├─→ Redis transaction status cache updated (COMPLETED)
           └─→ Balance caches refreshed

T=100ms+: Client polls GET /v1/payments/uuid-123
          ├─→ Status: COMPLETED (from Redis or DB)
          └─→ Optional: Check updated balance cache


┌─────────────────────────────────────────────────────────────────────────────┐
│                         KEY SYNCHRONIZATION POINTS                          │
└─────────────────────────────────────────────────────────────────────────────┘

1️⃣ REDIS IDEMPOTENCY LOCK
   Prevents duplicate processing if client retries same idempotencyKey

2️⃣ DB UNIQUE CONSTRAINTS (transactionId, idempotencyKey)
   Safety net if Redis fails during afterCommit()

3️⃣ KAFKA EVENT SOURCING
   PaymentInitiated → Ledger processes → PaymentCompleted/Failed
   Asynchronous, reliable settlement tracking

4️⃣ DB STATUS UPDATE (updateStatusIfPending)
   Only updates if status=PENDING, prevents double-processing

5️⃣ REDIS CACHE SYNC
   Transaction status cache + balance caches synchronized after settlement
   Provides fast queries without hitting DB

6️⃣ AFTER COMMIT CALLBACKS
   TransactionSynchronization ensures Redis & Kafka operations happen AFTER DB commit
   Prevents orphaned Redis entries if DB fails


┌─────────────────────────────────────────────────────────────────────────────┐
│                        FAILURE SCENARIOS                                    │
└─────────────────────────────────────────────────────────────────────────────┘

SCENARIO A: Redis down during afterCommit()
  → DB has INSERT ✓
  → DB has unique constraint ✓
  → Next request with same idempotencyKey:
     PaymentIdempotencyService checks DB via paymentExists()
     → Finds existing payment in DB
     → Returns alreadyProcessed, no duplicate

SCENARIO B: Kafka consumer fails
  → DB remains PENDING
  → Ledger service retries or manual intervention
  → When PaymentCompletedEvent finally arrives:
     DB: UPDATE status from PENDING → COMPLETED ✓
     Redis: Status cache updated ✓

SCENARIO C: Same idempotencyKey, different payload
  → PaymentIdempotencyRedisEntry.matchesCommand() validates
  → Returns CONFLICT (senderId/receiverId/amount/currency mismatch)
  → Prevents accidental payment confusion

SCENARIO D: Stale lock in Redis
  → retryAfterStaleLock flag allows exactly 1 retry
  → First attempt: Redis locked with IN_PROGRESS
  → Check DB via paymentExists() → payment found → alreadyProcessed
  → If payment not found: assume stale lock, release & retry once
