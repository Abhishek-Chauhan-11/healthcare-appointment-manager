# Database Model

| Table | Purpose | Key relationships/invariants |
|---|---|---|
| `users` | Accounts and roles | Unique email; ADMIN, DOCTOR, PATIENT |
| `doctor_profiles` | Clinical and schedule details | One-to-one with users |
| `appointments` | Holds, bookings, visits, and summaries | Doctor + patient; optimistic version; unique nullable `reservation_key` |
| `doctor_leaves` | Unavailable dates | Unique doctor/date |
| `notification_jobs` | Durable email outbox | Optional appointment link; retry status |
| `medication_reminders` | Scheduled prescription prompts | Appointment + patient |
| `google_calendar_tokens` | Per-user OAuth tokens | One-to-one with users |

## Double-booking protection

An active appointment stores `doctorId|date|startTime` in `reservation_key`. The database unique constraint is the final concurrency guard. Cancelled appointments set this key to `NULL`, freeing the slot. New selections begin as `HELD` for five minutes and become `BOOKED` after summary generation. A scheduled cleanup removes expired holds.

## Local and production databases

The default profile uses persistent H2 at `./data/healthcaredb`; `data/` is gitignored. The `prod` profile uses PostgreSQL configuration from environment variables. Hibernate schema update is convenient for this educational build; use reviewed Flyway migrations and `ddl-auto=validate` before handling real health records.
