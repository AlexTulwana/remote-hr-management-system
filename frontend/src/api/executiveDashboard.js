import { apiFetch } from './client';

export function getExecutiveKpis() {
  return apiFetch('/api/dashboard/executive/kpis');
}

export function getBranchComparison() {
  return apiFetch('/api/dashboard/executive/branch-comparison');
}

export function getTurnoverTrend(days = 90) {
  return apiFetch(`/api/dashboard/executive/turnover-trend?days=${days}`);
}
