import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import ErrorInline from '../components/ErrorInline';

const ROLE_HOME = {
  EMPLOYEE: '/employee',
  MANAGER: '/manager',
  HR: '/hr',
  ADMIN: '/executive',
};

function CometRing() {
  const dashesRef = useRef(null);
  const cometsARef = useRef(null);
  const cometsBRef = useRef(null);

  useEffect(() => {
    const count = 52;
    const step = 360 / count;
    let dashesSvg = '';
    for (let i = 0; i < count; i++) {
      dashesSvg += `<line x1="190" y1="12" x2="190" y2="28" stroke="#33322e" stroke-width="4" stroke-linecap="round" transform="rotate(${step * i} 190 190)"/>`;
    }
    dashesRef.current.innerHTML = dashesSvg;

    function trail(base) {
      const n = 6;
      let c = '';
      for (let t = 0; t < n; t++) {
        const op = 1 - t / n;
        c += `<line x1="190" y1="12" x2="190" y2="28" stroke="#f2f2f0" stroke-width="4" stroke-linecap="round" opacity="${op.toFixed(2)}" transform="rotate(${base - t * step} 190 190)"/>`;
      }
      return c;
    }
    cometsARef.current.innerHTML = trail(0);
    cometsBRef.current.innerHTML = trail(180);
  }, []);

  return (
    <svg width="380" height="380" viewBox="0 0 380 380" className="absolute top-0 left-0">
      <g ref={dashesRef} />
      <g ref={cometsARef} className="origin-[190px_190px] animate-[spin_4s_linear_infinite]" />
      <g ref={cometsBRef} className="origin-[190px_190px] animate-[spin_4s_linear_infinite]" />
    </svg>
  );
}

function SegmentClock() {
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
        const c = on[i] ? '#f2f2f0' : '#2a2a28';
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
      content += '<circle cx="51" cy="12" r="1.5" fill="#6b6a65"/><circle cx="51" cy="24" r="1.5" fill="#6b6a65"/>';
      ref.current.innerHTML = content;
    }

    render();
    const id = setInterval(render, 30000);
    return () => clearInterval(id);
  }, []);

  return <svg ref={ref} width="120" height="36" viewBox="0 0 120 36" role="img" aria-label="Current time" className="absolute top-4 right-4" />;
}

export default function Login() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      const role = await login(username, password);
      navigate(ROLE_HOME[role] || '/');
    } catch {
      setError('Incorrect username or password.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="min-h-screen bg-[#1a1a1a] flex justify-center items-start p-6">
      <div className="bg-[#0d0d0d] text-[#f2f2f0] rounded-xl px-4 pt-5 pb-8 relative w-[420px]">
        <SegmentClock />
        <div className="flex justify-center">
          <div className="relative w-[380px] h-[380px]">
            <CometRing />
            <form onSubmit={handleSubmit} className="relative z-10 w-[210px] mx-auto pt-28 flex flex-col gap-2.5 items-center">
              <p className="text-[15px] font-medium mb-1">Login</p>
              <input
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="Username"
                className="w-full h-9 rounded-full border border-[#33322e] bg-[#161616] text-[#f2f2f0] px-3.5 text-xs placeholder:text-[#6b6a65] focus:outline-none focus:border-[#f2f2f0]"
              />
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Password"
                className="w-full h-9 rounded-full border border-[#33322e] bg-[#161616] text-[#f2f2f0] px-3.5 text-xs placeholder:text-[#6b6a65] focus:outline-none focus:border-[#f2f2f0]"
              />
              {error ? <ErrorInline message={error} /> : null}
              <button
                type="submit"
                disabled={submitting}
                className="w-full h-[38px] rounded-full bg-[#f2f2f0] text-[#111] text-[13px] font-medium transition-colors duration-200 hover:bg-[#a8a6a0] disabled:opacity-60"
              >
                {submitting ? 'Logging in…' : 'Log in'}
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
}
