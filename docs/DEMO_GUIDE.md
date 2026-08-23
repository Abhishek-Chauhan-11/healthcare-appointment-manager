# Demonstration Guide

Live site: [healthcare-appointment-manager-1per.onrender.com](https://healthcare-appointment-manager-1per.onrender.com)

The Render free service can take about a minute to wake after inactivity. Use fictional demonstration data only.

## Accounts

| Role | Email | Password |
|---|---|---|
| Admin | `admin@healthcare.com` | `Admin@123` |
| Doctor | `doctor@healthcare.com` | `Doctor@123` |
| Patient | `patient@healthcare.com` | `Patient@123` |

## Recommended walkthrough

1. **Admin:** open Manage Doctors to show specialization, hours, duration, and fee. Open Leave Management and All Appointments.
2. **Patient:** search for `General Medicine`, choose a future date and open slot, enter fictional symptoms, and book. Show the AI pre-visit brief and urgency.
3. **Conflict prevention:** revisit the same doctor/date; the booked slot is no longer available. The database reservation key is the final concurrent-booking guard.
4. **Doctor:** open the new appointment, review the patient text and pre-visit brief, then complete the visit with fictional notes, prescription/follow-up, and an optional medication schedule.
5. **Patient:** open My Appointments to show the patient-friendly post-visit summary, clinician-entered prescription, and follow-up instructions.
6. **Leave handling:** as admin, add leave on a future booked date. Show that the appointment becomes cancelled and the slot reservation is released.
7. **Calendar:** connect a test Google account from each relevant dashboard. A booking creates independent patient/doctor events; reschedule and cancel synchronize both.
8. **Failure safety:** The missing/unavailable AI and email providers do not block bookings. AI has a local safe fallback; email jobs remain in the durable notification outbox for audit/retry.

## Suggested fictional text

Symptoms: `Mild headache and occasional tiredness for two days. No chest pain or difficulty breathing.`

Clinical notes: `Patient reports a mild headache and occasional tiredness for two days. No fever, dizziness, visual changes, weakness, or numbness. Patient is alert and stable during this demonstration visit.`

Prescription: `No medication prescribed. Maintain adequate hydration and a regular sleep schedule as discussed.`

Follow-up: `Follow up after three days if symptoms continue. Seek immediate medical attention for a sudden severe headache, confusion, weakness, numbness, difficulty speaking, or breathing problems.`
