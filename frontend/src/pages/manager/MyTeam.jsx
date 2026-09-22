import { useEffect, useRef, useState } from 'react';
import { useAuth } from '../../auth/AuthContext';
import { getDirectory, getOrgChartFrom } from '../../api/employeeDirectory';
import Card from '../../components/Card';
import Pill from '../../components/Pill';
import Button from '../../components/Button';
import Select from '../../components/Select';
import Avatar from '../../components/Avatar';
import SkeletonRow from '../../components/SkeletonRow';
import EmptyState from '../../components/EmptyState';
import ErrorInline from '../../components/ErrorInline';

const STATUS_VARIANT = {
  ACTIVE: 'success',
  ONBOARDING: 'info',
  RESIGNED: 'warning',
  TERMINATED: 'urgent',
};

function initialsOf(name) {
  if (!name) return '?';
  return name.split(' ').map((p) => p[0]).join('').slice(0, 2).toUpperCase();
}

function OrgNode({ node, depth }) {
  const reports = node.directReports || [];
  return (
    <div>
      <div className="flex items-center gap-2.5 py-1.5" style={{ paddingLeft: depth * 22 }}>
        <Avatar initials={initialsOf(node.fullName)} size="sm" />
        <div>
          <p className="text-[13px]">{node.fullName}</p>
          <p className="text-[11px] text-text-muted">
            {node.position || '—'}
            {node.department ? ` · ${node.department}` : ''}
          </p>
        </div>
      </div>
      {reports.map((child) => (
        <OrgNode key={child.employeeId} node={child} depth={depth + 1} />
      ))}
    </div>
  );
}

export default function MyTeam() {
  const { user } = useAuth();
  const [members, setMembers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [department, setDepartment] = useState('');
  const [status, setStatus] = useState('');
  const [selectedId, setSelectedId] = useState(user?.employeeId ?? null);
  const [treeState, setTreeState] = useState({ id: null, data: null, failed: false });
  const structureRef = useRef(null);

  useEffect(() => {
    let cancelled = false;
    getDirectory()
      .then((data) => {
        if (!cancelled) setMembers(data);
      })
      .catch(() => {
        if (!cancelled) setError('Could not load your team.');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (!selectedId) return undefined;
    let cancelled = false;
    getOrgChartFrom(selectedId)
      .then((data) => {
        if (!cancelled) setTreeState({ id: selectedId, data, failed: false });
      })
      .catch(() => {
        if (!cancelled) setTreeState({ id: selectedId, data: null, failed: true });
      });
    return () => {
      cancelled = true;
    };
  }, [selectedId]);

  const departments = [...new Set(members.map((m) => m.department).filter(Boolean))].sort();
  const statuses = [...new Set(members.map((m) => m.employmentStatus).filter(Boolean))].sort();
  const visible = members.filter(
    (m) =>
      (!department || m.department === department) &&
      (!status || m.employmentStatus === status),
  );

  const treeLoading = selectedId != null && treeState.id !== selectedId;
  const tree = treeState.id === selectedId ? treeState.data : null;
  const treeFailed = treeState.id === selectedId && treeState.failed;

  return (
    <div>
      <p className="text-[20px] font-medium mb-5">My Team</p>

      <Card className="mb-3.5">
        <div className="flex flex-wrap items-end gap-3">
          <Select label="Department" value={department} onChange={(e) => setDepartment(e.target.value)}>
            <option value="">All departments</option>
            {departments.map((d) => (
              <option key={d} value={d}>
                {d}
              </option>
            ))}
          </Select>
          <Select label="Status" value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="">All statuses</option>
            {statuses.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </Select>
          <p className="text-[12px] text-text-muted ml-auto">
            {visible.length} of {members.length} people
          </p>
        </div>
      </Card>

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
      ) : members.length === 0 ? (
        <Card>
          <EmptyState message="There is no one to show for your branch yet." />
        </Card>
      ) : visible.length === 0 ? (
        <Card>
          <EmptyState message="No one matches these filters." />
        </Card>
      ) : (
        <Card>
          <div className="flex flex-col divide-y divide-border">
            {visible.map((m) => (
              <div
                key={m.id}
                className={[
                  'flex items-center justify-between gap-3 py-3 px-2 -mx-2 rounded-lg',
                  selectedId === m.id ? 'bg-surface-1' : '',
                ].join(' ')}
              >
                <div className="flex items-center gap-2.5 min-w-0">
                  <Avatar initials={initialsOf(m.fullName)} size="sm" />
                  <div className="min-w-0">
                    <p className="text-[13px] font-medium">{m.fullName}</p>
                    <p className="text-[12px] text-text-secondary">
                      {m.position || '—'}
                      {m.department ? ` · ${m.department}` : ''}
                    </p>
                    <p className="text-[11px] text-text-muted">
                      {m.reportsToName ? `Reports to ${m.reportsToName}` : 'No manager set'}
                      {m.contactDetails ? ` · ${m.contactDetails}` : ''}
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-2 shrink-0">
                  <Pill variant={STATUS_VARIANT[m.employmentStatus] || 'neutral'}>
                    {m.employmentStatus}
                  </Pill>
                  <Button
                    variant="secondary"
                    onClick={() => {
                      setSelectedId(m.id);
                      requestAnimationFrame(() => {
                        structureRef.current?.scrollIntoView({ behavior: "smooth", block: "start" });
                      });
                    }}
                  >
                    View structure
                  </Button>
                </div>
              </div>
            ))}
          </div>
        </Card>
      )}

      {selectedId ? (
        <div className="mt-3.5" ref={structureRef}>
          <Card>
            <p className="text-[13px] font-medium text-text-secondary mb-3">Team structure</p>
            {treeLoading ? (
              <div className="flex flex-col">
                <SkeletonRow />
                <SkeletonRow />
              </div>
            ) : treeFailed ? (
              <ErrorInline message="Could not load the team structure." />
            ) : tree ? (
              <>
                <OrgNode node={tree} depth={0} />
                {(tree.directReports || []).length === 0 ? (
                  <p className="text-[12px] text-text-muted mt-2">No one reports to {tree.fullName}.</p>
                ) : null}
              </>
            ) : null}
          </Card>
        </div>
      ) : null}
    </div>
  );
}
