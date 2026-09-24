import { useEffect, useState } from 'react';
import {
  scheduleHearing,
  updateHearingOutcome,
  cancelHearing,
  getAllHearings,
  addHearingParticipant,
  getHearingParticipants,
} from '../../api/hearings';
import { getDirectory } from '../../api/employeeDirectory';
import { lookupUsers } from '../../api/users';
import Card from '../../components/Card';
import Pill from '../../components/Pill';
import Button from '../../components/Button';
import Select from '../../components/Select';
import SkeletonRow from '../../components/SkeletonRow';
import EmptyState from '../../components/EmptyState';
import ErrorInline from '../../components/ErrorInline';

const STATUS_VARIANT = {
  SCHEDULED: 'info',
  COMPLETED: 'success',
  CANCELLED: 'neutral',
};

const PARTICIPANT_ROLES = ['EMPLOYEE', 'MANAGER', 'SUPERVISOR', 'WITNESS', 'HR'];

function formatDateTime(value) {
  if (!value) return '';
  return new Date(value).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' });
}

function toDatetimeLocalValue(date) {
  const pad = (n) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function OutcomeDialog({ target, submitting, onCancel, onConfirm }) {
  const [outcome, setOutcome] = useState('');
  const [notes, setNotes] = useState('');

  if (!target) return null;

  return (
    <div className="fixed inset-0 bg-black/30 flex items-center justify-center z-50">
      <div className="bg-surface-2 border border-border rounded-xl p-5 w-[380px]">
        <p className="text-[15px] font-medium mb-1.5">Record outcome</p>
        <p className="text-[13px] text-text-secondary mb-3">{target.employee?.fullName}</p>
        <div className="flex flex-col gap-3">
          <div>
            <label className="text-[12px] text-text-secondary block mb-1">Outcome</label>
            <input
              value={outcome}
              onChange={(e) => setOutcome(e.target.value)}
              placeholder="e.g. Final written warning issued"
              className="w-full h-9 px-3 rounded-lg border border-border-strong bg-surface-1 text-text-primary text-[13px] focus:outline-none focus:border-text-secondary"
            />
          </div>
          <div>
            <label className="text-[12px] text-text-secondary block mb-1">Notes</label>
            <textarea
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              rows={3}
              placeholder="Notes from the hearing"
              className="w-full rounded-lg border border-border-strong bg-surface-1 text-text-primary p-2.5 text-[13px] focus:outline-none focus:border-text-secondary"
            />
          </div>
        </div>
        <div className="flex justify-end gap-2 mt-4">
          <Button variant="secondary" onClick={onCancel}>
            Cancel
          </Button>
          <Button variant="primary" disabled={submitting || !outcome.trim()} onClick={() => onConfirm(outcome.trim(), notes.trim())}>
            {submitting ? 'Saving…' : 'Save outcome'}
          </Button>
        </div>
      </div>
    </div>
  );
}

function ParticipantsPanel({ hearingId }) {
  const [participants, setParticipants] = useState([]);
  const [loading, setLoading] = useState(true);
  const [failed, setFailed] = useState(false);
  const [query, setQuery] = useState('');
  const [options, setOptions] = useState([]);
  const [selectedUserId, setSelectedUserId] = useState('');
  const [role, setRole] = useState('WITNESS');
  const [adding, setAdding] = useState(false);
  const [addError, setAddError] = useState('');

  function reload() {
    setLoading(true);
    getHearingParticipants(hearingId)
      .then((data) => {
        setParticipants(data);
        setFailed(false);
      })
      .catch(() => setFailed(true))
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    reload();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hearingId]);

  useEffect(() => {
    if (!query.trim()) {
      setOptions([]);
      return undefined;
    }
    let cancelled = false;
    lookupUsers(query.trim())
      .then((data) => {
        if (!cancelled) setOptions(data);
      })
      .catch(() => {
        if (!cancelled) setOptions([]);
      });
    return () => {
      cancelled = true;
    };
  }, [query]);

  async function handleAdd() {
    if (!selectedUserId) {
      setAddError('Search and choose a person first.');
      return;
    }
    setAdding(true);
    setAddError('');
    try {
      await addHearingParticipant(hearingId, Number(selectedUserId), role);
      setQuery('');
      setOptions([]);
      setSelectedUserId('');
      reload();
    } catch (err) {
      setAddError(err.message || 'Could not add this participant.');
    } finally {
      setAdding(false);
    }
  }

  return (
    <div className="mt-2 pt-2 border-t border-border">
      {loading ? (
        <SkeletonRow />
      ) : failed ? (
        <ErrorInline message="Could not load participants." />
      ) : participants.length === 0 ? (
        <p className="text-[12px] text-text-muted mb-2">No participants added yet.</p>
      ) : (
        <div className="flex flex-col gap-1 mb-2">
          {participants.map((p) => (
            <p key={p.id} className="text-[12px]">
              {p.personName} <span className="text-text-muted">· {p.role}</span>
              {p.attended ? <span className="text-success-text"> · attended</span> : null}
            </p>
          ))}
        </div>
      )}

      <div className="flex flex-col gap-2 mt-2">
        <div className="flex gap-2">
          <input
            value={query}
            onChange={(e) => {
              setQuery(e.target.value);
              setSelectedUserId('');
            }}
            placeholder="Search by name"
            className="flex-1 h-9 px-3 rounded-lg border border-border-strong bg-surface-1 text-text-primary text-[13px] focus:outline-none focus:border-text-secondary"
          />
          <Select value={role} onChange={(e) => setRole(e.target.value)}>
            {PARTICIPANT_ROLES.map((r) => (
              <option key={r} value={r}>
                {r}
              </option>
            ))}
          </Select>
        </div>
        {options.length > 0 ? (
          <div className="flex flex-col gap-1 border border-border rounded-lg p-1.5 max-h-36 overflow-y-auto">
            {options.map((o) => (
              <button
                key={o.userId}
                type="button"
                onClick={() => {
                  setSelectedUserId(String(o.userId));
                  setQuery(o.fullName);
                  setOptions([]);
                }}
                className={[
                  'text-left text-[12px] px-2 py-1 rounded-md hover:bg-surface-1',
                  String(o.userId) === selectedUserId ? 'bg-surface-1' : '',
                ].join(' ')}
              >
                {o.fullName} <span className="text-text-muted">· {o.position}</span>
              </button>
            ))}
          </div>
        ) : null}
        {addError ? <ErrorInline message={addError} /> : null}
        <Button variant="secondary" disabled={adding} onClick={handleAdd}>
          {adding ? 'Adding…' : 'Add participant'}
        </Button>
      </div>
    </div>
  );
}

