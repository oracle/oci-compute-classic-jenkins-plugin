package com.oracle.cloud.compute.jenkins;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import com.oracle.cloud.compute.jenkins.client.ComputeCloudClient;
import com.oracle.cloud.compute.jenkins.client.ComputeCloudClientException;
import com.oracle.cloud.compute.jenkins.model.InstanceOrchestration;
import com.oracle.cloud.compute.jenkins.ssh.SshComputerLauncher;

import hudson.Extension;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.model.Descriptor.FormException;
import hudson.slaves.AbstractCloudComputer;
import hudson.slaves.AbstractCloudSlave;
import hudson.slaves.CloudRetentionStrategy;
import hudson.slaves.NodeProperty;
import net.sf.json.JSONObject;

@SuppressWarnings("serial")
public class ComputeCloudAgent extends AbstractCloudSlave {
    private static final Logger LOGGER = Logger.getLogger(ComputeCloud.class.getName());

    private static CloudRetentionStrategy createRetentionStrategy(String idleTerminationMinutes) {
        int idleMinutes = idleTerminationMinutes == null || idleTerminationMinutes.trim().isEmpty() ? 0 : Integer.parseInt(idleTerminationMinutes);
        return new CloudRetentionStrategy(idleMinutes);
    }

    private final String cloudName;
    private final String orchName;

    public ComputeCloudAgent(
            final String name,
            final ComputeCloudAgentTemplate template,
            final String cloudName,
            final String orchName,
            final String host) throws IOException, FormException {
        this(
                name,
                template.getDescription(),
                template.getRemoteFS(),
                template.getNumExecutors(),
                template.getMode(),
                template.getLabelString(),
                template.getIdleTerminationMinutes(),
                Collections.<NodeProperty<?>> emptyList(),
                cloudName,
                orchName,
                template.getSshUserValue(),
                template.getSshConnectTimeoutMillis(),
                template.getPrivateKey(),
                template.getInitScript(),
                template.getInitScriptTimeoutSeconds(),
                host);
    }

    @DataBoundConstructor
    public ComputeCloudAgent(
            final String name,
            final String description,
            final String remoteFS,
            final String numExecutors,
            final Mode mode,
            final String labelString,
            final String idleTerminationMinutes,
            final List<? extends NodeProperty<?>> nodeProperties,
            final String cloudName,
            final String orchName,
            final String sshUser,
            final int sshConnectTimeoutMillis,
            final String privateKey,
            final String initScript,
            final int initScriptTimeoutSeconds,
            final String host)
            throws IOException, FormException {
        super(
                name,
                description,
                remoteFS,
                numExecutors,
                mode,
                labelString,
                new SshComputerLauncher(
                        host,
                        sshConnectTimeoutMillis,
                        privateKey,
                        initScript,
                        initScriptTimeoutSeconds,
                        sshUser),
                createRetentionStrategy(idleTerminationMinutes),
                nodeProperties);
        this.cloudName = cloudName;
        this.orchName = orchName;
    }

    @Override
    public AbstractCloudComputer<ComputeCloudAgent> createComputer() {
        return new ComputeCloudComputer(this);
    }

    ComputeCloud getCloud() {
        return (ComputeCloud) JenkinsUtil.getJenkinsInstance().getCloud(getCloudName());
    }

    public String getCloudName() {
        return cloudName;
    }

    /**
     * Terminates the instance in Oracle Cloud Infrastructure Compute Classic
     */
    @Override
    protected synchronized void _terminate(TaskListener listener) throws IOException, InterruptedException {
        ComputeCloud cloud = getCloud();
        if (cloud == null) {
            LOGGER.log(Level.SEVERE, "Unable to stop or delete orchestration {0} because the Oracle Cloud Infrastructure Compute Classic {1} does not exist",
                    new Object[] { orchName, ComputeCloud.nameToCloudName(getCloudName()) });
            return;
        }

        // recycle cloud resources; remove this node from jenkins has been done in parent class
        cloud.recycleCloudResources(orchName);
    }

    boolean isAlive() throws IOException, ComputeCloudClientException {
        ComputeCloud cloud = getCloud();
        if (cloud == null) {
            throw new IllegalStateException("the Oracle Cloud Infrastructure Compute Classic " + getCloudName() + " does not exist");
        }

        try (ComputeCloudClient client = cloud.createClient()) {
            InstanceOrchestration.Status status = client.getInstanceOrchestration(orchName).getStatus();
            if (status == InstanceOrchestration.Status.ready
                    || status == InstanceOrchestration.Status.starting
                    || status == InstanceOrchestration.Status.updating) {
                return true;
            }

        }
        return false;
    }

    @Override
    public Node reconfigure(final StaplerRequest req, JSONObject form) {
        if (form == null) {
            return null;
        }

        //TODO
        return null;
    }

    @Extension
    public static class ComputeCloudAgentDescriptor extends SlaveDescriptor {
        @Override
        public String getDisplayName() {
            return ""; // TODO
        }

        @Override
        public boolean isInstantiable() {
            return false;
        }
    }

}
