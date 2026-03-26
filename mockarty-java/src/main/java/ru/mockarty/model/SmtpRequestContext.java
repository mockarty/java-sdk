// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * SMTP request context for matching SMTP mocks.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SmtpRequestContext {

    @JsonProperty("serverName")
    private String serverName;

    @JsonProperty("senderConditions")
    private List<Condition> senderConditions;

    @JsonProperty("recipientConditions")
    private List<Condition> recipientConditions;

    @JsonProperty("subjectConditions")
    private List<Condition> subjectConditions;

    @JsonProperty("bodyConditions")
    private List<Condition> bodyConditions;

    @JsonProperty("headerConditions")
    private List<Condition> headerConditions;

    @JsonProperty("sortArray")
    private Boolean sortArray;

    public SmtpRequestContext() {
    }

    // Builder-style setters

    public SmtpRequestContext serverName(String serverName) {
        this.serverName = serverName;
        return this;
    }

    public SmtpRequestContext senderConditions(List<Condition> senderConditions) {
        this.senderConditions = senderConditions;
        return this;
    }

    public SmtpRequestContext addSenderCondition(Condition condition) {
        if (this.senderConditions == null) {
            this.senderConditions = new ArrayList<>();
        }
        this.senderConditions.add(condition);
        return this;
    }

    public SmtpRequestContext recipientConditions(List<Condition> recipientConditions) {
        this.recipientConditions = recipientConditions;
        return this;
    }

    public SmtpRequestContext addRecipientCondition(Condition condition) {
        if (this.recipientConditions == null) {
            this.recipientConditions = new ArrayList<>();
        }
        this.recipientConditions.add(condition);
        return this;
    }

    public SmtpRequestContext subjectConditions(List<Condition> subjectConditions) {
        this.subjectConditions = subjectConditions;
        return this;
    }

    public SmtpRequestContext addSubjectCondition(Condition condition) {
        if (this.subjectConditions == null) {
            this.subjectConditions = new ArrayList<>();
        }
        this.subjectConditions.add(condition);
        return this;
    }

    public SmtpRequestContext bodyConditions(List<Condition> bodyConditions) {
        this.bodyConditions = bodyConditions;
        return this;
    }

    public SmtpRequestContext addBodyCondition(Condition condition) {
        if (this.bodyConditions == null) {
            this.bodyConditions = new ArrayList<>();
        }
        this.bodyConditions.add(condition);
        return this;
    }

    public SmtpRequestContext headerConditions(List<Condition> headerConditions) {
        this.headerConditions = headerConditions;
        return this;
    }

    public SmtpRequestContext addHeaderCondition(Condition condition) {
        if (this.headerConditions == null) {
            this.headerConditions = new ArrayList<>();
        }
        this.headerConditions.add(condition);
        return this;
    }

    public SmtpRequestContext sortArray(boolean sortArray) {
        this.sortArray = sortArray;
        return this;
    }

    // Getters

    public String getServerName() {
        return serverName;
    }

    public List<Condition> getSenderConditions() {
        return senderConditions;
    }

    public List<Condition> getRecipientConditions() {
        return recipientConditions;
    }

    public List<Condition> getSubjectConditions() {
        return subjectConditions;
    }

    public List<Condition> getBodyConditions() {
        return bodyConditions;
    }

    public List<Condition> getHeaderConditions() {
        return headerConditions;
    }

    public Boolean getSortArray() {
        return sortArray;
    }

    @Override
    public String toString() {
        return "SmtpRequestContext{" +
                "serverName='" + serverName + '\'' +
                '}';
    }
}
