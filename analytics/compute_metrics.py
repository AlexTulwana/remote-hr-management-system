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
    Computed company-wide for the trailing `period_days` window ending at metric_date.
    """
    period_start = metric_date - timedelta(days=period_days)
    period_df = df[(df["snapshot_date"] > period_start) & (df["snapshot_date"] <= metric_date)]

    if period_df.empty:
        return pd.DataFrame()

    leavers = period_df[period_df["employment_status"].isin(["RESIGNED", "TERMINATED"])]
    leavers_count = leavers.groupby("snapshot_date")["headcount"].sum().sum()

    active_by_day = period_df[period_df["employment_status"] == "ACTIVE"].groupby("snapshot_date")["headcount"].sum()
    avg_active = active_by_day.mean() if not active_by_day.empty else 0

    turnover_rate = (leavers_count / avg_active) if avg_active > 0 else 0.0

    return pd.DataFrame([{
        "metric_date": metric_date,
        "branch_id": 0,
        "metric_name": "turnover_rate_30d",
        "metric_value": round(turnover_rate, 4),
    }])


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
