# UC-PAT-03 – Notify Patient (sescam-communication v0.1.0)

## Use Case
- **UC:** `UC-PAT-03`
- **RF:** `FR-PAT-06`, `FR-PAT-07`, `FR-PAT-08`, `FR-PAT-09`
- **Phase:** Construction
- **Iteration:** 3
- **Module:** `sescam-communication` (v0.1.0)
- **Priority:** Medium (ISO/IEC/IEEE 29148:2018 / Inherited from FR-PAT-06 to FR-PAT-09)
- **UML Relationships:**
  - Related Use Cases: UC-AAD-05 (booking, rescheduling, cancellation) and UC-PHY-04 (report signature) publish the events that trigger notifications; UC-PAT-01 publishes patient-side changes; UC-ADM-01 configures reminder offsets, templates, and channels; notifications link to UC-PAT-01 and UC-PAT-02.
- **Minutes:** [Meeting #4 Minutes](https://github.com/paticama/ISO26-ISO2-A1.01/wiki/Minutes-of-meetings#meeting-4-extraordinary-meeting--requisites-and-use-cases-restructuring)
- **GitHub Issue:** [#105](https://github.com/paticama/ISO26-ISO2-A1.01/issues/105)
- **Actor:** Timer (system clock) and system events (the patient does not initiate this use case)
- **Secondary Actors:** Patient User (recipient), external messaging gateway (e-mail / SMS / mobile push)
- **Description:** Sends patients automated notifications and reminders about their appointments, pre-procedure preparation instructions, and the availability of their diagnostic reports, through the patient portal and the configured external channels.
- **Preconditions:**
  1. The patient is registered with at least one contact channel, or has access to the patient portal.
  2. Reminder offsets, message templates, and channels are configured (UC-ADM-01).
- **Postconditions:**
  1. Each due notification is delivered once per triggering event, or its failure is recorded.
  2. The notification is available in the patient portal inbox regardless of the external channel outcome.
  3. The delivery status is recorded with timestamp and channel.
- **Main Success Scenario (Client–Server Interaction Flow):**
  1. **[Server]** The timer fires a scheduled job, or the system receives an event (appointment booked, rescheduled or cancelled; report signed).
  2. **[Server]** The system determines which notifications are due: appointment notice, appointment reminder at the configured offset, preparation instructions at the configured offset (only for examination types that require preparation), or report availability.
  3. **[Server]** The system builds the message from the configured template: appointment date, time, and location; preparation instructions from the Catalogue of Examination Types; or a report-availability notice without clinical content.
  4. **[Server]** The system selects the channels according to configuration and patient preferences and sends the message through the external gateway.
  5. **[Server]** The system records the delivery status and an audit entry.
  6. **[Client]** The notification appears in the patient portal inbox (and as a push notification on mobile); the patient can open the linked appointment (UC-PAT-01) or report (UC-PAT-02).
- **Alternative / Exception Flows:**
  - **A1. Delivery failure [Server]:** The system retries according to policy; after the last attempt it marks the notification as `Failed`. The notification remains available in the portal inbox.
  - **A2. Appointment cancelled or rescheduled before a reminder [Server]:** Pending reminders for the old slot are suppressed and replaced if a new slot exists.
  - **A3. Duplicate event [Server]:** The system detects that the event was already processed and sends no second notification.
  - **A4. No external contact channel [Server]:** The notification is delivered only to the portal inbox and the missing channel is flagged for Admissions.
  - **A5. Patient opted out of an optional channel [Server]:** That channel is skipped; the portal inbox is always used.

## RADIT Disciplines
- [x] Requirements: Specify notification triggers, reminder offsets, message content, channels, and delivery tracking.
- [ ] Architecture: Define event publication between modules and the messaging gateway adapter.
- [ ] Design: Define notification events, templates, scheduling, and idempotency contracts.
- [ ] Implementation: Implement the notification scheduler, event consumers, and gateway adapter.
- [ ] Testing: Tests for reminder timing, suppression after cancellation, idempotency, retries, and absence of clinical content in external messages.

## Contracts and Interfaces
- `Client:` Patient portal inbox (web) and push notifications (mobile).
- `Server:` Patient notification service with scheduler.
- `Event interface:` Consumes appointment events (UC-AAD-05, UC-PAT-01) and report events (UC-PHY-04).
- `Catalogue interface:` Reads preparation instructions for the examination type (UC-ADM-01).
- `Messaging gateway interface:` Sends e-mail, SMS, and push messages and returns the delivery status.
- `Audit interface:` Records notification, channel, timestamp, and outcome.
- `Errors:` `NOTIFICATION_FAILED`, `NO_CONTACT_CHANNEL`, `TEMPLATE_NOT_FOUND`, and `DUPLICATE_EVENT` are handled explicitly; no failed delivery is recorded as sent.

## Acceptance Criteria
- [ ] Appointment notices are sent when an appointment is booked, rescheduled, or cancelled.
- [ ] Reminders and preparation instructions are sent at the configured offsets, and only for active appointments.
- [ ] Report-availability notices are sent only after the report is signed, never for drafts.
- [ ] Messages sent through external channels contain no diagnostic findings.
- [ ] Each triggering event produces at most one notification per channel.
- [ ] Delivery failures are retried and visible; the notification is always available in the portal inbox.

## Decisions (DECISIONS.md)
- See [DECISIONS.md](../../feature/CU00-initial-structure/DECISIONS.md).
- Operational assumption: external messages carry only availability notices and appointment logistics; clinical content stays inside the authenticated portal.

## Effort Estimation
- **Estimated effort without AI:** 12 hours
- **Estimated effort with AI (Copilot):** 7 hours
- **Estimated effort with AI + 25% safety margin:** 8.75 hours

---

<div align="center">
  <sub>Software Engineering II • Superior School of Informatics (ESI) • University of Castilla-La Mancha (UCLM)</sub>
</div>
