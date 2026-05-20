/**
 * Load test: POST /v1/payments at a fixed arrival rate.
 *
 * Defaults: 250 TPS, 1_000_000 requests (~66.7 min).
 *
 *   node scripts/load-test.mjs
 *   node scripts/load-test.mjs --tps 250 --total 1000000 --url http://localhost:8080
 *   node scripts/load-test.mjs --total 10000 --summary evidence/run-summary.json
 */

import fs from 'node:fs';
import path from 'node:path';

const args = process.argv.slice(2);
function arg(name, fallback) {
  const i = args.indexOf(name);
  return i >= 0 && args[i + 1] ? args[i + 1] : fallback;
}

const TARGET_TPS = Number(arg('--tps', process.env.TPS ?? '250'));
const TOTAL = Number(arg('--total', process.env.TOTAL ?? '1000000'));
const BASE_URL = (arg('--url', process.env.BASE_URL ?? 'http://localhost:8080')).replace(/\/$/, '');
const SENDER_ID = Number(arg('--sender', '1001'));
const RECEIVER_ID = Number(arg('--receiver', '2002'));
const AMOUNT = Number(arg('--amount', '1'));
const CURRENCY = arg('--currency', 'INR');
const MAX_IN_FLIGHT = Number(arg('--max-in-flight', '2000'));
const SUMMARY_PATH = arg('--summary', process.env.SUMMARY_PATH ?? '');

const INTERVAL_MS = 1000 / TARGET_TPS;
const RUN_ID = Date.now();

const stats = {
  sent: 0,
  completed: 0,
  byStatus: new Map(),
  errors: 0,
  latencyMs: [],
  maxLatencySample: 50_000,
};

function recordStatus(status) {
  const key = String(status);
  stats.byStatus.set(key, (stats.byStatus.get(key) ?? 0) + 1);
}

function recordLatency(ms) {
  stats.latencyMs.push(ms);
  if (stats.latencyMs.length > stats.maxLatencySample) {
    stats.latencyMs.splice(0, stats.latencyMs.length - stats.maxLatencySample);
  }
}

async function sendPayment(seq) {
  const idempotencyKey = `load-${RUN_ID}-${seq}`;
  const started = performance.now();
  try {
    const res = await fetch(`${BASE_URL}/v1/payments`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Idempotency-Key': idempotencyKey,
      },
      body: JSON.stringify({
        senderId: SENDER_ID,
        receiverId: RECEIVER_ID,
        amount: AMOUNT,
        currency: CURRENCY,
      }),
    });
    await res.arrayBuffer();
    recordStatus(res.status);
    recordLatency(performance.now() - started);
  } catch {
    stats.errors++;
  } finally {
    stats.completed++;
  }
}

function percentile(sorted, p) {
  if (sorted.length === 0) return 0;
  const idx = Math.min(sorted.length - 1, Math.ceil((p / 100) * sorted.length) - 1);
  return sorted[idx];
}

function report(final = false) {
  const elapsedSec = (performance.now() - startMs) / 1000;
  const achievedTps = stats.completed / Math.max(elapsedSec, 0.001);
  const sorted = [...stats.latencyMs].sort((a, b) => a - b);
  const statusLine = [...stats.byStatus.entries()]
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([k, v]) => `${k}=${v}`)
    .join(' ');

  const line = [
    final ? 'FINAL' : 'progress',
    `completed=${stats.completed}/${TOTAL}`,
    `sent=${stats.sent}`,
    `inFlight=${stats.sent - stats.completed}`,
    `achievedTPS=${achievedTps.toFixed(1)}`,
    `targetTPS=${TARGET_TPS}`,
    `errors=${stats.errors}`,
    statusLine || 'no responses yet',
    sorted.length
      ? `latencyMs p50=${percentile(sorted, 50).toFixed(0)} p95=${percentile(sorted, 95).toFixed(0)} p99=${percentile(sorted, 99).toFixed(0)}`
      : '',
  ]
    .filter(Boolean)
    .join(' | ');

  console.log(line);
}

function writeSummary(elapsedSec, achievedTps) {
  if (!SUMMARY_PATH) return;
  const sorted = [...stats.latencyMs].sort((a, b) => a - b);
  const byStatus = Object.fromEntries(stats.byStatus);
  const payload = {
    runId: RUN_ID,
    targetTps: TARGET_TPS,
    total: TOTAL,
    completed: stats.completed,
    sent: stats.sent,
    errors: stats.errors,
    achievedTps: Number(achievedTps.toFixed(2)),
    elapsedSec: Number(elapsedSec.toFixed(2)),
    elapsedMinutes: Number((elapsedSec / 60).toFixed(2)),
    byStatus,
    latencyMs: {
      p50: percentile(sorted, 50),
      p95: percentile(sorted, 95),
      p99: percentile(sorted, 99),
    },
    config: {
      baseUrl: BASE_URL,
      senderId: SENDER_ID,
      receiverId: RECEIVER_ID,
      amount: AMOUNT,
      currency: CURRENCY,
      maxInFlight: MAX_IN_FLIGHT,
    },
    finishedAt: new Date().toISOString(),
  };
  fs.mkdirSync(path.dirname(SUMMARY_PATH), { recursive: true });
  fs.writeFileSync(SUMMARY_PATH, JSON.stringify(payload, null, 2));
  console.log(`summaryWritten=${SUMMARY_PATH}`);
}

const startMs = performance.now();
let nextArrival = startMs;
let finished = false;

const progressTimer = setInterval(() => {
  if (!finished) report(false);
}, 5000);

function scheduleArrivals() {
  const pump = () => {
    const now = performance.now();
    while (stats.sent < TOTAL && stats.sent - stats.completed < MAX_IN_FLIGHT) {
      if (now < nextArrival) break;
      stats.sent++;
      const seq = stats.sent;
      sendPayment(seq);
      nextArrival += INTERVAL_MS;
    }

    if (stats.completed >= TOTAL) {
      finished = true;
      clearInterval(progressTimer);
      const elapsedSec = (performance.now() - startMs) / 1000;
      const achievedTps = stats.completed / Math.max(elapsedSec, 0.001);
      report(true);
      writeSummary(elapsedSec, achievedTps);
      console.log(`elapsedMinutes=${(elapsedSec / 60).toFixed(2)}`);
      process.exit(stats.errors > 0 ? 1 : 0);
      return;
    }

    const delay = Math.max(0, nextArrival - performance.now());
    setTimeout(pump, Math.min(delay, INTERVAL_MS));
  };
  pump();
}

console.log(
  `load-test url=${BASE_URL} tps=${TARGET_TPS} total=${TOTAL} ` +
    `durationSec~${(TOTAL / TARGET_TPS).toFixed(0)} sender=${SENDER_ID} receiver=${RECEIVER_ID} amount=${AMOUNT}`,
);
scheduleArrivals();
