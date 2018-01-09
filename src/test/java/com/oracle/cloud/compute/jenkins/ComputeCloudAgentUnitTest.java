package com.oracle.cloud.compute.jenkins;

import java.io.IOException;

import org.jmock.Expectations;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import com.oracle.cloud.compute.jenkins.client.ComputeCloudClient;
import com.oracle.cloud.compute.jenkins.client.ComputeCloudClientException;
import com.oracle.cloud.compute.jenkins.model.InstanceOrchestration;

import hudson.model.TaskListener;

public class ComputeCloudAgentUnitTest {
    @Rule
    public final ComputeCloudMockery mockery = new ComputeCloudMockery();

    private static TaskListener newTerminateTaskListener() {
        return null;
    }

    @Test
    public void testTerminateFromReadyStatus() throws Exception {
        final ComputeCloudClient client = mockery.mock(ComputeCloudClient.class);
        mockery.checking(new Expectations() {{
            oneOf(client).getInstanceOrchestration("on"); will(returnValue(new InstanceOrchestration().status(InstanceOrchestration.Status.ready)));
            oneOf(client).stopOrchestration("on");
            oneOf(client).getInstanceOrchestration("on"); will(returnValue(new InstanceOrchestration().status(InstanceOrchestration.Status.stopped)));
            oneOf(client).deleteOrchestration("on");
            oneOf(client).close();
        }});
        TestComputeCloudAgent agent = new TestComputeCloudAgent.Builder()
                .orchName("on")
                .cloud(new TestComputeCloud.Builder().client(client).clock(new TestClock()).build())
                .build();
        agent._terminate(newTerminateTaskListener());
    }

    @Test
    public void testTerminateFromStoppedStatus() throws Exception {
        final ComputeCloudClient client = mockery.mock(ComputeCloudClient.class);
        mockery.checking(new Expectations() {{
            oneOf(client).getInstanceOrchestration("on"); will(returnValue(new InstanceOrchestration().status(InstanceOrchestration.Status.stopped)));
            oneOf(client).deleteOrchestration("on");
            oneOf(client).close();
        }});

        TestComputeCloudAgent agent = new TestComputeCloudAgent.Builder()
                .orchName("on")
                .cloud(new TestComputeCloud.Builder().client(client).clock(new TestClock()).build())
                .build();
        agent._terminate(newTerminateTaskListener());
    }

    @Test(expected = IOException.class)
    public void testTerminateStopError() throws Exception {
        final ComputeCloudClient client = mockery.mock(ComputeCloudClient.class);
        mockery.checking(new Expectations() {{
            oneOf(client).getInstanceOrchestration("on"); will(returnValue(new InstanceOrchestration().status(InstanceOrchestration.Status.ready)));
            oneOf(client).stopOrchestration("on"); will(throwException(new ComputeCloudClientException("test")));
            oneOf(client).close();
        }});
        TestComputeCloudAgent agent = new TestComputeCloudAgent.Builder()
                .orchName("on")
                .cloud(new TestComputeCloud.Builder().client(client).clock(new TestClock()).build())
                .build();
        agent._terminate(newTerminateTaskListener());
    }

    //Ignore this test as timeout value was set as const with value 10 minutes
    @Test(expected = IOException.class)
    @Ignore
    public void testTerminateTimeout() throws Exception {
        final ComputeCloudClient client = mockery.mock(ComputeCloudClient.class);
        mockery.checking(new Expectations() {{
            oneOf(client).getInstanceOrchestration("on"); will(returnValue(new InstanceOrchestration().status(InstanceOrchestration.Status.ready)));
            oneOf(client).stopOrchestration("on");
            allowing(client).getInstanceOrchestration("on"); will(returnValue(new InstanceOrchestration().status(InstanceOrchestration.Status.stopping)));
            oneOf(client).close();
        }});
        TestComputeCloudAgent agent = new TestComputeCloudAgent.Builder()
                .orchName("on")
                .cloud(new TestComputeCloud.Builder().client(client).clock(new TestClock()).build())
                .build();
        agent._terminate(newTerminateTaskListener());
    }

    @Test
    public void testTerminateCloudNotFound() throws Exception {
        TestComputeCloudAgent agent = new TestComputeCloudAgent.Builder()
                .cloudName(ComputeCloud.NAME_PREFIX + "cn")
                .orchName("on")
                .build();
        agent._terminate(newTerminateTaskListener());
    }

    @Test(expected = IllegalStateException.class)
    public void testAliveCloudNotFound() throws Exception {
        TestComputeCloudAgent agent = new TestComputeCloudAgent.Builder()
                .orchName("on")
                .build();
        agent.isAlive();
    }

    @Test
    public void testAlive() throws Exception {
      final ComputeCloudClient client = mockery.mock(ComputeCloudClient.class);
      mockery.checking(new Expectations() {{
          oneOf(client).getInstanceOrchestration("on"); will(returnValue(new InstanceOrchestration().status(InstanceOrchestration.Status.ready)));
          oneOf(client).close();
          oneOf(client).getInstanceOrchestration("on"); will(returnValue(new InstanceOrchestration().status(InstanceOrchestration.Status.starting)));
          oneOf(client).close();
          oneOf(client).getInstanceOrchestration("on"); will(returnValue(new InstanceOrchestration().status(InstanceOrchestration.Status.updating)));
          oneOf(client).close();
          oneOf(client).getInstanceOrchestration("on"); will(returnValue(new InstanceOrchestration().status(InstanceOrchestration.Status.stopping)));
          oneOf(client).close();
          oneOf(client).getInstanceOrchestration("on"); will(returnValue(new InstanceOrchestration().status(InstanceOrchestration.Status.stopped)));
          oneOf(client).close();
          oneOf(client).getInstanceOrchestration("on"); will(returnValue(new InstanceOrchestration().status(InstanceOrchestration.Status.error)));
          oneOf(client).close();
          oneOf(client).getInstanceOrchestration("on"); will(returnValue(new InstanceOrchestration().status(InstanceOrchestration.Status.scheduled)));
          oneOf(client).close();
      }});

        TestComputeCloudAgent agent = new TestComputeCloudAgent.Builder()
                .cloud(new TestComputeCloud.Builder().client(client).build())
                .orchName("on")
                .build();
        Assert.assertTrue(agent.isAlive());
        Assert.assertTrue(agent.isAlive());
        Assert.assertTrue(agent.isAlive());
        Assert.assertFalse(agent.isAlive());
        Assert.assertFalse(agent.isAlive());
        Assert.assertFalse(agent.isAlive());
        Assert.assertFalse(agent.isAlive());
    }
}
