import { apiFetch, apiFetchBlob } from './client';

export function getMyPayslips(employeeId) {
  return apiFetch(`/api/payslips/employee/${employeeId}`);
}

export async function downloadPayslip(id, fileName) {
  const blob = await apiFetchBlob(`/api/payslips/${id}/download`);
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = fileName;
  document.body.appendChild(a);
  a.click();
  a.remove();
  window.URL.revokeObjectURL(url);
}

export async function viewPayslip(id) {
  const blob = await apiFetchBlob(`/api/payslips/${id}/download`);
  const url = window.URL.createObjectURL(blob);
  window.open(url, '_blank');
}

export function emailPayslip(id) {
  return apiFetch(`/api/payslips/${id}/email`, { method: 'POST' });
}
