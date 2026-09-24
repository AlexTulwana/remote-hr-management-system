import { useEffect, useState } from 'react';
import { getAllOffboardings, startOffboarding, completeOffboarding } from '../../api/offboarding';
import { getDirectory } from '../../api/employeeDirectory';
import Card from '../../components/Card';
import Pill from '../../components/Pill';
import Button from '../../components/Button';
import Input from '../../components/Input';
import Select from '../../components/Select';
import SkeletonRow from '../../components/SkeletonRow';
import EmptyState from '../../components/EmptyState';
import ErrorInline from '../../components/ErrorInline';
import ConfirmDialog from '../../components/ConfirmDialog';

const STATUS_VARIANT = { IN_PROGRESS: 'info', SCHEDULED: 'warning', COMPLETE: 'success' };
const TYPE_VARIANT = { RESIGNATION: 'warning', TERMINATION: 'urgent' };
const INELIGIBLE = ['RESIGNED', 'TERMINATED'];

export default function Offboarding() {
  const [records, setRecords] = useState([]);
  const [employees, setEmployees] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [refresh, setRefresh] = useState(0);

  const [employeeId, setEmployeeId] = useState('');
  const [type, setType] = useState('RESIGNATION');
  const [effectiveDate, setEffectiveDate] = useState('');
  const [reason, setReason] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState('');

  const [confirmId, setConfirmId] = useState(null);
  const [actionError, setActionError] = useState('');

  useEffect(() => {
    let cancelled = false;
    Promise.all([getAllOffboardings(), getDirectory()])
      .then(([recs, dir]) => {
        if (cancelled) return;
        setRecords([...recs].sort((a, b) => (b.createdAt || '').localeCompare(a.createdAt || '')));
        setEmployees(dir);
        setError('');
      })
      .catch(() => {
        if (!cancelled) setError('Could not load offboarding records.');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [refresh]);

  const inProgressIds = new Set(
    records
      .filter((r) => r.status === 'IN_PROGRESS' || r.status === 'SCHEDULED')
      .map((r) => r.employeeId),
  );
  const eligible = employees.filter(
    (e) => !INELIGIBLE.includes(e.employmentStatus) && !inProgressIds.has(e.id),
  );
  const pending = records.find((r) => r.id === confirmId);
  const todayStr = new Date().toLocaleDateString('en-CA');
  const isFuture = Boolean(pending?.effectiveDate) && pending.effectiveDate > todayStr;

  async function handleStart() {
    if (!employeeId || !effectiveDate) {
      setFormError('Choose an employee and an effective date.');
      return;
    }
    setSubmitting(true);
    setFormError('');
    try {
      await startOffboarding(employeeId, { type, effectiveDate, reason });
      setEmployeeId('');
      setType('RESIGNATION');
      setEffectiveDate('');
      setReason('');
      setRefresh((n) => n + 1);
    } catch (e) {
      setFormError(e.message);
    } finally {
      setSubmitting(false);
    }
  }

  async function handleComplete() {
    const id = confirmId;
    setConfirmId(null);
    setActionError('');
    try {
      await completeOffboarding(id);
      setRefresh((n) => n + 1);
    } catch (e) {
      setActionError(e.message);
    }
  }

  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-[20px] font-medium">Offboarding</h1>

      <Card>
        <p className="text-[14px] font-medium mb-3">Start offboarding</p>
        <div className="grid grid-cols-1 md:grid-cols-4 gap-3 mb-3">
          <Select label="Employee" value={employeeId} onChange={(e) => setEmployeeId(e.target.value)}>
            <option value="">Select an employee</option>
            {eligible.map((e) => (
              <option key={e.id} value={e.id}>
                {e.fullName} ({e.employeeNumber})
              </option>
            ))}
          </Select>
          <Select label="Type" value={type} onChange={(e) => setType(e.target.value)}>
            <option value="RESIGNATION">Resignation</option>
            <option value="TERMINATION">Termination</option>
          </Select>
          <Input
            label="Effective date"
            type="date"
            value={effectiveDate}
            onChange={(e) => setEffectiveDate(e.target.value)}
          />
          <Input label="Reason" value={reason} onChange={(e) => setReason(e.target.value)} />
        </div>
        {formError ? <div className="mb-3"><ErrorInline message={formError} /></div> : null}
        <Button onClick={handleStart} disabled={submitting}>
          {submitting ? 'Starting...' : 'Start offboarding'}
        </Button>
      </Card>

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
          <EmptyState message="No offboarding records yet." />
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
                  {` · effective ${r.effectiveDate || '—'} · by ${r.performedBy || '—'}`}
                </p>
                {r.reason ? <p className="text-[12px] text-text-secondary mt-1">{r.reason}</p> : null}
              </div>
              <div className="flex items-center gap-2">
                <Pill variant={TYPE_VARIANT[r.type] || 'neutral'}>{r.type}</Pill>
                <Pill variant={STATUS_VARIANT[r.status] || 'neutral'}>{r.status}</Pill>
                {r.status === 'IN_PROGRESS' ? (
                  <Button variant="danger" onClick={() => setConfirmId(r.id)}>
                    Complete
                  </Button>
                ) : null}
              </div>
            </div>
          ))
        )}
      </Card>

      <ConfirmDialog
        open={confirmId !== null}
        title="Complete offboarding?"
        message={isFuture ? `${pending?.employeeName || 'The employee'} stays active and can still log in until ${pending?.effectiveDate}. They are then deactivated automatically, shortly after midnight.` : `This marks ${pending?.employeeName || 'the employee'} inactive and sets their status to ${
          pending?.type === 'RESIGNATION' ? 'RESIGNED' : 'TERMINATED'
        } (effective ${pending?.effectiveDate || '—'}). It cannot be undone from this page.`}
        confirmLabel={isFuture ? 'Schedule deactivation' : 'Complete offboarding'}
        onConfirm={handleComplete}
        onCancel={() => setConfirmId(null)}
      />
    </div>
  );
}
