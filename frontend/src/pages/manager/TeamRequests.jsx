import { useEffect, useState } from 'react';
import { useAuth } from '../../auth/AuthContext';
import { getBranchRequests, managerDecideRequest } from '../../api/employeeRequests';
import Card from '../../components/Card';
import Pill from '../../components/Pill';
import Button from '../../components/Button';
import SkeletonRow from '../../components/SkeletonRow';
import EmptyState from '../../components/EmptyState';
import ErrorInline from '../../components/ErrorInline';

const STATUS_VARIANT = {
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'urgent',
  ESCALATED: 'info',
};

const DECISIONS = {
  APPROVED: {
    title: 'Approve request?',
    confirm: 'Approve',
    busy: 'Approving…',
    required: false,
    hint: 'Optional comment — the employee will see this.',
  },
  REJECTED: {
    title: 'Reject request?',
    confirm: 'Reject',
    busy: 'Rejecting…',
    required: true,
    hint: 'Give a reason — the employee will see this.',
  },
  ESCALATED: {
    title: 'Escalate to HR?',
    confirm: 'Escalate',
    busy: 'Escalating…',
    required: true,
    hint: 'Explain why HR should decide — only managers and HR will see this.',
  },
};

function formatDateTime(value) {
  if (!value) return '';
  return new Date(value).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' });
}

function formatTypeLabel(type) {
  return type
    .toLowerCase()
    .split('_')
    .map((w) => w[0].toUpperCase() + w.slice(1))
    .join(' ');
}

function DecisionDialog({ target, submitting, onCancel, onConfirm }) {
  const [comment, setComment] = useState('');

  if (!target) return null;

  const config = DECISIONS[target.decision];
  const disabled = submitting || (config.required && !comment.trim());

  return (
    <div className="fixed inset-0 bg-black/30 flex items-center justify-center z-50">
      <div className="bg-surface-2 border border-border rounded-xl p-5 w-[380px]">
        <p className="text-[15px] font-medium mb-1.5">{config.title}</p>
        <p className="text-[13px] text-text-secondary mb-1">
          {target.request.employee?.fullName} — {formatTypeLabel(target.request.requestType)}
        </p>
        <p className="text-[12px] text-text-muted mb-3">{config.hint}</p>
        <textarea
          value={comment}
          onChange={(e) => setComment(e.target.value)}
          rows={3}
          placeholder="Comment"
          className="w-full rounded-lg border border-border-strong bg-surface-1 text-text-primary p-2.5 text-[13px] mb-4 focus:outline-none focus:border-text-secondary"
        />
        <div className="flex justify-end gap-2">
          <Button variant="secondary" onClick={onCancel}>
            Cancel
          </Button>
          <Button variant="primary" disabled={disabled} onClick={() => onConfirm(comment.trim())}>
            {submitting ? config.busy : config.confirm}
          </Button>
        </div>
      </div>
    </div>
  );
}

