package com.oracle.cloud.compute.jenkins;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.oracle.cloud.compute.jenkins.client.ComputeCloudClient;
import com.oracle.cloud.compute.jenkins.client.ComputeCloudUser;
import com.oracle.cloud.compute.jenkins.ssh.SshConnector;

import hudson.model.Node;
import hudson.security.ACL;
import hudson.slaves.Cloud;
import hudson.slaves.NodeProvisioner.PlannedNode;

/**
 * This class simplifies creation of dummy ComputeCloud instances for
 * unittesting so that constructor arguments do not need to be specified.
 */
public class TestComputeCloud extends ComputeCloud {
    public static class Builder {
        String cloudName = "cloudName";
        String apiEndpoint;
        String identityDomainName;
        String userName;
        String password;
        String instanceCapStr;
        int nextTemplateId;
        List<? extends ComputeCloudAgentTemplate> templates;

        ComputeCloudClient client;
        List<Node> nodes;
        ExecutorService threadPoolForRemoting;
        PlannedNodeFactory plannedNodeFactory;
        Clock clock;
        SshConnector sshConnector;
        ACL acl;

        public Builder cloudName(String cloudName) {
            this.cloudName = cloudName;
            return this;
        }

        public Builder apiEndpoint(String apiEndpoint) {
            this.apiEndpoint = apiEndpoint;
            return this;
        }

        public Builder apiEndpoint(URI apiEndpoint) {
            this.apiEndpoint = apiEndpoint.toString();
            return this;
        }

        public Builder identityDomainName(String identityDomainName) {
            this.identityDomainName = identityDomainName;
            return this;
        }

        public Builder userName(String userName) {
            this.userName = userName;
            return this;
        }

        public Builder user(ComputeCloudUser user) {
            return identityDomainName(user.getIdentityDomainName())
                    .userName(user.getUsername());
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder instanceCapStr(String instanceCapStr) {
            this.instanceCapStr = instanceCapStr;
            return this;
        }

        public Builder instanceCap(int instanceCap) {
            return instanceCapStr(String.valueOf(instanceCap));
        }

        public Builder nextTemplateId(int nextTemplateId) {
            this.nextTemplateId = nextTemplateId;
            return this;
        }

        public Builder templates(List<? extends ComputeCloudAgentTemplate> templates) {
            this.templates = templates;
            return this;
        }

        public Builder client(ComputeCloudClient client) {
            this.client = client;
            return this;
        }

        public Builder nodes(List<Node> nodes) {
            this.nodes = nodes;
            return this;
        }

        public Builder threadPoolForRemoting(ExecutorService threadPoolForRemoting) {
            this.threadPoolForRemoting = threadPoolForRemoting;
            return this;
        }

        public Builder plannedNodeFactory(PlannedNodeFactory plannedNodeFactory) {
            this.plannedNodeFactory = plannedNodeFactory;
            return this;
        }

        public Builder clock(Clock clock) {
            this.clock = clock;
            return this;
        }

        public Builder sshConnector(SshConnector sshConnector) {
            this.sshConnector = sshConnector;
            return this;
        }

        public Builder acl(ACL acl) {
            this.acl = acl;
            return this;
        }

        public TestComputeCloud build() {
            return new TestComputeCloud(this);
        }
    }

    public interface PlannedNodeFactory {
        PlannedNode newPlannedNode(String displayName, Future<Node> future, int numExecutors, ComputeCloudAgentTemplate template);
    }

    public static class TestPlannedNode extends PlannedNode {
        public static final PlannedNodeFactory FACTORY = new PlannedNodeFactory() {
            @Override
            public PlannedNode newPlannedNode(String displayName, Future<Node> future, int numExecutors, ComputeCloudAgentTemplate template) {
                return new TestPlannedNode(displayName, future, numExecutors, template);
            }
        };

        public final ComputeCloudAgentTemplate template;

        public TestPlannedNode(String displayName, Future<Node> future, int numExecutors, ComputeCloudAgentTemplate template) {
            super(displayName, future, numExecutors);
            this.template = template;
        }
    }

    private final ComputeCloudClient client;
    private final List<Node> nodes;
    private final ExecutorService threadPoolForRemoting;
    private final PlannedNodeFactory plannedNodeFactory;
    private final Clock clock;
    private final SshConnector sshConnector;
    private final ACL acl;

    public TestComputeCloud() {
        this(new Builder());
    }

    public TestComputeCloud(Builder builder) {
        super(
                builder.cloudName,
                builder.apiEndpoint,
                builder.identityDomainName,
                builder.userName,
                builder.password,
                builder.instanceCapStr,
                builder.nextTemplateId,
                builder.templates);
        this.client = builder.client;
        this.nodes = builder.nodes;
        this.threadPoolForRemoting = builder.threadPoolForRemoting;
        this.plannedNodeFactory = builder.plannedNodeFactory;
        this.clock = builder.clock;
        this.sshConnector = builder.sshConnector;
        this.acl = builder.acl;
    }

    @Override
    public ComputeCloudClient createClient() {
        return Objects.requireNonNull(client, "client");
    }

    @Override
    List<Node> getNodes() {
        return Objects.requireNonNull(nodes, "nodes");
    }

    @Override
    ExecutorService getThreadPoolForRemoting() {
        return Objects.requireNonNull(threadPoolForRemoting, "threadPoolForRemoting");
    }

    @Override
    PlannedNode newPlannedNode(String displayName, Future<Node> future, int numExecutors, ComputeCloudAgentTemplate template) {
        return plannedNodeFactory != null ?
                plannedNodeFactory.newPlannedNode(displayName, future, numExecutors, template) :
                super.newPlannedNode(displayName, future, numExecutors, template);
    }

    @Override
    Clock getClock() {
        return Objects.requireNonNull(clock, "clock");
    }

    @Override
    SshConnector getSshConnector() {
        return Objects.requireNonNull(sshConnector, "sshConnector");
    }

    @Override
    public ACL getACL() {
        return Objects.requireNonNull(acl, "acl");
    }

    @Override
    public String getEncryptedValue(String str) {
        return str;
    }

    @Override
    public String getPlainText(String str) {
        return str;
    }

    public static class TestDescriptor extends DescriptorImpl {
        public static class Builder {
            List<Cloud> clouds;
            ComputeCloudClientManager clientManager;

            public Builder clouds(List<Cloud> clouds) {
                this.clouds = clouds;
                return this;
            }

            public Builder clouds(Cloud... clouds) {
                return clouds(Arrays.asList(clouds));
            }

            public Builder clientManager(ComputeCloudClientManager clientManager) {
                this.clientManager = clientManager;
                return this;
            }

            public TestDescriptor build() {
                return new TestDescriptor(this);
            }
        }

        private final List<? extends Cloud> clouds;
        private final ComputeCloudClientManager clientManager;

        public TestDescriptor(Builder builder) {
            this.clouds = builder.clouds;
            this.clientManager = builder.clientManager;
        }

        @Override
        List<? extends Cloud> getClouds() {
            return Objects.requireNonNull(clouds, "clouds");
        }

        @Override
        ComputeCloudClientManager getComputeCloudClientManager() {
            return Objects.requireNonNull(clientManager, "clientManager");
        }
    }
}
