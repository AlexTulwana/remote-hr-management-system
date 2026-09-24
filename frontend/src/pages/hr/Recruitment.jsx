import { useEffect, useState } from 'react';
import {
  getAllPostings, createPosting, updatePosting, deletePosting,
  getApplicationsByPosting, viewCv, viewDocument,
  updateApplicationStatus, setApplicationOutcome, updateEmailTemplates,
  getInterviewsByApplication, scheduleInterview, updateInterviewStatus,
} from '../../api/recruitment';
import Card from '../../components/Card';
import Pill from '../../components/Pill';
import Button from '../../components/Button';
import Input from '../../components/Input';
import Select from '../../components/Select';
import SkeletonRow from '../../components/SkeletonRow';
import EmptyState from '../../components/EmptyState';
import ErrorInline from '../../components/ErrorInline';
import ConfirmDialog from '../../components/ConfirmDialog';
import { getBranches } from '../../api/branches';

const STATUS_VARIANT = {
  SUBMITTED: 'info', REVIEWED: 'info', INTERVIEW_SCHEDULED: 'info',
  REJECTED: 'urgent', HIRED: 'success',
};

const EMPTY_FORM = {
  title: '', description: '', requirements: '', department: '',
  startDate: '', endDate: '', maxApplications: '', branchId: '',
};

