package com.oracle.cloud.compute.jenkins.client;

import java.net.URI;

public class CachingComputeCloudClientFactory implements ComputeCloudClientFactory {
    private final ComputeCloudClientFactory factory;

    public CachingComputeCloudClientFactory(ComputeCloudClientFactory factory) {
        this.factory = factory;
    }

    @Override
    public ComputeCloudClient createClient(URI endpoint, ComputeCloudUser user, String password) {
        return new CachingComputeCloudClient(factory.createClient(endpoint, user, password));
    }
}
