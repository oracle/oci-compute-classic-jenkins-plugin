package com.oracle.cloud.compute.jenkins.client;

import java.util.Collections;

import org.jmock.Expectations;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.oracle.cloud.compute.jenkins.ComputeCloudMockery;
import com.oracle.cloud.compute.jenkins.TestComputeCloudAgentTemplate;
import com.oracle.cloud.compute.jenkins.model.ImageListSourceType;
import com.oracle.cloud.compute.jenkins.model.InstanceOrchestration;
import com.oracle.cloud.compute.jenkins.model.SSHKey;

public class ProxyComputeCloudClientUnitTest {
    private interface PreInvokeCallback {
        void preInvoke();
    }

    private static class TestProxyComputeCloudClient extends ProxyComputeCloudClient {
        private final PreInvokeCallback callback;

        TestProxyComputeCloudClient(ComputeCloudClient client, PreInvokeCallback callback) {
            super(client);
            this.callback = callback;
        }

        @Override
        protected void preInvoke() {
            callback.preInvoke();
        }
    }

    @Rule
    public final ComputeCloudMockery mockery = new ComputeCloudMockery();
    private final ComputeCloudClient mockClient = mockery.mock(ComputeCloudClient.class);
    private final PreInvokeCallback callback = mockery.mock(PreInvokeCallback.class);
    private final TestProxyComputeCloudClient client = new TestProxyComputeCloudClient(mockClient, callback);

    @Before
    public void before() {
        mockery.checking(new Expectations() {{ oneOf(callback).preInvoke(); }});
    }

    @Test
    public void testClose() throws Exception {
        // close does not call preInvoke, so call it manually to simplify the
        // @Before method.
        callback.preInvoke();

        mockery.checking(new Expectations() {{ oneOf(mockClient).close(); }});
        client.close();
    }

    @Test
    public void testAuthenticate() throws Exception {
        mockery.checking(new Expectations() {{ oneOf(mockClient).authenticate(); }});
        client.authenticate();
    }

    @Test
    public void testGetShapes() throws Exception {
        mockery.checking(new Expectations() {{ oneOf(mockClient).getShapes(); will(returnValue(Collections.emptyList())); }});
        Assert.assertEquals(Collections.emptyList(), client.getShapes());
    }

    @Test
    public void testGetSecurityLists() throws Exception {
        mockery.checking(new Expectations() {{ oneOf(mockClient).getSecurityLists(); will(returnValue(Collections.emptyList())); }});
        Assert.assertEquals(Collections.emptyList(), client.getSecurityLists());
    }

    @Test
    public void testGetSSHKeys() throws Exception {
        mockery.checking(new Expectations() {{ oneOf(mockClient).getSSHKeys(); will(returnValue(Collections.emptyList())); }});
        Assert.assertEquals(Collections.emptyList(), client.getSSHKeys());
    }

    @Test
    public void testGetSSHKey() throws Exception {
        final SSHKey sshKey = new SSHKey();
        mockery.checking(new Expectations() {{ oneOf(mockClient).getSSHKey("n"); will(returnValue(sshKey)); }});
        Assert.assertSame(sshKey, client.getSSHKey("n"));
    }

    @Test
    public void testGetImageLists() throws Exception {
        mockery.checking(new Expectations() {{ oneOf(mockClient).getImageLists(ImageListSourceType.ORACLE_PUBLIC_IMAGE); will(returnValue(Collections.emptyList())); }});
        Assert.assertEquals(Collections.emptyList(), client.getImageLists(ImageListSourceType.ORACLE_PUBLIC_IMAGE));
    }

    @Test
    public void testCreateInstanceOrchestration() throws Exception {
        final ComputeCloudInstanceOrchestrationConfig config = new TestComputeCloudAgentTemplate.Builder().build();
        mockery.checking(new Expectations() {{ oneOf(mockClient).createInstanceOrchestration("n", config); will(returnValue(null)); }});
        client.createInstanceOrchestration("n", config);
    }

    @Test
    public void testGetInstanceOrchestration() throws Exception {
        final InstanceOrchestration orch = new InstanceOrchestration();
        mockery.checking(new Expectations() {{ oneOf(mockClient).getInstanceOrchestration("n"); will(returnValue(orch)); }});
        Assert.assertSame(orch, client.getInstanceOrchestration("n"));
    }

    @Test
    public void testStartOrchestration() throws Exception {
        mockery.checking(new Expectations() {{ oneOf(mockClient).startOrchestration("n"); will(returnValue(null)); }});
        client.startOrchestration("n");
    }

    @Test
    public void testStopOrchestration() throws Exception {
        mockery.checking(new Expectations() {{ oneOf(mockClient).stopOrchestration("n"); will(returnValue(null)); }});
        client.stopOrchestration("n");
    }

    @Test
    public void testDeleteOrchestration() throws Exception {
        mockery.checking(new Expectations() {{ oneOf(mockClient).deleteOrchestration("n"); will(returnValue(null)); }});
        client.deleteOrchestration("n");
    }
}
