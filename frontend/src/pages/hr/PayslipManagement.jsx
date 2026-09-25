import { useEffect, useState } from 'react';
import { getDirectory } from '../../api/employeeDirectory';
import {
  getAllPayslips,
  uploadPayslip,
  bulkUploadPayslips,
  deletePayslip,
  viewPayslip,
  downloadPayslip,
} from '../../api/payslips';
import Card from '../../components/Card';
import Button from '../../components/Button';
import Select from '../../components/Select';
import Input from '../../components/Input';
import Pill from '../../components/Pill';
import ConfirmDialog from '../../components/ConfirmDialog';
import SkeletonRow from '../../components/SkeletonRow';
import EmptyState from '../../components/EmptyState';
import ErrorInline from '../../components/ErrorInline';

function formatPayPeriod(value) {
  if (!value) return '—';
  const [year, month] = value.split('-');
  if (!month) return value;
  const d = new Date(Number(year), Number(month) - 1, 1);
  return d.toLocaleDateString([], { month: 'long', year: 'numeric' });
}

function formatDate(value) {
  return new Date(value).toLocaleDateString([], { dateStyle: 'medium' });
}

export default function PayslipManagement() {
  const [payslips, setPayslips] = useState([]);
  const [employees, setEmployees] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [branch, setBranch] = useState('');
  const [payPeriodFilter, setPayPeriodFilter] = useState('');
  const [search, setSearch] = useState('');

  const [uploadEmployeeId, setUploadEmployeeId] = useState('');
  const [uploadPeriod, setUploadPeriod] = useState('');
  const [uploadFile, setUploadFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState('');

  const [bulkPeriod, setBulkPeriod] = useState('');
  const [bulkFiles, setBulkFiles] = useState([]);
  const [bulkUploading, setBulkUploading] = useState(false);
  const [bulkResults, setBulkResults] = useState(null);
  const [bulkError, setBulkError] = useState('');

  const [pendingDelete, setPendingDelete] = useState(null);

  useEffect(() => {
    loadAll();
  }, []);

  async function loadAll() {
    setLoading(true);
    setError('');
    try {
      const [payslipData, employeeData] = await Promise.all([getAllPayslips(), getDirectory()]);
      setPayslips(payslipData);
      setEmployees(employeeData);
    } catch {
      setError('Could not load payslips.');
    } finally {
      setLoading(false);
    }
  }

  async function handleUpload(e) {
    e.preventDefault();
    if (!uploadEmployeeId || !uploadPeriod || !uploadFile) return;
    setUploading(true);
    setUploadError('');
    try {
      await uploadPayslip(uploadEmployeeId, uploadPeriod, uploadFile);
      setUploadEmployeeId('');
      setUploadPeriod('');
      setUploadFile(null);
      e.target.reset();
      await loadAll();
    } catch {
      setUploadError('Upload failed. Check the file and try again.');
    } finally {
      setUploading(false);
    }
  }

  async function handleBulkUpload(e) {
    e.preventDefault();
    if (!bulkPeriod || bulkFiles.length === 0) return;
    setBulkUploading(true);
    setBulkError('');
    setBulkResults(null);
    try {
      const results = await bulkUploadPayslips(bulkPeriod, bulkFiles);
      setBulkResults(results);
      setBulkPeriod('');
      setBulkFiles([]);
      e.target.reset();
      await loadAll();
    } catch {
      setBulkError('Bulk upload failed to run. Try again.');
    } finally {
      setBulkUploading(false);
    }
  }

  async function confirmDelete() {
    if (!pendingDelete) return;
    try {
      await deletePayslip(pendingDelete.id);
      setPayslips((prev) => prev.filter((p) => p.id !== pendingDelete.id));
    } catch {
      setError('Could not delete that payslip.');
    } finally {
      setPendingDelete(null);
    }
  }

  const branches = [...new Set(payslips.map((p) => p.employee?.branchName).filter(Boolean))].sort();
  const payPeriods = [...new Set(payslips.map((p) => p.payPeriod).filter(Boolean))].sort().reverse();

  const visible = payslips.filter((p) => {
    if (branch && p.employee?.branchName !== branch) return false;
    if (payPeriodFilter && p.payPeriod !== payPeriodFilter) return false;
    if (search) {
      const q = search.toLowerCase();
      const name = (p.employee?.fullName || '').toLowerCase();
      const number = (p.employee?.employeeNumber || '').toLowerCase();
      if (!name.includes(q) && !number.includes(q)) return false;
    }
    return true;
  });

  const sortedEmployees = [...employees].sort((a, b) => a.fullName.localeCompare(b.fullName));

  return (
    <div>
      <p className="text-[20px] font-medium mb-5">Payslip Management</p>

      <div className="grid grid-cols-2 gap-3.5 mb-3.5">
        <Card>
          <p className="text-[13px] font-medium text-text-secondary mb-3">Upload payslip</p>
          <form onSubmit={handleUpload} className="flex flex-col gap-3">
            <Select
              label="Employee"
              value={uploadEmployeeId}
              onChange={(e) => setUploadEmployeeId(e.target.value)}
              required
            >
              <option value="">Select employee</option>
              {sortedEmployees.map((emp) => (
                <option key={emp.id} value={emp.id}>
                  {emp.fullName} ({emp.employeeNumber})
                </option>
              ))}
            </Select>
            <Input
              label="Pay period"
              type="month"
              value={uploadPeriod}
              onChange={(e) => setUploadPeriod(e.target.value)}
              required
            />
            <Input
              label="Payslip file"
              type="file"
              accept="application/pdf"
              onChange={(e) => setUploadFile(e.target.files[0] || null)}
              required
            />
            {uploadError ? <ErrorInline message={uploadError} /> : null}
            <Button type="submit" disabled={uploading}>
              {uploading ? 'Uploading...' : 'Upload'}
            </Button>
          </form>
        </Card>

        <Card>
          <p className="text-[13px] font-medium text-text-secondary mb-1">Bulk upload</p>
          <p className="text-[11px] text-text-muted mb-3">
            Each filename must contain the employee number, e.g. EMP-1001_July2026.pdf
          </p>
          <form onSubmit={handleBulkUpload} className="flex flex-col gap-3">
            <Input
              label="Pay period"
              type="month"
              value={bulkPeriod}
              onChange={(e) => setBulkPeriod(e.target.value)}
              required
            />
            <Input
              label="Payslip files"
              type="file"
              accept="application/pdf"
              multiple
              onChange={(e) => setBulkFiles(Array.from(e.target.files))}
              required
            />
            {bulkError ? <ErrorInline message={bulkError} /> : null}
            <Button type="submit" disabled={bulkUploading}>
              {bulkUploading ? 'Uploading...' : `Upload ${bulkFiles.length || ''} files`.trim()}
            </Button>
          </form>

          {bulkResults ? (
            <div className="mt-3 flex flex-col gap-1.5 max-h-40 overflow-y-auto">
              {bulkResults.map((r, i) => (
                <div key={i} className="flex items-center justify-between text-[11px]">
                  <span className="text-text-secondary truncate mr-2">{r.filename}</span>
                  <Pill variant={r.success ? 'success' : 'urgent'}>
                    {r.success ? 'Uploaded' : r.message}
                  </Pill>
                </div>
              ))}
            </div>
          ) : null}
        </Card>
      </div>

      <Card className="mb-3.5">
        <div className="flex flex-wrap items-end gap-3">
          <Select label="Branch" value={branch} onChange={(e) => setBranch(e.target.value)}>
            <option value="">All branches</option>
            {branches.map((b) => (
              <option key={b} value={b}>
                {b}
              </option>
            ))}
          </Select>
          <Select label="Pay period" value={payPeriodFilter} onChange={(e) => setPayPeriodFilter(e.target.value)}>
            <option value="">All periods</option>
            {payPeriods.map((p) => (
              <option key={p} value={p}>
                {formatPayPeriod(p)}
              </option>
            ))}
          </Select>
          <Input
            label="Search"
            placeholder="Name or employee number"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
          <p className="text-[12px] text-text-muted ml-auto">
            {visible.length} of {payslips.length} payslips
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
      ) : visible.length === 0 ? (
        <Card>
          <EmptyState message="No payslips match these filters." />
        </Card>
      ) : (
        <Card>
          <div className="flex flex-col divide-y divide-border">
            {[...visible]
              .sort((a, b) => (a.payPeriod < b.payPeriod ? 1 : -1))
              .map((p) => (
                <div key={p.id} className="flex items-center justify-between py-3">
                  <div>
                    <p className="text-[13px] font-medium">
                      {p.employee?.fullName || 'Unknown employee'}
                      <span className="text-text-muted font-normal">
                        {' '}
                        · {formatPayPeriod(p.payPeriod)}
                      </span>
                    </p>
                    <p className="text-[11px] text-text-muted mt-0.5">
                      {p.employee?.employeeNumber}
                      {p.employee?.branchName ? ` · ${p.employee.branchName}` : ''}
                      {' · Uploaded '}
                      {formatDate(p.uploadedDate)}
                    </p>
                  </div>
                  <div className="flex items-center gap-2 shrink-0">
                    <Button variant="secondary" onClick={() => viewPayslip(p.id)}>
                      View
                    </Button>
                    <Button
                      variant="secondary"
                      onClick={() => downloadPayslip(p.id, `payslip-${p.payPeriod}.pdf`)}
                    >
                      Download
                    </Button>
                    <Button variant="danger" onClick={() => setPendingDelete(p)}>
                      Delete
                    </Button>
                  </div>
                </div>
              ))}
          </div>
        </Card>
      )}

      <ConfirmDialog
        open={!!pendingDelete}
        title="Delete payslip"
        message={
          pendingDelete
            ? `Delete the ${formatPayPeriod(pendingDelete.payPeriod)} payslip for ${pendingDelete.employee?.fullName || 'this employee'}? This cannot be undone.`
            : ''
        }
        confirmLabel="Delete"
        onConfirm={confirmDelete}
        onCancel={() => setPendingDelete(null)}
      />
    </div>
  );
}
