package com.oracle.cloud.compute.jenkins;

import java.io.File;

import hudson.model.Node;

public class TestComputeCloudComputer extends ComputeCloudComputer {
    private static final File LOG_DIR = new File(System.getProperty("java.io.tmpdir"));

    private final ComputeCloudAgent agent;

    public TestComputeCloudComputer(ComputeCloudAgent agent) {
        super(agent);
        this.agent = agent;
    }

    @Override
    protected File getLogDir() {
        // Override to avoid Jenkins.getInstance(), which is null.
        return LOG_DIR;
    }

    @Override
    protected void setNode(Node node) {
        // Override to avoid setNumExecutors/addNewExecutorIfNecessary,
        // which do not work.
        nodeName = node.getNodeName();
    }

    @Override
    public ComputeCloudAgent getNode() {
        // Override to avoid Jenkins.getInstance(), which is null.
        return agent;
    }
}
