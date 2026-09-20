import { apiFetch } from './client';

export function getMe() {
  return apiFetch('/api/users/me');
}

export function lookupUsers(query, branchId) {
  const params = new URLSearchParams();
  if (query) params.set('query', query);
  if (branchId) params.set('branchId', branchId);
  return apiFetch(`/api/users/lookup?${params.toString()}`);
}

export function updateMyContact({ contactDetails, email }) {
  return apiFetch('/api/users/me/contact', {
    method: 'PUT',
    body: JSON.stringify({ contactDetails, email }),
  });
}
