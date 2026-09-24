import { useEffect, useState } from 'react';
import { useAuth } from '../../auth/AuthContext';
import { submitLeave, getMyLeave, getLeaveBalance } from '../../api/leave';
import Card from '../../components/Card';
import Pill from '../../components/Pill';
import StatCard from '../../components/StatCard';
import Button from '../../components/Button';
import Input from '../../components/Input';
import Select from '../../components/Select';
import SkeletonRow from '../../components/SkeletonRow';
import EmptyState from '../../components/EmptyState';
import ErrorInline from '../../components/ErrorInline';

const LEAVE_TYPES = ['ANNUAL', 'SICK', 'UNPAID', 'FAMILY_RESPONSIBILITY', 'STUDY', 'OTHER'];

const STATUS_VARIANT = {
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'urgent',
};

function formatDate(value) {
  return new Date(value).toLocaleDateString([], { dateStyle: 'medium' });
}

function formatTypeLabel(type) {
  return type
    .toLowerCase()
    .split('_')
    .map((w) => w[0].toUpperCase() + w.slice(1))
    .join(' ');
}

export default function LeaveRequests() {
  const { user } = useAuth();
  const employeeId = user?.employeeId;

  const [leave, setLeave] = useState([]);
  const [balance, setBalance] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [leaveType, setLeaveType] = useState(LEAVE_TYPES[0]);
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [reason, setReason] = useState('');
  const [attachment, setAttachment] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState('');

  useEffect(() => {
    if (!employeeId) {
      setLoading(false);
      return;
    }
    load();
  }, [employeeId]);

  async function load() {
    setLoading(true);
    setError('');
    try {
      const data = await getMyLeave(employeeId);
      setLeave(data);
      getLeaveBalance(employeeId).then(setBalance).catch(() => setBalance(null));
    } catch {
      setError('Could not load your leave requests.');
    } finally {
      setLoading(false);
    }
  }

  async function handleSubmit(e) {
    e.preventDefault();
    if (!startDate || !endDate) {
      setSubmitError('Choose a start and end date.');
      return;
    }
    if (!reason.trim()) {
      setSubmitError("Tell us why you're requesting leave.");
      return;
    }
    setSubmitting(true);
    setSubmitError('');
    try {
      await submitLeave(employeeId, { leaveType, startDate, endDate, reason, attachment });
      setStartDate('');
      setEndDate('');
      setReason('');
      setAttachment(null);
      e.target.reset();
      await load();
    } catch (err) {
      setSubmitError(err.message || 'Could not submit request.');
    } finally {
      setSubmitting(false);
    }
  }

  if (!employeeId) {
    return (
      <div>
        <p className="text-[20px] font-medium mb-5">Leave Requests</p>
        <Card>
          <EmptyState message="No employee record is linked to your account, so you can't submit leave requests." />
        </Card>
      </div>
    );
  }

  return (
    <div>
      <p className="text-[20px] font-medium mb-5">Leave Requests</p>

      {balance ? (
        <div className="grid grid-cols-4 gap-3.5 mb-3.5">
          <StatCard
            label={`Annual leave remaining (${balance.year})`}
            value={`${balance.remaining} of ${balance.allowance} days`}
            variant={balance.remaining === 0 ? 'urgent' : 'neutral'}
          />
          <StatCard label="Annual days used" value={balance.used} />
          <StatCard label="Annual days pending" value={balance.reserved} />
          <StatCard label="Other leave taken (days)" value={balance.otherDaysTaken} />
        </div>
      ) : null}

      <Card className="mb-3.5">
        <p className="text-[15px] font-medium mb-3">Submit a request</p>
        <form onSubmit={handleSubmit} className="flex flex-col gap-3">
          <div className="flex gap-3">
            <Select label="Type" value={leaveType} onChange={(e) => setLeaveType(e.target.value)}>
              {LEAVE_TYPES.map((t) => (
                <option key={t} value={t}>
                  {formatTypeLabel(t)}
                </option>
              ))}
            </Select>
            <Input
              label="Start date"
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
            />
            <Input
              label="End date"
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
            />
          </div>
          <div>
            <label className="text-[12px] text-text-secondary block mb-1">Reason</label>
            <textarea
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              rows={3}
              placeholder="Explain why you're requesting this leave"
              className="w-full rounded-lg border border-border-strong bg-surface-1 text-text-primary p-2.5 text-[13px] focus:outline-none focus:border-text-secondary"
            />
          </div>
          <div>
            <label className="text-[12px] text-text-secondary block mb-1">
              Supporting document (optional)
            </label>
            <input
              type="file"
              accept=".pdf,.jpg,.jpeg,.png,.docx"
              onChange={(e) => setAttachment(e.target.files[0] || null)}
              className="text-[13px]"
            />
          </div>
          {submitError ? <ErrorInline message={submitError} /> : null}
          <div>
            <Button type="submit" disabled={submitting}>
              {submitting ? 'Submitting…' : 'Submit request'}
            </Button>
          </div>
        </form>
      </Card>

      {loading ? (
        <div className="flex flex-col gap-2">
          <SkeletonRow />
          <SkeletonRow />
        </div>
      ) : error ? (
        <ErrorInline message={error} />
      ) : leave.length === 0 ? (
        <Card>
          <EmptyState message="You haven't submitted any leave requests yet." />
        </Card>
      ) : (
        <Card>
          <div className="flex flex-col divide-y divide-border">
            {leave.map((l) => (
              <div key={l.id} className="flex items-center justify-between py-3 first:pt-0 last:pb-0">
                <div>
                  <div className="flex items-center gap-2 mb-1">
                    <p className="text-[13px] font-medium">{formatTypeLabel(l.leaveType)}</p>
                    {l.hasAttachment ? <Pill variant="info">Attachment</Pill> : null}
                  </div>
                  <p className="text-[12px] text-text-secondary mb-0.5">
                    {formatDate(l.startDate)} – {formatDate(l.endDate)}
                  </p>
                  <p className="text-[12px] text-text-secondary">{l.reason}</p>
                  {l.status === 'REJECTED' && l.rejectionReason ? (
                    <p className="text-[12px] text-danger-text mt-1">
                      Rejected: {l.rejectionReason}
                    </p>
                  ) : null}
                  {l.decidedByName ? (
                    <p className="text-[12px] text-text-secondary mt-1">
                      {l.status === 'APPROVED' ? 'Approved' : 'Rejected'} by {l.decidedByName}
                      {l.decidedAt ? ` on ${formatDate(l.decidedAt)}` : ''}
                    </p>
                  ) : null}
                </div>
                <Pill variant={STATUS_VARIANT[l.status] || 'neutral'}>{l.status}</Pill>
              </div>
            ))}
          </div>
        </Card>
      )}
    </div>
  );
}
