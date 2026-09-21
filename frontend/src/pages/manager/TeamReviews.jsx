import { useEffect, useState } from 'react';
import { useAuth } from '../../auth/AuthContext';
import { getBranchReviews, createReview } from '../../api/reviews';
import { getDirectory } from '../../api/employeeDirectory';
import Card from '../../components/Card';
import Pill from '../../components/Pill';
import Button from '../../components/Button';
import Select from '../../components/Select';
import SkeletonRow from '../../components/SkeletonRow';
import EmptyState from '../../components/EmptyState';
import ErrorInline from '../../components/ErrorInline';

const CATEGORIES = [
  { key: 'communicationScore', label: 'Communication' },
  { key: 'teamworkScore', label: 'Teamwork' },
  { key: 'productivityScore', label: 'Productivity' },
  { key: 'attendanceScore', label: 'Attendance' },
];

const EMPTY_SCORES = {
  communicationScore: '',
  teamworkScore: '',
  productivityScore: '',
  attendanceScore: '',
};

function averageVariant(average) {
  if (average >= 4) return 'success';
  if (average >= 3) return 'warning';
  return 'urgent';
}

function formatDate(value) {
  if (!value) return '';
  return new Date(value).toLocaleDateString([], { dateStyle: 'medium' });
}

export default function TeamReviews() {
  const { user } = useAuth();
  const branchId = user?.branchId;

  const [reviews, setReviews] = useState([]);
  const [team, setTeam] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [reloadKey, setReloadKey] = useState(0);
  const [filterEmployeeId, setFilterEmployeeId] = useState('');

  const [showForm, setShowForm] = useState(false);
  const [formEmployeeId, setFormEmployeeId] = useState('');
  const [scores, setScores] = useState(EMPTY_SCORES);
  const [comment, setComment] = useState('');
  const [formError, setFormError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!branchId) return undefined;
    let cancelled = false;
    getBranchReviews(branchId)
      .then((data) => {
        if (cancelled) return;
        setReviews(data);
        setError('');
      })
      .catch(() => {
        if (!cancelled) setError('Could not load performance reviews.');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [branchId, reloadKey]);

  useEffect(() => {
    let cancelled = false;
    getDirectory()
      .then((data) => {
        if (!cancelled) setTeam(data);
      })
      .catch(() => {
        if (!cancelled) setTeam([]);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  async function handleSubmit(e) {
    e.preventDefault();
    if (!formEmployeeId) {
      setFormError('Choose an employee.');
      return;
    }
    if (CATEGORIES.some((c) => !scores[c.key])) {
      setFormError('Give a score from 1 to 5 for every category.');
      return;
    }
    setSubmitting(true);
    setFormError('');
    try {
      const payload = { employeeId: Number(formEmployeeId), comment: comment.trim() || null };
      CATEGORIES.forEach((c) => {
        payload[c.key] = Number(scores[c.key]);
      });
      await createReview(payload);
      setShowForm(false);
      setFormEmployeeId('');
      setScores(EMPTY_SCORES);
      setComment('');
      setReloadKey((k) => k + 1);
    } catch (err) {
      setFormError(err.message || 'Could not save this review.');
    } finally {
      setSubmitting(false);
    }
  }

  if (!branchId) {
    return (
      <div>
        <p className="text-[20px] font-medium mb-5">Performance Reviews</p>
        <Card>
          <EmptyState message="No branch is linked to your account, so there are no reviews to show." />
        </Card>
      </div>
    );
  }

  const candidates = team.filter(
    (m) => m.id !== user?.employeeId && m.employmentStatus === 'ACTIVE',
  );

  const reviewed = new Map();
  reviews.forEach((r) => {
    if (r.employee) reviewed.set(String(r.employee.id), r.employee.fullName);
  });

  const visible = reviews
    .filter((r) => !filterEmployeeId || String(r.employee?.id) === filterEmployeeId)
    .sort((a, b) => {
      const byDate = new Date(b.reviewDate) - new Date(a.reviewDate);
      return byDate !== 0 ? byDate : b.id - a.id;
    });

  return (
    <div>
      <div className="flex items-center justify-between mb-5">
        <p className="text-[20px] font-medium">Performance Reviews</p>
        {!showForm ? <Button onClick={() => setShowForm(true)}>Write review</Button> : null}
      </div>

      {showForm ? (
        <Card className="mb-3.5">
          <p className="text-[15px] font-medium mb-3">Write a review</p>
          <form onSubmit={handleSubmit} className="flex flex-col gap-3">
            <Select label="Employee" value={formEmployeeId} onChange={(e) => setFormEmployeeId(e.target.value)}>
              <option value="">Choose an employee</option>
              {candidates.map((m) => (
                <option key={m.id} value={m.id}>
                  {m.fullName}
                </option>
              ))}
            </Select>
            <div className="grid grid-cols-2 gap-3">
              {CATEGORIES.map((c) => (
                <Select
                  key={c.key}
                  label={c.label}
                  value={scores[c.key]}
                  onChange={(e) => setScores((prev) => ({ ...prev, [c.key]: e.target.value }))}
                >
                  <option value="">Score</option>
                  {[1, 2, 3, 4, 5].map((n) => (
                    <option key={n} value={n}>
                      {n}
                    </option>
                  ))}
                </Select>
              ))}
            </div>
            <div>
              <label className="text-[12px] text-text-secondary block mb-1">Comment (optional)</label>
              <textarea
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                rows={3}
                placeholder="How did they do this period?"
                className="w-full rounded-lg border border-border-strong bg-surface-1 text-text-primary p-2.5 text-[13px] focus:outline-none focus:border-text-secondary"
              />
            </div>
            {formError ? <ErrorInline message={formError} /> : null}
            <div className="flex gap-2">
              <Button type="submit" disabled={submitting}>
                {submitting ? 'Saving…' : 'Save review'}
              </Button>
              <Button
                type="button"
                variant="secondary"
                onClick={() => {
                  setShowForm(false);
                  setFormError('');
                }}
              >
                Cancel
              </Button>
            </div>
          </form>
        </Card>
      ) : null}

      {reviewed.size > 0 ? (
        <Card className="mb-3.5">
          <Select label="Show reviews for" value={filterEmployeeId} onChange={(e) => setFilterEmployeeId(e.target.value)}>
            <option value="">Everyone</option>
            {[...reviewed.entries()]
              .sort((a, b) => a[1].localeCompare(b[1]))
              .map(([id, name]) => (
                <option key={id} value={id}>
                  {name}
                </option>
              ))}
          </Select>
        </Card>
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
      ) : visible.length === 0 ? (
        <Card>
          <EmptyState message="No performance reviews yet." />
        </Card>
      ) : (
        <Card>
          <div className="flex flex-col divide-y divide-border">
            {visible.map((r) => (
              <div key={r.id} className="flex items-start justify-between gap-3 py-3 first:pt-0 last:pb-0">
                <div className="min-w-0">
                  <p className="text-[13px] font-medium mb-0.5">{r.employee?.fullName}</p>
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
