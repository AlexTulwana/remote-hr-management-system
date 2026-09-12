SHELL := /bin/bash

.PHONY: run test test-unit services controllers build package clean analytics kill-8080 check-env

REQUIRED_VARS = DB_PASSWORD GOOGLE_CLIENT_ID GOOGLE_CLIENT_SECRET MAIL_USERNAME MAIL_PASSWORD

check-env:
	@for v in $(REQUIRED_VARS); do \
		if [ -z "$${!v}" ]; then \
			echo "Missing required env var: $$v (see .env.example, then: source .env)"; \
			exit 1; \
		fi; \
	done

run: check-env
	./mvnw spring-boot:run

test: check-env
	./mvnw test

test-unit: check-env
	./mvnw test -Dtest='*ServiceTest,*ControllerTest'

services: check-env
	./mvnw test -Dtest=PayslipServiceTest,ExecutiveDashboardServiceTest,LeaveRequestServiceTest,EmployeeDocumentServiceTest,BranchServiceTest,JobPostingServiceTest,EmployeeRequestServiceTest,DashboardServiceTest,NotificationConsumerTest,OffboardingServiceTest,EscalationServiceTest,MessageServiceTest,EmployeeDashboardServiceTest,HearingServiceTest,HeadcountSnapshotServiceTest,CalendarServiceTest,EmployeeServiceTest,PerformanceReviewServiceTest,DisciplinaryCaseServiceTest,ApplicationServiceTest,EmployeeDirectoryServiceTest,AnnouncementServiceTest,ManagerDashboardServiceTest,InterviewServiceTest,AuthServiceTest,AttendanceServiceTest,RecruitmentAnalyticsServiceTest,HrDashboardServiceTest,RequestAnalyticsServiceTest,OnboardingServiceTest

controllers: check-env
	./mvnw test -Dtest=ApplicationControllerTest,InterviewControllerTest,RequestAnalyticsControllerTest,MessageControllerTest,BranchControllerTest,DisciplinaryCaseControllerTest,ManagerDashboardControllerTest,HearingControllerTest,EmployeeDashboardControllerTest,ExecutiveDashboardControllerTest,AnnouncementControllerTest,AttendanceControllerTest,AuthControllerTest,EmployeeControllerTest,EmployeeDirectoryControllerTest,EmployeeDocumentControllerTest,EmployeeRequestControllerTest,EscalationControllerTest,HrDashboardControllerTest,JobPostingControllerTest,LeaveRequestControllerTest,OffboardingControllerTest,OnboardingControllerTest,PayslipControllerTest,PerformanceReviewControllerTest,RecruitmentAnalyticsControllerTest

build:
	./mvnw clean compile

package:
	./mvnw clean package

clean:
	./mvnw clean

analytics: check-env
	cd ../analytics && source venv/bin/activate && python3 compute_metrics.py

kill-8080:
	-sudo lsof -i :8080
	-sudo fuser -k 8080/tcp
