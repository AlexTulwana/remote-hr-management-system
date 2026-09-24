import { apiFetch, apiFetchBlob } from './client';

// Job Postings
export function getAllPostings() {
  return apiFetch('/api/job-postings/all');
}

export function createPosting(data) {
  return apiFetch('/api/job-postings', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export function updatePosting(id, data) {
  return apiFetch(`/api/job-postings/${id}`, {
    method: 'PATCH',
    body: JSON.stringify(data),
  });
}

export function updateEmailTemplates(id, data) {
  return apiFetch(`/api/job-postings/${id}/email-templates`, {
    method: 'PATCH',
    body: JSON.stringify(data),
  });
}

export function deletePosting(id) {
  return apiFetch(`/api/job-postings/${id}`, { method: 'DELETE' });
}

// Applications
export function getApplicationsByPosting(jobPostingId) {
  return apiFetch(`/api/applications/job-posting/${jobPostingId}`);
}

export function getApplicationDocuments(id) {
  return apiFetch(`/api/applications/${id}/documents`);
}

export function viewCv(id) {
  return apiFetchBlob(`/api/applications/${id}/cv`).then(openInNewTab);
}

export function viewDocument(applicationId, documentId) {
  return apiFetchBlob(`/api/applications/${applicationId}/documents/${documentId}/download`).then(openInNewTab);
}

function openInNewTab(blob) {
  const url = window.URL.createObjectURL(blob);
  window.open(url, '_blank');
}

export function updateApplicationStatus(id, status) {
  return apiFetch(`/api/applications/${id}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  });
}

export function setApplicationOutcome(id, data) {
  return apiFetch(`/api/applications/${id}/outcome`, {
    method: 'PATCH',
    body: JSON.stringify(data),
  });
}

// Interviews
export function getInterviewsByApplication(applicationId) {
  return apiFetch(`/api/interviews/application/${applicationId}`);
}

export function scheduleInterview(data) {
  return apiFetch('/api/interviews', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export function updateInterviewStatus(id, status, notes) {
  return apiFetch(`/api/interviews/${id}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status, notes }),
  });
}
