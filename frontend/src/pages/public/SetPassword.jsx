import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { resetPassword } from '../../api/passwordReset';

const MIN_LENGTH = 8;

export default function SetPassword() {
  const [params] = useSearchParams();
  const token = params.get('token');

  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [done, setDone] = useState(false);
  const [error, setError] = useState('');

  function handleSubmit(e) {
    e.preventDefault();
    setError('');
    if (password.length < MIN_LENGTH) {
      setError(`Password must be at least ${MIN_LENGTH} characters.`);
      return;
    }
    if (password !== confirm) {
      setError('Passwords do not match.');
      return;
    }
    setSubmitting(true);
    resetPassword(token, password)
      .then(() => setDone(true))
      .catch((err) => setError(err.message))
      .finally(() => setSubmitting(false));
  }

  if (!token) {
    return (
      <div className="min-h-screen flex items-center justify-center p-6 text-text-secondary">
        This link is missing its token. Please use the link from your email.
      </div>
    );
  }

  if (done) {
    return (
      <div className="min-h-screen flex items-center justify-center p-6">
        <div className="max-w-md text-center">
          <p className="text-[20px] font-medium mb-2">Password set</p>
          <p className="text-[14px] text-text-secondary mb-4">You can now log in with your email and new password.</p>
          <Link to="/login" className="text-[14px] underline">
            Go to login
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex justify-center p-6">
      <form onSubmit={handleSubmit} className="max-w-sm w-full py-16 flex flex-col gap-3">
        <p className="text-[22px] font-medium">Set your password</p>
        <p className="text-[13px] text-text-muted mb-2">Choose a password for your HR account.</p>
        {error ? <p className="text-[13px] text-red-500">{error}</p> : null}

        <label className="flex flex-col gap-1.5">
          <span className="text-[13px] text-text-secondary">New password</span>
          <input
            type="password"
            required
            className="h-9 px-3 rounded-lg text-[13px] bg-surface-2 border border-border-strong"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </label>

        <label className="flex flex-col gap-1.5">
          <span className="text-[13px] text-text-secondary">Confirm password</span>
          <input
            type="password"
            required
            className="h-9 px-3 rounded-lg text-[13px] bg-surface-2 border border-border-strong"
            value={confirm}
            onChange={(e) => setConfirm(e.target.value)}
          />
        </label>

        <button
          type="submit"
          disabled={submitting}
          className="h-10 rounded-full bg-text-primary text-surface-2 text-[13px] font-medium mt-2 disabled:opacity-50"
        >
          {submitting ? 'Saving...' : 'Set password'}
        </button>
      </form>
    </div>
  );
}
