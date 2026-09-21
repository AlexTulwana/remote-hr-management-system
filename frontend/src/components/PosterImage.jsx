import { useEffect, useState } from 'react';
import { apiFetchBlob } from '../api/client';

export default function PosterImage({ announcementId, className = '' }) {
  const [state, setState] = useState({ id: null, url: null });

  useEffect(() => {
    let cancelled = false;
    let objectUrl = null;
    apiFetchBlob(`/api/announcements/${announcementId}/poster`)
      .then((blob) => {
        if (cancelled) return;
        objectUrl = URL.createObjectURL(blob);
        setState({ id: announcementId, url: objectUrl });
      })
      .catch(() => {
        if (!cancelled) setState({ id: announcementId, url: null });
      });
    return () => {
      cancelled = true;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [announcementId]);

  if (state.id !== announcementId || !state.url) return null;
  return <img src={state.url} alt="" className={className} />;
}
