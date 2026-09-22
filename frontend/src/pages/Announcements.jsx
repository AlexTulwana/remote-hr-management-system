import { useEffect, useState } from 'react';
import { useAuth } from '../auth/AuthContext';
import { getActiveAnnouncements, createAnnouncement, deleteAnnouncement } from '../api/announcements';
import { getBranches } from '../api/branches';
import Card from '../components/Card';
import Pill from '../components/Pill';
import Button from '../components/Button';
import Input from '../components/Input';
import Select from '../components/Select';
import PosterImage from '../components/PosterImage';
import SkeletonCard from '../components/SkeletonCard';
import EmptyState from '../components/EmptyState';
import ErrorInline from '../components/ErrorInline';
import ConfirmDialog from '../components/ConfirmDialog';

const CATEGORIES = ['General', 'Vacancy', 'Notice', 'Advert', 'Staff Meeting'];

function formatDate(dateStr) {
  if (!dateStr) return '';
  return new Date(dateStr).toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' });
}

function AnnouncementForm({ onCreated }) {
  const { user } = useAuth();
  const canPost = user?.role === 'MANAGER' || user?.role === 'HR' || user?.role === 'ADMIN';
  const isManager = user?.role === 'MANAGER';

  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [category, setCategory] = useState(CATEGORIES[0]);
  const [expiryDate, setExpiryDate] = useState('');
  const [poster, setPoster] = useState(null);
  const [audience, setAudience] = useState('everyone'); // 'everyone' | 'select'
  const [selectedBranchIds, setSelectedBranchIds] = useState([]);
  const [branches, setBranches] = useState([]);
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState('');

  useEffect(() => {
    if (!canPost || isManager) return;
    getBranches()
      .then(setBranches)
      .catch(() => setBranches([]));
  }, [canPost, isManager]);

  if (!canPost) return null;

  function toggleBranch(id) {
    setSelectedBranchIds((prev) =>
      prev.includes(id) ? prev.filter((b) => b !== id) : [...prev, id],
    );
  }

  async function handleSubmit(e) {
    e.preventDefault();
    if (!title.trim() || !content.trim()) {
      setSubmitError('Add a title and some content.');
      return;
    }
    if (!expiryDate) {
      setSubmitError('Set an expiry date.');
      return;
    }
    if (!isManager && audience === 'select' && selectedBranchIds.length === 0) {
      setSubmitError('Pick at least one branch, or choose Everyone.');
      return;
    }

    let branchIds;
    if (isManager) {
      branchIds = user.branchId ? [user.branchId] : [];
    } else if (audience === 'everyone') {
      branchIds = [];
    } else {
      branchIds = selectedBranchIds;
    }

    setSubmitting(true);
    setSubmitError('');
    try {
      await createAnnouncement({ title, content, category, expiryDate, branchIds, poster });
      setTitle('');
      setContent('');
      setCategory(CATEGORIES[0]);
      setExpiryDate('');
      setPoster(null);
      setAudience('everyone');
      setSelectedBranchIds([]);
      e.target.reset();
      await onCreated();
    } catch (err) {
      setSubmitError(err.message || 'Could not post the announcement.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Card className="mb-3.5">
      <p className="text-[15px] font-medium mb-3">Post an announcement</p>
      <form onSubmit={handleSubmit} className="flex flex-col gap-3">
        <div className="flex gap-3">
          <Input label="Title" value={title} onChange={(e) => setTitle(e.target.value)} />
          <Select label="Category" value={category} onChange={(e) => setCategory(e.target.value)}>
            {CATEGORIES.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </Select>
          <Input
            label="Expiry date"
            type="date"
            value={expiryDate}
            onChange={(e) => setExpiryDate(e.target.value)}
            required
          />
        </div>

        <div>
          <label className="text-[12px] text-text-secondary block mb-1">Content</label>
          <textarea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            rows={3}
            placeholder="What do you want to announce?"
            className="w-full rounded-lg border border-border-strong bg-surface-1 text-text-primary p-2.5 text-[13px] focus:outline-none focus:border-text-secondary"
          />
        </div>

        <div>
          <label className="text-[12px] text-text-secondary block mb-1">Poster image (optional)</label>
          <input
            type="file"
            accept=".jpg,.jpeg,.png"
            onChange={(e) => setPoster(e.target.files[0] || null)}
            className="text-[13px]"
          />
        </div>

        {isManager ? (
          <p className="text-[12px] text-text-muted">
            Posting to your branch{user.branchName ? `: ${user.branchName}` : ''}.
          </p>
        ) : (
          <div>
            <label className="text-[12px] text-text-secondary block mb-1.5">Audience</label>
            <div className="flex gap-2 mb-2">
              <Button
                type="button"
                variant={audience === 'everyone' ? 'primary' : 'secondary'}
                onClick={() => setAudience('everyone')}
              >
                Everyone
              </Button>
              <Button
                type="button"
                variant={audience === 'select' ? 'primary' : 'secondary'}
                onClick={() => setAudience('select')}
              >
                Select branches
              </Button>
            </div>
            {audience === 'select' ? (
              <div className="flex flex-wrap gap-2">
                {branches.map((b) => (
                  <label
                    key={b.id}
                    className="flex items-center gap-1.5 text-[12px] px-2.5 py-1.5 rounded-full border border-border-strong bg-surface-1 cursor-pointer"
                  >
                    <input
                      type="checkbox"
                      checked={selectedBranchIds.includes(b.id)}
                      onChange={() => toggleBranch(b.id)}
                    />
                    {b.name}
                  </label>
                ))}
              </div>
            ) : null}
          </div>
        )}

        {submitError ? <ErrorInline message={submitError} /> : null}
        <div>
          <Button type="submit" disabled={submitting}>
            {submitting ? 'Posting…' : 'Post announcement'}
          </Button>
        </div>
      </form>
    </Card>
  );
}

export default function Announcements() {
  const [announcements, setAnnouncements] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);

  async function load() {
    setLoading(true);
    setError('');
    try {
      const data = await getActiveAnnouncements();
      setAnnouncements(data);
    } catch {
      setError('Could not load announcements.');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  async function handleConfirmDelete() {
    setDeleting(true);
    try {
      await deleteAnnouncement(deleteTarget.id);
      setDeleteTarget(null);
      await load();
    } catch {
      setError('Could not delete announcement.');
      setDeleteTarget(null);
    } finally {
      setDeleting(false);
    }
  }

  return (
    <div>
      <p className="text-[20px] font-medium mb-5">Announcements</p>

      <AnnouncementForm onCreated={load} />

      {loading ? (
        <div className="flex flex-col gap-3.5">
          <SkeletonCard />
          <SkeletonCard />
        </div>
      ) : error ? (
        <ErrorInline message={error} />
      ) : announcements.length === 0 ? (
        <Card>
          <EmptyState message="No announcements right now. Check back later." />
        </Card>
      ) : (
        <div className="flex flex-col gap-3.5">
          {announcements.map((a) => (
            <Card key={a.id}>
              {a.hasPoster ? (
                <PosterImage
                  announcementId={a.id}
                  className="w-full rounded-lg mb-3 max-h-64 object-cover"
                />
              ) : null}
              <div className="flex items-center justify-between mb-1.5">
                <p className="text-[15px] font-medium">{a.title}</p>
                <Pill variant="neutral">{a.category}</Pill>
              </div>
              <p className="text-[13px] text-text-secondary mb-2.5">{a.content}</p>
              <div className="flex items-center justify-between gap-3">
                <p className="text-[11px] text-text-muted">
                  Posted by {a.postedByName} · {formatDate(a.postedDate)}
                  {a.branches && a.branches.length > 0
                    ? ` · ${a.branches.map((b) => b.name).join(', ')}`
                    : ' · Everyone'}
                </p>
                {a.canDelete ? (
                  <Button variant="secondary" onClick={() => setDeleteTarget(a)}>
                    Delete
                  </Button>
                ) : null}
              </div>
            </Card>
          ))}
        </div>
      )}

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title="Delete announcement?"
        message={deleteTarget ? `"${deleteTarget.title}" will be removed. This can't be undone.` : ''}
        confirmLabel={deleting ? 'Deleting…' : 'Delete'}
        onConfirm={handleConfirmDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </div>
  );
}
