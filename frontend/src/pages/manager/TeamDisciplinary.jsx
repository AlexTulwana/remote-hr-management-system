import { useEffect, useState } from 'react';
import { useAuth } from '../../auth/AuthContext';
import { getBranchCases, getCaseHistory, openCase, progressCase } from '../../api/disciplinaryCases';
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

const MANAGER_STAGES = ['VERBAL_WARNING', 'WRITTEN_WARNING'];

function formatDateTime(value) {
  if (!value) return '';
  return new Date(value).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' });
}

function MoveDialog({ target, submitting, onCancel, onConfirm }) {
  const [comment, setComment] = useState('');

  if (!target) return null;

  return (
    <div className="fixed inset-0 bg-black/30 flex items-center justify-center z-50">
      <div className="bg-surface-2 border border-border rounded-xl p-5 w-[380px]">
        <p className="text-[15px] font-medium mb-1.5">
          Move to {STAGE_LABEL[target.stage].toLowerCase()}?
        </p>
        <p className="text-[13px] text-text-secondary mb-1">
          {target.disciplinaryCase.employee?.fullName}
        </p>
        <p className="text-[12px] text-text-muted mb-3">Add a note for the record.</p>
        <textarea
          value={comment}
          onChange={(e) => setComment(e.target.value)}
          rows={3}
          placeholder="Note"
          className="w-full rounded-lg border border-border-strong bg-surface-1 text-text-primary p-2.5 text-[13px] mb-4 focus:outline-none focus:border-text-secondary"
        />
        <div className="flex justify-end gap-2">
          <Button variant="secondary" onClick={onCancel}>
            Cancel
          </Button>
          <Button
            variant="primary"
            disabled={submitting || !comment.trim()}
            onClick={() => onConfirm(comment.trim())}
          >
            {submitting ? 'Saving…' : 'Move case'}
          </Button>
        </div>
      </div>
    </div>
  );
}

function CaseRow({ item, expanded, history, historyLoading, historyFailed, onToggleHistory, onMove }) {
  const canMove = !item.closed && MANAGER_STAGES.includes(item.currentStage);
  const nextStage = item.currentStage === 'VERBAL_WARNING' ? 'WRITTEN_WARNING' : 'VERBAL_WARNING';

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
          {canMove ? (
            <Button variant="secondary" onClick={() => onMove(nextStage)}>
              {nextStage === 'WRITTEN_WARNING' ? 'Move to written warning' : 'Move back to verbal warning'}
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

export default function TeamDisciplinary() {
  const { user } = useAuth();
  const branchId = user?.branchId;

  const [cases, setCases] = useState([]);
  const [team, setTeam] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionError, setActionError] = useState('');
  const [reloadKey, setReloadKey] = useState(0);

  const [showForm, setShowForm] = useState(false);
  const [formEmployeeId, setFormEmployeeId] = useState('');
  const [formStage, setFormStage] = useState('VERBAL_WARNING');
  const [formReason, setFormReason] = useState('');
  const [formError, setFormError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const [moveTarget, setMoveTarget] = useState(null);
  const [expandedId, setExpandedId] = useState(null);
  const [historyState, setHistoryState] = useState({ id: null, data: [], failed: false });

  useEffect(() => {
    if (!branchId) return undefined;
    let cancelled = false;
    getBranchCases(branchId)
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
  }, [branchId, reloadKey]);

  useEffect(() => {
    let cancelled = false;
    getDirectory()
      .then((data) => {
        if (!cancelled) setTeam(data);
      })
      .catch(() => {
        if (!cancelled) setTeam([]);
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

  async function handleIssue(e) {
    e.preventDefault();
    if (!formEmployeeId) {
      setFormError('Choose an employee.');
      return;
    }
    if (!formReason.trim()) {
      setFormError('Give the reason for the warning.');
      return;
    }
    setSubmitting(true);
    setFormError('');
    try {
      await openCase({
        employeeId: Number(formEmployeeId),
        reason: formReason.trim(),
        initialStage: formStage,
      });
      setShowForm(false);
      setFormEmployeeId('');
      setFormReason('');
      setFormStage('VERBAL_WARNING');
      setReloadKey((k) => k + 1);
    } catch (err) {
      setFormError(err.message || 'Could not issue this warning.');
    } finally {
      setSubmitting(false);
    }
  }

  async function handleMove(comment) {
    setSubmitting(true);
    setActionError('');
    try {
      await progressCase(moveTarget.disciplinaryCase.id, moveTarget.stage, comment);
      setMoveTarget(null);
      setReloadKey((k) => k + 1);
    } catch (err) {
      setActionError(err.message || 'Could not update this case.');
      setMoveTarget(null);
    } finally {
      setSubmitting(false);
    }
  }

  if (!branchId) {
    return (
      <div>
        <p className="text-[20px] font-medium mb-5">Disciplinary Cases</p>
        <Card>
          <EmptyState message="No branch is linked to your account, so there are no cases to show." />
        </Card>
      </div>
    );
  }

  const candidates = team.filter(
    (m) => m.id !== user?.employeeId && m.employmentStatus === 'ACTIVE',
  );
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
        onMove={(stage) => setMoveTarget({ disciplinaryCase: c, stage })}
      />
    ));
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-5">
        <p className="text-[20px] font-medium">Disciplinary Cases</p>
        {!showForm ? <Button onClick={() => setShowForm(true)}>Issue warning</Button> : null}
      </div>

      {actionError ? (
        <div className="mb-3">
          <ErrorInline message={actionError} />
        </div>
      ) : null}

      {showForm ? (
        <Card className="mb-3.5">
          <p className="text-[15px] font-medium mb-3">Issue a warning</p>
          <form onSubmit={handleIssue} className="flex flex-col gap-3">
            <Select label="Employee" value={formEmployeeId} onChange={(e) => setFormEmployeeId(e.target.value)}>
              <option value="">Choose an employee</option>
              {candidates.map((m) => (
                <option key={m.id} value={m.id}>
                  {m.fullName}
                </option>
              ))}
            </Select>
            <Select label="Warning" value={formStage} onChange={(e) => setFormStage(e.target.value)}>
              <option value="VERBAL_WARNING">Verbal warning</option>
              <option value="WRITTEN_WARNING">Written warning</option>
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
            {formError ? <ErrorInline message={formError} /> : null}
            <div className="flex gap-2">
              <Button type="submit" disabled={submitting}>
                {submitting ? 'Issuing…' : 'Issue warning'}
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

      <MoveDialog
        key={moveTarget ? `${moveTarget.disciplinaryCase.id}-${moveTarget.stage}` : 'closed'}
        target={moveTarget}
        submitting={submitting}
        onConfirm={handleMove}
        onCancel={() => setMoveTarget(null)}
      />
    </div>
  );
}
