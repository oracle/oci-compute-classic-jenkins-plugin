package com.oracle.cloud.compute.jenkins.model;

import java.util.List;
import java.util.Objects;

public class InstanceOrchestration {
    // This is not a real model type.  It is the minimal data needed by the
    // plugin for an orchestration that contains an instance.

    public enum Status {
        // https://docs.oracle.com/cloud/latest/stcomputecs/STCSG/GUID-157BF5DB-BCC9-4492-B018-320A4F88BADB.htm
        starting,
        scheduled,
        ready,
        updating,
        error,
        stopping,
        stopped
    }

    private Status status;
    private String ip;
    private List<String> errors;

    public Status getStatus() {
        return status;
    }

    public InstanceOrchestration status(Status status) {
        this.status = status;
        return this;
    }

    public String getIp() {
        return ip;
    }

    public InstanceOrchestration ip(String ip) {
        this.ip = ip;
        return this;
    }

    public List<String> getErrors() {
        return errors;
    }

    public InstanceOrchestration errors(List<String> errors) {
        this.errors = errors;
        return this;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
                "[status=" + status +
                ", ip=" + ip +
                ']';
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + Objects.hashCode(status);
        result = 31 * result + Objects.hashCode(ip);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || o.getClass() != getClass()) {
            return false;
        }

        InstanceOrchestration i = (InstanceOrchestration)o;
        return Objects.equals(status, i.status) &&
                Objects.equals(ip, i.ip);
    }
}
