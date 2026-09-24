import { useEffect, useState } from 'react';
import { useAuth } from '../../auth/AuthContext';
import { getEmployeeReviews } from '../../api/reviews';
import Card from '../../components/Card';
import Pill from '../../components/Pill';
import SkeletonRow from '../../components/SkeletonRow';
import EmptyState from '../../components/EmptyState';
import ErrorInline from '../../components/ErrorInline';

const CATEGORIES = [
  { key: 'communicationScore', label: 'Communication' },
  { key: 'teamworkScore', label: 'Teamwork' },
  { key: 'productivityScore', label: 'Productivity' },
  { key: 'attendanceScore', label: 'Attendance' },
];

function averageVariant(average) {
  if (average >= 4) return 'success';
  if (average >= 3) return 'warning';
  return 'urgent';
}

function formatDate(value) {
  if (!value) return '';
  return new Date(value).toLocaleDateString([], { dateStyle: 'medium' });
}

export default function MyReviews() {
  const { user } = useAuth();
  const employeeId = user?.employeeId;

  const [reviews, setReviews] = useState([]);
  const [loading, setLoading] = useState(Boolean(employeeId));
  const [error, setError] = useState('');

  useEffect(() => {
    if (!employeeId) return undefined;
    let cancelled = false;
    getEmployeeReviews(employeeId)
      .then((data) => {
        if (!cancelled) {
          setReviews(data);
          setError('');
        }
      })
      .catch(() => {
        if (!cancelled) setError('Could not load your performance reviews.');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [employeeId]);

  if (!employeeId) {
    return (
      <div>
        <p className="text-[20px] font-medium mb-5">Performance Reviews</p>
        <Card>
          <EmptyState message="No employee profile is linked to your account, so there are no reviews to show." />
        </Card>
      </div>
    );
  }

  const sorted = [...reviews].sort((a, b) => {
    const byDate = new Date(b.reviewDate) - new Date(a.reviewDate);
    return byDate !== 0 ? byDate : b.id - a.id;
  });

  return (
    <div>
      <p className="text-[20px] font-medium mb-5">Performance Reviews</p>

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
      ) : sorted.length === 0 ? (
        <Card>
          <EmptyState message="No performance reviews yet." />
        </Card>
      ) : (
        <Card>
          <div className="flex flex-col divide-y divide-border">
            {sorted.map((r) => (
              <div key={r.id} className="flex items-start justify-between gap-3 py-3 first:pt-0 last:pb-0">
                <div className="min-w-0">
                  <p className="text-[12px] text-text-secondary mb-0.5">
                    {CATEGORIES.map((c) => `${c.label} ${r[c.key]}`).join(' · ')}
                  </p>
                  {r.comment ? <p className="text-[12px] text-text-secondary mb-0.5">{r.comment}</p> : null}
                  <p className="text-[11px] text-text-muted">
                    Reviewed by {r.reviewerName || 'unknown'} on {formatDate(r.reviewDate)}
                  </p>
                </div>
                <div className="shrink-0 whitespace-nowrap">
                  <Pill variant={averageVariant(r.averageScore)}>{r.averageScore.toFixed(1)} / 5</Pill>
                </div>
              </div>
            ))}
          </div>
        </Card>
      )}
    </div>
  );
}
