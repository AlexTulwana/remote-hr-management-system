import { useEffect, useState } from 'react';
import {
  getAllOnboardings,
  getOnboardingCandidates,
  getManagerOptions,
  startOnboardingFromApplication,
  resendOnboardingInvite,
  completeOnboarding,
} from '../../api/onboarding';
import { useAuth } from '../../auth/AuthContext';
import Card from '../../components/Card';
import Pill from '../../components/Pill';
import Button from '../../components/Button';
import Input from '../../components/Input';
import Select from '../../components/Select';
import SkeletonRow from '../../components/SkeletonRow';
import EmptyState from '../../components/EmptyState';
import ErrorInline from '../../components/ErrorInline';
import ConfirmDialog from '../../components/ConfirmDialog';

const STATUS_VARIANT = { IN_PROGRESS: 'info', COMPLETE: 'success' };
const NO_BRANCH_ROLES = ['HR', 'ADMIN'];

export default function Onboarding() {
  const { user } = useAuth();
  const isAdmin = user?.role === 'ADMIN';

  const [records, setRecords] = useState([]);
  const [candidates, setCandidates] = useState([]);
  const [managers, setManagers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [refresh, setRefresh] = useState(0);

  const [applicationId, setApplicationId] = useState('');
  const [role, setRole] = useState('EMPLOYEE');
  const [startDate, setStartDate] = useState('');
  const [reportsToId, setReportsToId] = useState('');
  const [notes, setNotes] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState('');
  const [notice, setNotice] = useState('');

  const [confirm, setConfirm] = useState(null); // { kind: 'start' | 'resend' | 'complete', id? }
  const [actionError, setActionError] = useState('');

  useEffect(() => {
    let cancelled = false;
    Promise.all([getAllOnboardings(), getOnboardingCandidates(), getManagerOptions()])
      .then(([recs, cands, mgrs]) => {
        if (cancelled) return;
        setRecords([...recs].sort((a, b) => (b.createdAt || '').localeCompare(a.createdAt || '')));
        setCandidates(cands);
        setManagers(mgrs);
        setError('');
      })
      .catch(() => {
        if (!cancelled) setError('Could not load onboarding data.');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [refresh]);

  const candidate = candidates.find((c) => String(c.applicationId) === String(applicationId));
  const managerChoices = managers.filter((m) => {
    if (!candidate || NO_BRANCH_ROLES.includes(role)) return true;
    return m.role !== 'MANAGER' || m.branchId === candidate.branchId;
  });
  const pendingRecord = records.find((r) => r.id === confirm?.id);

  function resetForm() {
    setApplicationId('');
    setRole('EMPLOYEE');
    setStartDate('');
    setReportsToId('');
    setNotes('');
  }

  function askStart() {
    setNotice('');
    if (!applicationId || !startDate || !reportsToId) {
      setFormError('Choose a candidate, a start date and a manager.');
      return;
    }
    setFormError('');
    setConfirm({ kind: 'start' });
  }

  async function runStart() {
    setConfirm(null);
    setSubmitting(true);
    setFormError('');
    try {
      await startOnboardingFromApplication(applicationId, {
        role,
        startDate,
        reportsToId: Number(reportsToId),
        notes,
      });
      setNotice(`Onboarding started. An invite was emailed to ${candidate?.candidateEmail || 'the candidate'}.`);
      resetForm();
      setRefresh((n) => n + 1);
    } catch (e) {
      setFormError(e.message);
    } finally {
      setSubmitting(false);
    }
  }

  async function runResend() {
    const id = confirm.id;
    setConfirm(null);
    setActionError('');
    setNotice('');
    try {
      await resendOnboardingInvite(id);
      setNotice('A new invite was emailed.');
    } catch (e) {
      setActionError(e.message);
    }
  }

  async function runComplete() {
    const id = confirm.id;
    setConfirm(null);
    setActionError('');
    setNotice('');
    try {
      await completeOnboarding(id);
      setRefresh((n) => n + 1);
    } catch (e) {
      setActionError(e.message);
    }
  }

  const dialog = {
    start: {
      title: 'Start onboarding?',
      message: `This creates the employee profile and login for ${candidate?.candidateName || 'the candidate'} and emails an invite to ${candidate?.candidateEmail || 'their address'}.`,
      confirmLabel: 'Create and send invite',
      onConfirm: runStart,
    },
    resend: {
      title: 'Resend invite?',
      message: `This emails a new set-password link to ${pendingRecord?.employeeName || 'the new hire'}. The old link stops working.`,
      confirmLabel: 'Resend invite',
      onConfirm: runResend,
    },
    complete: {
      title: 'Complete onboarding?',
      message: `This sets ${pendingRecord?.employeeName || 'the employee'} to ACTIVE.`,
      confirmLabel: 'Complete',
      onConfirm: runComplete,
    },
  }[confirm?.kind];

  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-[20px] font-medium">Onboarding</h1>

      <Card>
        <p className="text-[14px] font-medium mb-3">Onboard an accepted candidate</p>
        {!loading && candidates.length === 0 ? (
          <p className="text-[13px] text-text-muted mb-3">
            No accepted candidates are waiting. Accept an application in Recruitment first.
          </p>
        ) : null}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-3 mb-3">
          <Select
            label="Candidate"
            value={applicationId}
            onChange={(e) => {
              setApplicationId(e.target.value);
              setReportsToId('');
            }}
          >
            <option value="">Select a candidate</option>
            {candidates.map((c) => (
              <option key={c.applicationId} value={c.applicationId}>
                {c.candidateName} · {c.jobPostingTitle}
                {c.branchName ? ` · ${c.branchName}` : ''}
              </option>
            ))}
          </Select>
          <Select
            label="Role"
            value={role}
            onChange={(e) => {
              setRole(e.target.value);
              setReportsToId('');
            }}
          >
            <option value="EMPLOYEE">Employee</option>
            <option value="MANAGER">Manager</option>
            <option value="HR">HR</option>
            {isAdmin ? <option value="ADMIN">Admin</option> : null}
          </Select>
          <Input
            label="Start date"
            type="date"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
          />
          <Select label="Reports to" value={reportsToId} onChange={(e) => setReportsToId(e.target.value)}>
            <option value="">Select a manager</option>
            {managerChoices.map((m) => (
              <option key={m.employeeId} value={m.employeeId}>
                {m.fullName} ({m.employeeNumber}) · {m.role}
                {m.branchName ? ` · ${m.branchName}` : ''}
              </option>
            ))}
          </Select>
          <div className="md:col-span-2">
            <Input label="Notes" value={notes} onChange={(e) => setNotes(e.target.value)} />
          </div>
        </div>
        {candidate ? (
          <p className="text-[12px] text-text-muted mb-3">
            Login username will be {candidate.candidateEmail}. The employee number is generated automatically.
            {NO_BRANCH_ROLES.includes(role) ? ' HR and Admin staff are not placed in a branch.' : ''}
          </p>
        ) : null}
        {formError ? <div className="mb-3"><ErrorInline message={formError} /></div> : null}
        <Button onClick={askStart} disabled={submitting}>
          {submitting ? 'Starting...' : 'Start onboarding'}
        </Button>
      </Card>

      {notice ? <p className="text-[13px] text-text-secondary">{notice}</p> : null}
      {error ? <ErrorInline message={error} /> : null}
      {actionError ? <ErrorInline message={actionError} /> : null}

      <Card className="p-0">
        {loading ? (
          <>
            <SkeletonRow />
            <SkeletonRow />
            <SkeletonRow />
          </>
        ) : records.length === 0 ? (
          <EmptyState message="No onboarding records yet." />
        ) : (
          records.map((r) => (
            <div
              key={r.id}
              className="flex items-center justify-between gap-3 px-3.5 py-3 border-b border-border last:border-b-0"
            >
              <div>
                <p className="text-[13px]">
                  {r.employeeName} <span className="text-text-muted">({r.employeeNumber})</span>
                </p>
                <p className="text-[11px] text-text-muted">
                  {r.position || '—'}
                  {r.department ? ` · ${r.department}` : ''}
                  {r.branchName ? ` · ${r.branchName}` : ''}
                  {` · starts ${r.startDate || '—'} · by ${r.performedBy || '—'}`}
                </p>
                {r.notes ? <p className="text-[12px] text-text-secondary mt-1">{r.notes}</p> : null}
              </div>
              <div className="flex items-center gap-2">
                <Pill variant={STATUS_VARIANT[r.status] || 'neutral'}>{r.status}</Pill>
                {r.status === 'IN_PROGRESS' ? (
                  <>
                    <Button variant="secondary" onClick={() => setConfirm({ kind: 'resend', id: r.id })}>
                      Resend invite
                    </Button>
                    <Button variant="secondary" onClick={() => setConfirm({ kind: 'complete', id: r.id })}>
                      Mark complete
                    </Button>
                  </>
                ) : null}
              </div>
            </div>
          ))
        )}
      </Card>

      <ConfirmDialog
        open={confirm !== null}
        title={dialog?.title || ''}
        message={dialog?.message || ''}
        confirmLabel={dialog?.confirmLabel || 'Confirm'}
        onConfirm={dialog?.onConfirm || (() => {})}
        onCancel={() => setConfirm(null)}
      />
    </div>
  );
}
