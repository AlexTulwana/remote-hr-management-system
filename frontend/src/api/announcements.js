import { apiFetch } from './client';

export function getActiveAnnouncements() {
  return apiFetch('/api/announcements');
}
