import { apiFetch } from './client';

export function getMe() {
  return apiFetch('/api/users/me');
}

export function lookupUsers(query) {
  return apiFetch(`/api/users/lookup?query=${encodeURIComponent(query)}`);
}
