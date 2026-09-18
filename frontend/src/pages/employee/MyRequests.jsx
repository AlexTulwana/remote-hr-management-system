import { useEffect, useState } from 'react';
import { useAuth } from '../../auth/AuthContext';
import { submitEmployeeRequest, getMyEmployeeRequests } from '../../api/employeeRequests';
import Card from '../../components/Card';
import Pill from '../../components/Pill';
import Button from '../../components/Button';
import Select from '../../components/Select';
import SkeletonRow from '../../components/SkeletonRow';
import EmptyState from '../../components/EmptyState';
import ErrorInline from '../../components/ErrorInline';

const REQUEST_TYPES = ['DOCUMENT', 'EQUIPMENT', 'SHIFT_CHANGE', 'REMOTE_WORK', 'OTHER'];

const STATUS_VARIANT = {
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'urgent',
  ESCALATED: 'info',
};

function formatDateTime(value) {
  return new Date(value).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' });
}

function formatTypeLabel(type) {
  return type
    .toLowerCase()
    .split('_')
    .map((w) => w[0].toUpperCase() + w.slice(1))
    .join(' ');
}

export default function MyRequests() {
  const { user } = useAuth();
  const employeeId = user?.employeeId;

  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [requestType, setRequestType] = useState(REQUEST_TYPES[0]);
  const [description, setDescription] = useState('');
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
      const data = await getMyEmployeeRequests(employeeId);
      setRequests(data);
    } catch {
      setError('Could not load your requests.');
    } finally {
      setLoading(false);
    }
  }

  async function handleSubmit(e) {
    e.preventDefault();
    if (!description.trim()) {
      setSubmitError('Describe what you need.');
      return;
    }
    setSubmitting(true);
    setSubmitError('');
    try {
      await submitEmployeeRequest({ requestType, description });
      setDescription('');
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
        <p className="text-[20px] font-medium mb-5">My Requests</p>
        <Card>
          <EmptyState message="No employee record is linked to your account, so you can't submit requests." />
        </Card>
      </div>
    );
  }

  return (
    <div>
      <p className="text-[20px] font-medium mb-5">My Requests</p>

      <Card className="mb-3.5">
        <p className="text-[15px] font-medium mb-3">Submit a request</p>
        <form onSubmit={handleSubmit} className="flex flex-col gap-3">
          <Select label="Type" value={requestType} onChange={(e) => setRequestType(e.target.value)}>
            {REQUEST_TYPES.map((t) => (
              <option key={t} value={t}>
                {formatTypeLabel(t)}
              </option>
            ))}
          </Select>
          <div>
            <label className="text-[12px] text-text-secondary block mb-1">Description</label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={3}
              placeholder="Describe what you need"
              className="w-full rounded-lg border border-border-strong bg-surface-1 text-text-primary p-2.5 text-[13px] focus:outline-none focus:border-text-secondary"
            />
          </div>
          {submitError ? <ErrorInline message={submitError} /> : null}
          <div>
            <Button type="submit" disabled={submitting}>
              {submitting ? 'Submitting...' : 'Submit request'}
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
      ) : requests.length === 0 ? (
        <Card>
          <EmptyState message="You haven't submitted any requests yet." />
        </Card>
      ) : (
        <Card>
          <div className="flex flex-col divide-y divide-border">
            {requests.map((r) => (
              <div key={r.id} className="flex items-center justify-between py-3 first:pt-0 last:pb-0">
                <div>
                  <div className="flex items-center gap-2 mb-1">
                    <p className="text-[13px] font-medium">{formatTypeLabel(r.requestType)}</p>
                  </div>
                  <p className="text-[12px] text-text-secondary mb-0.5">{r.description}</p>
                  <p className="text-[11px] text-text-muted">
                    Submitted {formatDateTime(r.submittedAt)}
                  </p>
                  {r.managerComment ? (
                    <p className="text-[12px] text-text-secondary mt-1">
                      Manager: {r.managerComment}
                    </p>
                  ) : null}
                  {r.hrComment ? (
                    <p className="text-[12px] text-text-secondary mt-1">HR: {r.hrComment}</p>
                  ) : null}
                </div>
                <Pill variant={STATUS_VARIANT[r.status] || 'neutral'}>{r.status}</Pill>
              </div>
            ))}
          </div>
        </Card>
      )}
    </div>
  );
}
