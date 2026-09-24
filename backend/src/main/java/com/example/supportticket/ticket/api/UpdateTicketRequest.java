package com.example.supportticket.ticket.api;

import com.example.supportticket.ticket.domain.TicketPriority;

public class UpdateTicketRequest {

    private String title;
    private String description;
    private TicketPriority priority;
    private String assignee;
    private boolean titlePresent;
    private boolean descriptionPresent;
    private boolean priorityPresent;
    private boolean assigneePresent;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.titlePresent = true;
        this.title = trim(title);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.descriptionPresent = true;
        this.description = trim(description);
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public void setPriority(TicketPriority priority) {
        this.priorityPresent = true;
        this.priority = priority;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assigneePresent = true;
        this.assignee = normalizeOptional(assignee);
    }

    public boolean isTitlePresent() {
        return titlePresent;
    }

    public boolean isDescriptionPresent() {
        return descriptionPresent;
    }

    public boolean isPriorityPresent() {
        return priorityPresent;
    }

    public boolean isAssigneePresent() {
        return assigneePresent;
    }

    public boolean hasAnyField() {
        return titlePresent || descriptionPresent || priorityPresent || assigneePresent;
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String normalizeOptional(String value) {
        String normalized = trim(value);
        return normalized == null || normalized.isEmpty() ? null : normalized;
    }
}
