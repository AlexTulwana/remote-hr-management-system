import { apiFetch } from './client';

export function getAllOffboardings() {
  return apiFetch('/api/offboarding');
}

export function startOffboarding(employeeId, data) {
  return apiFetch(`/api/offboarding/${employeeId}`, {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export function completeOffboarding(id) {
  return apiFetch(`/api/offboarding/${id}/complete`, { method: 'PATCH' });
}
