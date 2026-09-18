import { useEffect, useState } from 'react';
import { getPendingLeave } from '../../api/managerDashboard';
import { approveLeave, rejectLeave } from '../../api/leave';
import Card from '../../components/Card';
import Pill from '../../components/Pill';
import Button from '../../components/Button';
import ConfirmDialog from '../../components/ConfirmDialog';
import SkeletonRow from '../../components/SkeletonRow';
import EmptyState from '../../components/EmptyState';
import ErrorInline from '../../components/ErrorInline';

function formatDate(value) {
  return new Date(value).toLocaleDateString([], { dateStyle: 'medium' });
}

function RejectDialog({ open, onCancel, onConfirm, submitting }) {
  const [reason, setReason] = useState('');

  if (!open) return null;

  return (
    <div className="fixed inset-0 bg-black/30 flex items-center justify-center z-50">
      <div className="bg-surface-2 border border-border rounded-xl p-5 w-[360px]">
        <p className="text-[15px] font-medium mb-1.5">Reject leave request?</p>
        <p className="text-[13px] text-text-secondary mb-3">
          Give a reason — the employee will see this.
        </p>
        <textarea
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          rows={3}
          placeholder="Reason for rejection"
          className="w-full rounded-lg border border-border-strong bg-surface-1 text-text-primary p-2.5 text-[13px] mb-4 focus:outline-none focus:border-text-secondary"
        />
        <div className="flex justify-end gap-2">
          <Button variant="secondary" onClick={onCancel}>
            Cancel
          </Button>
          <Button variant="primary" disabled={!reason.trim() || submitting} onClick={() => onConfirm(reason)}>
            {submitting ? 'Rejecting…' : 'Reject'}
          </Button>
        </div>
      </div>
    </div>
  );
}

export default function LeaveApprovals() {
  const [leave, setLeave] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionError, setActionError] = useState('');

  const [approveTarget, setApproveTarget] = useState(null);
  const [rejectTarget, setRejectTarget] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  async function load() {
    setLoading(true);
    setError('');
    try {
      const data = await getPendingLeave();
      setLeave(data);
    } catch {
      setError('Could not load pending leave requests.');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  async function handleConfirmApprove() {
    setSubmitting(true);
    setActionError('');
    try {
      await approveLeave(approveTarget.id);
      setApproveTarget(null);
      await load();
    } catch {
      setActionError('Could not approve this request.');
      setApproveTarget(null);
    } finally {
      setSubmitting(false);
    }
  }

  async function handleConfirmReject(reason) {
    setSubmitting(true);
    setActionError('');
    try {
      await rejectLeave(rejectTarget.id, reason);
      setRejectTarget(null);
      await load();
    } catch {
      setActionError('Could not reject this request.');
      setRejectTarget(null);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div>
      <p className="text-[20px] font-medium mb-5">Leave Approvals</p>

      {actionError ? <div className="mb-3"><ErrorInline message={actionError} /></div> : null}

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
      ) : leave.length === 0 ? (
        <Card>
          <EmptyState message="No pending leave requests." />
        </Card>
      ) : (
        <Card>
          <div className="flex flex-col divide-y divide-border">
            {leave.map((l) => (
              <div key={l.id} className="flex items-center justify-between py-3 first:pt-0 last:pb-0">
                <div>
                  <div className="flex items-center gap-2 mb-1">
                    <p className="text-[13px] font-medium">{l.employee.fullName}</p>
                    <Pill variant="neutral">{l.leaveType}</Pill>
                    {l.hasAttachment ? <Pill variant="info">Attachment</Pill> : null}
                  </div>
                  <p className="text-[12px] text-text-secondary mb-0.5">
                    {formatDate(l.startDate)} – {formatDate(l.endDate)}
                  </p>
                  <p className="text-[12px] text-text-secondary">{l.reason}</p>
                </div>
                <div className="flex items-center gap-2">
                  <Button variant="secondary" onClick={() => setRejectTarget(l)}>
                    Reject
                  </Button>
                  <Button variant="primary" onClick={() => setApproveTarget(l)}>
                    Approve
                  </Button>
                </div>
              </div>
            ))}
          </div>
        </Card>
      )}

      <ConfirmDialog
        open={Boolean(approveTarget)}
        title="Approve leave request?"
        message={
          approveTarget
            ? `${approveTarget.employee.fullName}'s ${approveTarget.leaveType} leave (${formatDate(approveTarget.startDate)} – ${formatDate(approveTarget.endDate)}) will be approved.`
            : ''
        }
        confirmLabel={submitting ? 'Approving…' : 'Approve'}
        onConfirm={handleConfirmApprove}
        onCancel={() => setApproveTarget(null)}
      />

      <RejectDialog
        open={Boolean(rejectTarget)}
        submitting={submitting}
        onConfirm={handleConfirmReject}
        onCancel={() => setRejectTarget(null)}
      />
    </div>
  );
}