function HearingRow({ item, expanded, onToggleParticipants, onOutcome, onCancel }) {
  return (
    <div className="py-3 first:pt-0 last:pb-0">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <div className="flex items-center gap-2 mb-1">
            <p className="text-[13px] font-medium">{item.employee?.fullName}</p>
            <Pill variant={STATUS_VARIANT[item.status] || 'neutral'}>{item.status}</Pill>
          </div>
          <p className="text-[12px] text-text-secondary mb-0.5">
            {item.caseType} · {formatDateTime(item.hearingDateTime)}
          </p>
          {item.description ? <p className="text-[12px] text-text-muted mb-0.5">{item.description}</p> : null}
          {item.meetingLink ? (
            <a
              href={item.meetingLink}
              target="_blank"
              rel="noreferrer"
              className="text-[12px] text-info-text underline"
            >
              Meeting link
            </a>
          ) : null}
          {item.outcome ? (
            <p className="text-[12px] text-text-secondary mt-1">
              <span className="font-medium">Outcome:</span> {item.outcome}
            </p>
          ) : null}
          {item.notes ? <p className="text-[12px] text-text-muted">{item.notes}</p> : null}
        </div>
        <div className="flex items-center gap-2 shrink-0">
          <Button variant="secondary" onClick={onToggleParticipants}>
            {expanded ? 'Hide participants' : 'Participants'}
          </Button>
          {item.status === 'SCHEDULED' ? (
            <>
              <Button variant="secondary" onClick={onOutcome}>
                Record outcome
              </Button>
              <Button variant="danger" onClick={onCancel}>
                Cancel
              </Button>
            </>
          ) : null}
        </div>
      </div>
      {expanded ? <ParticipantsPanel hearingId={item.id} /> : null}
    </div>
  );
}

