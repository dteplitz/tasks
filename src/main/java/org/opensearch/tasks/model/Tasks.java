/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.tasks.model;


import java.util.List;

public class Tasks {
    private String id;
    private String title;
    private String description;
    private String status;
    private String assignee;
    private String creationDate;
    private String completionDate;
    private String plannedDate;
    private List<String> tags;
    private String securityStandards;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public String getCompletionDate() {
        return completionDate;
    }

    public void setCompletionDate(String completionDate) {
        this.completionDate = completionDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getPlannedDate() {
        return plannedDate;
    }

    public void setPlannedDate(String plannedDate) {
        this.plannedDate = plannedDate;
    }

    public String getSecurityStandards() {
        return securityStandards;
    }

    public void setSecurityStandards(String securityStandards) {
        this.securityStandards = securityStandards;
    }

    @Override
    public String toString() {
        return "Tasks{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", status='" + status + '\'' +
                ", assignee='" + assignee + '\'' +
                ", creationDate='" + creationDate + '\'' +
                ", completionDate='" + completionDate + '\'' +
                ", plannedDate='" + plannedDate + '\'' +
                ", securityStandards=" + securityStandards +
                ", tags=" + tags +
                '}';
    }
}
