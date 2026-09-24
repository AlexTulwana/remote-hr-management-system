import { apiFetch } from './client';

export function getBranchCases(branchId) {
  return apiFetch(`/api/disciplinary-cases/branch/${branchId}`);
}

export function getAllCases() {
  return apiFetch('/api/disciplinary-cases');
}

export function getAllOpenCases() {
  return apiFetch('/api/disciplinary-cases/open');
}

export function getCaseHistory(caseId) {
  return apiFetch(`/api/disciplinary-cases/${caseId}/history`);
}

export function openCase({ employeeId, reason, initialStage, linkedEscalationId }) {
  return apiFetch('/api/disciplinary-cases', {
    method: 'POST',
    body: JSON.stringify({ employeeId, reason, initialStage, linkedEscalationId }),
  });
}

export function progressCase(caseId, stage, comment, linkedHearingId) {
  return apiFetch(`/api/disciplinary-cases/${caseId}/progress`, {
    method: 'PATCH',
    body: JSON.stringify({ stage, comment, linkedHearingId }),
  });
}
