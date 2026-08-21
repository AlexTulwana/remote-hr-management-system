"""
Nightly analytics job.
Reads headcount_snapshots (written by the Java scheduled job) and computes
derived metrics (headcount totals, turnover rate) into analytics_results.

Run via cron shortly after the Java snapshot job (e.g. 00:10 daily).
"""

import os
from datetime import date, timedelta
import pandas as pd
from sqlalchemy import create_engine, text

# --- DB connection ---
# Reuses the same DB credentials as the Java app, via env vars.
DB_USER = "hr_app"
DB_PASSWORD = os.environ["DB_PASSWORD"]
DB_HOST = "localhost"
DB_NAME = "hr_system"

engine = create_engine(f"mysql+mysqlconnector://{DB_USER}:{DB_PASSWORD}@{DB_HOST}/{DB_NAME}")


def load_snapshots(start_date: date, end_date: date) -> pd.DataFrame:
    """Load headcount_snapshots for a date range into a DataFrame."""
    query = text("""
        SELECT snapshot_date, branch_id, department, employment_status, headcount
        FROM headcount_snapshots
        WHERE snapshot_date BETWEEN :start AND :end
    """)
    with engine.connect() as conn:
        return pd.read_sql(query, conn, params={"start": start_date, "end": end_date})


def compute_headcount_totals(df: pd.DataFrame, metric_date: date) -> pd.DataFrame:
    """Total active headcount per branch, plus company-wide total, for the given date."""
    today = df[df["snapshot_date"] == metric_date]
    active = today[today["employment_status"] == "ACTIVE"]

    per_branch = active.groupby("branch_id")["headcount"].sum().reset_index()
    per_branch = per_branch.rename(columns={"headcount": "metric_value"})
    per_branch["metric_name"] = "headcount_active_total"

    company_wide = pd.DataFrame([{
        "branch_id": 0,
        "metric_value": active["headcount"].sum(),
        "metric_name": "headcount_active_total",
    }])

    result = pd.concat([per_branch, company_wide], ignore_index=True)
    result["metric_date"] = metric_date
    return result


def compute_turnover_rate(df: pd.DataFrame, metric_date: date, period_days: int = 30) -> pd.DataFrame:
    """
    Turnover rate = (resigned + terminated in period) / average active headcount in period.
    Computed both company-wide (branch_id=0) and per-branch, for the trailing
    `period_days` window ending at metric_date.
    """
    period_start = metric_date - timedelta(days=period_days)
    period_df = df[(df["snapshot_date"] > period_start) & (df["snapshot_date"] <= metric_date)]

    if period_df.empty:
        return pd.DataFrame()

    results = []

    # --- Company-wide (unchanged, branch_id=0) ---
    leavers = period_df[period_df["employment_status"].isin(["RESIGNED", "TERMINATED"])]
    leavers_count = leavers.groupby("snapshot_date")["headcount"].sum().sum()

    active_by_day = period_df[period_df["employment_status"] == "ACTIVE"].groupby("snapshot_date")["headcount"].sum()
    avg_active = active_by_day.mean() if not active_by_day.empty else 0

    company_rate = (leavers_count / avg_active) if avg_active > 0 else 0.0
    results.append({
        "metric_date": metric_date,
        "branch_id": 0,
        "metric_name": "turnover_rate_30d",
        "metric_value": round(company_rate, 4),
    })

    # --- Per-branch ---
    leavers_by_branch = (
        leavers.groupby(["snapshot_date", "branch_id"])["headcount"].sum()
        .groupby("branch_id").sum()
    )
    active_by_branch_day = (
        period_df[period_df["employment_status"] == "ACTIVE"]
        .groupby(["snapshot_date", "branch_id"])["headcount"].sum()
    )
    avg_active_by_branch = active_by_branch_day.groupby("branch_id").mean()

    all_branch_ids = set(leavers_by_branch.index) | set(avg_active_by_branch.index)
    for branch_id in all_branch_ids:
        branch_leavers = leavers_by_branch.get(branch_id, 0)
        branch_avg_active = avg_active_by_branch.get(branch_id, 0)
        branch_rate = (branch_leavers / branch_avg_active) if branch_avg_active > 0 else 0.0
        results.append({
            "metric_date": metric_date,
            "branch_id": int(branch_id),
            "metric_name": "turnover_rate_30d",
            "metric_value": round(branch_rate, 4),
        })

    return pd.DataFrame(results)


def write_results(results: pd.DataFrame):
    """Upsert results into analytics_results, one row at a time (idempotent)."""
    if results.empty:
        print("No results to write.")
        return

    upsert_sql = text("""
        INSERT INTO analytics_results (metric_date, branch_id, metric_name, metric_value, computed_at)
        VALUES (:metric_date, :branch_id, :metric_name, :metric_value, NOW())
        ON DUPLICATE KEY UPDATE metric_value = :metric_value, computed_at = NOW()
    """)

    with engine.begin() as conn:
        for _, row in results.iterrows():
            conn.execute(upsert_sql, {
                "metric_date": row["metric_date"],
                "branch_id": int(row["branch_id"]),
                "metric_name": row["metric_name"],
                "metric_value": float(row["metric_value"]),
            })

    print(f"Wrote {len(results)} metric rows.")


def main():
    today = date.today()
    lookback_start = today - timedelta(days=35)  # enough history for the 30-day turnover window

    df = load_snapshots(lookback_start, today)
    if df.empty:
        print("No snapshot data found - nothing to compute.")
        return

    headcount_results = compute_headcount_totals(df, today)
    turnover_results = compute_turnover_rate(df, today)

    all_results = pd.concat([headcount_results, turnover_results], ignore_index=True)
    write_results(all_results)


if __name__ == "__main__":
    main()
