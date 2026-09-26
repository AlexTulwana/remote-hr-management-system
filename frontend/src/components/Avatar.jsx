import { useEffect, useState } from 'react';
import { apiFetchBlob } from '../api/client';
import { profilePictureUrl } from '../api/users';

export default function Avatar({ initials, employeeId, hasPicture, size = 'md' }) {
  const [imgUrl, setImgUrl] = useState(null);

  const sizes = {
    sm: 'w-[26px] h-[26px] text-[11px]',
    md: 'w-8 h-8 text-[12px]',
    lg: 'w-16 h-16 text-[20px]',
  };

  useEffect(() => {
    if (!hasPicture || !employeeId) {
      setImgUrl(null);
      return;
    }
    let cancelled = false;
    let objectUrl = null;
    apiFetchBlob(profilePictureUrl(employeeId))
      .then((blob) => {
        if (cancelled) return;
        objectUrl = URL.createObjectURL(blob);
        setImgUrl(objectUrl);
      })
      .catch(() => {
        if (!cancelled) setImgUrl(null);
      });
    return () => {
      cancelled = true;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [employeeId, hasPicture]);

  return (
    <div
      className={[
        'rounded-full bg-surface-1 border border-border flex items-center justify-center font-medium shrink-0 overflow-hidden',
        sizes[size],
      ].join(' ')}
    >
      {imgUrl ? (
        <img src={imgUrl} alt="" className="w-full h-full object-cover" />
      ) : (
        initials
      )}
    </div>
  );
}
