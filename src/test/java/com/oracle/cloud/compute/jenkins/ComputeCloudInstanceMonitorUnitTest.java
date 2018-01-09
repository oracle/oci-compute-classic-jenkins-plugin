package com.oracle.cloud.compute.jenkins;

import java.util.Arrays;
import java.util.List;

import org.jmock.Expectations;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import com.oracle.cloud.compute.jenkins.client.ComputeCloudClient;
import com.oracle.cloud.compute.jenkins.model.InstanceOrchestration;
import com.oracle.cloud.compute.jenkins.model.InstanceOrchestration.Status;

import hudson.model.Node;

public class ComputeCloudInstanceMonitorUnitTest {
    @Rule
    public final ComputeCloudMockery mockery = new ComputeCloudMockery();

    static class TestComputeCloudInstanceMonitor extends ComputeCloudInstanceMonitor {
        boolean removed;
        Node agent;

        TestComputeCloudInstanceMonitor(Node agent) {
            this.agent = agent;
        }

        @Override
        protected List<Node> getNodes() {
            return Arrays.asList(agent);
        }

        @Override
        protected void removeNode(ComputeCloudAgent agent) {
            removed = true;
        }
    }

    private TestComputeCloudAgent newComputeCloudAgent(final Status status, final boolean terminate) throws Exception {
        final ComputeCloudClient client = mockery.mock(ComputeCloudClient.class);
        mockery.checking(new Expectations() {{
            oneOf(client).getInstanceOrchestration("on"); will(returnValue(new InstanceOrchestration().status(status)));
            oneOf(client).close();
            if (terminate) {
                oneOf(client).getInstanceOrchestration("on"); will(returnValue(new InstanceOrchestration().status(Status.stopped)));
                oneOf(client).deleteOrchestration("on");
                oneOf(client).close();
            }
        }});
        TestComputeCloudAgent agent = new TestComputeCloudAgent.Builder()
                .orchName("on")
                .cloud(new TestComputeCloud.Builder().client(client).clock(new TestClock()).build())
                .build();
        return agent;
    }

    @Test
    public void testExecuteAlive() throws Exception {
        TestComputeCloudAgent agent = newComputeCloudAgent(Status.ready, false);
        TestComputeCloudInstanceMonitor monitor = new TestComputeCloudInstanceMonitor(agent);
        monitor.execute(null);
        Assert.assertFalse(monitor.removed);
    }

    @Test
    public void testExecuteNotAlive() throws Exception {
        TestComputeCloudAgent agent = newComputeCloudAgent(Status.stopped, true);
        TestComputeCloudInstanceMonitor monitor = new TestComputeCloudInstanceMonitor(agent);
        monitor.execute(null);
        Assert.assertTrue(monitor.removed);
    }

    @Test
    public void testExecuteError() {
        TestComputeCloudAgent agent = new TestComputeCloudAgent.Builder().build();
        TestComputeCloudInstanceMonitor monitor = new TestComputeCloudInstanceMonitor(agent);
        monitor.execute(null);
        Assert.assertTrue(monitor.removed);
    }
}
