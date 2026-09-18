import { apiFetch } from './client';

export function submitEmployeeRequest({ requestType, description }) {
  return apiFetch('/api/employee-requests', {
    method: 'POST',
    body: JSON.stringify({ requestType, description }),
  });
}

export function getMyEmployeeRequests(employeeId) {
  return apiFetch(`/api/employee-requests/employee/${employeeId}`);
}
