package com.oracle.cloud.compute.jenkins;

import hudson.slaves.AbstractCloudComputer;

public class ComputeCloudComputer extends AbstractCloudComputer<ComputeCloudAgent> {
    public ComputeCloudComputer(ComputeCloudAgent slave) {
        super(slave);
    }
}
