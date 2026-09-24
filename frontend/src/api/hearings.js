import { apiFetch } from './client';

export function scheduleHearing({ employeeId, caseType, description, hearingDateTime, meetingLink }) {
  return apiFetch('/api/hearings', {
    method: 'POST',
    body: JSON.stringify({ employeeId, caseType, description, hearingDateTime, meetingLink }),
  });
}

export function updateHearingOutcome(hearingId, outcome, notes) {
  return apiFetch(`/api/hearings/${hearingId}/outcome`, {
    method: 'PATCH',
    body: JSON.stringify({ outcome, notes }),
  });
}

export function cancelHearing(hearingId) {
  return apiFetch(`/api/hearings/${hearingId}/cancel`, {
    method: 'PATCH',
  });
}

export function getAllHearings() {
  return apiFetch('/api/hearings');
}

export function getHearingById(hearingId) {
  return apiFetch(`/api/hearings/${hearingId}`);
}

export function addHearingParticipant(hearingId, userId, role) {
  return apiFetch(`/api/hearings/${hearingId}/participants`, {
    method: 'POST',
    body: JSON.stringify({ userId, role }),
  });
}

export function getHearingParticipants(hearingId) {
  return apiFetch(`/api/hearings/${hearingId}/participants`);
}
