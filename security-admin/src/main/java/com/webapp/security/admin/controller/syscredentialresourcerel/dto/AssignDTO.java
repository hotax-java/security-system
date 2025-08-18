package com.webapp.security.admin.controller.syscredentialresourcerel.dto;

import lombok.Data;

import java.util.List;

@Data
public class AssignDTO {
    private List<Long> resourceIds;
}