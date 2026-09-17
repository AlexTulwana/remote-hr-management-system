import { useEffect, useState } from 'react';
import { getEmployeeDashboardSummary } from '../../api/dashboard';
import Card from '../../components/Card';
import Pill from '../../components/Pill';
import StatCard from '../../components/StatCard';
import SkeletonCard from '../../components/SkeletonCard';
import ErrorInline from '../../components/ErrorInline';

export default function EmployeeDashboard() {
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);
      try {
        const data = await getEmployeeDashboardSummary();
        if (!cancelled) setSummary(data);
      } catch (err) {
        if (!cancelled) setError(err.message);
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, []);

  if (loading) {
    return (
      <div className="space-y-4">
        <div className="h-20 bg-surface-1 rounded-lg animate-pulse" />
        <div className="grid grid-cols-2 md:grid-cols-3 gap-2.5">
          {Array.from({ length: 6 }).map((_, i) => (
            <SkeletonCard key={i} />
          ))}
        </div>
      </div>
    );
  }

  if (error) {
    return <ErrorInline message={error} />;
  }

  const statusVariant = summary.employmentStatus === 'ACTIVE' ? 'success' : 'neutral';

  return (
    <div className="space-y-4">
      <Card>
        <div className="flex items-center justify-between flex-wrap gap-2">
          <div>
            <div className="text-[16px] font-semibold text-text-primary">
              {summary.fullName}
            </div>
            <div className="text-[12px] text-text-secondary mt-0.5">
              {summary.position} · {summary.department} · {summary.branchName}
            </div>
            <div className="text-[12px] text-text-secondary mt-0.5">
              {summary.employeeNumber}
            </div>
          </div>
          <Pill variant={statusVariant}>{summary.employmentStatus}</Pill>
        </div>
      </Card>

      <div className="grid grid-cols-2 md:grid-cols-3 gap-2.5">
        <StatCard label="Pending leave requests" value={summary.pendingLeaveRequests} />
        <StatCard label="Approved leave requests" value={summary.approvedLeaveRequests} />
        <StatCard label="Rejected leave requests" value={summary.rejectedLeaveRequests} />
        <StatCard label="Attendance this month" value={summary.attendanceRecordsThisMonth} />
        <StatCard label="Pending requests" value={summary.pendingRequestsCount} />
        <StatCard
          label="Open disciplinary cases"
          value={summary.openDisciplinaryCasesCount}
          variant={summary.openDisciplinaryCasesCount > 0 ? 'urgent' : 'neutral'}
        />
      </div>
    </div>
  );
}
