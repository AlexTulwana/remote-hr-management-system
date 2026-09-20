const BASE_URL = 'http://localhost:8080';

let onUnauthorized = null;

export function setUnauthorizedHandler(handler) {
  onUnauthorized = handler;
}

function getToken() {
  return localStorage.getItem('token');
}

function handleAuthFailure() {
  if (onUnauthorized) {
    onUnauthorized();
  } else {
    localStorage.removeItem('token');
    window.location.href = '/login';
  }
}

export async function apiFetch(path, options = {}) {
  const token = getToken();

  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...options.headers,
  };

  const response = await fetch(`${BASE_URL}${path}`, { ...options, headers });

  if (response.status === 401 || response.status === 403) {
    handleAuthFailure();
    throw new Error('Unauthorized');
  }

  if (!response.ok) {
    let message = `Request failed (${response.status})`;
    try {
      const body = await response.json();
      message = body.message || body.error || message;
    } catch {
      // response wasn't JSON, keep default message
    }
    throw new Error(message);
  }

  if (response.status === 204) return null;
  return response.json();
}

// For multipart/form-data uploads — browser must set its own Content-Type
// (with boundary), so we can't go through apiFetch's JSON header.
export async function apiFetchFormData(path, formData, options = {}) {
  const token = getToken();

  const headers = {
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...options.headers,
  };

  const response = await fetch(`${BASE_URL}${path}`, {
    method: 'POST',
    ...options,
    headers,
    body: formData,
  });

  if (response.status === 401 || response.status === 403) {
    handleAuthFailure();
    throw new Error('Unauthorized');
  }

  if (!response.ok) {
    let message = `Request failed (${response.status})`;
    try {
      const body = await response.json();
      message = body.message || body.error || message;
    } catch {
      // response wasn't JSON, keep default message
    }
    throw new Error(message);
  }

  return response.json();
}

// For file downloads — returns a Blob instead of parsing JSON.
export async function apiFetchBlob(path) {
  const token = getToken();

  const response = await fetch(`${BASE_URL}${path}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });

  if (response.status === 401 || response.status === 403) {
    handleAuthFailure();
    throw new Error('Unauthorized');
  }

  if (!response.ok) {
    throw new Error(`Request failed (${response.status})`);
  }

  return response.blob();
}
