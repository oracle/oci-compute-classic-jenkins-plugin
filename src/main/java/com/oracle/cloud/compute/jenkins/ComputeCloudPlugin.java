package com.oracle.cloud.compute.jenkins;

import com.oracle.cloud.compute.jenkins.client.AutoAuthComputeCloudClientFactory;
import com.oracle.cloud.compute.jenkins.client.CachingComputeCloudClientFactory;
import com.oracle.cloud.compute.jenkins.client.JaxrsComputeCloudClientFactory;

import hudson.Extension;
import hudson.Plugin;

@SuppressWarnings("deprecation")
@Extension
public class ComputeCloudPlugin extends Plugin {
    public static final ComputeCloudClientManager CLIENT_MANAGER = new ComputeCloudClientManager(
            new AutoAuthComputeCloudClientFactory(new CachingComputeCloudClientFactory(JaxrsComputeCloudClientFactory.INSTANCE)),
            JenkinsUtil.getJenkinsInstance().clouds);

    @Override
    public void stop() throws Exception {
        CLIENT_MANAGER.close();
    }
}
