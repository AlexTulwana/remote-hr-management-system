import { apiFetch } from './client';

export function getPendingRequests() {
  return apiFetch('/api/dashboard/manager/pending-requests');
}

export function getHeadcount() {
  return apiFetch('/api/dashboard/manager/headcount');
}

export function getDisciplinaryCases() {
  return apiFetch('/api/dashboard/manager/disciplinary-cases');
}

export function getPendingLeave() {
  return apiFetch('/api/dashboard/manager/leave');
}

export function getUpcomingSchedule() {
  return apiFetch('/api/dashboard/manager/schedule');
}
