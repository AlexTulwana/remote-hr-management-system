"""
One-off demo data seeder. NOT part of the app - run manually before a demo.
Seeds: employees.salary, attendance, headcount_snapshots (with a few synthetic
resignations for non-zero turnover), and real payslip PDFs + payslips rows.
"""

import os
import random
import uuid
from datetime import date, datetime, timedelta

from sqlalchemy import create_engine, text
from reportlab.lib.pagesizes import A4
from reportlab.pdfgen import canvas

DB_USER = "hr_app"
DB_PASSWORD = os.environ["DB_PASSWORD"]
DB_HOST = "localhost"
DB_NAME = "hr_system"

engine = create_engine(f"mysql+mysqlconnector://{DB_USER}:{DB_PASSWORD}@{DB_HOST}/{DB_NAME}")

BACKEND_UPLOAD_DIR = os.path.expanduser(
    "~/IdeaProjects/remote-hr-management-system/backend/uploads/payslips"
)

random.seed(42)

SALARY_BANDS = {
    "HR Administrator": (48000, 58000),
    "HR Officer": (30000, 36000),
    "Engineering Manager": (55000, 65000),
    "Branch Manager": (50000, 60000),
    "Software Engineer": (34000, 42000),
    "Support Analyst": (24000, 30000),
    "Web developer": (28000, 34000),
    "Team Member": (18000, 24000),
}
DEFAULT_BAND = (20000, 26000)


def get_employees():
    with engine.connect() as conn:
        rows = conn.execute(text("""
            SELECT id, full_name, employee_number, position, department, branch_id
            FROM employees
            WHERE employment_status = 'ACTIVE'
        """)).mappings().all()
    return list(rows)


def get_branch_names():
    with engine.connect() as conn:
        rows = conn.execute(text("SELECT id, name FROM branches")).mappings().all()
    return {r["id"]: r["name"] for r in rows}


def seed_salaries(employees):
    with engine.begin() as conn:
        for emp in employees:
            low, high = SALARY_BANDS.get(emp["position"], DEFAULT_BAND)
            salary = round(random.uniform(low, high), 2)
            conn.execute(
                text("UPDATE employees SET salary = :salary WHERE id = :id"),
                {"salary": salary, "id": emp["id"]},
            )
    print(f"Seeded salaries for {len(employees)} employees.")


def seed_attendance(employees, weeks=3):
    today = date.today()
    days = []
    d = today
    while len(days) < weeks * 5:
        if d.weekday() < 5:
            days.append(d)
        d -= timedelta(days=1)

    inserted = 0
    with engine.begin() as conn:
        for emp in employees:
            for day in days:
                exists = conn.execute(
                    text("SELECT 1 FROM attendance WHERE employee_id = :eid AND date = :d"),
                    {"eid": emp["id"], "d": day},
                ).first()
                if exists:
                    continue
                clock_in_hour = random.choice([8, 8, 8, 9])
                clock_in_minute = random.randint(0, 45)
                hours = round(random.uniform(7.5, 9.0), 2)
                clock_in = f"{clock_in_hour:02d}:{clock_in_minute:02d}:00"
                total_minutes = clock_in_hour * 60 + clock_in_minute + int(hours * 60)
                clock_out = f"{(total_minutes // 60) % 24:02d}:{total_minutes % 60:02d}:00"
                conn.execute(
                    text("""
                        INSERT INTO attendance (employee_id, date, clock_in, clock_out, hours_worked)
                        VALUES (:eid, :d, :cin, :cout, :hrs)
                    """),
                    {"eid": emp["id"], "d": day, "cin": clock_in, "cout": clock_out, "hrs": hours},
                )
                inserted += 1
    print(f"Seeded {inserted} attendance records.")


def seed_headcount_snapshots(employees, days=35):
    by_branch_dept = {}
    for emp in employees:
        if emp["branch_id"] is None:
            continue
        key = (emp["branch_id"], emp["department"])
        by_branch_dept[key] = by_branch_dept.get(key, 0) + 1

    today = date.today()
    dates = [today - timedelta(days=i) for i in range(days)]

    branches = sorted({b for b, _ in by_branch_dept})
    resignation_events = []
    for branch_id in branches:
        depts = [d for b, d in by_branch_dept if b == branch_id]
        for _ in range(2):
            resignation_events.append({
                "branch_id": branch_id,
                "department": random.choice(depts),
                "date": today - timedelta(days=random.randint(1, 29)),
            })

    inserted = 0
    with engine.begin() as conn:
        for d in dates:
            for (branch_id, department), headcount in by_branch_dept.items():
                conn.execute(
                    text("""
                        INSERT INTO headcount_snapshots
                            (snapshot_date, branch_id, department, employment_status, headcount, created_at)
                        VALUES (:d, :b, :dept, 'ACTIVE', :hc, :now)
                        ON DUPLICATE KEY UPDATE headcount = :hc, created_at = :now
                    """),
                    {"d": d, "b": branch_id, "dept": department, "hc": headcount, "now": datetime.now()},
                )
                inserted += 1
            for event in resignation_events:
                if event["date"] == d:
                    conn.execute(
                        text("""
                            INSERT INTO headcount_snapshots
                                (snapshot_date, branch_id, department, employment_status, headcount, created_at)
                            VALUES (:d, :b, :dept, 'RESIGNED', 1, :now)
                            ON DUPLICATE KEY UPDATE headcount = 1, created_at = :now
                        """),
                        {"d": d, "b": event["branch_id"], "dept": event["department"], "now": datetime.now()},
                    )
                    inserted += 1
    print(f"Seeded {inserted} headcount_snapshot rows across {days} days.")


