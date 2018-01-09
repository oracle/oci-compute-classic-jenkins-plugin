package com.oracle.cloud.compute.jenkins.client;

import java.net.URI;

public class AutoAuthComputeCloudClientFactory implements ComputeCloudClientFactory {
    private final ComputeCloudClientFactory factory;

    public AutoAuthComputeCloudClientFactory(ComputeCloudClientFactory factory) {
        this.factory = factory;
    }

    @Override
    public ComputeCloudClient createClient(URI endpoint, ComputeCloudUser user, String password) {
        return new AutoAuthComputeCloudClient(factory.createClient(endpoint, user, password));
    }
}
