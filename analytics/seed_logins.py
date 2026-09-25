"""
One-off demo seeder: creates User login accounts for every employee
that doesn't have one yet, so HR/Admin can message anyone company-wide.
Username = employee email (lowercased), password = TestPass123! (same
as existing test accounts), hashed with bcrypt to match Spring's
BCryptPasswordEncoder.
"""

import os
import bcrypt
from sqlalchemy import create_engine, text

DB_USER = "hr_app"
DB_PASSWORD = os.environ["DB_PASSWORD"]
DB_HOST = "localhost"
DB_NAME = "hr_system"

engine = create_engine(f"mysql+mysqlconnector://{DB_USER}:{DB_PASSWORD}@{DB_HOST}/{DB_NAME}")

DEMO_PASSWORD = "TestPass123!"


def infer_role(position):
    p = position.lower()
    if "hr administrator" in p:
        return "ADMIN"
    if "hr officer" in p:
        return "HR"
    if "manager" in p:
        return "MANAGER"
    return "EMPLOYEE"


def main():
    hashed = bcrypt.hashpw(DEMO_PASSWORD.encode(), bcrypt.gensalt()).decode()

    with engine.connect() as conn:
        employees = conn.execute(text("""
            SELECT id, full_name, email, position
            FROM employees
            WHERE id NOT IN (SELECT employee_id FROM users WHERE employee_id IS NOT NULL)
            AND email IS NOT NULL
        """)).mappings().all()

    created = 0
    skipped = []
    with engine.begin() as conn:
        for emp in employees:
            username = emp["email"].strip().lower()
            exists = conn.execute(
                text("SELECT 1 FROM users WHERE username = :u"), {"u": username}
            ).first()
            if exists:
                skipped.append(emp["full_name"])
                continue

            role = infer_role(emp["position"])
            conn.execute(
                text("""
                    INSERT INTO users (username, password, role, employee_id)
                    VALUES (:username, :password, :role, :employee_id)
                """),
                {
                    "username": username,
                    "password": hashed,
                    "role": role,
                    "employee_id": emp["id"],
                },
            )
            created += 1

    print(f"Created {created} login accounts (password: {DEMO_PASSWORD}).")
    if skipped:
        print(f"Skipped {len(skipped)} (username already exists): {skipped}")


if __name__ == "__main__":
    main()
