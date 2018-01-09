package com.oracle.cloud.compute.jenkins;

import static com.oracle.cloud.compute.jenkins.ComputeCloudTestUtils.ENDPOINT;
import static com.oracle.cloud.compute.jenkins.ComputeCloudTestUtils.ENDPOINT2;
import static com.oracle.cloud.compute.jenkins.ComputeCloudTestUtils.INVALID_ENDPOINT;
import static com.oracle.cloud.compute.jenkins.ComputeCloudTestUtils.INVALID_IDENTITY_DOMAIN_NAME;
import static com.oracle.cloud.compute.jenkins.ComputeCloudTestUtils.INVALID_USER_NAME;
import static com.oracle.cloud.compute.jenkins.ComputeCloudTestUtils.PASSWORD;
import static com.oracle.cloud.compute.jenkins.ComputeCloudTestUtils.PASSWORD2;
import static com.oracle.cloud.compute.jenkins.ComputeCloudTestUtils.USER;
import static com.oracle.cloud.compute.jenkins.ComputeCloudTestUtils.USER2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.jmock.Expectations;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import com.oracle.cloud.compute.jenkins.client.ComputeCloudClient;
import com.oracle.cloud.compute.jenkins.client.ComputeCloudClientFactory;

import hudson.slaves.Cloud;

public class ComputeCloudClientManagerUnitTest {
    @Rule
    public final ComputeCloudMockery mockery = new ComputeCloudMockery();

    @Test(expected = NullPointerException.class)
    public void testNullClientFactory() {
        new ComputeCloudClientManager(null, Collections.<Cloud>emptyList()).close();
    }

    @Test(expected = NullPointerException.class)
    public void testNullActiveClouds() {
        new ComputeCloudClientManager(mockery.mock(ComputeCloudClientFactory.class), null).close();
    }

    @Test
    public void testClientKey() {
        ComputeCloudClientManager.ClientKey k = new ComputeCloudClientManager.ClientKey(ENDPOINT, USER, PASSWORD);
        Assert.assertNotNull(k.toString());
        Assert.assertEquals(k, k);
        Assert.assertEquals(k, new ComputeCloudClientManager.ClientKey(ENDPOINT, USER, PASSWORD));
        Assert.assertNotEquals(k, null);
        Assert.assertNotEquals(k, "");
        Assert.assertNotEquals(k, new ComputeCloudClientManager.ClientKey(ENDPOINT2, USER, PASSWORD));
        Assert.assertNotEquals(k, new ComputeCloudClientManager.ClientKey(ENDPOINT, USER2, PASSWORD));
        Assert.assertNotEquals(k, new ComputeCloudClientManager.ClientKey(ENDPOINT, USER, PASSWORD2));
        Assert.assertEquals(k.hashCode(), new ComputeCloudClientManager.ClientKey(ENDPOINT, USER, PASSWORD).hashCode());
        Assert.assertNotEquals(k.hashCode(), new ComputeCloudClientManager.ClientKey(ENDPOINT2, USER, PASSWORD).hashCode());
        Assert.assertNotEquals(k.hashCode(), new ComputeCloudClientManager.ClientKey(ENDPOINT, USER2, PASSWORD).hashCode());
        Assert.assertNotEquals(k.hashCode(), new ComputeCloudClientManager.ClientKey(ENDPOINT, USER, PASSWORD2).hashCode());
    }

    @Test
    public void test() {
        try (ComputeCloudClientManager cm = new ComputeCloudClientManager(mockery.mock(ComputeCloudClientFactory.class), Collections.<Cloud>emptyList())) {
            Assert.assertNotNull(cm.toString());
        }
    }

