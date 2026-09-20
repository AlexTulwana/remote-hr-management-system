import { apiFetch } from './client';

export function getDirectory() {
  return apiFetch('/api/employee-directory');
}

export function getOrgChartFrom(employeeId) {
  return apiFetch(`/api/employee-directory/org-chart/${employeeId}`);
}
