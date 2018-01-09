package com.oracle.cloud.compute.jenkins;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.Extension;
import hudson.model.AsyncPeriodicWork;
import hudson.model.Node;
import hudson.model.TaskListener;

@Extension
public class ComputeCloudInstanceMonitor extends AsyncPeriodicWork {
    private static final Logger LOGGER = Logger.getLogger(ComputeCloudInstanceMonitor.class.getName());

    private static final Long recurrencePeriod = TimeUnit.MINUTES.toMillis(10);

    public ComputeCloudInstanceMonitor(){
        super("Oracle Cloud Infrastructure Compute Classic instances monitor");
        LOGGER.log(Level.FINE, "Oracle Cloud Infrastructure Compute Classic check alive period is {0}ms", recurrencePeriod);
    }

    @Override
    public long getRecurrencePeriod() {
        return recurrencePeriod;
    }

    List<Node> getNodes() {
        return JenkinsUtil.getJenkinsInstance().getNodes();
    }

    @Override
    protected void execute(TaskListener listener) {
        for (Node node : getNodes()) {
            if (node instanceof ComputeCloudAgent) {
                final ComputeCloudAgent agent = (ComputeCloudAgent)node;
                try {
                    if (!agent.isAlive()) {
                        LOGGER.fine("Compute instance is offline: " + agent.getDisplayName());
                        agent._terminate(listener);
                        LOGGER.info("Compute instance is terminated: " + agent.getDisplayName());
                        removeNode(agent);
                    } else {
                        LOGGER.fine("Compute instance is online: " + agent.getDisplayName());
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to terminate node : " + agent.getDisplayName(), e);
                    removeNode(agent);
                }
            }
        }
    }

    void removeNode(ComputeCloudAgent agent) {
        try {
            JenkinsUtil.getJenkinsInstance().removeNode(agent);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to remove node: " + agent.getDisplayName());
        }
    }
}
