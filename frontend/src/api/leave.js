import { apiFetch, apiFetchFormData, apiFetchBlob } from './client';

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

export function getAllLeave() {
  return apiFetch('/api/leave');
}

export function getLeaveBalance(employeeId) {
  return apiFetch(`/api/leave/balance/${employeeId}`);
}

export function getAllLeaveBalances() {
  return apiFetch('/api/leave/balances');
}

export function getLeaveAttachment(leaveId) {
  return apiFetchBlob(`/api/leave/${leaveId}/attachment`);
}

export async function openLeaveAttachment(leaveId) {
  const blob = await getLeaveAttachment(leaveId);
  const url = window.URL.createObjectURL(blob);
  window.open(url, '_blank');
}
