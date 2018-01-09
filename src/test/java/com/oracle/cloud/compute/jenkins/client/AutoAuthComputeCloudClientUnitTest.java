package com.oracle.cloud.compute.jenkins.client;

import java.util.Collections;

import org.jmock.Expectations;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import com.oracle.cloud.compute.jenkins.ComputeCloudMockery;
import com.oracle.cloud.compute.jenkins.TestClock;

public class AutoAuthComputeCloudClientUnitTest {
    @Rule
    public final ComputeCloudMockery mockery = new ComputeCloudMockery();

    @Test
    public void testAuthenticate() throws Exception {
        final ComputeCloudClient mockClient = mockery.mock(ComputeCloudClient.class);

        TestClock clock = new TestClock();
        try (ComputeCloudClient client = new AutoAuthComputeCloudClient(mockClient, clock)) {
            mockery.checking(new Expectations() {{ oneOf(mockClient).authenticate(); }});
            client.authenticate();

            mockery.checking(new Expectations() {{ oneOf(mockClient).authenticate(); }});
            client.authenticate();

            clock.nanoTime += AutoAuthComputeCloudClient.AUTHENTICATION_TIMEOUT_NANOS - AutoAuthComputeCloudClient.REAUTHENTICATE_SLACK_NANOS - 1;
            mockery.checking(new Expectations() {{ oneOf(mockClient).authenticate(); }});
            client.authenticate();

            clock.nanoTime++;
            mockery.checking(new Expectations() {{ oneOf(mockClient).authenticate(); }});
            client.authenticate();

            mockery.checking(new Expectations() {{ oneOf(mockClient).close(); }});
        }
    }

    @Test
    public void testAutoAuthenticate() throws Exception {
        final ComputeCloudClient mockClient = mockery.mock(ComputeCloudClient.class);
        mockery.checking(new Expectations() {{ allowing(mockClient).getShapes(); will(returnValue(Collections.emptyList())); }});

        TestClock clock = new TestClock();
        try (ComputeCloudClient client = new AutoAuthComputeCloudClient(mockClient, clock)) {
            mockery.checking(new Expectations() {{ oneOf(mockClient).authenticate(); }});
            client.getShapes();

            client.getShapes();

            clock.nanoTime += AutoAuthComputeCloudClient.AUTHENTICATION_TIMEOUT_NANOS - AutoAuthComputeCloudClient.REAUTHENTICATE_SLACK_NANOS - 1;
            client.getShapes();

            clock.nanoTime++;
            mockery.checking(new Expectations() {{ oneOf(mockClient).authenticate(); }});
            client.getShapes();

            mockery.checking(new Expectations() {{ oneOf(mockClient).close(); }});
        }
    }

    @Test
    public void testAuthenticateError() throws Exception {
        final ComputeCloudClient mockClient = mockery.mock(ComputeCloudClient.class);
        try (ComputeCloudClient client = new AutoAuthComputeCloudClient(mockClient, new TestClock())) {
            mockery.checking(new Expectations() {{ oneOf(mockClient).authenticate(); will(throwException(new ComputeCloudClientException("test"))); }});
            try {
                client.authenticate();
                Assert.fail();
            } catch (ComputeCloudClientException e) {}

            mockery.checking(new Expectations() {{ oneOf(mockClient).authenticate(); }});
            client.authenticate();

            mockery.checking(new Expectations() {{ oneOf(mockClient).close(); }});
        }
    }

    @Test
    public void testGetShapes() throws Exception {
        final ComputeCloudClient mockClient = mockery.mock(ComputeCloudClient.class);
        try (ComputeCloudClient client = new AutoAuthComputeCloudClient(mockClient, new TestClock())) {
            mockery.checking(new Expectations() {{ oneOf(mockClient).authenticate(); }});
            mockery.checking(new Expectations() {{ oneOf(mockClient).getShapes(); will(returnValue(Collections.emptyList())); }});
            Assert.assertEquals(Collections.emptyList(), client.getShapes());

            mockery.checking(new Expectations() {{ oneOf(mockClient).close(); }});
        }
    }

    @Test
    public void testGetSecurityLists() throws Exception {
        final ComputeCloudClient mockClient = mockery.mock(ComputeCloudClient.class);
        try (ComputeCloudClient client = new AutoAuthComputeCloudClient(mockClient, new TestClock())) {
            mockery.checking(new Expectations() {{ oneOf(mockClient).authenticate(); }});
            mockery.checking(new Expectations() {{ oneOf(mockClient).getSecurityLists(); will(returnValue(Collections.emptyList())); }});
            Assert.assertEquals(Collections.emptyList(), client.getSecurityLists());

            mockery.checking(new Expectations() {{ oneOf(mockClient).close(); }});
        }
    }
}
