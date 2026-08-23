# Requirements Checklist

This file maps the assignment specification to the implemented feature and its main code or screen.

| Requirement | Implementation / verification |
|---|---|
| Patient, doctor, admin portals | Spring Security route groups and role dashboards |
| Patient registration/login | `/register`, `/login`, BCrypt passwords |
| Search doctors by specialization | `/patient/doctors`; active-doctor repository query |
| Configure doctor details and slots | `/admin/doctors`; working times and duration on `DoctorProfile` |
| Real-time slot availability | `/patient/doctors/{id}/book`; excludes past, leave, active holds, booked visits, and completed reservations |
| Double/concurrent-booking protection | Five-minute `HELD` record plus unique `reservation_key` database constraint |
| Reschedule/cancel | Patient appointment forms; reservation key released or replaced atomically |
| Doctor leave handling | `/admin/leaves`; affected bookings cancelled, calendars removed, patient notification queued |
| Pre-visit AI summary and urgency | `AiSummaryService`; Gemini, optional OpenAI, deterministic safe fallback |
| Doctor notes and prescriptions | Doctor visit workflow with ownership checks and input limits |
| Patient-friendly post-visit summary | Generated on visit completion with source notes stored separately |
| Medication reminders | `MedicationReminderService`; frequency-based scheduled jobs |
| Booking/reminder/cancellation email | Durable `NotificationJob` outbox with SendGrid and bounded retries |
| Google Calendar OAuth | Per-user OAuth token, session state check, minimal `calendar.events` scope |
| Calendar create/update/delete | Separate patient/doctor event IDs synchronized on booking/reschedule/cancel |
| Failure handling | AI fallback; email outbox audit/retry; calendar best effort; booking remains authoritative |
| Database | H2 locally, PostgreSQL in Render, documented entities and invariants |
| API and documentation | `docs/API.md`, `docs/DATABASE.md`, `docs/SYSTEM_DESIGN.md`, `LLM_PROMPTS.md` |
| Deployment | Render Blueprint (`render.yaml`) and Docker image; health endpoint |
| Automated verification | GitHub Actions Java 17 `clean verify` plus booking, leave, AI, calendar, and notification tests |

## Submission items

- Complete source: repository or GitHub **Code → Download ZIP**
- Configuration template: `.env.example`
- API documentation: `docs/API.md`
- Database design: `docs/DATABASE.md`
- LLM prompts: `LLM_PROMPTS.md`
- Calendar setup: README Google Calendar section
- Hosted URL and demo credentials: README and `docs/DEMO_GUIDE.md`
- System design (under 800 words): `docs/SYSTEM_DESIGN.md`
