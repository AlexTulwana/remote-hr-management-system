import { apiFetch } from './client';

export function getCalendar(from, to, branchId) {
  const params = new URLSearchParams({ from, to });
  if (branchId) params.set('branchId', branchId);
  return apiFetch(`/api/calendar?${params.toString()}`);
}