PAY_PERIODS = ["2026-07", "2026-08", "2026-09"]
MONTH_NAMES = {"07": "July", "08": "August", "09": "September"}


def existing_payslip_periods(employee_id):
    with engine.connect() as conn:
        rows = conn.execute(
            text("SELECT pay_period FROM payslips WHERE employee_id = :eid"),
            {"eid": employee_id},
        ).all()
    return {r[0] for r in rows}


def make_payslip_pdf(path, emp, branch_name, period, salary):
    year, month = period.split("-")
    month_name = MONTH_NAMES.get(month, month)

    c = canvas.Canvas(path, pagesize=A4)
    width, height = A4

    c.setFont("Helvetica-Bold", 16)
    c.drawString(50, height - 60, "Nova HR - Payslip")

    c.setFont("Helvetica", 10)
    c.drawString(50, height - 90, f"Pay period: {month_name} {year}")
    c.line(50, height - 100, width - 50, height - 100)

    c.setFont("Helvetica-Bold", 11)
    c.drawString(50, height - 130, "Employee details")
    c.setFont("Helvetica", 10)
    c.drawString(50, height - 148, f"Name: {emp['full_name']}")
    c.drawString(50, height - 164, f"Employee number: {emp['employee_number']}")
    c.drawString(50, height - 180, f"Position: {emp['position']}")
    c.drawString(50, height - 196, f"Department: {emp['department']}")
    c.drawString(50, height - 212, f"Branch: {branch_name or 'N/A'}")

    gross = salary
    paye = round(gross * 0.18, 2)
    uif = round(gross * 0.01, 2)
    net = round(gross - paye - uif, 2)

    c.setFont("Helvetica-Bold", 11)
    c.drawString(50, height - 250, "Earnings & deductions")
    c.setFont("Helvetica", 10)
    rows = [
        ("Gross salary", gross),
        ("PAYE (18%)", -paye),
        ("UIF (1%)", -uif),
        ("Net pay", net),
    ]
    y = height - 270
    for label, amount in rows:
        c.drawString(50, y, label)
        c.drawRightString(width - 50, y, f"R {amount:,.2f}")
        y -= 18

    c.setFont("Helvetica-Oblique", 8)
    c.drawString(50, 40, "This is a demo-generated payslip for testing purposes.")
    c.save()


def seed_payslips(employees, branch_names):
    os.makedirs(BACKEND_UPLOAD_DIR, exist_ok=True)
    inserted = 0
    with engine.begin() as conn:
        for emp in employees:
            existing = existing_payslip_periods(emp["id"])
            salary_row = conn.execute(
                text("SELECT salary FROM employees WHERE id = :id"), {"id": emp["id"]}
            ).first()
            salary = salary_row[0] if salary_row else 25000
            branch_name = branch_names.get(emp["branch_id"])

            for period in PAY_PERIODS:
                if period in existing:
                    continue
                year, month = period.split("-")
                month_name = MONTH_NAMES.get(month, month)
                file_uuid = uuid.uuid4()
                filename = f"{file_uuid}_{emp['employee_number']}_{month_name}{year}.pdf"
                full_path = os.path.join(BACKEND_UPLOAD_DIR, filename)

                make_payslip_pdf(full_path, emp, branch_name, period, salary)

                conn.execute(
                    text("""
                        INSERT INTO payslips (employee_id, pay_period, file_path, uploaded_date)
                        VALUES (:eid, :period, :path, :uploaded)
                    """),
                    {
                        "eid": emp["id"],
                        "period": period,
                        "path": f"uploads/payslips/{filename}",
                        "uploaded": date(int(year), int(month), 25),
                    },
                )
                inserted += 1
    print(f"Generated {inserted} payslip PDFs + DB rows.")


def main():
    employees = get_employees()
    branch_names = get_branch_names()

    seed_salaries(employees)
    seed_attendance(employees)
    seed_headcount_snapshots(employees)
    seed_payslips(employees, branch_names)

    print("\nDone. Next: run compute_metrics.py, then restart the backend if it's running.")


if __name__ == "__main__":
    main()
