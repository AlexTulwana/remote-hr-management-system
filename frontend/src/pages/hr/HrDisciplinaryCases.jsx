import { useEffect, useState } from 'react';
import {
  getAllCases,
  getCaseHistory,
  openCase,
  progressCase,
} from '../../api/disciplinaryCases';
import { getDirectory } from '../../api/employeeDirectory';
import Card from '../../components/Card';
import Pill from '../../components/Pill';
import Button from '../../components/Button';
import Select from '../../components/Select';
import SkeletonRow from '../../components/SkeletonRow';
import EmptyState from '../../components/EmptyState';
import ErrorInline from '../../components/ErrorInline';

const STAGE_LABEL = {
  VERBAL_WARNING: 'Verbal warning',
  WRITTEN_WARNING: 'Written warning',
  FINAL_WRITTEN_WARNING: 'Final written warning',
  HEARING: 'Hearing',
  DISMISSAL: 'Dismissal',
  RESOLVED: 'Resolved',
  CLOSED: 'Closed',
};

const STAGE_VARIANT = {
  VERBAL_WARNING: 'warning',
  WRITTEN_WARNING: 'urgent',
  FINAL_WRITTEN_WARNING: 'urgent',
  HEARING: 'info',
  DISMISSAL: 'urgent',
  RESOLVED: 'success',
  CLOSED: 'neutral',
};

const ALL_STAGES = Object.keys(STAGE_LABEL);

function formatDateTime(value) {
  if (!value) return '';
  return new Date(value).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' });
}

function ProgressDialog({ target, submitting, onCancel, onConfirm }) {
  const [stage, setStage] = useState(target?.currentStage || 'VERBAL_WARNING');
  const [comment, setComment] = useState('');
  const [linkedHearingId, setLinkedHearingId] = useState('');

  if (!target) return null;

  return (
    <div className="fixed inset-0 bg-black/30 flex items-center justify-center z-50">
      <div className="bg-surface-2 border border-border rounded-xl p-5 w-[380px]">
        <p className="text-[15px] font-medium mb-1.5">Progress case</p>
        <p className="text-[13px] text-text-secondary mb-3">{target.employee?.fullName}</p>
        <div className="flex flex-col gap-3">
          <Select label="New stage" value={stage} onChange={(e) => setStage(e.target.value)}>
            {ALL_STAGES.map((s) => (
              <option key={s} value={s}>
                {STAGE_LABEL[s]}
              </option>
            ))}
          </Select>
          <div>
            <label className="text-[12px] text-text-secondary block mb-1">Comment</label>
            <textarea
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              rows={3}
              placeholder="Note for the record"
              className="w-full rounded-lg border border-border-strong bg-surface-1 text-text-primary p-2.5 text-[13px] focus:outline-none focus:border-text-secondary"
            />
          </div>
          <div>
            <label className="text-[12px] text-text-secondary block mb-1">Linked hearing ID (optional)</label>
            <input
              type="number"
              value={linkedHearingId}
              onChange={(e) => setLinkedHearingId(e.target.value)}
              placeholder="e.g. 12"
              className="w-full h-9 px-3 rounded-lg border border-border-strong bg-surface-1 text-text-primary text-[13px] focus:outline-none focus:border-text-secondary"
            />
          </div>
        </div>
        <div className="flex justify-end gap-2 mt-4">
          <Button variant="secondary" onClick={onCancel}>
            Cancel
          </Button>
          <Button
            variant="primary"
            disabled={submitting || !comment.trim()}
            onClick={() =>
              onConfirm(stage, comment.trim(), linkedHearingId ? Number(linkedHearingId) : undefined)
            }
          >
            {submitting ? 'Saving…' : 'Progress case'}
          </Button>
        </div>
      </div>
    </div>
  );
}

