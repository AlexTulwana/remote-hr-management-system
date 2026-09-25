import { useEffect, useState } from 'react';
import { getTurnoverTrend, getBranchComparison } from '../../api/executiveDashboard';
import Card from '../../components/Card';
import Pill from '../../components/Pill';
import Button from '../../components/Button';
import EmptyState from '../../components/EmptyState';
import ErrorInline from '../../components/ErrorInline';

function formatDate(value) {
  return new Date(value).toLocaleDateString([], { dateStyle: 'medium' });
}

function formatPercent(value) {
  return `${(value || 0).toFixed(1)}%`;
}

const RANGE_OPTIONS = [
  { label: '30d', days: 30 },
  { label: '90d', days: 90 },
  { label: '180d', days: 180 },
  { label: '365d', days: 365 },
];

export default function TurnoverAnalytics() {
  const [days, setDays] = useState(90);
  const [trend, setTrend] = useState([]);
  const [branches, setBranches] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);
      try {
        const [trendData, branchData] = await Promise.all([
          getTurnoverTrend(days),
          getBranchComparison(),
        ]);
        if (!cancelled) {
          setTrend(trendData);
          setBranches(branchData);
        }
      } catch (err) {
        if (!cancelled) setError(err.message);
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, [days]);

  const maxRate = Math.max(1, ...trend.map((t) => t.turnoverRate));

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between flex-wrap gap-2">
        <p className="text-[20px] font-medium">Turnover & Analytics</p>
        <div className="flex gap-1.5">
          {RANGE_OPTIONS.map((opt) => (
            <Button
              key={opt.days}
              variant={days === opt.days ? 'primary' : 'secondary'}
              onClick={() => setDays(opt.days)}
            >
              {opt.label}
            </Button>
          ))}
        </div>
      </div>

      {loading ? (
        <div className="h-40 bg-surface-1 rounded-lg animate-pulse" />
      ) : error ? (
        <ErrorInline message={error} />
      ) : (
        <>
          <Card>
            <p className="text-[13px] font-medium mb-3">
              Company-wide turnover trend ({days} days)
            </p>
            {trend.length === 0 ? (
              <EmptyState message="No turnover data for this period yet." />
            ) : (
              <div className="flex items-end gap-1 h-40 overflow-x-auto">
                {trend.map((point) => (
                  <div
                    key={point.date}
                    className="flex flex-col items-center justify-end shrink-0 w-6 group relative"
                  >
                    <div
                      className="w-full bg-text-primary rounded-t"
                      style={{ height: `${Math.max(2, (point.turnoverRate / maxRate) * 140)}px` }}
                      title={`${formatDate(point.date)}: ${formatPercent(point.turnoverRate)}`}
                    />
                  </div>
                ))}
              </div>
            )}
            {trend.length > 0 ? (
              <div className="flex items-center justify-between mt-2 text-[11px] text-text-muted">
                <span>{formatDate(trend[0].date)}</span>
                <span>{formatDate(trend[trend.length - 1].date)}</span>
              </div>
            ) : null}
          </Card>

          <Card>
            <p className="text-[13px] font-medium mb-3">Turnover by branch (30 days)</p>
            {branches.length === 0 ? (
              <EmptyState message="No branches yet." />
            ) : (
              <div className="flex flex-col divide-y divide-border">
                {[...branches]
                  .sort((a, b) => b.turnoverRate30d - a.turnoverRate30d)
                  .map((b) => (
                    <div key={b.branchId} className="flex items-center justify-between py-2.5">
                      <span className="text-[12.5px] text-text-primary">{b.branchName}</span>
                      <Pill variant={b.turnoverRate30d > 10 ? 'urgent' : 'neutral'}>
                        {formatPercent(b.turnoverRate30d)}
                      </Pill>
                    </div>
                  ))}
              </div>
            )}
          </Card>
        </>
      )}
    </div>
  );
}
