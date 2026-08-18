# CreditConnect

A small backend that models a digital lending marketplace. A customer applies for
a loan, the system checks eligibility and asks every lender that can serve the
request for an offer, the customer accepts one offer, and the resulting loan is
repaid instalment by instalment.

I built this to understand how a lending platform actually fits together — the
application/offer/loan split, the state transitions, and what goes wrong when two
requests try to accept the same offer at once.

## Tech

- Java 21, Spring Boot 3.5
- PostgreSQL 16, schema managed by Flyway
- Spring Data JPA, Bean Validation
- Maven
- H2 in-memory for the test suite

## Running it

Start the database:

```
docker compose up -d
```

Run the app:

```
mvn spring-boot:run
```

It listens on port 8080. Flyway creates the schema and seeds three lenders on
first start. Hibernate runs with `ddl-auto=validate`, so if an entity and the
migration ever disagree the application refuses to start.

Run the tests:

```
mvn test
```

The tests use H2, so no database or Docker is needed for them.

## Domain model

```
User                    Lender
 id                      id
 name                    name
 email                   availableAmount
 monthlyIncome           minIncome
                         baseInterestRate
                         maxTenureMonths

LoanApplication         LoanOffer               Loan                 Repayment
 id                      id                      id                   id
 user                    application             application          loan
 amount                  lender                  lender               instalmentNumber
 tenureMonths            offeredAmount           principal            dueDate
 status                  interestRate            interestRate         amount
 rejectionReason         status                  tenureMonths         status
                                                 emiAmount            paidAt
                                                 status
```

`LoanApplication` is the request. `Loan` is the funded contract. They are separate
tables because an application can be rejected and never become a loan, and a loan
has its own lifecycle after that.

## Status flow

```
LoanApplication
    PENDING ──────────────> OFFERS_AVAILABLE ──────> ACCEPTED
       │                          │
       └──────> REJECTED          └──────> CANCELLED

Offer     AVAILABLE ──> ACCEPTED
          AVAILABLE ──> REJECTED     (a sibling offer was accepted)

Loan      ACTIVE ──> COMPLETED       (final instalment paid)

Repayment PENDING ──> PAID
```

## Business rules

Eligibility ceiling, based on monthly income:

```
income >= 50,000   ->  maximum loan = 5 x income
income <  50,000   ->  maximum loan = 2 x income
```

So someone earning ₹60,000 a month can borrow up to ₹3,00,000. Ask for more and
the application comes back `REJECTED` with the limit in the reason.

A lender produces an offer only if it is active, has enough money left in its
pool, the applicant clears its minimum income, and the requested tenure is within
its maximum.

Pricing is the lender's base rate, plus 1.5 percentage points if the requested
amount is more than three times monthly income — more leverage relative to income
is priced higher.

EMI uses the standard reducing balance formula:

```
emi = P * r * (1+r)^n / ((1+r)^n - 1)
```

where `r` is the monthly rate. All money is `BigDecimal`.

## API

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/users` | create a customer |
| GET | `/api/users/{id}` | fetch a customer |
| GET | `/api/users/{id}/loans` | that customer's funded loans |
| POST | `/api/loans/apply` | submit an application, which triggers evaluation |
| GET | `/api/loans/{id}` | application detail and status |
| GET | `/api/loans/{id}/offers` | offers generated for an application |
| POST | `/api/offers/{id}/accept` | accept an offer, creating the loan and schedule |
| POST | `/api/loans/{id}/repay` | pay the next pending instalment |

One naming quirk worth stating plainly: `GET /api/loans/{id}` and
`POST /api/loans/{id}/repay` both take an **application** id, while
`GET /api/users/{id}/loans` returns funded loans. I kept `/loans/{id}` pointing at
the application because that is the id the customer gets back when they apply.

## Walkthrough

Create a customer:

```
curl -X POST http://localhost:8080/api/users \
  -H 'Content-Type: application/json' \
  -d '{"name":"Rahul Sharma","email":"rahul@example.com","monthlyIncome":60000}'
```

```json
{"id":1,"name":"Rahul Sharma","email":"rahul@example.com","monthlyIncome":60000}
```

Apply for ₹50,000 over 12 months:

```
curl -X POST http://localhost:8080/api/loans/apply \
  -H 'Content-Type: application/json' \
  -d '{"userId":1,"amount":50000,"tenureMonths":12}'
```

```json
{"applicationId":1,"userId":1,"amount":50000,"tenureMonths":12,
 "status":"OFFERS_AVAILABLE","rejectionReason":null,"offerCount":3}
