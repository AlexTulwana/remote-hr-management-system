import { apiFetch } from './client';

export function clockIn(employeeId) {
  return apiFetch(`/api/attendance/clock-in/${employeeId}`, { method: 'POST' });
}

export function clockOut(attendanceId) {
  return apiFetch(`/api/attendance/clock-out/${attendanceId}`, { method: 'PATCH' });
}

export function getMyAttendance(employeeId) {
  return apiFetch(`/api/attendance/employee/${employeeId}`);
}
