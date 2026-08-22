# System Design

## Architecture

The application is a Spring Boot modular monolith. Thymeleaf controllers provide role-specific browser workflows; a small read API exposes equivalent views. Spring Security authenticates by email and authorizes ADMIN, DOCTOR, and PATIENT route groups. Services own business rules, Spring Data repositories own persistence, and scheduled workers process temporary holds and notifications.

## Main workflow

1. An admin creates a doctor profile with specialty, hours, and slot duration.
2. A patient searches active doctors and requests slots for a date.
3. Availability excludes past times, leave dates, booked appointments, and unexpired holds.
4. Booking inserts a five-minute `HELD` appointment with a database-unique reservation key. This prevents two concurrent requests from owning the same slot.
5. The AI service generates a pre-visit brief and urgency cue. If OpenAI is unavailable, a deterministic safe fallback is used. The hold becomes `BOOKED`.
6. Calendar synchronization is attempted only for connected users. Email confirmation jobs are committed to an outbox and sent asynchronously.
7. A doctor reviews the brief, records clinical notes/prescription/follow-up, and completes the visit. A patient-friendly summary and optional medication-reminder schedule are generated.

## Reliability boundaries

Booking data is authoritative. OpenAI, Google, and SendGrid failures never roll back a successful database operation. Calendar calls are best effort. Notifications use stored jobs with bounded retries; when no email provider is configured they remain visible as skipped audit entries. Expired holds and due reminders are processed by idempotent scheduled scans.

## Security and privacy

Passwords use BCrypt. Route rules enforce roles; patient and doctor service methods also query by the authenticated user's ID to prevent cross-account access. POST forms use CSRF protection. OAuth state is stored in the HTTP session. Tokens and API secrets are environment-configured and excluded from Git. Calendar descriptions intentionally omit medical data.

For a real deployment, encrypt OAuth tokens at rest, use a managed secret store, disable demo users, add MFA, implement audit/event retention, verify backup restoration, perform threat modeling and dependency scanning, and complete applicable health-data compliance work.

## Scaling

The unique database constraint remains safe across multiple web instances. Scheduled jobs are suitable for one instance; at larger scale, workers should claim jobs using row locks or a queue and use a distributed scheduler. PostgreSQL indexes should be added for doctor/date/status and notification due-time queries. External calls can be moved behind a message broker without changing the booking model.
