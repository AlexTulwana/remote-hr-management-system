import { apiFetch } from './client';

export function getAllOnboardings() {
  return apiFetch('/api/onboarding');
}

export function startOnboarding(employeeId, data) {
  return apiFetch(`/api/onboarding/${employeeId}`, {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export function completeOnboarding(id) {
  return apiFetch(`/api/onboarding/${id}/complete`, { method: 'PATCH' });
}

export function getOnboardingCandidates() {
  return apiFetch('/api/onboarding/candidates');
}

export function getManagerOptions() {
  return apiFetch('/api/onboarding/managers');
}

export function startOnboardingFromApplication(applicationId, data) {
  return apiFetch(`/api/onboarding/from-application/${applicationId}`, {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export function resendOnboardingInvite(id) {
  return apiFetch(`/api/onboarding/${id}/resend-invite`, { method: 'POST' });
}
