import { useState } from 'react';
import { useAuth } from '../auth/AuthContext';
import Card from '../components/Card';
import Avatar from '../components/Avatar';
import Pill from '../components/Pill';
import Input from '../components/Input';
import Button from '../components/Button';
import ConfirmDialog from '../components/ConfirmDialog';
import { useRef } from 'react';
import { updateMyContact, uploadProfilePicture } from '../api/users';

function initialsOf(name) {
  if (!name) return '?';
  return name
    .split(' ')
    .map((part) => part[0])
    .join('')
    .slice(0, 2)
    .toUpperCase();
}

function Field({ label, value }) {
  return (
    <div>
      <p className="text-[12px] text-text-secondary mb-1">{label}</p>
      <p className="text-[13px]">{value || '—'}</p>
    </div>
  );
}

export default function Profile() {
  const { user, updateUser } = useAuth();
  const [editing, setEditing] = useState(false);
  const [contactDetails, setContactDetails] = useState('');
  const [email, setEmail] = useState('');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [pictureError, setPictureError] = useState('');
  const [uploadingPicture, setUploadingPicture] = useState(false);
  const fileInputRef = useRef(null);

  const canEdit = Boolean(user.employeeId);

  async function handlePictureChange(e) {
    const file = e.target.files?.[0];
    if (!file) return;
    setPictureError('');
    if (!['image/jpeg', 'image/png'].includes(file.type)) {
      setPictureError('Only JPG or PNG files are allowed.');
      return;
    }
    if (file.size > 2 * 1024 * 1024) {
      setPictureError('File must be under 2MB.');
      return;
    }
    setUploadingPicture(true);
    try {
      const updated = await uploadProfilePicture(file);
      updateUser(updated);
    } catch (err) {
      setPictureError(err.message || 'Could not upload picture.');
    } finally {
      setUploadingPicture(false);
      e.target.value = '';
    }
  }

  function startEdit() {
    setContactDetails(user.contactDetails || '');
    setEmail(user.email || '');
    setError('');
    setEditing(true);
  }

  function cancelEdit() {
    setEditing(false);
    setConfirmOpen(false);
    setError('');
  }

  async function save() {
    setSaving(true);
    setError('');
    try {
      const updated = await updateMyContact({
        contactDetails: contactDetails.trim(),
        email: email.trim(),
      });
      updateUser(updated);
      setEditing(false);
    } catch (err) {
      setError(err.message || 'Could not save your changes');
    } finally {
      setSaving(false);
      setConfirmOpen(false);
    }
  }

  function handleSave() {
    if (!contactDetails.trim() || !email.trim()) {
      setError('Both email and contact details are required');
      return;
    }
    if (email.trim() !== (user.email || '')) {
      setConfirmOpen(true);
      return;
    }
    save();
  }

  return (
    <div>
      <div className="flex items-center gap-3 mb-5">
        <div className="relative">
          <Avatar
            initials={initialsOf(user.fullName)}
            employeeId={user.employeeId}
            hasPicture={user.hasProfilePicture}
            size="lg"
          />
          {canEdit ? (
            <>
              <input
                ref={fileInputRef}
                type="file"
                accept="image/jpeg,image/png"
                className="hidden"
                onChange={handlePictureChange}
              />
              <button
                onClick={() => fileInputRef.current?.click()}
                disabled={uploadingPicture}
                className="absolute -bottom-1 -right-1 w-6 h-6 rounded-full bg-text-primary text-[11px] flex items-center justify-center border border-border-strong"
                aria-label="Change profile picture"
              >
                ✎
              </button>
            </>
          ) : null}
        </div>
        <div>
          <p className="text-[20px] font-medium">{user.fullName || user.username}</p>
          <Pill variant="neutral">{user.role}</Pill>
          {pictureError ? <p className="text-[12px] mt-1">{pictureError}</p> : null}
          {uploadingPicture ? <p className="text-[12px] text-text-secondary mt-1">Uploading...</p> : null}
        </div>
      </div>

      <Card>
        <p className="text-[13px] font-medium text-text-secondary mb-3.5">Employment details</p>
        <div className="grid grid-cols-2 gap-y-4 gap-x-6">
          <Field label="Employee number" value={user.employeeNumber} />
          <Field label="Employment status" value={user.employmentStatus} />
          <Field label="Position" value={user.position} />
          <Field label="Department" value={user.department} />
          <Field label="Branch" value={user.branchName} />
          <Field label="Username" value={user.username} />
        </div>
      </Card>

      <div className="mt-2.5">
        <Card>
          <div className="flex items-center justify-between mb-3.5">
            <p className="text-[13px] font-medium text-text-secondary">Contact details</p>
            {canEdit && !editing ? (
              <Button variant="secondary" onClick={startEdit}>
                Edit
              </Button>
            ) : null}
          </div>
          {editing ? (
            <div className="flex flex-col gap-3.5">
              <Input
                label="Email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
              <Input
                label="Contact details"
                value={contactDetails}
                onChange={(e) => setContactDetails(e.target.value)}
              />
              {error ? (
                <p className="text-[13px]" role="alert">
                  {error}
                </p>
              ) : null}
              <div className="flex justify-end gap-2">
                <Button variant="secondary" onClick={cancelEdit} disabled={saving}>
                  Cancel
                </Button>
                <Button onClick={handleSave} disabled={saving}>
                  {saving ? 'Saving...' : 'Save'}
                </Button>
              </div>
            </div>
          ) : (
            <div className="grid grid-cols-2 gap-y-4 gap-x-6">
              <Field label="Email" value={user.email} />
              <Field label="Contact details" value={user.contactDetails} />
            </div>
          )}
        </Card>
      </div>

      <ConfirmDialog
        open={confirmOpen}
        title="Change email address?"
        message={`Payslip emails and notifications will go to ${email.trim()} from now on.`}
        confirmLabel="Change email"
        onConfirm={save}
        onCancel={() => setConfirmOpen(false)}
      />
    </div>
  );
}
