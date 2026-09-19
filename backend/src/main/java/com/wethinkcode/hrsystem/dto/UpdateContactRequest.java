package com.wethinkcode.hrsystem.dto;

import lombok.Data;

@Data
public class UpdateContactRequest {
    private String contactDetails;
    private String email;
}
