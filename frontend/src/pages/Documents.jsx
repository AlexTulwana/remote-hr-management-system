import { useEffect, useState } from 'react';
import { useAuth } from '../auth/AuthContext';
import { getDocuments, uploadDocument, downloadDocument, deleteDocument } from '../api/documents';
import Card from '../components/Card';
import Pill from '../components/Pill';
import Button from '../components/Button';
import Input from '../components/Input';
import Select from '../components/Select';
import SkeletonRow from '../components/SkeletonRow';
import EmptyState from '../components/EmptyState';
import ErrorInline from '../components/ErrorInline';
import ConfirmDialog from '../components/ConfirmDialog';

const DOCUMENT_TYPES = ['CONTRACT', 'ID_COPY', 'QUALIFICATION', 'DISCIPLINARY', 'CV', 'COVER_LETTER', 'OTHER'];

function formatDate(dateStr) {
  if (!dateStr) return '';
  return new Date(dateStr).toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' });
}

function formatTypeLabel(type) {
  return type
    .toLowerCase()
    .split('_')
    .map((w) => w[0].toUpperCase() + w.slice(1))
    .join(' ');
}

export default function Documents() {
  const { user } = useAuth();
  const isHrOrAdmin = user?.role === 'HR' || user?.role === 'ADMIN';
  const employeeId = user?.employeeId;

  const [documents, setDocuments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [file, setFile] = useState(null);
  const [documentType, setDocumentType] = useState(DOCUMENT_TYPES[0]);
  const [description, setDescription] = useState('');
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState('');

  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);

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
      const data = await getDocuments(employeeId);
      setDocuments(data);
    } catch {
      setError('Could not load documents.');
    } finally {
      setLoading(false);
    }
  }

  async function handleUpload(e) {
    e.preventDefault();
    if (!file) {
      setUploadError('Choose a file first.');
      return;
    }
    setUploading(true);
    setUploadError('');
    try {
      await uploadDocument(employeeId, { file, documentType, description });
      setFile(null);
      setDescription('');
      e.target.reset();
      await load();
    } catch (err) {
      setUploadError(err.message || 'Upload failed.');
    } finally {
      setUploading(false);
    }
  }

  async function handleConfirmDelete() {
    setDeleting(true);
    try {
      await deleteDocument(deleteTarget.id);
      setDeleteTarget(null);
      await load();
    } catch {
      setError('Could not delete document.');
      setDeleteTarget(null);
    } finally {
      setDeleting(false);
    }
  }

  if (!employeeId) {
    return (
      <div>
        <p className="text-[20px] font-medium mb-5">My Documents</p>
        <Card>
          <EmptyState message="No employee record is linked to your account, so there are no documents to show." />
        </Card>
      </div>
    );
  }

  return (
    <div>
      <p className="text-[20px] font-medium mb-5">My Documents</p>

      {isHrOrAdmin ? (
        <Card className="mb-3.5">
          <p className="text-[15px] font-medium mb-3">Upload a document</p>
          <form onSubmit={handleUpload} className="flex flex-col gap-3">
            <div className="flex gap-3">
              <Select
                label="Type"
                value={documentType}
                onChange={(e) => setDocumentType(e.target.value)}
              >
                {DOCUMENT_TYPES.map((t) => (
                  <option key={t} value={t}>
                    {formatTypeLabel(t)}
                  </option>
                ))}
              </Select>
              <Input
                label="Description (optional)"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                className="flex-1"
              />
            </div>
            <input
              type="file"
              accept=".pdf,.jpg,.jpeg,.png,.docx"
              onChange={(e) => setFile(e.target.files[0] || null)}
              className="text-[13px]"
            />
            {uploadError ? <ErrorInline message={uploadError} /> : null}
            <div>
              <Button type="submit" disabled={uploading}>
                {uploading ? 'Uploading…' : 'Upload'}
              </Button>
            </div>
          </form>
        </Card>
      ) : null}

      {loading ? (
        <div className="flex flex-col gap-2">
          <SkeletonRow />
          <SkeletonRow />
          <SkeletonRow />
        </div>
      ) : error ? (
        <ErrorInline message={error} />
      ) : documents.length === 0 ? (
        <Card>
          <EmptyState message="No documents yet." />
        </Card>
      ) : (
        <Card>
          <div className="flex flex-col divide-y divide-border">
            {documents.map((doc) => (
              <div key={doc.id} className="flex items-center justify-between py-3 first:pt-0 last:pb-0">
                <div>
                  <div className="flex items-center gap-2 mb-1">
                    <p className="text-[13px] font-medium">{doc.fileName}</p>
                    <Pill variant="neutral">{formatTypeLabel(doc.documentType)}</Pill>
                  </div>
                  {doc.description ? (
                    <p className="text-[12px] text-text-secondary mb-0.5">{doc.description}</p>
                  ) : null}
                  <p className="text-[11px] text-text-muted">
                    Uploaded by {doc.uploadedByName} · {formatDate(doc.uploadedAt)}
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <Button variant="secondary" onClick={() => downloadDocument(doc.id, doc.fileName)}>
                    Download
                  </Button>
                  {isHrOrAdmin ? (
                    <Button variant="secondary" onClick={() => setDeleteTarget(doc)}>
                      Delete
                    </Button>
                  ) : null}
                </div>
              </div>
            ))}
          </div>
        </Card>
      )}

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title="Delete document?"
        message={deleteTarget ? `"${deleteTarget.fileName}" will be removed. This can't be undone.` : ''}
        confirmLabel={deleting ? 'Deleting…' : 'Delete'}
        onConfirm={handleConfirmDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </div>
  );
}