export default function TeamRequests() {
  const { user } = useAuth();
  const branchId = user?.branchId;

  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionError, setActionError] = useState('');
  const [reloadKey, setReloadKey] = useState(0);
  const [target, setTarget] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!branchId) return undefined;
    let cancelled = false;
    getBranchRequests(branchId)
      .then((data) => {
        if (cancelled) return;
        setRequests(data);
        setError('');
      })
      .catch(() => {
        if (!cancelled) setError('Could not load requests.');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [branchId, reloadKey]);

  async function handleConfirm(comment) {
    setSubmitting(true);
    setActionError('');
    try {
      await managerDecideRequest(target.request.id, target.decision, comment || null);
      setTarget(null);
      setReloadKey((k) => k + 1);
    } catch (err) {
      setActionError(err.message || 'Could not save this decision.');
      setTarget(null);
    } finally {
      setSubmitting(false);
    }
  }

  if (!branchId) {
    return (
      <div>
        <p className="text-[20px] font-medium mb-5">Team Requests</p>
        <Card>
          <EmptyState message="No branch is linked to your account, so there are no requests to show." />
        </Card>
      </div>
    );
  }

  const pending = requests
    .filter((r) => r.status === 'PENDING')
    .sort((a, b) => new Date(a.submittedAt) - new Date(b.submittedAt));
  const history = requests
    .filter((r) => r.status !== 'PENDING')
    .sort((a, b) => new Date(b.submittedAt) - new Date(a.submittedAt));

  return (
    <div>
      <p className="text-[20px] font-medium mb-5">Team Requests</p>

      {actionError ? (
        <div className="mb-3">
          <ErrorInline message={actionError} />
        </div>
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
          <p className="text-[13px] font-medium text-text-secondary mb-2">Pending</p>
          <Card className="mb-5">
            {pending.length === 0 ? (
              <EmptyState message="No pending requests." />
            ) : (
              <div className="flex flex-col divide-y divide-border">
                {pending.map((r) => (
                  <div key={r.id} className="flex items-center justify-between py-3 first:pt-0 last:pb-0">
                    <div>
                      <div className="flex items-center gap-2 mb-1">
                        <p className="text-[13px] font-medium">{r.employee?.fullName}</p>
                        <Pill variant="neutral">{formatTypeLabel(r.requestType)}</Pill>
                      </div>
                      <p className="text-[12px] text-text-secondary mb-0.5">{r.description}</p>
                      <p className="text-[11px] text-text-muted">
                        Submitted {formatDateTime(r.submittedAt)}
                      </p>
                    </div>
                    <div className="flex items-center gap-2">
                      <Button variant="secondary" onClick={() => setTarget({ request: r, decision: 'REJECTED' })}>
                        Reject
                      </Button>
                      <Button variant="secondary" onClick={() => setTarget({ request: r, decision: 'ESCALATED' })}>
                        Escalate to HR
                      </Button>
                      <Button variant="primary" onClick={() => setTarget({ request: r, decision: 'APPROVED' })}>
                        Approve
                      </Button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </Card>

          <p className="text-[13px] font-medium text-text-secondary mb-2">History</p>
          <Card>
            {history.length === 0 ? (
              <EmptyState message="No decided requests yet." />
            ) : (
              <div className="flex flex-col divide-y divide-border">
                {history.map((r) => (
                  <div key={r.id} className="flex items-start justify-between py-3 first:pt-0 last:pb-0">
                    <div>
                      <div className="flex items-center gap-2 mb-1">
                        <p className="text-[13px] font-medium">{r.employee?.fullName}</p>
                        <Pill variant="neutral">{formatTypeLabel(r.requestType)}</Pill>
                      </div>
                      <p className="text-[12px] text-text-secondary mb-0.5">{r.description}</p>
                      <p className="text-[11px] text-text-muted">
                        Submitted {formatDateTime(r.submittedAt)}
                        {r.resolvedAt ? ` · Resolved ${formatDateTime(r.resolvedAt)}` : ''}
                        {r.handledByName ? ` · Handled by ${r.handledByName}` : ''}
                      </p>
                      {r.managerComment ? (
                        <p className="text-[12px] text-text-secondary mt-1">Manager: {r.managerComment}</p>
                      ) : null}
                      {r.hrComment ? (
                        <p className="text-[12px] text-text-secondary mt-1">HR: {r.hrComment}</p>
                      ) : null}
                      {r.escalationComment ? (
                        <p className="text-[12px] text-text-secondary mt-1">
                          Escalation note (not shown to the employee): {r.escalationComment}
                        </p>
                      ) : null}
                    </div>
                    <Pill variant={STATUS_VARIANT[r.status] || 'neutral'}>{r.status}</Pill>
                  </div>
                ))}
              </div>
            )}
          </Card>
        </>
      )}

      <DecisionDialog
        key={target ? `${target.request.id}-${target.decision}` : 'closed'}
        target={target}
        submitting={submitting}
        onConfirm={handleConfirm}
        onCancel={() => setTarget(null)}
      />
    </div>
  );
}
