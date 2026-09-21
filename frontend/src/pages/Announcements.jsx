import { useEffect, useState } from 'react';
import { getActiveAnnouncements } from '../api/announcements';
import Card from '../components/Card';
import Pill from '../components/Pill';
import PosterImage from '../components/PosterImage';
import SkeletonCard from '../components/SkeletonCard';
import EmptyState from '../components/EmptyState';
import ErrorInline from '../components/ErrorInline';

function formatDate(dateStr) {
  if (!dateStr) return '';
  return new Date(dateStr).toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' });
}

export default function Announcements() {
  const [announcements, setAnnouncements] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
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
    load();
  }, []);

  return (
    <div>
      <p className="text-[20px] font-medium mb-5">Announcements</p>

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
              <p className="text-[11px] text-text-muted">
                Posted by {a.postedByName} · {formatDate(a.postedDate)}
                {a.branches && a.branches.length > 0
                  ? ` · ${a.branches.map((b) => b.name).join(', ')}`
                  : ' · Everyone'}
              </p>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
