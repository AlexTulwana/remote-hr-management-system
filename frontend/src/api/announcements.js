import { apiFetch, apiFetchFormData } from './client';

export function getActiveAnnouncements() {
  return apiFetch('/api/announcements');
}

export function createAnnouncement({ title, content, category, expiryDate, branchIds, poster }) {
  const formData = new FormData();
  formData.append('title', title);
  formData.append('content', content);
  formData.append('category', category);
  if (expiryDate) {
    formData.append('expiryDate', expiryDate);
  }
  if (branchIds && branchIds.length > 0) {
    branchIds.forEach((id) => formData.append('branchIds', id));
  }
  if (poster) {
    formData.append('poster', poster);
  }
  return apiFetchFormData('/api/announcements', formData);
}

export function deleteAnnouncement(id) {
  return apiFetch(`/api/announcements/${id}`, { method: 'DELETE' });
}
