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
