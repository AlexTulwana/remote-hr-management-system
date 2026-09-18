import { useEffect, useState } from 'react';
import { useAuth } from '../../auth/AuthContext';
import { clockIn, clockOut, getMyAttendance } from '../../api/attendance';
import Card from '../../components/Card';
import Button from '../../components/Button';
import SkeletonRow from '../../components/SkeletonRow';
import EmptyState from '../../components/EmptyState';
import ErrorInline from '../../components/ErrorInline';

function formatDate(value) {
  return new Date(value).toLocaleDateString([], { dateStyle: 'medium' });
}

function formatTime(value) {
  if (!value) return '—';
  const [h, m] = value.split(':');
  const d = new Date();
  d.setHours(Number(h), Number(m));
  return d.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' });
}

function todayIso() {
  return new Date().toLocaleDateString('en-CA'); // yyyy-mm-dd, local time
}

export default function Attendance() {
  const { user } = useAuth();
  const employeeId = user?.employeeId;

  const [records, setRecords] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionError, setActionError] = useState('');
  const [submitting, setSubmitting] = useState(false);

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
      const data = await getMyAttendance(employeeId);
      setRecords(data);
    } catch {
      setError('Could not load your attendance records.');
    } finally {
      setLoading(false);
    }
  }

  const openRecord = records.find((r) => r.date === todayIso() && !r.clockOut);

  async function handleClockIn() {
    setSubmitting(true);
    setActionError('');
    try {
      await clockIn(employeeId);
      await load();
    } catch {
      setActionError('Could not clock in.');
    } finally {
      setSubmitting(false);
    }
  }

  async function handleClockOut() {
    setSubmitting(true);
    setActionError('');
    try {
      await clockOut(openRecord.id);
      await load();
    } catch {
      setActionError('Could not clock out.');
    } finally {
      setSubmitting(false);
    }
  }

  if (!employeeId) {
    return (
      <div>
        <p className="text-[20px] font-medium mb-5">Attendance</p>
        <Card>
          <EmptyState message="No employee record is linked to your account, so there's nothing to track." />
        </Card>
      </div>
    );
  }

  return (
    <div>
      <p className="text-[20px] font-medium mb-5">Attendance</p>

      <Card className="mb-3.5">
        <div className="flex items-center justify-between flex-wrap gap-3">
          <div>
            <p className="text-[15px] font-medium">
              {openRecord ? 'Currently clocked in' : 'Not clocked in today'}
            </p>
            {openRecord ? (
              <p className="text-[12px] text-text-secondary mt-0.5">
                Since {formatTime(openRecord.clockIn)}
              </p>
            ) : null}
          </div>
          {openRecord ? (
            <Button onClick={handleClockOut} disabled={submitting}>
              {submitting ? 'Clocking out…' : 'Clock out'}
            </Button>
          ) : (
            <Button onClick={handleClockIn} disabled={submitting}>
              {submitting ? 'Clocking in…' : 'Clock in'}
            </Button>
          )}
        </div>
        {actionError ? (
          <div className="mt-3">
            <ErrorInline message={actionError} />
          </div>
        ) : null}
      </Card>

      {loading ? (
        <div className="flex flex-col gap-2">
          <SkeletonRow />
          <SkeletonRow />
          <SkeletonRow />
        </div>
      ) : error ? (
        <ErrorInline message={error} />
      ) : records.length === 0 ? (
        <Card>
          <EmptyState message="No attendance records yet." />
        </Card>
      ) : (
        <Card>
          <div className="flex flex-col divide-y divide-border">
            {[...records]
              .sort((a, b) => (a.date < b.date ? 1 : -1))
              .map((r) => (
                <div key={r.id} className="flex items-center justify-between py-2.5 first:pt-0 last:pb-0">
                  <p className="text-[12.5px] text-text-primary">{formatDate(r.date)}</p>
                  <p className="text-[12.5px] text-text-secondary">
                    {formatTime(r.clockIn)} – {formatTime(r.clockOut)}
                  </p>
                  <p className="text-[12.5px] text-text-primary w-16 text-right">
                    {r.hoursWorked != null ? `${r.hoursWorked}h` : '—'}
                  </p>
                </div>
              ))}
          </div>
        </Card>
      )}
    </div>
  );
}
