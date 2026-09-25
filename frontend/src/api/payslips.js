import { apiFetch, apiFetchBlob, apiFetchFormData } from './client';

export function getMyPayslips(employeeId) {
  return apiFetch(`/api/payslips/employee/${employeeId}`);
}

export function getAllPayslips() {
  return apiFetch('/api/payslips');
}

export function uploadPayslip(employeeId, payPeriod, file) {
  const formData = new FormData();
  formData.append('payPeriod', payPeriod);
  formData.append('file', file);
  return apiFetchFormData(`/api/payslips/${employeeId}`, formData);
}

export function bulkUploadPayslips(payPeriod, files) {
  const formData = new FormData();
  formData.append('payPeriod', payPeriod);
  files.forEach((file) => formData.append('files', file));
  return apiFetchFormData('/api/payslips/bulk', formData);
}

export function deletePayslip(id) {
  return apiFetch(`/api/payslips/${id}`, { method: 'DELETE' });
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
