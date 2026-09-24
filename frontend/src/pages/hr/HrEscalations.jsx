import { useEffect, useState } from 'react';
import {
  getAllEscalations,
  linkEscalationToHearing,
  updateEscalationStatus,
} from '../../api/escalations';
import { getAllHearings } from '../../api/hearings';
import Card from '../../components/Card';
import Pill from '../../components/Pill';
import Button from '../../components/Button';
import Select from '../../components/Select';
import SkeletonRow from '../../components/SkeletonRow';
import EmptyState from '../../components/EmptyState';
import ErrorInline from '../../components/ErrorInline';

const STATUS_LABEL = {
  SUBMITTED: 'Submitted',
  UNDER_REVIEW: 'Under review',
  HEARING_SCHEDULED: 'Hearing scheduled',
  RESOLVED: 'Resolved',
};

const STATUS_VARIANT = {
  SUBMITTED: 'warning',
  UNDER_REVIEW: 'info',
  HEARING_SCHEDULED: 'urgent',
  RESOLVED: 'success',
};

const ALL_STATUSES = Object.keys(STATUS_LABEL);

const TYPE_LABEL = {
  MANAGER_ESCALATION: 'Manager escalation',
  EMPLOYEE_COMPLAINT: 'Employee complaint',
};

function formatDateTime(value) {
  if (!value) return '';
  return new Date(value).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' });
}

function LinkHearingDialog({ target, hearings, submitting, onCancel, onConfirm }) {
  const [hearingId, setHearingId] = useState('');

  if (!target) return null;

  return (
    <div className="fixed inset-0 bg-black/30 flex items-center justify-center z-50">
      <div className="bg-surface-2 border border-border rounded-xl p-5 w-[380px]">
        <p className="text-[15px] font-medium mb-1.5">Link to a hearing</p>
        <p className="text-[13px] text-text-secondary mb-3">
          {target.reporterName} · {TYPE_LABEL[target.type] || target.type}
        </p>
        <Select label="Hearing" value={hearingId} onChange={(e) => setHearingId(e.target.value)}>
          <option value="">Choose a hearing</option>
          {hearings.map((h) => (
            <option key={h.id} value={h.id}>
              {h.employee?.fullName} · {h.caseType} · {formatDateTime(h.hearingDateTime)}
            </option>
          ))}
        </Select>
        <div className="flex justify-end gap-2 mt-4">
          <Button variant="secondary" onClick={onCancel}>
            Cancel
          </Button>
          <Button
            variant="primary"
            disabled={submitting || !hearingId}
            onClick={() => onConfirm(Number(hearingId))}
          >
            {submitting ? 'Linking…' : 'Link hearing'}
          </Button>
        </div>
      </div>
    </div>
  );
}

function EscalationRow({ item, onLinkHearing, onChangeStatus }) {
  return (
    <div className="py-3 first:pt-0 last:pb-0">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <div className="flex items-center gap-2 mb-1">
            <p className="text-[13px] font-medium">{item.reporterName}</p>
            <Pill variant="neutral">{TYPE_LABEL[item.type] || item.type}</Pill>
            <Pill variant={STATUS_VARIANT[item.status] || 'neutral'}>
              {STATUS_LABEL[item.status] || item.status}
            </Pill>
          </div>
          {item.aboutEmployeeName ? (
            <p className="text-[12px] text-text-secondary mb-0.5">About: {item.aboutEmployeeName}</p>
          ) : null}
          <p className="text-[12px] text-text-secondary mb-0.5">{item.reason}</p>
          <p className="text-[11px] text-text-muted">Submitted {formatDateTime(item.submittedAt)}</p>
          {item.linkedHearingId ? (
            <p className="text-[11px] text-text-muted">Linked hearing #{item.linkedHearingId}</p>
          ) : null}
        </div>
        <div className="flex items-center gap-2 shrink-0">
          <Select value={item.status} onChange={(e) => onChangeStatus(item, e.target.value)}>
            {ALL_STATUSES.map((s) => (
              <option key={s} value={s}>
                {STATUS_LABEL[s]}
              </option>
            ))}
          </Select>
          {!item.linkedHearingId ? (
            <Button variant="secondary" onClick={() => onLinkHearing(item)}>
              Link hearing
            </Button>
          ) : null}
        </div>
      </div>
    </div>
  );
}

export default function HrEscalations() {
  const [escalations, setEscalations] = useState([]);
  const [hearings, setHearings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionError, setActionError] = useState('');
  const [reloadKey, setReloadKey] = useState(0);
  const [statusFilter, setStatusFilter] = useState('ALL');

  const [linkTarget, setLinkTarget] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    let cancelled = false;
    getAllEscalations()
      .then((data) => {
        if (cancelled) return;
        setEscalations(data);
        setError('');
      })
      .catch(() => {
        if (!cancelled) setError('Could not load escalations.');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [reloadKey]);

  useEffect(() => {
    let cancelled = false;
    getAllHearings()
      .then((data) => {
        if (!cancelled) setHearings(data);
      })
      .catch(() => {
        if (!cancelled) setHearings([]);
      });
    return () => {
      cancelled = true;
    };
  }, [reloadKey]);

  async function handleLinkHearing(hearingId) {
    setSubmitting(true);
    setActionError('');
    try {
      await linkEscalationToHearing(linkTarget.id, hearingId);
      setLinkTarget(null);
      setReloadKey((k) => k + 1);
    } catch (err) {
      setActionError(err.message || 'Could not link this hearing.');
      setLinkTarget(null);
    } finally {
      setSubmitting(false);
    }
  }

  async function handleChangeStatus(item, status) {
    setActionError('');
    try {
      await updateEscalationStatus(item.id, status);
      setReloadKey((k) => k + 1);
    } catch (err) {
      setActionError(err.message || 'Could not update the status.');
    }
  }

  const filtered =
    statusFilter === 'ALL' ? escalations : escalations.filter((e) => e.status === statusFilter);
  const sorted = [...filtered].sort((a, b) => new Date(b.submittedAt) - new Date(a.submittedAt));

  return (
    <div>
      <div className="flex items-center justify-between mb-5">
        <p className="text-[20px] font-medium">Escalations</p>
        <Select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
          <option value="ALL">All statuses</option>
          {ALL_STATUSES.map((s) => (
            <option key={s} value={s}>
              {STATUS_LABEL[s]}
            </option>
          ))}
        </Select>
      </div>

      {actionError ? (
        <div className="mb-3">
          <ErrorInline message={actionError} />
        </div>
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
      ) : (
        <Card>
          {sorted.length === 0 ? (
            <EmptyState message="No escalations to show." />
          ) : (
            <div className="flex flex-col divide-y divide-border">
              {sorted.map((item) => (
                <EscalationRow
                  key={item.id}
                  item={item}
                  onLinkHearing={setLinkTarget}
                  onChangeStatus={handleChangeStatus}
                />
              ))}
            </div>
          )}
        </Card>
      )}

      <LinkHearingDialog
        key={linkTarget ? linkTarget.id : 'closed'}
        target={linkTarget}
        hearings={hearings}
        submitting={submitting}
        onCancel={() => setLinkTarget(null)}
        onConfirm={handleLinkHearing}
      />
    </div>
  );
}
