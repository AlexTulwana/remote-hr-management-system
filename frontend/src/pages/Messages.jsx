import { useEffect, useState, useRef } from 'react';
import { useAuth } from '../auth/AuthContext';
import { getInbox, sendMessage } from '../api/messages';
import { lookupUsers } from '../api/users';
import Card from '../components/Card';
import Button from '../components/Button';
import Avatar from '../components/Avatar';
import Pill from '../components/Pill';
import SkeletonRow from '../components/SkeletonRow';
import EmptyState from '../components/EmptyState';
import ErrorInline from '../components/ErrorInline';

function initialsOf(name) {
  if (!name) return '?';
  return name.split(' ').map((p) => p[0]).join('').slice(0, 2).toUpperCase();
}

function formatTime(sentAt) {
  if (!sentAt) return '';
  const date = new Date(sentAt);
  return date.toLocaleString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
}

function readLabel(message) {
  if (!message.read) return 'Not read yet';
  return message.readAt ? `Read ${formatTime(message.readAt)}` : 'Read';
}

function StatusPill({ message, isOutgoing }) {
  if (isOutgoing) {
    if (!message.read) return <Pill variant="neutral">Not read yet</Pill>;
    return (
      <Pill variant="success">
        {message.readAt ? `Read ${formatTime(message.readAt)}` : 'Read'}
      </Pill>
    );
  }
  return message.read ? (
    <Pill variant="success">Read</Pill>
  ) : (
    <Pill variant="urgent">Unread</Pill>
  );
}

function Composer({ onSent }) {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState([]);
  const [recipient, setRecipient] = useState(null);
  const [content, setContent] = useState('');
  const [searching, setSearching] = useState(false);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState('');
  const debounceRef = useRef(null);

  function handleQueryChange(value) {
    setQuery(value);
    setRecipient(null);
    clearTimeout(debounceRef.current);
    if (value.trim().length < 2) {
      setResults([]);
      return;
    }
    debounceRef.current = setTimeout(async () => {
      setSearching(true);
      try {
        const matches = await lookupUsers(value.trim());
        setResults(matches);
      } catch {
        setResults([]);
      } finally {
        setSearching(false);
      }
    }, 300);
  }

  function pickRecipient(person) {
    setRecipient(person);
    setQuery(person.fullName);
    setResults([]);
  }

  async function handleSend() {
    if (!recipient || !content.trim()) return;
    setSending(true);
    setError('');
    try {
      const sent = await sendMessage(recipient.userId, content.trim());
      setContent('');
      setRecipient(null);
      setQuery('');
      onSent(sent);
    } catch {
      setError('Could not send this message. Try again.');
    } finally {
      setSending(false);
    }
  }

  return (
    <Card className="mb-5">
      <p className="text-[13px] font-medium text-text-secondary mb-3">New message</p>

      <div className="relative mb-2.5">
        <input
          value={query}
          onChange={(e) => handleQueryChange(e.target.value)}
          placeholder="Search by name"
          className="w-full h-9 rounded-lg border border-border bg-surface-2 px-3 text-[13px] focus:outline-none focus:border-border-strong"
        />
        {results.length > 0 ? (
          <div className="absolute z-10 top-10 left-0 right-0 bg-surface-2 border border-border rounded-lg overflow-hidden">
            {results.map((person) => (
              <button
                key={person.userId}
                type="button"
                onClick={() => pickRecipient(person)}
                className="w-full text-left px-3 py-2 hover:bg-surface-1 transition-colors duration-200 flex items-center gap-2.5"
              >
                <Avatar initials={initialsOf(person.fullName)} size="sm" />
                <div>
                  <p className="text-[13px]">{person.fullName}</p>
                  <p className="text-[11px] text-text-muted">
                    {person.position}
                    {person.branchName ? ` · ${person.branchName}` : ''}
                  </p>
                </div>
              </button>
            ))}
          </div>
        ) : null}
        {searching ? <p className="text-[11px] text-text-muted mt-1">Searching…</p> : null}
      </div>

      <textarea
        value={content}
        onChange={(e) => setContent(e.target.value)}
        placeholder="Write a message"
        rows={3}
        className="w-full rounded-lg border border-border bg-surface-2 px-3 py-2 text-[13px] resize-none focus:outline-none focus:border-border-strong mb-2.5"
      />

      {error ? <div className="mb-2.5"><ErrorInline message={error} /></div> : null}

      <div className="flex justify-end">
        <Button
          variant="primary"
          onClick={handleSend}
          disabled={!recipient || !content.trim() || sending}
        >
          {sending ? 'Sending…' : 'Send message'}
        </Button>
      </div>
    </Card>
  );
}

export default function Messages() {
  const { user } = useAuth();
  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [expandedId, setExpandedId] = useState(null);

  function toggleMessage(message) {
    setExpandedId((current) => (current === message.id ? null : message.id));
  }

  async function loadInbox() {
    setLoading(true);
    setError('');
    try {
      const data = await getInbox(user.userId);
      setMessages(data);
    } catch {
      setError('Could not load your messages.');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadInbox();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function handleSent(sent) {
    setMessages((prev) => [sent, ...prev]);
  }

  return (
    <div>
      <p className="text-[20px] font-medium mb-5">Messages</p>

      <Composer onSent={handleSent} />

      <Card className="p-0 overflow-hidden">
        {loading ? (
          <>
            <SkeletonRow />
            <SkeletonRow />
            <SkeletonRow />
          </>
        ) : error ? (
          <div className="p-3.5">
            <ErrorInline message={error} />
          </div>
        ) : messages.length === 0 ? (
          <EmptyState message="No messages yet. Messages you send or receive will appear here." />
        ) : (
          messages.map((m) => {
            const isOutgoing = m.senderId === user.userId;
            const otherName = isOutgoing ? m.recipientName : m.senderName;
            return (
              <div
                key={m.id}
                role="button"
                tabIndex={0}
                onClick={() => toggleMessage(m)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    toggleMessage(m);
                  }
                }}
                className={[
                  'flex items-start gap-2.5 px-3.5 py-2.5 border-b border-border last:border-b-0 hover:bg-surface-1 transition-colors duration-200 cursor-pointer',
                  expandedId === m.id ? 'bg-surface-1' : '',
                ].join(' ')}
              >
                <Avatar initials={initialsOf(otherName)} size="sm" />
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between gap-2">
                    <p className="text-[13px]">
                      {isOutgoing ? 'To ' : ''}
                      {otherName}
                    </p>
                    <p className="text-[11px] text-text-muted shrink-0">{formatTime(m.sentAt)}</p>
                  </div>
                  <p
                    className={[
                      'text-[13px] text-text-secondary',
                      expandedId === m.id ? 'whitespace-pre-wrap break-words' : 'truncate',
                    ].join(' ')}
                  >
                    {m.content}
                  </p>
                  {expandedId === m.id ? (
                    <div className="mt-2 pt-2 border-t border-border text-[11px] text-text-muted flex flex-col gap-0.5">
                      <p>
                        From {m.senderName} to {m.recipientName}
                      </p>
                      <p>Sent {formatTime(m.sentAt)}</p>
                      <p>{readLabel(m)}</p>
                    </div>
                  ) : null}
                </div>
                <div className="shrink-0">
                  <StatusPill message={m} isOutgoing={isOutgoing} />
                </div>
              </div>
            );
          })
        )}
      </Card>
    </div>
  );
}
