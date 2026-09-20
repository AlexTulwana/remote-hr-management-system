import { apiFetch } from './client';

export function getBranchCases(branchId) {
  return apiFetch(`/api/disciplinary-cases/branch/${branchId}`);
}

export function getCaseHistory(caseId) {
  return apiFetch(`/api/disciplinary-cases/${caseId}/history`);
}

export function openCase({ employeeId, reason, initialStage }) {
  return apiFetch('/api/disciplinary-cases', {
    method: 'POST',
    body: JSON.stringify({ employeeId, reason, initialStage }),
  });
}

export function progressCase(caseId, stage, comment) {
  return apiFetch(`/api/disciplinary-cases/${caseId}/progress`, {
    method: 'PATCH',
    body: JSON.stringify({ stage, comment }),
  });
}
