import { apiFetch } from './client';

export function getEmployeeDashboardSummary() {
  return apiFetch('/api/dashboard/employee/summary');
}