export default function HrHearings() {
  const [hearings, setHearings] = useState([]);
  const [employees, setEmployees] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionError, setActionError] = useState('');
  const [reloadKey, setReloadKey] = useState(0);

  const [showForm, setShowForm] = useState(false);
  const [formEmployeeId, setFormEmployeeId] = useState('');
  const [formCaseType, setFormCaseType] = useState('');
  const [formDescription, setFormDescription] = useState('');
  const [formDateTime, setFormDateTime] = useState('');
  const [formMeetingLink, setFormMeetingLink] = useState('');
  const [formError, setFormError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const [outcomeTarget, setOutcomeTarget] = useState(null);
  const [expandedId, setExpandedId] = useState(null);

  useEffect(() => {
    let cancelled = false;
    getAllHearings()
      .then((data) => {
        if (cancelled) return;
        setHearings(data);
        setError('');
      })
      .catch(() => {
        if (!cancelled) setError('Could not load hearings.');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [reloadKey]);

  useEffect(() => {
    let cancelled = false;
    getDirectory()
      .then((data) => {
        if (!cancelled) setEmployees(data);
      })
      .catch(() => {
        if (!cancelled) setEmployees([]);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  async function handleSchedule(e) {
    e.preventDefault();
    if (!formEmployeeId) {
      setFormError('Choose an employee.');
      return;
    }
    if (!formCaseType.trim()) {
      setFormError('Give a case type.');
      return;
    }
    if (!formDateTime) {
      setFormError('Choose a date and time.');
      return;
    }
    setSubmitting(true);
    setFormError('');
    try {
      await scheduleHearing({
        employeeId: Number(formEmployeeId),
        caseType: formCaseType.trim(),
        description: formDescription.trim(),
        hearingDateTime: formDateTime,
        meetingLink: formMeetingLink.trim() || undefined,
      });
      setShowForm(false);
      setFormEmployeeId('');
      setFormCaseType('');
      setFormDescription('');
      setFormDateTime('');
      setFormMeetingLink('');
      setReloadKey((k) => k + 1);
    } catch (err) {
      setFormError(err.message || 'Could not schedule this hearing.');
    } finally {
      setSubmitting(false);
    }
  }

  async function handleOutcome(outcome, notes) {
    setSubmitting(true);
    setActionError('');
    try {
      await updateHearingOutcome(outcomeTarget.id, outcome, notes);
      setOutcomeTarget(null);
      setReloadKey((k) => k + 1);
    } catch (err) {
      setActionError(err.message || 'Could not save the outcome.');
      setOutcomeTarget(null);
    } finally {
      setSubmitting(false);
    }
  }

  async function handleCancel(hearing) {
    setActionError('');
    try {
      await cancelHearing(hearing.id);
      setReloadKey((k) => k + 1);
    } catch (err) {
      setActionError(err.message || 'Could not cancel this hearing.');
    }
  }

  const candidates = employees.filter((m) => m.employmentStatus === 'ACTIVE');
  const scheduled = hearings
    .filter((h) => h.status === 'SCHEDULED')
    .sort((a, b) => new Date(a.hearingDateTime) - new Date(b.hearingDateTime));
  const past = hearings
    .filter((h) => h.status !== 'SCHEDULED')
    .sort((a, b) => new Date(b.hearingDateTime) - new Date(a.hearingDateTime));

  function renderRows(list) {
    return list.map((h) => (
      <HearingRow
        key={h.id}
        item={h}
        expanded={expandedId === h.id}
        onToggleParticipants={() => setExpandedId(expandedId === h.id ? null : h.id)}
        onOutcome={() => setOutcomeTarget(h)}
        onCancel={() => handleCancel(h)}
      />
    ));
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-5">
        <p className="text-[20px] font-medium">Hearings</p>
        {!showForm ? <Button onClick={() => setShowForm(true)}>Schedule hearing</Button> : null}
      </div>

      {actionError ? (
        <div className="mb-3">
          <ErrorInline message={actionError} />
        </div>
      ) : null}

      {showForm ? (
        <Card className="mb-3.5">
          <p className="text-[15px] font-medium mb-3">Schedule a hearing</p>
          <form onSubmit={handleSchedule} className="flex flex-col gap-3">
            <Select label="Employee" value={formEmployeeId} onChange={(e) => setFormEmployeeId(e.target.value)}>
              <option value="">Choose an employee</option>
              {candidates.map((m) => (
                <option key={m.id} value={m.id}>
                  {m.fullName}
                </option>
              ))}
            </Select>
            <div>
              <label className="text-[12px] text-text-secondary block mb-1">Case type</label>
              <input
                value={formCaseType}
                onChange={(e) => setFormCaseType(e.target.value)}
                placeholder="e.g. Misconduct"
                className="w-full h-9 px-3 rounded-lg border border-border-strong bg-surface-1 text-text-primary text-[13px] focus:outline-none focus:border-text-secondary"
              />
            </div>
            <div>
              <label className="text-[12px] text-text-secondary block mb-1">Description</label>
              <textarea
                value={formDescription}
                onChange={(e) => setFormDescription(e.target.value)}
                rows={3}
                placeholder="Details for the hearing"
                className="w-full rounded-lg border border-border-strong bg-surface-1 text-text-primary p-2.5 text-[13px] focus:outline-none focus:border-text-secondary"
              />
            </div>
            <div>
              <label className="text-[12px] text-text-secondary block mb-1">Date and time</label>
              <input
                type="datetime-local"
                value={formDateTime}
                min={toDatetimeLocalValue(new Date())}
                onChange={(e) => setFormDateTime(e.target.value)}
                className="w-full h-9 px-3 rounded-lg border border-border-strong bg-surface-1 text-text-primary text-[13px] focus:outline-none focus:border-text-secondary"
              />
            </div>
            <div>
              <label className="text-[12px] text-text-secondary block mb-1">Meeting link (optional)</label>
              <input
                value={formMeetingLink}
                onChange={(e) => setFormMeetingLink(e.target.value)}
                placeholder="https://..."
                className="w-full h-9 px-3 rounded-lg border border-border-strong bg-surface-1 text-text-primary text-[13px] focus:outline-none focus:border-text-secondary"
              />
            </div>
            {formError ? <ErrorInline message={formError} /> : null}
            <div className="flex gap-2">
              <Button type="submit" disabled={submitting}>
                {submitting ? 'Scheduling…' : 'Schedule hearing'}
              </Button>
              <Button
                type="button"
                variant="secondary"
                onClick={() => {
                  setShowForm(false);
                  setFormError('');
                }}
              >
                Cancel
              </Button>
            </div>
          </form>
        </Card>
      ) : null}

      {loading ? (
        <Card>
          <div className="flex flex-col">
            <SkeletonRow />
            <SkeletonRow />
            <SkeletonRow />
          </div>
        </Card>
      ) : error ? (
        <ErrorInline message={error} />
      ) : (
        <>
          <p className="text-[13px] font-medium text-text-secondary mb-2">Scheduled</p>
          <Card className="mb-5">
            {scheduled.length === 0 ? (
              <EmptyState message="No scheduled hearings." />
            ) : (
              <div className="flex flex-col divide-y divide-border">{renderRows(scheduled)}</div>
            )}
          </Card>

          <p className="text-[13px] font-medium text-text-secondary mb-2">Past</p>
          <Card>
            {past.length === 0 ? (
              <EmptyState message="No past hearings yet." />
            ) : (
              <div className="flex flex-col divide-y divide-border">{renderRows(past)}</div>
            )}
          </Card>
        </>
      )}

      <OutcomeDialog
        key={outcomeTarget ? outcomeTarget.id : 'closed'}
        target={outcomeTarget}
        submitting={submitting}
        onConfirm={handleOutcome}
        onCancel={() => setOutcomeTarget(null)}
      />
    </div>
  );
}
