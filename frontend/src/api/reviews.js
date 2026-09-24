import { apiFetch } from './client';

export function getBranchReviews(branchId) {
  return apiFetch(`/api/reviews/branch/${branchId}`);
}

export function createReview({
  employeeId,
  communicationScore,
  teamworkScore,
  productivityScore,
  attendanceScore,
  comment,
}) {
  return apiFetch('/api/reviews', {
    method: 'POST',
    body: JSON.stringify({
      employeeId,
      communicationScore,
      teamworkScore,
      productivityScore,
      attendanceScore,
      comment,
    }),
  });
}

export function getEmployeeReviews(employeeId) {
  return apiFetch(`/api/reviews/employee/${employeeId}`);
}

export function getAllReviews() {
  return apiFetch('/api/reviews');
}
