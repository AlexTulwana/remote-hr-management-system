import { apiFetch, apiFetchFormData, apiFetchBlob } from './client';

export async function getDocuments(employeeId) {
  return apiFetch(`/api/employees/${employeeId}/documents`);
}

export async function uploadDocument(employeeId, { file, documentType, description }) {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('documentType', documentType);
  if (description) formData.append('description', description);

  return apiFetchFormData(`/api/employees/${employeeId}/documents`, formData);
}

export async function downloadDocument(id, fileName) {
  const blob = await apiFetchBlob(`/api/employees/documents/${id}/download`);
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = fileName;
  document.body.appendChild(a);
  a.click();
  a.remove();
  window.URL.revokeObjectURL(url);
}

export async function deleteDocument(id) {
  return apiFetch(`/api/employees/documents/${id}`, { method: 'DELETE' });
}
