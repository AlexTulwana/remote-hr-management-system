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

export function getBranchRequests(branchId) {
  return apiFetch(`/api/employee-requests/branch/${branchId}`);
}

export function managerDecideRequest(id, decision, comment) {
  return apiFetch(`/api/employee-requests/${id}/manager-decision`, {
    method: 'PATCH',
    body: JSON.stringify({ decision, comment }),
  });
}
