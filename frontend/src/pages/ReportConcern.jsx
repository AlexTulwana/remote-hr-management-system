import { useState } from 'react';
import { submitEscalation } from '../api/escalations';
import Card from '../components/Card';
import Button from '../components/Button';
import Select from '../components/Select';
import ErrorInline from '../components/ErrorInline';

export default function ReportConcern() {
  const [type, setType] = useState('EMPLOYEE_COMPLAINT');
  const [reason, setReason] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    if (!reason.trim()) {
      setError('Describe what happened.');
      return;
    }
    setSubmitting(true);
    setError('');
    try {
      await submitEscalation({ type, reason: reason.trim() });
      setSubmitted(true);
      setReason('');
    } catch (err) {
      setError(err.message || 'Could not submit this report.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div>
      <p className="text-[20px] font-medium mb-5">Report a concern</p>
      <Card className="max-w-lg">
        {submitted ? (
          <p className="text-[13px] text-success-text">
            Submitted. HR will review this and follow up if needed.
          </p>
        ) : (
          <form onSubmit={handleSubmit} className="flex flex-col gap-3">
            <Select label="Type" value={type} onChange={(e) => setType(e.target.value)}>
              <option value="EMPLOYEE_COMPLAINT">Employee complaint</option>
              <option value="MANAGER_ESCALATION">Manager escalation</option>
            </Select>
            <div>
              <label className="text-[12px] text-text-secondary block mb-1">What happened?</label>
              <textarea
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                rows={5}
                placeholder="Describe your concern"
                className="w-full rounded-lg border border-border-strong bg-surface-1 text-text-primary p-2.5 text-[13px] focus:outline-none focus:border-text-secondary"
              />
            </div>
            {error ? <ErrorInline message={error} /> : null}
            <Button type="submit" disabled={submitting}>
              {submitting ? 'Submitting…' : 'Submit report'}
            </Button>
          </form>
        )}
      </Card>
    </div>
  );
}
