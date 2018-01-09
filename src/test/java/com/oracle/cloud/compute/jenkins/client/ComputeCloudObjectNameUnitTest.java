package com.oracle.cloud.compute.jenkins.client;

import org.junit.Assert;
import org.junit.Test;

public class ComputeCloudObjectNameUnitTest {
    @Test
    public void testParse() {
        ComputeCloudObjectName n = ComputeCloudObjectName.parse("/Compute-acme/jack.jones@example.com/name");
        Assert.assertEquals("/Compute-acme/jack.jones@example.com/name", n.getString());
        Assert.assertEquals(ComputeCloudUser.parse("/Compute-acme/jack.jones@example.com"), n.getUser());
        Assert.assertEquals("name", n.getName());

        Assert.assertNotNull(n.toString());
        Assert.assertEquals(n, ComputeCloudObjectName.parse("/Compute-acme/jack.jones@example.com/name"));
        Assert.assertNotEquals(n, null);
        Assert.assertNotEquals(n, "");
        Assert.assertNotEquals(n, ComputeCloudObjectName.parse("/Compute-x/jack.jones@example.com/name"));
        Assert.assertNotEquals(n, ComputeCloudObjectName.parse("/Compute-acme/x/name"));
        Assert.assertNotEquals(n, ComputeCloudObjectName.parse("/Compute-acme/jack.jones@example.com/x"));
        Assert.assertEquals(n.hashCode(), ComputeCloudObjectName.parse("/Compute-acme/jack.jones@example.com/name").hashCode());
        Assert.assertNotEquals(n.hashCode(), ComputeCloudObjectName.parse("/Compute-x/jack.jones@example.com/name").hashCode());
        Assert.assertNotEquals(n.hashCode(), ComputeCloudObjectName.parse("/Compute-acme/x@example.com/name").hashCode());
        Assert.assertNotEquals(n.hashCode(), ComputeCloudObjectName.parse("/Compute-acme/jack.jones@example.com/x").hashCode());
    }

    @Test
    public void testParseNameWithSpecialChar() {
        ComputeCloudObjectName n = ComputeCloudObjectName.parse("/Compute-acme/jack.jones@example.com/name_part1/name_part2");
        Assert.assertEquals("/Compute-acme/jack.jones@example.com/name_part1/name_part2", n.getString());
        Assert.assertEquals(ComputeCloudUser.parse("/Compute-acme/jack.jones@example.com"), n.getUser());
        Assert.assertEquals("name_part1/name_part2", n.getName());

        Assert.assertNotNull(n.toString());
        Assert.assertEquals(n, ComputeCloudObjectName.parse("/Compute-acme/jack.jones@example.com/name_part1/name_part2"));
        Assert.assertNotNull(n);
        Assert.assertNotEquals(n, "");
        Assert.assertNotEquals(n, ComputeCloudObjectName.parse("/Compute-x/jack.jones@example.com/name_part1/name_part2"));
        Assert.assertNotEquals(n, ComputeCloudObjectName.parse("/Compute-acme/x/name_part1/name_part2"));
        Assert.assertNotEquals(n, ComputeCloudObjectName.parse("/Compute-acme/jack.jones@example.com/x"));
        Assert.assertEquals(n.hashCode(), ComputeCloudObjectName.parse("/Compute-acme/jack.jones@example.com/name_part1/name_part2").hashCode());
        Assert.assertNotEquals(n.hashCode(), ComputeCloudObjectName.parse("/Compute-x/jack.jones@example.com/name_part1/name_part2").hashCode());
        Assert.assertNotEquals(n.hashCode(), ComputeCloudObjectName.parse("/Compute-acme/x@example.com/name_part1/name_part2").hashCode());
        Assert.assertNotEquals(n.hashCode(), ComputeCloudObjectName.parse("/Compute-acme/jack.jones@example.com/x").hashCode());
    }

    @Test(expected = NullPointerException.class)
    public void testParseNull() {
        ComputeCloudObjectName.parse(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseInvalidUser() {
        ComputeCloudObjectName.parse("/x/y/name");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseEmptyName() {
        ComputeCloudObjectName.parse("/Compute-acme/jack.jones@example.com/");
    }

    @Test
    public void testValueOf() {
        ComputeCloudUser u = ComputeCloudUser.parse("/Compute-acme/jack.jones@example.com");
        ComputeCloudObjectName n = ComputeCloudObjectName.valueOf(u, "name");
        Assert.assertEquals("/Compute-acme/jack.jones@example.com/name", n.getString());
        Assert.assertEquals(u, n.getUser());
        Assert.assertEquals("name", n.getName());
    }

    @Test(expected = NullPointerException.class)
    public void testValueOfNullUser() {
        ComputeCloudObjectName.valueOf(null, "name");
    }

    @Test(expected = NullPointerException.class)
    public void testValueOfNullName() {
        ComputeCloudObjectName.valueOf(ComputeCloudUser.parse("/Compute-acme/jack.jones@example.com"), null);
    }
}
