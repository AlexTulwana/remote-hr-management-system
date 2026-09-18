import { useEffect, useState } from 'react';
import {
  getPendingRequests,
  getHeadcount,
  getDisciplinaryCases,
  getPendingLeave,
  getUpcomingSchedule,
} from '../../api/managerDashboard';
import { useAuth } from '../../auth/AuthContext';
import Card from '../../components/Card';
import Pill from '../../components/Pill';
import StatCard from '../../components/StatCard';
import EmptyState from '../../components/EmptyState';
import ErrorInline from '../../components/ErrorInline';

function formatDateTime(value) {
  return new Date(value).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' });
}

function formatDate(value) {
  return new Date(value).toLocaleDateString([], { dateStyle: 'medium' });
}

const STATUS_VARIANT = {
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'urgent',
};

const SCHEDULE_TYPE_VARIANT = {
  HEARING: 'urgent',
  CALENDAR_EVENT: 'info',
};

function SectionCard({ title, count, children }) {
  return (
    <Card>
      <div className="flex items-center justify-between mb-2.5">
        <div className="text-[13px] font-medium text-text-primary">{title}</div>
        {typeof count === 'number' ? (
          <span className="text-[11px] text-text-secondary">{count}</span>
        ) : null}
      </div>
      {children}
    </Card>
  );
}

function Row({ primary, secondary, right }) {
  return (
    <div className="flex items-center justify-between py-2 border-b border-border last:border-b-0">
      <div>
        <div className="text-[12.5px] text-text-primary">{primary}</div>
        {secondary ? (
          <div className="text-[11.5px] text-text-secondary mt-0.5">{secondary}</div>
        ) : null}
      </div>
      {right}
    </div>
  );
}

export default function ManagerDashboard() {
  const { user } = useAuth();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);
      try {
        const [pendingRequests, headcount, disciplinaryCases, pendingLeave, schedule] =
          await Promise.all([
            getPendingRequests(),
            getHeadcount(),
            getDisciplinaryCases(),
            getPendingLeave(),
            getUpcomingSchedule(),
          ]);
        if (!cancelled) {
          setData({ pendingRequests, headcount, disciplinaryCases, pendingLeave, schedule });
        }
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
            <div key={i} className="h-20 bg-surface-1 rounded-lg animate-pulse" />
          ))}
        </div>
      </div>
    );
  }

  if (error) {
    return <ErrorInline message={error} />;
  }

  const { pendingRequests, headcount, disciplinaryCases, pendingLeave, schedule } = data;

  return (
    <div className="space-y-4">
      <Card>
        <div className="flex items-center justify-between flex-wrap gap-2">
          <div>
            <div className="text-[16px] font-semibold text-text-primary">{user.fullName}</div>
            <div className="text-[12px] text-text-secondary mt-0.5">
              {user.position} · {user.department} · {user.branchName}
            </div>
          </div>
          <Pill variant="info">MANAGER</Pill>
        </div>
      </Card>

      <div className="grid grid-cols-2 md:grid-cols-3 gap-2.5">
        <StatCard label="Total employees" value={headcount.totalEmployees} />
        <StatCard label="Active employees" value={headcount.activeEmployees} />
        <StatCard label="Pending requests" value={pendingRequests.length} />
        <StatCard label="Pending leave" value={pendingLeave.length} />
        <StatCard
          label="Open disciplinary cases"
          value={disciplinaryCases.length}
          variant={disciplinaryCases.length > 0 ? 'urgent' : 'neutral'}
        />
        <StatCard label="Upcoming (14 days)" value={schedule.length} />
      </div>

      {Object.keys(headcount.activeByDepartment).length > 0 ? (
        <SectionCard title="Active employees by department">
          <div className="flex flex-wrap gap-1.5">
            {Object.entries(headcount.activeByDepartment).map(([dept, count]) => (
              <Pill key={dept} variant="neutral">
                {dept}: {count}
              </Pill>
            ))}
          </div>
        </SectionCard>
      ) : null}

      <SectionCard title="Upcoming schedule" count={schedule.length}>
        {schedule.length === 0 ? (
          <EmptyState message="Nothing scheduled in the next 14 days." />
        ) : (
          schedule.map((item, i) => (
            <Row
              key={i}
              primary={item.title}
              secondary={
                formatDateTime(item.dateTime) + (item.location ? ` · ${item.location}` : '')
              }
              right={
                <Pill variant={SCHEDULE_TYPE_VARIANT[item.type] || 'neutral'}>
                  {item.type.replace('_', ' ')}
                </Pill>
              }
            />
          ))
        )}
      </SectionCard>

      <SectionCard title="Pending requests" count={pendingRequests.length}>
        {pendingRequests.length === 0 ? (
          <EmptyState message="No pending employee requests." />
        ) : (
          pendingRequests.map((r) => (
            <Row
              key={r.id}
              primary={`${r.employee.fullName} — ${r.requestType.replace('_', ' ')}`}
              secondary={`${r.description} · ${formatDateTime(r.submittedAt)}`}
              right={<Pill variant={STATUS_VARIANT[r.status] || 'neutral'}>{r.status}</Pill>}
            />
          ))
        )}
      </SectionCard>

      <SectionCard title="Pending leave" count={pendingLeave.length}>
        {pendingLeave.length === 0 ? (
          <EmptyState message="No pending leave requests." />
        ) : (
          pendingLeave.map((l) => (
            <Row
              key={l.id}
              primary={`${l.employee.fullName} — ${l.leaveType}`}
              secondary={`${formatDate(l.startDate)} – ${formatDate(l.endDate)} · ${l.reason}`}
              right={<Pill variant={STATUS_VARIANT[l.status] || 'neutral'}>{l.status}</Pill>}
            />
          ))
        )}
      </SectionCard>

      <SectionCard title="Open disciplinary cases" count={disciplinaryCases.length}>
        {disciplinaryCases.length === 0 ? (
          <EmptyState message="No open disciplinary cases." />
        ) : (
          disciplinaryCases.map((c) => (
            <Row
              key={c.id}
              primary={`${c.employee.fullName} — ${c.reason}`}
              secondary={`Opened ${formatDate(c.openedAt)}${
                c.openedByUsername ? ` by ${c.openedByUsername}` : ''
              }`}
              right={<Pill variant="urgent">{c.currentStage.replace('_', ' ')}</Pill>}
            />
          ))
        )}
      </SectionCard>
    </div>
  );
}
