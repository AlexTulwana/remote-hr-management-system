import { useEffect, useState } from 'react';
import { useAuth } from '../../auth/AuthContext';
import { getMyPayslips, downloadPayslip, viewPayslip, emailPayslip } from '../../api/payslips';
import Card from '../../components/Card';
import Button from '../../components/Button';
import SkeletonRow from '../../components/SkeletonRow';
import EmptyState from '../../components/EmptyState';
import ErrorInline from '../../components/ErrorInline';

function formatPayPeriod(value) {
  const [year, month] = value.split('-');
  if (!month) return value;
  const d = new Date(Number(year), Number(month) - 1, 1);
  return d.toLocaleDateString([], { month: 'long', year: 'numeric' });
}

function formatDate(value) {
  return new Date(value).toLocaleDateString([], { dateStyle: 'medium' });
}

export default function Payslips() {
  const { user } = useAuth();
  const employeeId = user?.employeeId;

  const [payslips, setPayslips] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [emailingId, setEmailingId] = useState(null);
  const [emailStatus, setEmailStatus] = useState({});

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
      const data = await getMyPayslips(employeeId);
      setPayslips(data);
    } catch {
      setError('Could not load your payslips.');
    } finally {
      setLoading(false);
    }
  }

  async function handleEmail(id) {
    setEmailingId(id);
    setEmailStatus((prev) => ({ ...prev, [id]: null }));
    try {
      await emailPayslip(id);
      setEmailStatus((prev) => ({ ...prev, [id]: 'sent' }));
    } catch {
      setEmailStatus((prev) => ({ ...prev, [id]: 'error' }));
    } finally {
      setEmailingId(null);
    }
  }

  if (!employeeId) {
    return (
      <div>
        <p className="text-[20px] font-medium mb-5">Payslips</p>
        <Card>
          <EmptyState message="No employee record is linked to your account, so there are no payslips to show." />
        </Card>
      </div>
    );
  }

  return (
    <div>
      <p className="text-[20px] font-medium mb-5">Payslips</p>

      {loading ? (
        <div className="flex flex-col gap-2">
          <SkeletonRow />
          <SkeletonRow />
          <SkeletonRow />
        </div>
      ) : error ? (
        <ErrorInline message={error} />
      ) : payslips.length === 0 ? (
        <Card>
          <EmptyState message="No payslips have been uploaded yet." />
        </Card>
      ) : (
        <Card>
          <div className="flex flex-col divide-y divide-border">
            {[...payslips]
              .sort((a, b) => (a.payPeriod < b.payPeriod ? 1 : -1))
              .map((p) => (
                <div key={p.id} className="flex items-center justify-between py-3 first:pt-0 last:pb-0">
                  <div>
                    <p className="text-[13px] font-medium">{formatPayPeriod(p.payPeriod)}</p>
                    <p className="text-[11px] text-text-muted mt-0.5">
                      Uploaded {formatDate(p.uploadedDate)}
                    </p>
                    {emailStatus[p.id] === 'sent' ? (
                      <p className="text-[11px] text-success-text mt-1">Emailed to your inbox.</p>
                    ) : null}
                    {emailStatus[p.id] === 'error' ? (
                      <p className="text-[11px] text-danger-text mt-1">Could not send email.</p>
                    ) : null}
                  </div>
                  <div className="flex items-center gap-2">
                    <Button variant="secondary" onClick={() => viewPayslip(p.id)}>
                      View
                    </Button>
                    <Button
                      variant="secondary"
                      onClick={() => downloadPayslip(p.id, `payslip-${p.payPeriod}.pdf`)}
                    >
                      Download
                    </Button>
                    <Button
                      variant="secondary"
                      disabled={emailingId === p.id}
                      onClick={() => handleEmail(p.id)}
                    >
                      {emailingId === p.id ? 'Sending...' : 'Email'}
                    </Button>
                  </div>
                </div>
              ))}
          </div>
        </Card>
      )}
    </div>
  );
}
