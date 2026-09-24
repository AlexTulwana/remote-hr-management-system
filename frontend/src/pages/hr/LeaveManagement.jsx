import { useEffect, useMemo, useState } from 'react';
import {
  getAllLeave,
  getAllLeaveBalances,
  approveLeave,
  rejectLeave,
  openLeaveAttachment,
} from '../../api/leave';
import Card from '../../components/Card';
import Pill from '../../components/Pill';
import Button from '../../components/Button';
import Input from '../../components/Input';
import Select from '../../components/Select';
import ConfirmDialog from '../../components/ConfirmDialog';
import SkeletonRow from '../../components/SkeletonRow';
import EmptyState from '../../components/EmptyState';
import ErrorInline from '../../components/ErrorInline';

const LEAVE_TYPES = ['ANNUAL', 'SICK', 'UNPAID', 'FAMILY_RESPONSIBILITY', 'STUDY', 'OTHER'];
const STATUSES = ['PENDING', 'APPROVED', 'REJECTED'];
const STATUS_VARIANT = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'urgent' };

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

export default function LeaveManagement() {
  const [tab, setTab] = useState('requests');

  const [requests, setRequests] = useState([]);
  const [reqLoading, setReqLoading] = useState(true);
  const [reqError, setReqError] = useState('');

  const [balances, setBalances] = useState([]);
  const [balLoading, setBalLoading] = useState(true);
  const [balError, setBalError] = useState('');

  const [search, setSearch] = useState('');
  const [branchFilter, setBranchFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('PENDING');
  const [typeFilter, setTypeFilter] = useState('ALL');

  const [actionError, setActionError] = useState('');
  const [approveTarget, setApproveTarget] = useState(null);
  const [rejectTarget, setRejectTarget] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  async function loadRequests() {
    setReqLoading(true);
    setReqError('');
    try {
      setRequests(await getAllLeave());
    } catch {
      setReqError('Could not load leave requests.');
    } finally {
      setReqLoading(false);
    }
  }

  async function loadBalances() {
    setBalLoading(true);
    setBalError('');
    try {
      setBalances(await getAllLeaveBalances());
    } catch {
      setBalError('Could not load leave balances.');
    } finally {
      setBalLoading(false);
    }
  }

  useEffect(() => {
    loadRequests();
    loadBalances();
  }, []);

  const branches = useMemo(() => {
    const names = [
      ...requests.map((r) => r.employee?.branchName),
      ...balances.map((b) => b.branchName),
    ].filter(Boolean);
    return [...new Set(names)].sort();
  }, [requests, balances]);

  const filteredRequests = useMemo(() => {
    const q = search.trim().toLowerCase();
    return requests
      .filter((r) => statusFilter === 'ALL' || r.status === statusFilter)
      .filter((r) => typeFilter === 'ALL' || r.leaveType === typeFilter)
      .filter((r) => branchFilter === 'ALL' || r.employee?.branchName === branchFilter)
      .filter((r) => !q || (r.employee?.fullName || '').toLowerCase().includes(q))
      .sort((a, b) => b.startDate.localeCompare(a.startDate));
  }, [requests, search, branchFilter, statusFilter, typeFilter]);

  const filteredBalances = useMemo(() => {
    const q = search.trim().toLowerCase();
    return balances
      .filter((b) => branchFilter === 'ALL' || b.branchName === branchFilter)
      .filter((b) => !q || (b.fullName || '').toLowerCase().includes(q));
  }, [balances, search, branchFilter]);

  async function handleViewAttachment(id) {
    setActionError('');
    try {
      await openLeaveAttachment(id);
    } catch {
      setActionError('Could not open the attachment.');
    }
  }

  async function handleConfirmApprove() {
    setSubmitting(true);
    setActionError('');
    try {
      await approveLeave(approveTarget.id);
      setApproveTarget(null);
      await Promise.all([loadRequests(), loadBalances()]);
    } catch (err) {
      setActionError(err.message || 'Could not approve this request.');
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
      await Promise.all([loadRequests(), loadBalances()]);
    } catch (err) {
      setActionError(err.message || 'Could not reject this request.');
      setRejectTarget(null);
    } finally {
      setSubmitting(false);
    }
  }

  const tabs = [
    ['requests', 'Requests'],
    ['balances', 'Balances'],
  ];

  return (
    <div>
      <p className="text-[20px] font-medium mb-5">Leave Management</p>

      <div className="flex gap-5 border-b border-border mb-4">
        {tabs.map(([key, label]) => (
          <button
            key={key}
            onClick={() => setTab(key)}
            className={[
              'pb-2 text-[13px] font-medium cursor-pointer border-b-2 -mb-px transition-colors',
              tab === key
                ? 'border-text-primary text-text-primary'
                : 'border-transparent text-text-secondary hover:text-text-primary',
            ].join(' ')}
          >
            {label}
          </button>
        ))}
      </div>

      <div className="flex flex-wrap items-end gap-3 mb-4">
        <Input
          label="Search employee"
          placeholder="Name"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <Select label="Branch" value={branchFilter} onChange={(e) => setBranchFilter(e.target.value)}>
          <option value="ALL">All branches</option>
          {branches.map((b) => (
            <option key={b} value={b}>
              {b}
            </option>
          ))}
        </Select>
        {tab === 'requests' ? (
          <>
            <Select label="Status" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
              <option value="ALL">All statuses</option>
              {STATUSES.map((s) => (
                <option key={s} value={s}>
                  {formatTypeLabel(s)}
                </option>
              ))}
            </Select>
            <Select label="Type" value={typeFilter} onChange={(e) => setTypeFilter(e.target.value)}>
              <option value="ALL">All types</option>
              {LEAVE_TYPES.map((t) => (
                <option key={t} value={t}>
                  {formatTypeLabel(t)}
                </option>
              ))}
            </Select>
          </>
        ) : null}
      </div>

      {actionError ? <div className="mb-3"><ErrorInline message={actionError} /></div> : null}

      {tab === 'requests' ? (
        reqLoading ? (
          <Card>
            <div className="flex flex-col">
              <SkeletonRow />
              <SkeletonRow />
              <SkeletonRow />
            </div>
          </Card>
        ) : reqError ? (
          <ErrorInline message={reqError} />
        ) : filteredRequests.length === 0 ? (
          <Card>
            <EmptyState message="No leave requests match these filters." />
          </Card>
        ) : (
          <Card>
            <div className="flex flex-col divide-y divide-border">
              {filteredRequests.map((l) => (
                <div key={l.id} className="flex items-center justify-between py-3 first:pt-0 last:pb-0">
                  <div>
                    <div className="flex items-center gap-2 mb-1">
                      <p className="text-[13px] font-medium">{l.employee?.fullName}</p>
                      <Pill variant="neutral">{formatTypeLabel(l.leaveType)}</Pill>
                      <Pill variant={STATUS_VARIANT[l.status] || 'neutral'}>{l.status}</Pill>
                      {l.employee?.branchName ? (
                        <span className="text-[12px] text-text-secondary">{l.employee.branchName}</span>
                      ) : null}
                    </div>
                    <p className="text-[12px] text-text-secondary mb-0.5">
                      {formatDate(l.startDate)} – {formatDate(l.endDate)}
                    </p>
                    <p className="text-[12px] text-text-secondary">{l.reason}</p>
                    {l.status === 'REJECTED' && l.rejectionReason ? (
                      <p className="text-[12px] text-danger-text mt-1">Rejected: {l.rejectionReason}</p>
                    ) : null}
                    {l.decidedByName ? (
                      <p className="text-[12px] text-text-secondary mt-1">
                        {l.status === 'APPROVED' ? 'Approved' : 'Rejected'} by {l.decidedByName}
                        {l.decidedAt ? ` on ${formatDate(l.decidedAt)}` : ''}
                      </p>
                    ) : null}
                  </div>
                  <div className="flex items-center gap-2">
                    {l.hasAttachment ? (
                      <Button variant="secondary" onClick={() => handleViewAttachment(l.id)}>
                        View attachment
                      </Button>
                    ) : null}
                    {l.status === 'PENDING' ? (
                      <>
                        <Button variant="secondary" onClick={() => setRejectTarget(l)}>
                          Reject
                        </Button>
                        <Button variant="primary" onClick={() => setApproveTarget(l)}>
                          Approve
                        </Button>
                      </>
                    ) : null}
                  </div>
                </div>
              ))}
            </div>
          </Card>
        )
      ) : balLoading ? (
        <Card>
          <div className="flex flex-col">
            <SkeletonRow />
            <SkeletonRow />
            <SkeletonRow />
          </div>
        </Card>
      ) : balError ? (
        <ErrorInline message={balError} />
      ) : filteredBalances.length === 0 ? (
        <Card>
          <EmptyState message="No active employees match these filters." />
        </Card>
      ) : (
        <Card>
          <p className="text-[12px] text-text-secondary mb-3">
            Annual leave balances for {filteredBalances[0].year} (working days, active employees only)
          </p>
          <div className="overflow-x-auto">
            <table className="w-full text-[13px]">
              <thead>
                <tr className="text-left text-[12px] text-text-secondary border-b border-border">
                  <th className="pb-2 font-medium">Employee</th>
                  <th className="pb-2 font-medium">Branch</th>
                  <th className="pb-2 font-medium">Allowance</th>
                  <th className="pb-2 font-medium">Used</th>
                  <th className="pb-2 font-medium">Pending</th>
                  <th className="pb-2 font-medium">Remaining</th>
                  <th className="pb-2 font-medium">Other leave taken</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {filteredBalances.map((b) => (
                  <tr key={b.employeeId}>
                    <td className="py-2.5">
                      <p className="font-medium">{b.fullName}</p>
                      <p className="text-[12px] text-text-secondary">{b.employeeNumber}</p>
                    </td>
                    <td className="py-2.5 text-text-secondary">{b.branchName || '—'}</td>
                    <td className="py-2.5">{b.allowance}</td>
                    <td className="py-2.5">{b.used}</td>
                    <td className="py-2.5">{b.reserved}</td>
                    <td className="py-2.5">
                      <Pill variant={b.remaining === 0 ? 'urgent' : 'success'}>{b.remaining}</Pill>
                    </td>
                    <td className="py-2.5">{b.otherDaysTaken}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      <ConfirmDialog
        open={Boolean(approveTarget)}
        title="Approve leave request?"
        message={
          approveTarget
            ? `${approveTarget.employee?.fullName}'s ${formatTypeLabel(approveTarget.leaveType)} leave (${formatDate(approveTarget.startDate)} – ${formatDate(approveTarget.endDate)}) will be approved.`
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