function CaseRow({ item, expanded, history, historyLoading, historyFailed, onToggleHistory, onProgress }) {
  return (
    <div className="py-3 first:pt-0 last:pb-0">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <div className="flex items-center gap-2 mb-1">
            <p className="text-[13px] font-medium">{item.employee?.fullName}</p>
            <Pill variant={STAGE_VARIANT[item.currentStage] || 'neutral'}>
              {STAGE_LABEL[item.currentStage] || item.currentStage}
            </Pill>
          </div>
          <p className="text-[12px] text-text-secondary mb-0.5">{item.reason}</p>
          <p className="text-[11px] text-text-muted">
            Opened {formatDateTime(item.openedAt)}
            {item.openedByUsername ? ` by ${item.openedByUsername}` : ''}
          </p>
        </div>
        <div className="flex items-center gap-2 shrink-0">
          <Button variant="secondary" onClick={onToggleHistory}>
            {expanded ? 'Hide history' : 'History'}
          </Button>
          {!item.closed ? (
            <Button variant="secondary" onClick={onProgress}>
              Progress
            </Button>
          ) : null}
        </div>
      </div>

      {expanded ? (
        <div className="mt-2 pt-2 border-t border-border">
          {historyLoading ? (
            <SkeletonRow />
          ) : historyFailed ? (
            <ErrorInline message="Could not load the history." />
          ) : (
            history.map((h) => (
              <div key={h.id} className="py-1">
                <p className="text-[12px]">
                  <span className="font-medium">{STAGE_LABEL[h.stage] || h.stage}</span>
                  {' · '}
                  {formatDateTime(h.actionedAt)}
                  {h.actionedByUsername ? ` · ${h.actionedByUsername}` : ''}
                </p>
                {h.comment ? <p className="text-[12px] text-text-secondary">{h.comment}</p> : null}
              </div>
            ))
          )}
        </div>
      ) : null}
    </div>
  );
}

