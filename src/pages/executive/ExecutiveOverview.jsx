import { useEffect, useState } from 'react';
import { getExecutiveKpis, getBranchComparison } from '../../api/executiveDashboard';
import Card from '../../components/Card';
import StatCard from '../../components/StatCard';
import Pill from '../../components/Pill';
import EmptyState from '../../components/EmptyState';
import ErrorInline from '../../components/ErrorInline';

function formatCurrency(value) {
  return new Intl.NumberFormat('en-ZA', { style: 'currency', currency: 'ZAR', maximumFractionDigits: 0 }).format(value || 0);
}

function formatPercent(value) {
  return `${(value || 0).toFixed(1)}%`;
}

export default function ExecutiveOverview() {
  const [kpis, setKpis] = useState(null);
  const [branches, setBranches] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);
      try {
        const [kpiData, branchData] = await Promise.all([
          getExecutiveKpis(),
          getBranchComparison(),
        ]);
        if (!cancelled) {
          setKpis(kpiData);
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
  }, []);

  if (loading) {
    return (
      <div className="space-y-4">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-2.5">
          {Array.from({ length: 7 }).map((_, i) => (
            <div key={i} className="h-20 bg-surface-1 rounded-lg animate-pulse" />
          ))}
        </div>
      </div>
    );
  }

  if (error) {
    return <ErrorInline message={error} />;
  }

  return (
    <div className="space-y-4">
      <p className="text-[20px] font-medium">Executive Overview</p>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-2.5">
        <StatCard label="Total employees" value={kpis.totalEmployees} />
        <StatCard label="Active employees" value={kpis.activeEmployees} />
        <StatCard
          label="Turnover (30d)"
          value={formatPercent(kpis.turnoverRate30d)}
          variant={kpis.turnoverRate30d > 10 ? 'urgent' : 'neutral'}
        />
        <StatCard label="Pending items" value={kpis.totalPendingItems} />
        <StatCard label="Applications" value={kpis.totalApplications} />
        <StatCard label="Hired" value={kpis.hiredCount} />
        <StatCard label="Total salary spend" value={formatCurrency(kpis.totalSalarySpend)} />
      </div>

      <Card>
        <p className="text-[13px] font-medium mb-3">Branch comparison</p>
        {branches.length === 0 ? (
          <EmptyState message="No branches yet." />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-[13px]">
              <thead>
                <tr className="text-left text-[12px] text-text-secondary border-b border-border">
                  <th className="pb-2 font-medium">Branch</th>
                  <th className="pb-2 font-medium">Active headcount</th>
                  <th className="pb-2 font-medium">Total salary</th>
                  <th className="pb-2 font-medium">Average salary</th>
                  <th className="pb-2 font-medium">Turnover (30d)</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {branches.map((b) => (
                  <tr key={b.branchId}>
                    <td className="py-2.5 font-medium">{b.branchName}</td>
                    <td className="py-2.5">{b.activeHeadcount}</td>
                    <td className="py-2.5">{formatCurrency(b.totalSalary)}</td>
                    <td className="py-2.5">{formatCurrency(b.averageSalary)}</td>
                    <td className="py-2.5">
                      <Pill variant={b.turnoverRate30d > 10 ? 'urgent' : 'neutral'}>
                        {formatPercent(b.turnoverRate30d)}
                      </Pill>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </div>
  );
}
