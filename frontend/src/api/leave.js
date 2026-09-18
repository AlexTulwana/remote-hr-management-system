import { apiFetch, apiFetchFormData } from './client';

export function submitLeave(employeeId, { leaveType, startDate, endDate, reason, attachment }) {
  const formData = new FormData();
  formData.append('leaveType', leaveType);
  formData.append('startDate', startDate);
  formData.append('endDate', endDate);
  formData.append('reason', reason);
  if (attachment) formData.append('attachment', attachment);

  return apiFetchFormData(`/api/leave/${employeeId}`, formData);
}

export function getMyLeave(employeeId) {
  return apiFetch(`/api/leave/employee/${employeeId}`);
}

export function approveLeave(leaveId) {
  return apiFetch(`/api/leave/${leaveId}/approve`, { method: 'PATCH' });
}

export function rejectLeave(leaveId, reason) {
  return apiFetch(`/api/leave/${leaveId}/reject`, {
    method: 'PATCH',
    body: JSON.stringify({ reason }),
  });
}