export default function HrDisciplinaryCases() {
  const [cases, setCases] = useState([]);
  const [employees, setEmployees] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionError, setActionError] = useState('');
  const [reloadKey, setReloadKey] = useState(0);

  const [showForm, setShowForm] = useState(false);
  const [formEmployeeId, setFormEmployeeId] = useState('');
  const [formStage, setFormStage] = useState('VERBAL_WARNING');
  const [formReason, setFormReason] = useState('');
  const [formLinkedEscalationId, setFormLinkedEscalationId] = useState('');
  const [formError, setFormError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const [progressTarget, setProgressTarget] = useState(null);
  const [expandedId, setExpandedId] = useState(null);
  const [historyState, setHistoryState] = useState({ id: null, data: [], failed: false });

  useEffect(() => {
    let cancelled = false;
    getAllCases()
      .then((data) => {
        if (cancelled) return;
        setCases(data);
        setError('');
      })
      .catch(() => {
        if (!cancelled) setError('Could not load disciplinary cases.');
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

  useEffect(() => {
    if (!expandedId) return undefined;
    let cancelled = false;
    getCaseHistory(expandedId)
      .then((data) => {
        if (!cancelled) setHistoryState({ id: expandedId, data, failed: false });
      })
      .catch(() => {
        if (!cancelled) setHistoryState({ id: expandedId, data: [], failed: true });
      });
    return () => {
      cancelled = true;
    };
  }, [expandedId, reloadKey]);

  async function handleOpen(e) {
    e.preventDefault();
    if (!formEmployeeId) {
      setFormError('Choose an employee.');
      return;
    }
    if (!formReason.trim()) {
      setFormError('Give the reason for the case.');
      return;
    }
    setSubmitting(true);
    setFormError('');
    try {
      await openCase({
        employeeId: Number(formEmployeeId),
        reason: formReason.trim(),
        initialStage: formStage,
        linkedEscalationId: formLinkedEscalationId ? Number(formLinkedEscalationId) : undefined,
      });
      setShowForm(false);
      setFormEmployeeId('');
      setFormReason('');
      setFormStage('VERBAL_WARNING');
      setFormLinkedEscalationId('');
      setReloadKey((k) => k + 1);
    } catch (err) {
      setFormError(err.message || 'Could not open this case.');
    } finally {
      setSubmitting(false);
    }
  }

  async function handleProgress(stage, comment, linkedHearingId) {
    setSubmitting(true);
    setActionError('');
    try {
      await progressCase(progressTarget.id, stage, comment, linkedHearingId);
      setProgressTarget(null);
      setReloadKey((k) => k + 1);
    } catch (err) {
      setActionError(err.message || 'Could not update this case.');
      setProgressTarget(null);
    } finally {
      setSubmitting(false);
    }
  }

  const candidates = employees.filter((m) => m.employmentStatus === 'ACTIVE');
  const openCases = cases
    .filter((c) => !c.closed)
    .sort((a, b) => new Date(b.openedAt) - new Date(a.openedAt));
  const closedCases = cases
    .filter((c) => c.closed)
    .sort((a, b) => new Date(b.openedAt) - new Date(a.openedAt));

  const historyLoading = expandedId != null && historyState.id !== expandedId;
  const history = historyState.id === expandedId ? historyState.data : [];
  const historyFailed = historyState.id === expandedId && historyState.failed;

  function renderRows(list) {
    return list.map((c) => (
      <CaseRow
        key={c.id}
        item={c}
        expanded={expandedId === c.id}
        history={history}
        historyLoading={historyLoading}
        historyFailed={historyFailed}
        onToggleHistory={() => setExpandedId(expandedId === c.id ? null : c.id)}
        onProgress={() => setProgressTarget(c)}
      />
    ));
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-5">
        <p className="text-[20px] font-medium">Disciplinary Cases</p>
        {!showForm ? <Button onClick={() => setShowForm(true)}>Open case</Button> : null}
      </div>

      {actionError ? (
        <div className="mb-3">
          <ErrorInline message={actionError} />
        </div>
      ) : null}

      {showForm ? (
        <Card className="mb-3.5">
          <p className="text-[15px] font-medium mb-3">Open a disciplinary case</p>
          <form onSubmit={handleOpen} className="flex flex-col gap-3">
            <Select label="Employee" value={formEmployeeId} onChange={(e) => setFormEmployeeId(e.target.value)}>
              <option value="">Choose an employee</option>
              {candidates.map((m) => (
                <option key={m.id} value={m.id}>
                  {m.fullName}
                </option>
              ))}
            </Select>
            <Select label="Initial stage" value={formStage} onChange={(e) => setFormStage(e.target.value)}>
              {ALL_STAGES.map((s) => (
                <option key={s} value={s}>
                  {STAGE_LABEL[s]}
                </option>
              ))}
            </Select>
            <div>
              <label className="text-[12px] text-text-secondary block mb-1">Reason</label>
              <textarea
                value={formReason}
                onChange={(e) => setFormReason(e.target.value)}
                rows={3}
                placeholder="What happened?"
                className="w-full rounded-lg border border-border-strong bg-surface-1 text-text-primary p-2.5 text-[13px] focus:outline-none focus:border-text-secondary"
              />
            </div>
            <div>
              <label className="text-[12px] text-text-secondary block mb-1">
                Linked escalation ID (optional)
              </label>
              <input
                type="number"
                value={formLinkedEscalationId}
                onChange={(e) => setFormLinkedEscalationId(e.target.value)}
                placeholder="e.g. 7"
                className="w-full h-9 px-3 rounded-lg border border-border-strong bg-surface-1 text-text-primary text-[13px] focus:outline-none focus:border-text-secondary"
              />
            </div>
            {formError ? <ErrorInline message={formError} /> : null}
            <div className="flex gap-2">
              <Button type="submit" disabled={submitting}>
                {submitting ? 'Opening…' : 'Open case'}
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
          <p className="text-[13px] font-medium text-text-secondary mb-2">Open</p>
          <Card className="mb-5">
            {openCases.length === 0 ? (
              <EmptyState message="No open disciplinary cases." />
            ) : (
              <div className="flex flex-col divide-y divide-border">{renderRows(openCases)}</div>
            )}
          </Card>

          <p className="text-[13px] font-medium text-text-secondary mb-2">Closed</p>
          <Card>
            {closedCases.length === 0 ? (
              <EmptyState message="No closed cases yet." />
            ) : (
              <div className="flex flex-col divide-y divide-border">{renderRows(closedCases)}</div>
            )}
          </Card>
        </>
      )}

      <ProgressDialog
        key={progressTarget ? progressTarget.id : 'closed'}
        target={progressTarget}
        submitting={submitting}
        onConfirm={handleProgress}
        onCancel={() => setProgressTarget(null)}
      />
    </div>
  );
}
