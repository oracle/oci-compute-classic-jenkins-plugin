package com.oracle.cloud.compute.jenkins.model;

import org.junit.Assert;
import org.junit.Test;

import com.oracle.cloud.compute.jenkins.model.InstanceOrchestration;

public class InstanceOrchestrationUnitTest {
    @Test
    public void test() {
        InstanceOrchestration o = new InstanceOrchestration();
        Assert.assertNotNull(o.toString());
        Assert.assertNull(o.getStatus());
        Assert.assertNull(o.getIp());
        Assert.assertEquals(o, new InstanceOrchestration());
        Assert.assertNotEquals(o, null);
        Assert.assertNotEquals(o, "");
        Assert.assertNotEquals(o, new InstanceOrchestration().status(InstanceOrchestration.Status.ready));
        Assert.assertNotEquals(o, new InstanceOrchestration().ip("ip"));
        Assert.assertEquals(o.hashCode(), new InstanceOrchestration().hashCode());
        Assert.assertNotEquals(o.hashCode(), new InstanceOrchestration().status(InstanceOrchestration.Status.ready).hashCode());
        Assert.assertNotEquals(o.hashCode(), new InstanceOrchestration().ip("ip").hashCode());

        Assert.assertSame(o, o
                .status(InstanceOrchestration.Status.ready)
                .ip("ip"));
        Assert.assertEquals(o, new InstanceOrchestration()
                .status(InstanceOrchestration.Status.ready)
                .ip("ip"));
        Assert.assertEquals(InstanceOrchestration.Status.ready, o.getStatus());
        Assert.assertEquals("ip", o.getIp());
    }
}
