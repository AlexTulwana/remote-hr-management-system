import { useState } from 'react';
import { Link } from 'react-router-dom';
import { forgotPassword } from '../../api/passwordReset';

export default function ForgotPassword() {
  const [username, setUsername] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [done, setDone] = useState(false);
  const [error, setError] = useState('');

  function handleSubmit(e) {
    e.preventDefault();
    setError('');
    if (!username.trim()) {
      setError('Enter your username, employee number, or ID number.');
      return;
    }
    setSubmitting(true);
    forgotPassword(username.trim())
      .then(() => setDone(true))
      .catch((err) => setError(err.message))
      .finally(() => setSubmitting(false));
  }

  if (done) {
    return (
      <div className="min-h-screen flex items-center justify-center p-6">
        <div className="max-w-md text-center">
          <p className="text-[20px] font-medium mb-2">Check your email</p>
          <p className="text-[14px] text-text-secondary mb-4">
            If an account matches what you entered, a password reset link has been sent.
            The link is valid for 15 minutes.
          </p>
          <Link to="/login" className="text-[14px] underline">
            Back to login
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex justify-center p-6">
      <form onSubmit={handleSubmit} className="max-w-sm w-full py-16 flex flex-col gap-3">
        <p className="text-[22px] font-medium">Forgot your password?</p>
        <p className="text-[13px] text-text-muted mb-2">
          Enter your username, employee number, or ID number and we'll email you a reset link.
        </p>
        {error ? <p className="text-[13px] text-red-500">{error}</p> : null}

        <label className="flex flex-col gap-1.5">
          <span className="text-[13px] text-text-secondary">Username</span>
          <input
            type="text"
            required
            className="h-9 px-3 rounded-lg text-[13px] bg-surface-2 border border-border-strong"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
          />
        </label>

        <button
          type="submit"
          disabled={submitting}
          className="h-10 rounded-full bg-text-primary text-surface-2 text-[13px] font-medium mt-2 disabled:opacity-50"
        >
          {submitting ? 'Sending...' : 'Send reset link'}
        </button>

        <Link to="/login" className="text-[13px] text-text-muted underline text-center mt-2">
          Back to login
        </Link>
      </form>
    </div>
  );
}
