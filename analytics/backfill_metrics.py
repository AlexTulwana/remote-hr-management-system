"""
Backfills analytics_results for the past N days using the seeded
headcount_snapshots history. Reuses compute_metrics.py's own functions
so the math stays identical to the real nightly job.
"""

from datetime import date, timedelta
import compute_metrics as cm

def main():
    today = date.today()
    lookback_start = today - timedelta(days=65)  # extra buffer for 30d windows

    df = cm.load_snapshots(lookback_start, today)
    if df.empty:
        print("No snapshot data found.")
        return

    for i in range(35, -1, -1):
        metric_date = today - timedelta(days=i)
        headcount_results = cm.compute_headcount_totals(df, metric_date)
        turnover_results = cm.compute_turnover_rate(df, metric_date)
        import pandas as pd
        all_results = pd.concat([headcount_results, turnover_results], ignore_index=True)
        cm.write_results(all_results)

    print("Backfill complete.")

if __name__ == "__main__":
    main()
