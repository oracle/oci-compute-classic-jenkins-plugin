package com.oracle.cloud.compute.jenkins;

import java.io.IOException;
import java.security.UnrecoverableKeyException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.oracle.cloud.compute.jenkins.model.ImageListSourceType;

import hudson.model.Node;
import hudson.model.labels.LabelAtom;
import jenkins.bouncycastle.api.PEMEncodable;

public class TestComputeCloudAgentTemplate extends ComputeCloudAgentTemplate {
    public static class Builder {
        String description;
        String numExecutors;
        Node.Mode mode;
        String labelString;
        String idleTerminationMinutes;
        int templateId;
        String orchDescription;
        String shapeName;
        List<String> securityListNames;
        String imageListSource;
        String imageListName;
        String imageListEntry;
        boolean hypervisorPVEnabled;
        String volumeSize;
        String remoteFS;
        String sshUser;
        String sshConnectTimeoutSeconds;
        String sshKeyName;
        String privateKey;
        String initScript;
        String startTimeoutSeconds;
        String initScriptTimeoutSeconds;

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder numExecutors(String numExecutors) {
            this.numExecutors = numExecutors;
            return this;
        }

        public Builder numExecutors(int numExecutors) {
            return numExecutors(Integer.toString(numExecutors));
        }

        public Builder mode(Node.Mode mode) {
            this.mode = mode;
            return this;
        }

        public Builder labelString(String labelString) {
            this.labelString = labelString;
            return this;
        }

        public Builder idleTerminationMinutes(String idleTerminationMinutes) {
            this.idleTerminationMinutes = idleTerminationMinutes;
            return this;
        }

        public Builder templateId(int templateId) {
            this.templateId = templateId;
            return this;
        }

        public Builder orchDescription(String orchDescription) {
            this.orchDescription = orchDescription;
            return this;
        }

        public Builder shapeName(String shapeName) {
            this.shapeName = shapeName;
            return this;
        }

        public Builder securityListNames(List<String> securityListNames) {
            this.securityListNames = securityListNames;
            return this;
        }

        public Builder securityListNames(String... securityListNames) {
            return securityListNames(Arrays.asList(securityListNames));
        }

        public Builder imageListSource(String imageListSource) {
            this.imageListSource = imageListSource.isEmpty() ? ImageListSourceType.ORACLE_PUBLIC_IMAGE.toString() : imageListSource;
            return this;
        }

        public Builder imageListName(String imageListName) {
            this.imageListName = imageListName;
            return this;
        }

        public Builder iamgeListEntry(String imageListEntry) {
            this.imageListEntry = imageListEntry;
            return this;
        }

        public Builder hypervisorPVEnabled(boolean hypervisorPVEnabled) {
            this.hypervisorPVEnabled = hypervisorPVEnabled;
            return this;
        }

        public Builder volumeSize(String volumeSize) {
            this.volumeSize = volumeSize;
            return this;
        }

        public Builder remoteFS(String remoteFS) {
            this.remoteFS = remoteFS;
            return this;
        }

        public Builder sshUser(String sshUser) {
            this.sshUser = sshUser;
            return this;
        }

        public Builder sshConnectTimeoutSeconds(String sshKeyName) {
            this.sshConnectTimeoutSeconds = sshKeyName;
            return this;
        }

        public Builder sshKeyName(String sshKeyName) {
            this.sshKeyName = sshKeyName;
            return this;
        }

        public Builder privateKey(String privateKey) {
            this.privateKey = privateKey;
            return this;
        }

        public Builder initScript(String initScript) {
            this.initScript = initScript;
            return this;
        }

        public Builder initScriptTimeoutSeconds(String initScriptTimeoutSeconds) {
            this.initScriptTimeoutSeconds = initScriptTimeoutSeconds;
            return this;
        }

        public Builder startTimeoutSeconds(String startTimeoutSeconds) {
            this.startTimeoutSeconds = startTimeoutSeconds;
            return this;
        }

        public TestComputeCloudAgentTemplate build() {
            return new TestComputeCloudAgentTemplate(this);
        }
    }

    public TestComputeCloudAgentTemplate() {
        this(new Builder());
    }

    public TestComputeCloudAgentTemplate(Builder builder) {
        super(
                builder.description,
                builder.numExecutors,
                builder.mode,
                builder.labelString,
                builder.idleTerminationMinutes,
                builder.templateId,
                builder.orchDescription,
                builder.shapeName,
                // TODO: Pass the full list.
                builder.securityListNames == null || builder.securityListNames.isEmpty() ? null : builder.securityListNames,
                builder.imageListSource == null || builder.imageListSource.isEmpty() ? ImageListSourceType.ORACLE_PUBLIC_IMAGE.toString() : builder.imageListSource,
                builder.imageListName,
                builder.imageListEntry,
                builder.hypervisorPVEnabled,
                builder.volumeSize,
                builder.remoteFS,
                builder.sshUser,
                builder.sshConnectTimeoutSeconds,
                builder.sshKeyName,
                builder.privateKey,
                builder.initScript,
                builder.startTimeoutSeconds,
                builder.initScriptTimeoutSeconds);
    }

    @Override
    Collection<LabelAtom> parseLabels(String strings) {
        return ComputeCloudTestUtils.parseLabels(strings);
    }

    public static class TestDescriptor extends DescriptorImpl {
        public static class Builder {
            ComputeCloud.DescriptorImpl cloudDescriptor;
            PEMDecoder pemDecoder;

            public Builder cloudDescriptor(ComputeCloud.DescriptorImpl cloudDescriptor) {
                this.cloudDescriptor = cloudDescriptor;
                return this;
            }

            public Builder pemDecoder(PEMDecoder pemDecoder) {
                this.pemDecoder = pemDecoder;
                return this;
            }

            public TestDescriptor build() {
                return new TestDescriptor(this);
            }
        }

        public interface PEMDecoder {
            PEMEncodable decode(String pem) throws UnrecoverableKeyException, IOException;
        }

        private final ComputeCloud.DescriptorImpl cloudDescriptor;
        private final PEMDecoder pemDecoder;

        public TestDescriptor(Builder builder) {
            this.cloudDescriptor = builder.cloudDescriptor;
            this.pemDecoder = builder.pemDecoder;
        }

        @Override
        ComputeCloud.DescriptorImpl getComputeCloudDescriptor() {
            return Objects.requireNonNull(cloudDescriptor, "cloudDescriptor");
        }

        @Override
        PEMEncodable decodePEM(String pem) throws UnrecoverableKeyException, IOException {
            return pemDecoder == null ? super.decodePEM(pem) : pemDecoder.decode(pem);
        }
    }
}