export default function Recruitment() {
  const [postings, setPostings] = useState([]);
  const [branches, setBranches] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [formOpen, setFormOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [formError, setFormError] = useState('');

  const [expandedPostingId, setExpandedPostingId] = useState(null);
  const [applications, setApplications] = useState([]);
  const [appsLoading, setAppsLoading] = useState(false);

  const [expandedAppId, setExpandedAppId] = useState(null);
  const [interviews, setInterviews] = useState([]);
  const [interviewsLoading, setInterviewsLoading] = useState(false);
  const [interviewForm, setInterviewForm] = useState(null);

  const [copiedId, setCopiedId] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [templateTarget, setTemplateTarget] = useState(null);
  const [templateForm, setTemplateForm] = useState(null);

  function loadPostings() {
    setLoading(true);
    getAllPostings()
      .then(setPostings)
      .catch(() => setError('Could not load job postings.'))
      .finally(() => setLoading(false));
  }

  useEffect(loadPostings, []);
  useEffect(() => { getBranches().then(setBranches).catch(() => {}); }, []);

  function openCreateForm() {
    setForm(EMPTY_FORM);
    setEditingId(null);
    setFormError('');
    setFormOpen(true);
  }

  function openEditForm(p) {
    setForm({
      title: p.title, description: p.description, requirements: p.requirements,
      department: p.department, startDate: p.startDate, endDate: p.endDate,
      maxApplications: p.maxApplications, branchId: p.branchId || '',
    });
    setEditingId(p.id);
    setFormError('');
    setFormOpen(true);
  }

  function submitForm(e) {
    e.preventDefault();
    setFormError('');
    const payload = { ...form, maxApplications: Number(form.maxApplications), branchId: form.branchId ? Number(form.branchId) : null };
    const action = editingId ? updatePosting(editingId, payload) : createPosting(payload);
    action
      .then(() => {
        setFormOpen(false);
        loadPostings();
      })
      .catch((err) => setFormError(err.message));
  }

  function confirmDelete() {
    deletePosting(deleteTarget.id)
      .then(() => {
        setDeleteTarget(null);
        if (expandedPostingId === deleteTarget.id) setExpandedPostingId(null);
        loadPostings();
      })
      .catch((err) => setFormError(err.message));
  }

  function openTemplates(p) {
    setTemplateTarget(p);
    setTemplateForm({
      interviewInviteEmailTemplate: p.interviewInviteEmailTemplate || '',
      acceptedEmailTemplate: p.acceptedEmailTemplate || '',
      rejectedEmailTemplate: p.rejectedEmailTemplate || '',
    });
  }

  function submitTemplates(e) {
    e.preventDefault();
    updateEmailTemplates(templateTarget.id, templateForm)
      .then(() => {
        setTemplateTarget(null);
        loadPostings();
      })
      .catch((err) => setFormError(err.message));
  }

  function togglePosting(id) {
    if (expandedPostingId === id) {
      setExpandedPostingId(null);
      return;
    }
    setExpandedPostingId(id);
    setExpandedAppId(null);
    setAppsLoading(true);
    getApplicationsByPosting(id)
      .then(setApplications)
      .catch(() => setApplications([]))
      .finally(() => setAppsLoading(false));
  }

  function toggleApplication(id) {
    if (expandedAppId === id) {
      setExpandedAppId(null);
      return;
    }
    setExpandedAppId(id);
    setInterviewForm(null);
    setInterviewsLoading(true);
    getInterviewsByApplication(id)
      .then(setInterviews)
      .catch(() => setInterviews([]))
      .finally(() => setInterviewsLoading(false));
  }

  function refreshApplications() {
    if (expandedPostingId) {
      getApplicationsByPosting(expandedPostingId).then(setApplications).catch(() => {});
    }
  }

  function formatDateTime(dt) {
    if (!dt) return '';
    const d = new Date(dt);
    return d.toLocaleString('en-ZA', { dateStyle: 'medium', timeStyle: 'short' });
  }

  function refreshInterviews() {
    if (expandedAppId) {
      getInterviewsByApplication(expandedAppId).then(setInterviews).catch(() => {});
    }
  }

  function handleStatusChange(appId, status) {
    updateApplicationStatus(appId, status).then(refreshApplications);
  }

  function handleOutcome(appId, outcome) {
    const reason = window.prompt(`Reason for ${outcome}?`);
    if (reason === null) return;
    setApplicationOutcome(appId, { outcome, outcomeReason: reason, meetsRequirements: outcome === 'ACCEPTED' })
      .then(refreshApplications);
  }

  function submitInterview(e) {
    e.preventDefault();
    scheduleInterview({ ...interviewForm, applicationId: expandedAppId })
      .then(() => {
        setInterviewForm(null);
        refreshInterviews();
      });
  }

  function copyApplyLink(id) {
    const url = `${window.location.origin}/apply/${id}`;
    navigator.clipboard.writeText(url).then(() => {
      setCopiedId(id);
      setTimeout(() => setCopiedId((current) => (current === id ? null : current)), 2000);
    });
  }

  function handleInterviewStatus(interviewId, status) {
    updateInterviewStatus(interviewId, status, '').then(refreshInterviews);
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-5">
        <p className="text-[20px] font-medium">Recruitment</p>
        <Button variant="primary" onClick={openCreateForm}>New Posting</Button>
      </div>

      {formOpen ? (
        <Card className="mb-3.5">
          <form onSubmit={submitForm} className="flex flex-col gap-3">
            <p className="text-[13px] font-medium">{editingId ? 'Edit posting' : 'New posting'}</p>
            {formError ? <ErrorInline message={formError} /> : null}
            <Input label="Title" required value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} />
            <Input label="Department" required value={form.department} onChange={(e) => setForm({ ...form, department: e.target.value })} />
            <Input label="Description" required value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
            <Input label="Requirements" required value={form.requirements} onChange={(e) => setForm({ ...form, requirements: e.target.value })} />
            <Select label="Branch" value={form.branchId} onChange={(e) => setForm({ ...form, branchId: e.target.value })}>
              <option value="">Company-wide / Remote</option>
              {branches.map((b) => (
                <option key={b.id} value={b.id}>{b.name}</option>
              ))}
            </Select>
            <div className="flex gap-3">
              <Input label="Start date" type="date" required value={form.startDate} onChange={(e) => setForm({ ...form, startDate: e.target.value })} />
              <Input label="End date" type="date" required value={form.endDate} onChange={(e) => setForm({ ...form, endDate: e.target.value })} />
              <Input label="Max applications" type="number" required value={form.maxApplications} onChange={(e) => setForm({ ...form, maxApplications: e.target.value })} />
            </div>
            <div className="flex gap-2 justify-end">
              <Button type="button" variant="secondary" onClick={() => setFormOpen(false)}>Cancel</Button>
              <Button type="submit" variant="primary">{editingId ? 'Save' : 'Create'}</Button>
            </div>
          </form>
        </Card>
      ) : null}

      {loading ? (
        <Card><div className="flex flex-col"><SkeletonRow /><SkeletonRow /></div></Card>
      ) : error ? (
        <ErrorInline message={error} />
      ) : postings.length === 0 ? (
        <Card><EmptyState message="No job postings yet." /></Card>
      ) : (
        <div className="flex flex-col gap-3">
          {postings.map((p) => (
            <Card key={p.id}>
              <div className="flex items-center justify-between gap-3">
                <div className="min-w-0 cursor-pointer" onClick={() => togglePosting(p.id)}>
                  <p className="text-[14px] font-medium">{p.title}</p>
                  <p className="text-[12px] text-text-muted">
                    {p.department} · {p.branchName || 'Company-wide'} · {p.startDate} to {p.endDate} · max {p.maxApplications} · posted by {p.postedByName || '—'}
                  </p>
                </div>
                <div className="flex items-center gap-2 shrink-0">
                  <Button variant="secondary" onClick={() => copyApplyLink(p.id)}>
                    {copiedId === p.id ? 'Copied!' : 'Copy apply link'}
                  </Button>
                  <Button variant="secondary" onClick={() => togglePosting(p.id)}>
                    {expandedPostingId === p.id ? 'Hide applications' : 'View applications'}
                  </Button>
                  <Button variant="secondary" onClick={() => openEditForm(p)}>Edit</Button>
                  <Button variant="secondary" onClick={() => openTemplates(p)}>Email templates</Button>
                  <Button variant="danger" onClick={() => setDeleteTarget(p)}>Delete</Button>
                </div>
              </div>

              {expandedPostingId === p.id ? (
                <div className="mt-3.5 pt-3.5 border-t border-border flex flex-col gap-2.5">
                  {appsLoading ? (
                    <SkeletonRow />
                  ) : applications.length === 0 ? (
                    <EmptyState message="No applications yet." />
                  ) : (
                    applications.map((a) => (
                      <div key={a.id} className="rounded-lg bg-surface-1 p-3">
                        <div className="flex items-center justify-between gap-3">
                          <div className="min-w-0 cursor-pointer" onClick={() => toggleApplication(a.id)}>
                            <p className="text-[13px] font-medium">{a.candidateName}</p>
                            <p className="text-[12px] text-text-muted">
                              {a.candidateEmail} {a.candidatePhone ? `· ${a.candidatePhone}` : ''}
                            </p>
                          </div>
                          <div className="flex items-center gap-2 shrink-0">
                            <Pill variant={STATUS_VARIANT[a.status] || 'neutral'}>{a.status}</Pill>
                            {a.outcome ? <Pill variant={a.outcome === 'ACCEPTED' ? 'success' : 'urgent'}>{a.outcome}</Pill> : null}
                            {a.hasCv ? (
                              <Button variant="secondary" onClick={() => viewCv(a.id)}>CV</Button>
                            ) : null}
                          </div>
                        </div>

                        {expandedAppId === a.id ? (
                          <div className="mt-3 pt-3 border-t border-border flex flex-col gap-3">
                            {a.documents.length > 0 ? (
                              <div className="flex flex-wrap gap-2">
                                {a.documents.map((d) => (
                                  <Button key={d.id} variant="secondary" onClick={() => viewDocument(a.id, d.id)}>
                                    {d.documentType}
                                  </Button>
                                ))}
                              </div>
                            ) : null}

                            <div className="flex flex-wrap items-center gap-2">
                              <Select value={a.status} onChange={(e) => handleStatusChange(a.id, e.target.value)}>
                                {['SUBMITTED', 'REVIEWED', 'INTERVIEW_SCHEDULED', 'REJECTED', 'HIRED'].map((s) => (
                                  <option key={s} value={s}>{s}</option>
                                ))}
                              </Select>
                              {!a.outcome ? (
                                <>
                                  <Button variant="secondary" onClick={() => handleOutcome(a.id, 'ACCEPTED')}>Accept</Button>
                                  <Button variant="danger" onClick={() => handleOutcome(a.id, 'REJECTED')}>Reject</Button>
                                </>
                              ) : null}
                            </div>

                            <div>
                              <div className="flex items-center justify-between mb-2">
                                <p className="text-[12px] font-medium text-text-secondary">Interviews</p>
                                <Button variant="secondary" onClick={() => setInterviewForm({ type: 'ONLINE', interviewDateTime: '', location: '', meetingLink: '' })}>
                                  Schedule interview
                                </Button>
                              </div>

                              {interviewForm ? (
                                <form onSubmit={submitInterview} className="flex flex-col gap-2 mb-2 bg-surface-2 p-3 rounded-lg">
                                  <Select value={interviewForm.type} onChange={(e) => setInterviewForm({ ...interviewForm, type: e.target.value })}>
                                    <option value="ONLINE">Online</option>
                                    <option value="IN_PERSON">In person</option>
                                  </Select>
                                  <Input type="datetime-local" label="Date & time" required
                                    value={interviewForm.interviewDateTime}
                                    onChange={(e) => setInterviewForm({ ...interviewForm, interviewDateTime: e.target.value })} />
                                  {interviewForm.type === 'IN_PERSON' ? (
                                    <Input label="Location" value={interviewForm.location} onChange={(e) => setInterviewForm({ ...interviewForm, location: e.target.value })} />
                                  ) : (
                                    <Input label="Meeting link" value={interviewForm.meetingLink} onChange={(e) => setInterviewForm({ ...interviewForm, meetingLink: e.target.value })} />
                                  )}
                                  <div className="flex gap-2 justify-end">
                                    <Button type="button" variant="secondary" onClick={() => setInterviewForm(null)}>Cancel</Button>
                                    <Button type="submit" variant="primary">Schedule</Button>
                                  </div>
                                </form>
                              ) : null}

                              {interviewsLoading ? (
                                <SkeletonRow />
                              ) : interviews.length === 0 ? (
                                <p className="text-[12px] text-text-muted">No interviews scheduled.</p>
                              ) : (
                                interviews.map((iv) => (
                                  <div key={iv.id} className="flex items-center justify-between py-1.5">
                                    <p className="text-[12px]">
                                      {iv.type} · {formatDateTime(iv.interviewDateTime)} {iv.location ? `· ${iv.location}` : ''} {iv.meetingLink ? `· ${iv.meetingLink}` : ''}
                                    </p>
                                    <Select value={iv.status} onChange={(e) => handleInterviewStatus(iv.id, e.target.value)}>
                                      {['SCHEDULED', 'COMPLETED', 'CANCELLED'].map((s) => (
                                        <option key={s} value={s}>{s}</option>
                                      ))}
                                    </Select>
                                  </div>
                                ))
                              )}
                            </div>
                          </div>
                        ) : null}
                      </div>
                    ))
                  )}
                </div>
              ) : null}
            </Card>
          ))}
        </div>
      )}

      {templateTarget ? (
        <div className="fixed inset-0 bg-black/30 flex items-center justify-center z-50">
          <form onSubmit={submitTemplates} className="bg-surface-2 border border-border rounded-xl p-5 w-[520px] flex flex-col gap-3 max-h-[80vh] overflow-y-auto">
            <p className="text-[15px] font-medium">Email templates — {templateTarget.title}</p>
            <p className="text-[12px] text-text-muted -mt-2">
              Use {'{candidateName}'} and {'{jobTitle}'} as placeholders. Leave blank to use the default message.
            </p>

            <label className="flex flex-col gap-1.5">
              <span className="text-[13px] text-text-secondary">Interview invite email</span>
              <textarea rows={4} className="px-3 py-2 rounded-lg text-[13px] bg-surface-1 border border-border-strong"
                value={templateForm.interviewInviteEmailTemplate}
                onChange={(e) => setTemplateForm({ ...templateForm, interviewInviteEmailTemplate: e.target.value })} />
            </label>

            <label className="flex flex-col gap-1.5">
              <span className="text-[13px] text-text-secondary">Accepted email</span>
              <textarea rows={4} className="px-3 py-2 rounded-lg text-[13px] bg-surface-1 border border-border-strong"
                value={templateForm.acceptedEmailTemplate}
                onChange={(e) => setTemplateForm({ ...templateForm, acceptedEmailTemplate: e.target.value })} />
            </label>

            <label className="flex flex-col gap-1.5">
              <span className="text-[13px] text-text-secondary">Rejected email</span>
              <textarea rows={4} className="px-3 py-2 rounded-lg text-[13px] bg-surface-1 border border-border-strong"
                value={templateForm.rejectedEmailTemplate}
                onChange={(e) => setTemplateForm({ ...templateForm, rejectedEmailTemplate: e.target.value })} />
            </label>

            <div className="flex gap-2 justify-end">
              <Button type="button" variant="secondary" onClick={() => setTemplateTarget(null)}>Cancel</Button>
              <Button type="submit" variant="primary">Save templates</Button>
            </div>
          </form>
        </div>
      ) : null}

      <ConfirmDialog
        open={!!deleteTarget}
        title="Delete posting"
        message={deleteTarget ? `Delete "${deleteTarget.title}"? This cannot be undone.` : ''}
        confirmLabel="Delete"
        onConfirm={confirmDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </div>
  );
}
