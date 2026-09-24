import { apiFetch } from './client';

export function submitEscalation({ aboutEmployeeId, type, reason }) {
  return apiFetch('/api/escalations', {
    method: 'POST',
    body: JSON.stringify({ aboutEmployeeId, type, reason }),
  });
}

export function getAllEscalations() {
  return apiFetch('/api/escalations');
}

export function linkEscalationToHearing(escalationId, hearingId) {
  return apiFetch(`/api/escalations/${escalationId}/link-hearing/${hearingId}`, {
    method: 'PATCH',
  });
}

export function updateEscalationStatus(escalationId, status) {
  return apiFetch(`/api/escalations/${escalationId}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  });
}
