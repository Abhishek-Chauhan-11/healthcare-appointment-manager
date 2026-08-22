# Role API

All endpoints require the normal form-login session and the stated role. Responses are JSON. The web interface is the primary client; these routes provide a concise integration surface.

| Method | Path | Role | Purpose |
|---|---|---|---|
| GET | `/api/patient/doctors?specialization=` | Patient | Search active doctors |
| GET | `/api/patient/doctors/{doctorId}/slots?date=YYYY-MM-DD` | Patient | List available start times |
| GET | `/api/patient/appointments` | Patient | Patient's appointment history and summaries |
| GET | `/api/doctor/appointments` | Doctor | Assigned appointment list |
| GET | `/api/admin/appointments` | Admin | Clinic-wide operational list |

Example:

```json
{
  "id": 14,
  "doctor": "Dr. Demo",
  "patient": "Demo Patient",
  "date": "2026-08-24",
  "start": "09:00:00",
  "end": "09:30:00",
  "status": "BOOKED",
  "urgency": "LOW",
  "preVisitSummary": "...",
  "postVisitSummary": null,
  "prescription": null,
  "followUpInstructions": null
}
```

State-changing operations use server-rendered POST forms with Spring Security CSRF protection. Authorization is checked both by route role and by ownership-aware repository lookups for patient/doctor records.
