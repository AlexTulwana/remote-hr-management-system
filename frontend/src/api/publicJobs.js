import { apiFetch, apiFetchFormData } from './client';

export function getOpenPostings() {
  return apiFetch('/api/job-postings');
}

export function getPostingById(id) {
  return apiFetch(`/api/job-postings/${id}`);
}

export function submitApplication(jobPostingId, formData) {
  return apiFetchFormData(`/api/applications/${jobPostingId}`, formData);
}
