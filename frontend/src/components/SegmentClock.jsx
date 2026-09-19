import { useEffect, useRef } from 'react';

export default function SegmentClock({
  scale = 1,
  onColor = '#f2f2f0',
  offColor = '#2a2a28',
  dotColor = '#6b6a65',
  className = '',
}) {
  const ref = useRef(null);

  useEffect(() => {
    const segMap = {
      0: [1, 1, 1, 0, 1, 1, 1], 1: [0, 0, 1, 0, 0, 1, 0], 2: [1, 0, 1, 1, 1, 0, 1],
      3: [1, 0, 1, 1, 0, 1, 1], 4: [0, 1, 1, 1, 0, 1, 0], 5: [1, 1, 0, 1, 0, 1, 1],
      6: [1, 1, 0, 1, 1, 1, 1], 7: [1, 0, 1, 0, 0, 1, 0], 8: [1, 1, 1, 1, 1, 1, 1],
      9: [1, 1, 1, 1, 0, 1, 1],
    };

    function digitPaths(x, y, on) {
      const w = 11, h = 19;
      const s = [
        [x, y, x + w, y], [x, y, x, y + h / 2], [x + w, y, x + w, y + h / 2],
        [x, y + h / 2, x + w, y + h / 2], [x, y + h / 2, x, y + h],
        [x + w, y + h / 2, x + w, y + h], [x, y + h, x + w, y + h],
      ];
      let o = '';
      for (let i = 0; i < 7; i++) {
        const c = on[i] ? onColor : offColor;
        o += `<line x1="${s[i][0]}" y1="${s[i][1]}" x2="${s[i][2]}" y2="${s[i][3]}" stroke="${c}" stroke-width="2.5" stroke-linecap="round" stroke-dasharray="2 2.5"/>`;
      }
      return o;
    }

    function render() {
      const now = new Date();
      const hh = String(now.getHours()).padStart(2, '0');
      const mm = String(now.getMinutes()).padStart(2, '0');
      const digits = hh + mm;
      const xs = [2, 20, 62, 80];
      let content = '';
      for (let i = 0; i < 4; i++) content += digitPaths(xs[i], 6, segMap[digits[i]]);
      content += `<circle cx="51" cy="12" r="1.5" fill="${dotColor}"/><circle cx="51" cy="24" r="1.5" fill="${dotColor}"/>`;
      ref.current.innerHTML = content;
    }

    render();
    const id = setInterval(render, 30000);
    return () => clearInterval(id);
  }, [onColor, offColor, dotColor]);

  return (
    <svg
      ref={ref}
      width={120 * scale}
      height={36 * scale}
      viewBox="0 0 120 36"
      role="img"
      aria-label="Current time"
      className={className}
    />
  );
}
