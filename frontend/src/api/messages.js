import { apiFetch } from './client';

export function getInbox(userId) {
  return apiFetch(`/api/messages/inbox/${userId}`);
}

export function getUnread(userId) {
  return apiFetch(`/api/messages/unread/${userId}`);
}

export function sendMessage(recipientId, content) {
  return apiFetch('/api/messages', {
    method: 'POST',
    body: JSON.stringify({ recipientId, content }),
  });
}

export function markAsRead(messageId) {
  return apiFetch(`/api/messages/${messageId}/read`, {
    method: 'PATCH',
  });
}
