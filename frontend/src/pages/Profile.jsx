import { useAuth } from '../auth/AuthContext';
import Card from '../components/Card';
import Avatar from '../components/Avatar';
import Pill from '../components/Pill';

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
  const { user } = useAuth();

  return (
    <div>
      <div className="flex items-center gap-3 mb-5">
        <Avatar initials={initialsOf(user.fullName)} size="md" />
        <div>
          <p className="text-[20px] font-medium">{user.fullName || user.username}</p>
          <Pill variant="neutral">{user.role}</Pill>
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
          <p className="text-[13px] font-medium text-text-secondary mb-3.5">Contact details</p>
          <div className="grid grid-cols-2 gap-y-4 gap-x-6">
            <Field label="Email" value={user.email} />
            <Field label="Contact details" value={user.contactDetails} />
          </div>
        </Card>
      </div>
    </div>
  );
}
