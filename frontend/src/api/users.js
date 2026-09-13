import { apiFetch } from './client';

export function getMe() {
  return apiFetch('/api/users/me');
}
