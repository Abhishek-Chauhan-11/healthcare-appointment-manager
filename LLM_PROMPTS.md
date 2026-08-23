# LLM Prompts and Safety

The application uses Google's Gemini API when `GEMINI_API_KEY` is configured. OpenAI's Responses API remains an optional secondary provider when `OPENAI_API_KEY` is configured. Prompts live in `AiSummaryService` so they are version-controlled and auditable.

## Pre-visit prompt

Input: patient-authored symptoms.

Required output headings:

```text
Urgency: Low, Medium, or High
Chief complaint:
Important details:
Suggested questions:
- question 1
- question 2
- question 3
Safety note:
```

The instruction explicitly prohibits diagnosis. If every configured provider is unavailable, the fallback preserves the original text, assigns negation-aware keyword urgency, provides generic history questions, and shows an emergency safety warning.

## Post-visit prompt

Input: clinician-authored clinical notes, prescription, and follow-up instructions.

Required output headings:

```text
Visit summary:
Medication schedule:
Follow-up steps:
Safety note:
```

The model must not invent diagnoses, medicine doses, facts, or advice. Clinician-entered source fields remain stored separately from the generated patient-friendly summary.

## Guardrails

- AI output is never used to confirm, reject, or automatically prioritize a booking.
- High-urgency output is a visible clinical cue, not a diagnosis.
- Provider failures do not block appointment creation or visit completion.
- Raw clinical data is not logged by the AI service.
- Production deployments should establish retention, consent, access-control, and provider data-processing policies before sending health information to an external model.