    @Test
    public void testCreateClient() {
        Collection<Cloud> clouds = new ArrayList<>();
        clouds.add(new TestCloud());
        clouds.add(new TestComputeCloud.Builder()
                .apiEndpoint(INVALID_ENDPOINT)
                .user(USER)
                .password(PASSWORD)
                .build());
        clouds.add(new TestComputeCloud.Builder()
                .apiEndpoint(ENDPOINT)
                .identityDomainName(INVALID_IDENTITY_DOMAIN_NAME)
                .userName(INVALID_USER_NAME)
                .password(PASSWORD)
                .build());

        final ComputeCloudClientFactory factory = mockery.mock(ComputeCloudClientFactory.class);
        try (ComputeCloudClientManager cm = new ComputeCloudClientManager(factory, clouds)) {
            TestComputeCloud cloud1 = new TestComputeCloud.Builder().apiEndpoint(ENDPOINT).user(USER).password(PASSWORD).build();
            clouds.add(cloud1);
            final ComputeCloudClient client1 = mockery.mock(ComputeCloudClient.class);
            mockery.checking(new Expectations() {{ oneOf(factory).createClient(ENDPOINT, USER, PASSWORD); will(returnValue(client1)); }});
            cm.createClient(cloud1).close();
            cm.createClient(cloud1).close();
            cm.createClient(ENDPOINT, USER, PASSWORD).close();

            TestComputeCloud cloud1x = new TestComputeCloud.Builder().apiEndpoint(ENDPOINT).user(USER).password(PASSWORD).build();
            clouds.add(cloud1x);
            cm.createClient(cloud1x).close();
            cm.createClient(ENDPOINT, USER, PASSWORD).close();

            TestComputeCloud cloud2 = new TestComputeCloud.Builder().apiEndpoint(ENDPOINT2).user(USER).password(PASSWORD).build();
            clouds.add(cloud2);
            final ComputeCloudClient client2 = mockery.mock(ComputeCloudClient.class);
            mockery.checking(new Expectations() {{ oneOf(factory).createClient(ENDPOINT2, USER, PASSWORD); will(returnValue(client2)); }});
            cm.createClient(cloud2).close();
            cm.createClient(cloud2).close();
            cm.createClient(ENDPOINT2, USER, PASSWORD).close();

            TestComputeCloud cloud3 = new TestComputeCloud.Builder().apiEndpoint(ENDPOINT).user(USER2).password(PASSWORD).build();
            clouds.add(cloud3);
            final ComputeCloudClient client3 = mockery.mock(ComputeCloudClient.class);
            mockery.checking(new Expectations() {{ oneOf(factory).createClient(ENDPOINT, USER2, PASSWORD); will(returnValue(client3)); }});
            cm.createClient(cloud3).close();
            cm.createClient(cloud3).close();
            cm.createClient(ENDPOINT, USER2, PASSWORD).close();

            TestComputeCloud cloud4 = new TestComputeCloud.Builder().apiEndpoint(ENDPOINT).user(USER).password(PASSWORD2).build();
            clouds.add(cloud4);
            final ComputeCloudClient client4 = mockery.mock(ComputeCloudClient.class);
            mockery.checking(new Expectations() {{ oneOf(factory).createClient(ENDPOINT, USER, PASSWORD2); will(returnValue(client4)); }});
            cm.createClient(cloud4).close();
            cm.createClient(cloud4).close();
            cm.createClient(ENDPOINT, USER, PASSWORD2).close();

            Assert.assertNotNull(cm.toString());

            mockery.checking(new Expectations() {{ oneOf(client1).close(); }});
            mockery.checking(new Expectations() {{ oneOf(client2).close(); }});
            mockery.checking(new Expectations() {{ oneOf(client3).close(); }});
            mockery.checking(new Expectations() {{ oneOf(client4).close(); }});
        }
    }

    @Test
    public void testCloseTwice() {
        final ComputeCloudClientFactory factory = mockery.mock(ComputeCloudClientFactory.class);
        try (ComputeCloudClientManager cm = new ComputeCloudClientManager(factory, Collections.<Cloud>emptyList())) {
            final ComputeCloudClient mockClient = mockery.mock(ComputeCloudClient.class);
            mockery.checking(new Expectations() {{ oneOf(factory).createClient(ENDPOINT, USER, PASSWORD); will(returnValue(mockClient)); }});
            try (ComputeCloudClient client = cm.createClient(ENDPOINT, USER, PASSWORD)) {
                client.close();
            }
            mockery.checking(new Expectations() {{ oneOf(mockClient); }});
        }
    }

    @Test
    public void testRemoveUnusedClients() {
        final ComputeCloudClientFactory factory = mockery.mock(ComputeCloudClientFactory.class);
        try (ComputeCloudClientManager cm = new ComputeCloudClientManager(factory, Collections.<Cloud>emptyList())) {
            TestComputeCloud cloud1 = new TestComputeCloud.Builder().apiEndpoint(ENDPOINT).user(USER).password(PASSWORD).build();
            final ComputeCloudClient client1 = mockery.mock(ComputeCloudClient.class);
            mockery.checking(new Expectations() {{ oneOf(factory).createClient(ENDPOINT, USER, PASSWORD); will(returnValue(client1)); }});
            cm.createClient(cloud1).close();
            cm.createClient(cloud1).close();

            TestComputeCloud cloud2 = new TestComputeCloud.Builder().apiEndpoint(ENDPOINT2).user(USER).password(PASSWORD).build();
            mockery.checking(new Expectations() {{ oneOf(client1).close(); }});
            final ComputeCloudClient client2 = mockery.mock(ComputeCloudClient.class);
            mockery.checking(new Expectations() {{ oneOf(factory).createClient(ENDPOINT2, USER, PASSWORD); will(returnValue(client2)); }});
            cm.createClient(cloud2).close();
            cm.createClient(cloud2).close();

            mockery.checking(new Expectations() {{ oneOf(client2).close(); }});
        }
    }
}
