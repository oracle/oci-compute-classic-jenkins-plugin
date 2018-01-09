package com.oracle.cloud.compute.jenkins.client;

import org.junit.Assert;
import org.junit.Test;

public class ComputeCloudUserUnitTest {
    @Test
    public void test() {
        ComputeCloudUser u = ComputeCloudUser.parse("/Compute-acme/jack.jones@example.com");
        Assert.assertEquals("/Compute-acme/jack.jones@example.com", u.getString());
        Assert.assertEquals("Compute-acme", u.getFullIdentityDomainName());
        Assert.assertEquals("acme", u.getIdentityDomainName());
        Assert.assertEquals("jack.jones@example.com", u.getUsername());

        Assert.assertNotNull(u.toString());
        Assert.assertEquals(u, ComputeCloudUser.parse("/Compute-acme/jack.jones@example.com"));
        Assert.assertNotEquals(u, null);
        Assert.assertNotEquals(u, "");
        Assert.assertNotEquals(u, ComputeCloudUser.parse("/Compute-x/jack.jones@example.com"));
        Assert.assertNotEquals(u, ComputeCloudUser.parse("/Compute-acme/x"));
        Assert.assertEquals(u.hashCode(), ComputeCloudUser.parse("/Compute-acme/jack.jones@example.com").hashCode());
        Assert.assertNotEquals(u.hashCode(), ComputeCloudUser.parse("/Compute-x/jack.jones@example.com").hashCode());
        Assert.assertNotEquals(u.hashCode(), ComputeCloudUser.parse("/Compute-acme/x").hashCode());
    }

    @Test
    public void testOraclePublic() {
        Assert.assertSame(ComputeCloudUser.ORACLE_PUBLIC, ComputeCloudUser.parse("/oracle/public"));
        Assert.assertEquals("/oracle/public", ComputeCloudUser.ORACLE_PUBLIC.getString());
        Assert.assertEquals("oracle", ComputeCloudUser.ORACLE_PUBLIC.getFullIdentityDomainName());
        Assert.assertNull(ComputeCloudUser.ORACLE_PUBLIC.getIdentityDomainName());
        Assert.assertEquals("public", ComputeCloudUser.ORACLE_PUBLIC.getUsername());

        Assert.assertNotNull(ComputeCloudUser.ORACLE_PUBLIC.toString());
        Assert.assertEquals(ComputeCloudUser.ORACLE_PUBLIC, ComputeCloudUser.parse("/oracle/public"));
        Assert.assertNotEquals(ComputeCloudUser.ORACLE_PUBLIC, ComputeCloudUser.parse("/Compute-acme/x"));
        Assert.assertEquals(ComputeCloudUser.ORACLE_PUBLIC.hashCode(), ComputeCloudUser.parse("/oracle/public").hashCode());
        Assert.assertNotEquals(ComputeCloudUser.ORACLE_PUBLIC.hashCode(), ComputeCloudUser.parse("/Compute-acme/x").hashCode());
    }

    @Test(expected = NullPointerException.class)
    public void testNull() {
        ComputeCloudUser.parse(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmpty() {
        ComputeCloudUser.parse("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyIdentityDomainName() {
        ComputeCloudUser.parse("/Compute-/jack.jones@example.com");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyUsername() {
        ComputeCloudUser.parse("/Compute-acme/");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidOracleUsername() {
        ComputeCloudUser.parse("/oracle/x");
    }

    @Test
    public void testValueOf() {
        ComputeCloudUser u = ComputeCloudUser.valueOf("acme", "jack.jones@example.com");
        Assert.assertEquals("/Compute-acme/jack.jones@example.com", u.getString());
        Assert.assertEquals("Compute-acme", u.getFullIdentityDomainName());
        Assert.assertEquals("acme", u.getIdentityDomainName());
        Assert.assertEquals("jack.jones@example.com", u.getUsername());
    }

    @Test(expected = NullPointerException.class)
    public void testValueOfNullIdentityDomainName() {
        ComputeCloudUser.valueOf(null, "jack.jones@example.com");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValueOfInvalidIdentityDomainName() {
        ComputeCloudUser.valueOf("a/b", "jack.jones@example.com");
    }

    @Test(expected = NullPointerException.class)
    public void testValueOfNullUsername() {
        ComputeCloudUser.valueOf("acme", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValueOfInvalidUsername() {
        ComputeCloudUser.valueOf("acme", "a/b");
    }
}
