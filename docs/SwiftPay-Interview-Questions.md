# SwiftPay — 2nd Round Interview Questions

**Project:** SwiftPay Real-Time Payment Ledger  
**Purpose:** Demo / technical interview preparation  
**Modules:** gateway-service, ledger-service, swiftpay-shared

---

## Part 1 — Demo Opener

1. Walk us through one payment end-to-end in 3 minutes. Where does the request enter, where does money actually move, and where does the client see the final status?

2. Show us insufficient funds. What HTTP status do we get, and what happens if funds look OK at the gateway but fail at settlement?

3. Replay the same Idempotency-Key. What comes back the second time? What if the body changes but the key stays the same?

4. What happens if Kafka is down or slow when you POST a payment? Do we lose money? Do we get duplicates?

5. Why did you skip Service C (ClickHouse analytics)? If we asked you to add it next week, where would it sit in your architecture?

---

## Part 2 — Architecture & Design

6. Why did you split one monolith into two microservices?

7. Why does Gateway not directly update balances?

8. Why did you choose asynchronous Kafka settlement instead of synchronous REST settlement?

9. Why did you use Kafka between services but HTTP for balance validation?

10. Why not use direct database access from gateway to ledger tables?

11. Why did you choose Redis for idempotency?

12. Why did you choose PostgreSQL instead of MongoDB?

13. Why is transaction status first PENDING and later COMPLETED?

14. What happens if Kafka is down after the transaction row is inserted?

15. Why did you use one parent Maven with multi-module architecture?

16. Why did you skip Service C analytics?

17. How would you scale this architecture to 10 million TPS?

18. Why two services (Gateway + Ledger) instead of one Spring Boot app? What breaks if we merge them tomorrow?

19. Both services connect to the same PostgreSQL database. In production, would you keep that? How would you split the data ownership?

20. Who is the source of truth for balance — Redis, Gateway, or Ledger? What happens when Redis and Postgres disagree?

21. What alternative architectures did you reject? (e.g. saga vs outbox, sync transfer, event sourcing)

22. If load test at 250 TPS failed, what would you tune first — connection pools, Kafka partitions, Redis, or HTTP balance calls?

23. Your README shows gateway → HTTP balance → Redis → validate. Why cache balance in Redis if you already called Ledger authoritatively?

---

## Part 3 — How You Arrived at This Architecture

24. Show us your architecture view — did you start with a C4 diagram, a sequence diagram, or code first? What changed after implementation?

25. What were the top 3 non-functional requirements that drove the design?

26. You said you started with a monolith and moved to microservices using an agent. What was still monolith at that point?

27. What triggered the split — performance, team boundaries, hackathon requirements, or agent suggestion?

28. Why Maven parent POM with 3 modules instead of two separate Git repos? When would you split repos?

29. What lives in swiftpay-shared and what must never go there?

30. How do you version and deploy gateway vs ledger independently if they share events in swiftpay-shared?

31. Would you use the same structure for 10 microservices? Where is the line?

---

## Part 4 — Kafka & Async Flow

32. Explain producer and consumer flow in your project.

33. Why is Gateway producer and Ledger consumer first?

34. Why did you later add Ledger producer and Gateway consumer?

35. What happens if the consumer crashes after consuming a message?

36. How do Kafka retries work in your system?

37. Why did you configure Kafka listener concurrency?

38. What is eventual consistency?

39. What is the difference between synchronous consistency vs eventual consistency?

40. Why is async better under heavy load?

41. What happens if duplicate Kafka events arrive?

42. How would you avoid duplicate settlement?

43. What is at-least-once delivery?

44. What happens if Kafka processing is slower than HTTP requests?

45. Why is payment settlement asynchronous via Kafka instead of a synchronous debit in the POST handler?

46. Why PENDING → COMPLETED instead of returning COMPLETED in the POST response?

47. What guarantees do you have that payment.completed is processed exactly once on the gateway side?

48. If the gateway saves PENDING and crashes before publishing to Kafka, what is the recovery story?

49. If Ledger settles successfully but the gateway never consumes payment.completed, what does the client see?

