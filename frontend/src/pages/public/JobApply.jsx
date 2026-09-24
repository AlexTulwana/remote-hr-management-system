import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { getPostingById, submitApplication } from '../../api/publicJobs';

export default function JobApply() {
  const { id } = useParams();
  const [posting, setPosting] = useState(null);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);

  const [form, setForm] = useState({ candidateName: '', candidateEmail: '', candidatePhone: '', coverLetter: '' });
  const [cvFile, setCvFile] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    getPostingById(id)
      .then(setPosting)
      .catch(() => setNotFound(true))
      .finally(() => setLoading(false));
  }, [id]);

  function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setSubmitting(true);

    const formData = new FormData();
    formData.append('candidateName', form.candidateName);
    formData.append('candidateEmail', form.candidateEmail);
    if (form.candidatePhone) formData.append('candidatePhone', form.candidatePhone);
    if (form.coverLetter) formData.append('coverLetter', form.coverLetter);
    if (cvFile) formData.append('cv', cvFile);

    submitApplication(id, formData)
      .then(() => setSubmitted(true))
      .catch((err) => setError(err.message))
      .finally(() => setSubmitting(false));
  }

  if (loading) {
    return <div className="min-h-screen flex items-center justify-center text-text-secondary">Loading...</div>;
  }

  if (notFound) {
    return (
      <div className="min-h-screen flex items-center justify-center text-text-secondary">
        This job posting is not available.
      </div>
    );
  }

  if (submitted) {
    return (
      <div className="min-h-screen flex items-center justify-center p-6">
        <div className="max-w-md text-center">
          <p className="text-[20px] font-medium mb-2">Application submitted</p>
          <p className="text-[14px] text-text-secondary">
            Thank you for applying to {posting.title}. We'll be in touch if you're shortlisted.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex justify-center p-6">
      <div className="max-w-xl w-full py-10">
        <p className="text-[22px] font-medium">{posting.title}</p>
        <p className="text-[13px] text-text-muted mb-5">
          {posting.department} {posting.branchName ? `· ${posting.branchName}` : '· Company-wide'} · Closes {posting.endDate}
        </p>

        <div className="mb-6">
          <p className="text-[13px] font-medium text-text-secondary mb-1">Description</p>
          <p className="text-[14px] whitespace-pre-wrap">{posting.description}</p>
        </div>

        <div className="mb-6">
          <p className="text-[13px] font-medium text-text-secondary mb-1">Requirements</p>
          <p className="text-[14px] whitespace-pre-wrap">{posting.requirements}</p>
        </div>

        <form onSubmit={handleSubmit} className="flex flex-col gap-3 border-t border-border pt-5">
          <p className="text-[15px] font-medium">Apply now</p>
          {error ? <p className="text-[13px] text-red-500">{error}</p> : null}

          <label className="flex flex-col gap-1.5">
            <span className="text-[13px] text-text-secondary">Full name</span>
            <input required className="h-9 px-3 rounded-lg text-[13px] bg-surface-2 border border-border-strong"
              value={form.candidateName} onChange={(e) => setForm({ ...form, candidateName: e.target.value })} />
          </label>

          <label className="flex flex-col gap-1.5">
            <span className="text-[13px] text-text-secondary">Email</span>
            <input type="email" required className="h-9 px-3 rounded-lg text-[13px] bg-surface-2 border border-border-strong"
              value={form.candidateEmail} onChange={(e) => setForm({ ...form, candidateEmail: e.target.value })} />
          </label>

          <label className="flex flex-col gap-1.5">
            <span className="text-[13px] text-text-secondary">Phone (optional)</span>
            <input className="h-9 px-3 rounded-lg text-[13px] bg-surface-2 border border-border-strong"
              value={form.candidatePhone} onChange={(e) => setForm({ ...form, candidatePhone: e.target.value })} />
          </label>

          <label className="flex flex-col gap-1.5">
            <span className="text-[13px] text-text-secondary">Cover letter (optional)</span>
            <textarea rows={4} className="px-3 py-2 rounded-lg text-[13px] bg-surface-2 border border-border-strong"
              value={form.coverLetter} onChange={(e) => setForm({ ...form, coverLetter: e.target.value })} />
          </label>

          <label className="flex flex-col gap-1.5">
            <span className="text-[13px] text-text-secondary">CV</span>
            <input type="file" accept=".pdf,.doc,.docx" required
              onChange={(e) => setCvFile(e.target.files[0])} />
          </label>

          <button type="submit" disabled={submitting}
            className="h-10 rounded-full bg-text-primary text-surface-2 text-[13px] font-medium mt-2 disabled:opacity-50">
            {submitting ? 'Submitting...' : 'Submit application'}
          </button>
        </form>
      </div>
    </div>
  );
}
