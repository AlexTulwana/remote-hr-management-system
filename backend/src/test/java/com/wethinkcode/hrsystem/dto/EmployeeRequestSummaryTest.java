package com.wethinkcode.hrsystem.dto;

import com.wethinkcode.hrsystem.model.EmployeeRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmployeeRequestSummaryTest {

    private EmployeeRequest escalated() {
        EmployeeRequest request = new EmployeeRequest();
        request.setId(1L);
        request.setStatus("ESCALATED");
        request.setManagerComment("visible to everyone");
        request.setEscalationComment("HR only note");
        return request;
    }

    @Test
    void from_hidesEscalationComment() {
        EmployeeRequestSummary summary = EmployeeRequestSummary.from(escalated());

        assertThat(summary.getEscalationComment()).isNull();
        assertThat(summary.getManagerComment()).isEqualTo("visible to everyone");
    }

    @Test
    void forStaff_includesEscalationComment() {
        EmployeeRequestSummary summary = EmployeeRequestSummary.forStaff(escalated());

        assertThat(summary.getEscalationComment()).isEqualTo("HR only note");
    }
}