```

See the offers, cheapest first:

```
curl http://localhost:8080/api/loans/1/offers
```

```json
[{"offerId":1,"lenderId":1,"lender":"HDFC Bank","amount":50000.0,"interestRate":12.0,"status":"AVAILABLE"},
 {"offerId":2,"lenderId":2,"lender":"ABC NBFC","amount":50000.0,"interestRate":13.5,"status":"AVAILABLE"},
 {"offerId":3,"lenderId":3,"lender":"XYZ NBFC","amount":50000.0,"interestRate":15.0,"status":"AVAILABLE"}]
```

Accept the cheapest one:

```
curl -X POST http://localhost:8080/api/offers/1/accept
```

```json
{"loanId":1,"applicationId":1,"lender":"HDFC Bank","principal":50000.0,
 "interestRate":12.0,"tenureMonths":12,"emiAmount":4442.44,
 "status":"ACTIVE","pendingInstalments":12}
```

The other two offers are now `REJECTED`, and trying to accept one returns 409:

```
curl -X POST http://localhost:8080/api/offers/2/accept
```

```json
{"timestamp":"2026-08-18T12:13:12.028045","status":409,"error":"Conflict",
 "message":"Loan application 1 is not accepting offers (status ACCEPTED)",
 "path":"/api/offers/2/accept","fieldErrors":null}
```

Pay an instalment:

```
curl -X POST http://localhost:8080/api/loans/1/repay
```

```json
{"repaymentId":1,"instalmentNumber":1,"dueDate":"2026-09-18","amount":4442.44,
 "status":"PAID","remainingInstalments":11,"loanStatus":"ACTIVE"}
```

After the twelfth instalment the loan flips to `COMPLETED`.

Asking for more than the eligibility ceiling is not an error, it is a rejected
application:

```
curl -X POST http://localhost:8080/api/loans/apply \
  -H 'Content-Type: application/json' \
  -d '{"userId":1,"amount":400000,"tenureMonths":12}'
```

```json
{"applicationId":2,"userId":1,"amount":400000,"tenureMonths":12,"status":"REJECTED",
 "rejectionReason":"Requested amount exceeds eligibility limit of 300000.00","offerCount":0}
```

Invalid input returns 400 with the offending fields:

```json
{"timestamp":"2026-08-18T12:13:12.166621","status":400,"error":"Bad Request",
 "message":"Request validation failed","path":"/api/users",
 "fieldErrors":{"name":"name is required","email":"email must be valid",
                "monthlyIncome":"monthlyIncome must be greater than zero"}}
```

## Accepting an offer, and why it needs a lock

Accepting is the only operation with a real race. If the same application is
accepted twice at the same moment, you would get two loans against one
application, and the lender's pool would be debited twice.

`OfferService.acceptOffer` is `@Transactional` and starts by re-reading the parent
application with `@Lock(LockModeType.PESSIMISTIC_WRITE)`, which issues
`SELECT ... FOR UPDATE`. Concurrent accepts against the same application queue
behind that row lock, so the second one only begins after the first has committed
and finds the application already `ACCEPTED`. It gets a 409.

There is also a partial unique index in the schema:

```sql
create unique index idx_loan_offer_one_accepted
    on loan_offer (application_id)
    where status = 'ACCEPTED';
```

That is a backstop, not the mechanism. It means the database refuses a second
accepted offer even if the service logic is wrong.

`OfferAcceptanceConcurrencyTest` fires two threads at the same offer off a
`CountDownLatch` and asserts that exactly one succeeds and the other fails with
`InvalidLoanStateException` specifically. That distinction matters: I checked by
removing the lock, and the test then fails with a
`DataIntegrityViolationException` instead — both threads sail past the status
check and only the unique index stops the second one. Asserting on the exception
type is what makes the test prove the lock is doing the work.

## Layout

```
controller/   HTTP and DTO mapping
service/      business logic and transaction boundaries
repository/   Spring Data JPA interfaces
model/        JPA entities and status enums
dto/          request and response records
exception/    custom exceptions and the @RestControllerAdvice
```

`EligibilityService` holds the lending rules and has no repository dependencies,
so the rules are unit tested on their own without a database.

## Limitations

Things I left out on purpose, to keep the project small enough to understand
completely:

- No authentication. Callers pass user ids directly.
- Repayments are all-or-nothing per instalment. No partial payment, no
  prepayment, no late fees.
- Offers never expire.
- The eligibility rule is a fixed income multiple, not a credit score.
- No UPI, Aadhaar, credit bureau, or any real disbursal.
