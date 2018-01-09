package com.oracle.cloud.compute.jenkins;

import java.io.IOException;

import hudson.model.Descriptor.FormException;
import jenkins.model.Jenkins;

@SuppressWarnings("serial")
public class TestComputeCloudAgent extends ComputeCloudAgent {
    public static class Builder {
        private String numExecutors;
        private String cloudName;
        private String orchName;

        private ComputeCloud cloud;

        public Builder numExecutors(String numExecutors) {
            this.numExecutors = numExecutors;
            return this;
        }

        public Builder numExecutors(int numExecutors) {
            return numExecutors(String.valueOf(numExecutors));
        }

        public Builder cloudName(String cloudName) {
            this.cloudName = cloudName;
            return this;
        }

        public Builder orchName(String orchName) {
            this.orchName = orchName;
            return this;
        }

        public Builder cloud(ComputeCloud cloud) {
            this.cloud = cloud;
            return this;
        }

        private void appendXml(StringBuilder xml, String name, Object value) {
            if (value != null) {
                xml.append("  <").append(name).append(">").append(value).append("</").append(name).append(">\n");
            }
        }

        public TestComputeCloudAgent build() {
            StringBuilder xml = new StringBuilder();
            xml.append("<slave class='").append(TestComputeCloudAgent.class.getName()).append("'>\n");
            appendXml(xml, "numExecutors", numExecutors);
            appendXml(xml, "cloudName", cloudName);
            appendXml(xml, "orchName", orchName);
            xml.append("</slave>");

            TestComputeCloudAgent agent = (TestComputeCloudAgent)Jenkins.XSTREAM2.fromXML(xml.toString());
            agent.cloud = cloud;
            return agent;
        }
    }

    private ComputeCloud cloud;

    protected TestComputeCloudAgent() throws FormException, IOException {
        // Always create an instance by deserializing XML to avoid the Slave
        // constructor, which attempts to initialize nodeProperties using
        // Jenkins.getInstance(), which is null.
        super(
                null, // name
                null, // template
                null, // cloudName
                null, // orchName
                null); // host
        throw new UnsupportedOperationException();
    }

    @Override
    protected Object readResolve() {
        // Override to avoid Slave.readResolve, which attempts to initialize
        // nodeProperties using Jenkins.getInstance(), which is null.
        return this;
    }

    @Override
    public ComputeCloud getCloud() {
        return cloud;
    }
}
