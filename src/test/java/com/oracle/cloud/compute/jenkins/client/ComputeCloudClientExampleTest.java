package com.oracle.cloud.compute.jenkins.client;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.oracle.cloud.compute.jenkins.TestComputeCloudAgentTemplate;
import com.oracle.cloud.compute.jenkins.model.InstanceOrchestration;

public class ComputeCloudClientExampleTest {
    private static URI newURI(String uri) {
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            throw new Error(e);
        }
    }

    private static final URI ENDPOINT = newURI("https://todo");
    private static final ComputeCloudUser USER = ComputeCloudUser.parse("/Compute-todo/todo@todo");
    private static final String PASSWORD = "todo";
    private static final String ORCHESTRATION_NAME = "todo";

    /**
     * An example of using {@link ComputeCloudClient}.  Modify the parameters
     * above, remove the {@code @Ignore} below, and run the test.
     */
    @Test
    @Ignore
    public void test() throws Exception {
        try (ComputeCloudClient client = new AutoAuthComputeCloudClientFactory(JaxrsComputeCloudClientFactory.INSTANCE).createClient(ENDPOINT, USER, PASSWORD)) {
            client.createInstanceOrchestration(ORCHESTRATION_NAME, new TestComputeCloudAgentTemplate.Builder().build());

            client.startOrchestration(ORCHESTRATION_NAME);
            awaitStatusChange(client, ORCHESTRATION_NAME, InstanceOrchestration.Status.starting, InstanceOrchestration.Status.ready);

            client.stopOrchestration(ORCHESTRATION_NAME);
            awaitStatusChange(client, ORCHESTRATION_NAME, InstanceOrchestration.Status.stopping, InstanceOrchestration.Status.stopped);

            client.deleteOrchestration(ORCHESTRATION_NAME);
        }
    }

    private static void awaitStatusChange(ComputeCloudClient client, String orchName, InstanceOrchestration.Status fromStatus, InstanceOrchestration.Status toStatus) throws Exception {
        for (int i = 0;; i++) {
            InstanceOrchestration.Status status = client.getInstanceOrchestration(orchName).getStatus();
            System.out.println("Instance status " + i + ": " + status);

            if (status != fromStatus) {
                Assert.assertEquals(toStatus, status);
                break;
            }
            Assert.assertTrue("Timeout", i < 30);

            Thread.sleep(10000);
        }
    }
}
