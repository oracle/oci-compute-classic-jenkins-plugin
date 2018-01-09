package com.oracle.cloud.compute.jenkins.client;

import java.net.URI;

public interface ComputeCloudClientFactory {
    ComputeCloudClient createClient(URI endpoint, ComputeCloudUser user, String password);
}
