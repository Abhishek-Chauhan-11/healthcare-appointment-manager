# Healthcare Appointment Manager

A role-based clinic appointment system built with Java 17, Spring Boot, Thymeleaf, and PostgreSQL/H2. It supports the complete appointment lifecycle, AI-assisted clinical summaries, email and medication reminders, doctor leave handling, and optional Google Calendar synchronization.

## What is included

- Patient registration and secure login with BCrypt passwords
- Admin, doctor, and patient authorization
- Admin doctor profiles, working hours, slot duration, and leave management
- Doctor search by specialization and real-time available slots
- Atomic five-minute slot holds to prevent double booking
- Booking, cancellation, rescheduling, and 24-hour reminders
- AI pre-visit summaries with urgency triage and safe local fallback
- Doctor visit notes, prescriptions, patient-friendly post-visit summaries, and medication reminders
- SendGrid notification outbox with retries and audit status
- Google Calendar OAuth, event creation, update, and cancellation
- Read-only JSON endpoints for each role
- H2 for local use, PostgreSQL production profile, Docker, Render blueprint, and GitHub Actions CI

External integrations are optional. Without API keys, bookings and summaries still work; AI uses a deterministic safety-focused fallback and notification jobs are retained as skipped audit records.

## Requirements

- Windows 11 (or macOS/Linux)
- JDK 17 or newer. Your JDK 25 installation can compile the configured Java 17 target.
- Git
- IntelliJ IDEA or another Java IDE

Maven does not need to be installed because the repository includes Maven Wrapper.

## Run on Windows

Open Command Prompt in the project folder:

```bat
git pull origin main
mvnw.cmd clean test
mvnw.cmd spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080).

Local demo accounts:

| Role | Email | Password |
|---|---|---|
| Admin | `admin@healthcare.com` | `Admin@123` |
| Doctor | `doctor@healthcare.com` | `Doctor@123` |
| Patient | `patient@healthcare.com` | `Patient@123` |

The local H2 console is at [http://localhost:8080/h2-console](http://localhost:8080/h2-console) and is restricted to an authenticated admin. Use JDBC URL `jdbc:h2:file:./data/healthcaredb`, user `sa`, and a blank password.

## Optional integrations

Copy `.env.example` values into IntelliJ's Run Configuration environment variables (or set them in Windows). Never commit real keys.

### Gemini AI (free-tier option)

Set `GEMINI_API_KEY`. The app uses Google's native Gemini API with `GEMINI_MODEL` (default `gemini-2.5-flash-lite`). If the key is missing, the quota is exceeded, or the provider is unavailable, the safety-focused local fallback is used automatically.

The Gemini free tier is intended only for fictional demonstration data in this educational project. Do not submit real patient or confidential health information.

### OpenAI (optional alternative)

Set `OPENAI_API_KEY` to keep OpenAI available as a secondary provider. Gemini is tried first when both keys are present. See [LLM_PROMPTS.md](LLM_PROMPTS.md) for prompts and safety rules.

### SendGrid

Set `SENDGRID_API_KEY` and a verified `SENDGRID_FROM_EMAIL`. Email is sent by a retryable outbox worker, so a temporary provider failure never rolls back a booking.

### Google Calendar

1. In Google Cloud, enable Google Calendar API.
2. Create an OAuth 2.0 Web application client.
3. Add `http://localhost:8080/calendar/callback` as an authorized redirect URI.
4. Set `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, and `GOOGLE_REDIRECT_URI`.
5. Sign in and click **Connect Calendar** on a dashboard.

Only the `calendar.events` scope is requested. Medical symptoms are never placed in event descriptions.

## Database and API

Hibernate creates/updates the local schema. The production profile uses PostgreSQL. Entity details are in [docs/DATABASE.md](docs/DATABASE.md), JSON routes are in [docs/API.md](docs/API.md), and the architecture is in [docs/SYSTEM_DESIGN.md](docs/SYSTEM_DESIGN.md).

## Tests and build

```bat
mvnw.cmd clean verify
```

CI runs the same command with Java 17 on every push and pull request. The test profile uses an isolated in-memory H2 database and no external API keys.

## Docker and deployment

```bash
docker build -t healthcare-appointment-manager .
docker run -p 8080:8080 healthcare-appointment-manager
```

`render.yaml` provisions the web service and PostgreSQL database. For a public deployment, set API credentials in the host's secret environment settings. Set `DEMO_DATA_ENABLED=false` and provision real users before production use; the included demo credentials are only for evaluation.

## Important medical/privacy note

This is an educational appointment-management project, not a diagnostic device or production electronic health record. AI output is assistive, must be reviewed by a clinician, and must not replace emergency services or professional judgment. A real deployment requires a privacy/security review, encrypted secret management, backups, audit policy, and compliance work appropriate to its jurisdiction.
