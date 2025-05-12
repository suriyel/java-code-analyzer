package com.codeanalyzer.api;

/**
 * 项目响应
 */
public class ProjectResponse {
    private String projectId;
    private String status;
    private String message;

    public ProjectResponse() {
    }

    public ProjectResponse(String projectId, String status, String message) {
        this.projectId = projectId;
        this.status = status;
        this.message = message;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
