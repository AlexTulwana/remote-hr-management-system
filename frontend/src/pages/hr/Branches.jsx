import { useEffect, useState } from 'react';
import { getBranches, createBranch, updateBranch, deleteBranch } from '../../api/branches';
import Card from '../../components/Card';
import Button from '../../components/Button';
import Input from '../../components/Input';
import ConfirmDialog from '../../components/ConfirmDialog';
import SkeletonRow from '../../components/SkeletonRow';
import EmptyState from '../../components/EmptyState';
import ErrorInline from '../../components/ErrorInline';

export default function Branches() {
  const [branches, setBranches] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [newName, setNewName] = useState('');
  const [newAddress, setNewAddress] = useState('');
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState('');

  const [editingId, setEditingId] = useState(null);
  const [editName, setEditName] = useState('');
  const [editAddress, setEditAddress] = useState('');
  const [saving, setSaving] = useState(false);
  const [editError, setEditError] = useState('');

  const [pendingDelete, setPendingDelete] = useState(null);
  const [deleteError, setDeleteError] = useState('');

  useEffect(() => {
    load();
  }, []);

  async function load() {
    setLoading(true);
    setError('');
    try {
      const data = await getBranches();
      setBranches(data);
    } catch {
      setError('Could not load branches.');
    } finally {
      setLoading(false);
    }
  }

  async function handleCreate(e) {
    e.preventDefault();
    if (!newName.trim()) return;
    setCreating(true);
    setCreateError('');
    try {
      await createBranch({ name: newName.trim(), address: newAddress.trim() });
      setNewName('');
      setNewAddress('');
      await load();
    } catch (err) {
      setCreateError(err.message || 'Could not create branch.');
    } finally {
      setCreating(false);
    }
  }

  function startEdit(branch) {
    setEditingId(branch.id);
    setEditName(branch.name);
    setEditAddress(branch.address || '');
    setEditError('');
  }

  function cancelEdit() {
    setEditingId(null);
    setEditError('');
  }

  async function saveEdit(id) {
    if (!editName.trim()) return;
    setSaving(true);
    setEditError('');
    try {
      await updateBranch(id, { name: editName.trim(), address: editAddress.trim() });
      setEditingId(null);
      await load();
    } catch (err) {
      setEditError(err.message || 'Could not update branch.');
    } finally {
      setSaving(false);
    }
  }

  async function confirmDelete() {
    if (!pendingDelete) return;
    setDeleteError('');
    try {
      await deleteBranch(pendingDelete.id);
      setPendingDelete(null);
      await load();
    } catch (err) {
      setPendingDelete(null);
      setDeleteError(err.message || 'Could not delete branch.');
    }
  }

  const sorted = [...branches].sort((a, b) => a.name.localeCompare(b.name));

  return (
    <div>
      <p className="text-[20px] font-medium mb-5">Branches</p>

      <Card className="mb-3.5">
        <p className="text-[13px] font-medium text-text-secondary mb-3">Add branch</p>
        <form onSubmit={handleCreate} className="flex flex-wrap items-end gap-3">
          <Input label="Name" value={newName} onChange={(e) => setNewName(e.target.value)} required />
          <Input label="Address" value={newAddress} onChange={(e) => setNewAddress(e.target.value)} />
          {createError ? <ErrorInline message={createError} /> : null}
          <Button type="submit" disabled={creating}>
            {creating ? 'Adding...' : 'Add branch'}
          </Button>
        </form>
      </Card>

      {deleteError ? (
        <div className="mb-3.5">
          <ErrorInline message={deleteError} />
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
      ) : sorted.length === 0 ? (
        <Card>
          <EmptyState message="No branches yet." />
        </Card>
      ) : (
        <Card>
          <div className="flex flex-col divide-y divide-border">
            {sorted.map((b) => (
              <div key={b.id} className="py-3">
                {editingId === b.id ? (
                  <div className="flex flex-wrap items-end gap-3">
                    <Input label="Name" value={editName} onChange={(e) => setEditName(e.target.value)} required />
                    <Input label="Address" value={editAddress} onChange={(e) => setEditAddress(e.target.value)} />
                    {editError ? <ErrorInline message={editError} /> : null}
                    <div className="flex gap-2">
                      <Button variant="secondary" onClick={cancelEdit} disabled={saving}>
                        Cancel
                      </Button>
                      <Button onClick={() => saveEdit(b.id)} disabled={saving}>
                        {saving ? 'Saving...' : 'Save'}
                      </Button>
                    </div>
                  </div>
                ) : (
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-[13px] font-medium">{b.name}</p>
                      <p className="text-[11px] text-text-muted mt-0.5">{b.address || '—'}</p>
                    </div>
                    <div className="flex items-center gap-2 shrink-0">
                      <Button variant="secondary" onClick={() => startEdit(b)}>
                        Edit
                      </Button>
                      <Button variant="danger" onClick={() => setPendingDelete(b)}>
                        Delete
                      </Button>
                    </div>
                  </div>
                )}
              </div>
            ))}
          </div>
        </Card>
      )}

      <ConfirmDialog
        open={!!pendingDelete}
        title="Delete branch"
        message={pendingDelete ? `Delete ${pendingDelete.name}? This cannot be undone.` : ''}
        confirmLabel="Delete"
        onConfirm={confirmDelete}
        onCancel={() => setPendingDelete(null)}
      />
    </div>
  );
}
