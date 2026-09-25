import { apiFetch } from './client';

export function getBranches() {
  return apiFetch('/api/branches');
}

export function createBranch(data) {
  return apiFetch('/api/branches', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export function updateBranch(id, data) {
  return apiFetch(`/api/branches/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

export function deleteBranch(id) {
  return apiFetch(`/api/branches/${id}`, {
    method: 'DELETE',
  });
}