50. Describe your Kafka consumer retry strategy. When do you stop retrying? What goes to DLQ (or doesn't)?

---

## Part 5 — Database & Transactions

51. Explain atomic settlement.

52. How do debit and credit happen safely?

53. Why use database transaction boundaries?

54. What isolation level did you use and why?

55. How did you prevent deadlocks?

56. Why lock accounts in ordered userId sequence?

57. What happens under hot account contention?

58. Why not optimistic locking?

59. Why did you choose PostgreSQL row locking?

60. Explain ACID properties in your project.

61. What happens if the server crashes during settlement?

62. How do you prevent deadlocks when many payments hit the same two accounts?

---

## Part 6 — Redis & Idempotency

63. Explain idempotency in your project.

64. Why 24-hour TTL?

65. What happens if the same Idempotency-Key comes with a different request body?

66. Why did you use Redis instead of DB unique constraint only?

67. What Redis keys are used?

68. What happens when Redis goes down?

69. What is the difference between cache and idempotency store?

70. The brief mentioned idempotency on transaction_id; you used Idempotency-Key header + server UUID. Defend that design choice.

71. After 24 hours, same key — what happens on day 25 if the client retries?

---

## Part 7 — Clean Architecture & Code Quality

72. Explain your port vs infrastructure packages (or service/cache/repository layers).

73. What is PaymentInitiationUseCase vs PaymentInitiationService? Why the interface?

74. How do you test settlement logic without Kafka and Redis running?

75. Show one integration test. What is real (Testcontainers) vs mocked/in-process?

76. How do you enforce API standards — OpenAPI, error shape, HTTP codes? Show 409 vs 422 vs 404.

---

## Part 8 — Performance & Load Testing

77. Explain your 1 million transaction load test.

78. What is 250 TPS?

79. How did you maintain fixed TPS?

80. What is max-in-flight?

81. Why max-in-flight 2000?

82. Why Tomcat max threads 300/400?

83. Why Hikari pool 64?

84. Why Kafka concurrency 8?

85. Why reduce SQL logging during load tests?

86. What bottlenecks did you observe?

87. Why were some requests failing initially?

88. How did you improve success rate from 35% to 99.9%?

89. Why did latency reduce drastically in the final test?

90. What is backpressure?

91. What is thread starvation?

92. What is the difference between CPU bottleneck vs DB bottleneck?

93. What would happen if TPS increases to 1000?

---

## Part 9 — Docker & Kubernetes

94. Why use Docker?

95. What services are inside Docker Compose?

96. How do services communicate inside Docker?

97. Why use environment variables?

98. What is the difference between Docker Compose and Kubernetes?

99. How would Kubernetes help scaling?

100. What is a pod?

101. What is a deployment?

102. What is a service in Kubernetes?

103. Did you test Kubernetes locally?

104. How would you autoscale the gateway service?

105. Run through docker compose up. What starts first? How does gateway find ledger?

106. You have K8s manifests — how is this different from Compose for local hackathon vs Minikube demo?

---

## Part 10 — CI/CD & Testing

107. What tests did you write?

108. What is the difference between unit test and integration test?

109. Why are E2E tests important?

110. How did GitHub Actions help?

111. Why separate integration profile?

112. What does the /health endpoint validate?

113. Walk through your GitHub Actions pipeline. What runs on every PR? What needs Docker?

114. How are secrets and environment-specific URLs handled?

115. What is missing for real production?

---

## Part 11 — AI Usage (Very Important)

116. What parts did AI help you with?

117. What parts did you personally design?

118. How did you validate AI-generated code?

119. How did you identify wrong architecture suggestions from AI?

120. Give an example where you corrected AI logic.

121. How did you ensure the code matches your intended flow?

122. How do you understand code you didn't fully write?

123. Why is using AI different from copying someone else's project?

124. What technical decisions were fully yours?

125. If the interviewer changes one requirement now, can you modify the system?

126. Explain one Kafka class line by line.

127. Explain one Redis implementation line by line.

128. Explain settlement flow without reading code.

129. Did the agent suggest microservices, or did you? How did you decide the agent's suggestion was correct?

130. Where did the agent help most — configs, Docker, tests, or architecture?

131. What did the agent get wrong that you had to fix?

132. If we ban AI tomorrow, could you explain and extend every file in the payment flow?

---

## Part 12 — Cloud Native Java Developer

133. Java 21 — what features did you actually use?

134. Spring Boot 3.4 — why Spring over Quarkus for this workload?

135. How would you scale gateway horizontally?

136. What is your partition strategy for payment.initiated — by sender, receiver, or transaction id?

137. Connection pool sizing for Postgres under 250 TPS — what numbers did you use and why?

138. Observability gap: you have logging and health — where are metrics, traces, and correlation IDs?

139. Incident scenario: Ledger DB is up but slow; Kafka lag grows. What alarms fire and what do you do first?

---

## Part 13 — Behavioral & Judgment

140. You had 2 days. What did you cut consciously?

141. What is the biggest technical debt in this repo?

142. If a payment double-credits a user in production, how would you RCA with this codebase?

143. How would you onboard a junior to this repo in one hour?

---

## Part 14 — Live Exercise (They May Spring These)

144. Add a GET endpoint on gateway that proxies ledger history — or explain why you kept history only on Ledger.

145. Change idempotency from 24h to 7 days — what files change?

146. Introduce a third currency or a payment fee — which service owns the rule?

147. Draw on whiteboard: sequence diagram for duplicate POST with same idempotency key while first request is still PENDING.

148. Explain PendingPaymentService line by line.

149. Change idempotency from 24h to 7 days — what files change?

---

## Part 15 — Advanced Questions

150. What is the eventual consistency tradeoff?

151. How would you implement saga pattern here?

152. What happens during network partition?

153. How would you support rollback/refund?

154. How would you secure payment APIs?

155. How would you add authentication?

156. How would you avoid replay attacks?

157. How would you implement rate limiting?

158. How would you partition Kafka topics?

159. If the database becomes the bottleneck, what next?

160. Add a GET endpoint on gateway that proxies ledger history.

---

## Part 16 — Questions You Should Ask Them

161. Do you standardize on database-per-service or allow shared Postgres in early products?

162. Is synchronous balance check + async settlement acceptable in your domain, or do you require strong consistency on POST?

163. What is your Kafka operational model — MSK, Confluent, Strimzi on K8s?

164. How do you expect teams to use AI tooling in code review and production ownership?

---

*Generated for SwiftPay hackathon / Cloud Native Java Developer 2nd round preparation.*
