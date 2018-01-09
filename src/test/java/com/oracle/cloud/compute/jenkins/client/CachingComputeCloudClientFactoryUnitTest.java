package com.oracle.cloud.compute.jenkins.client;

import static com.oracle.cloud.compute.jenkins.ComputeCloudTestUtils.ENDPOINT;
import static com.oracle.cloud.compute.jenkins.ComputeCloudTestUtils.PASSWORD;
import static com.oracle.cloud.compute.jenkins.ComputeCloudTestUtils.USER;

import java.util.Collections;

import org.jmock.Expectations;
import org.junit.Rule;
import org.junit.Test;

import com.oracle.cloud.compute.jenkins.ComputeCloudMockery;

public class CachingComputeCloudClientFactoryUnitTest {
    @Rule
    public final ComputeCloudMockery mockery = new ComputeCloudMockery();

    @Test
    public void test() throws Exception {
        final ComputeCloudClientFactory factory = mockery.mock(ComputeCloudClientFactory.class);
        final ComputeCloudClient mockClient = mockery.mock(ComputeCloudClient.class);
        mockery.checking(new Expectations() {{ oneOf(factory).createClient(ENDPOINT, USER, PASSWORD); will(returnValue(mockClient)); }});
        ComputeCloudClient client = new CachingComputeCloudClientFactory(factory).createClient(ENDPOINT, USER, PASSWORD);

        mockery.checking(new Expectations() {{ oneOf(mockClient).getShapes(); will(returnValue(Collections.emptyList())); }});
        client.getShapes();
        client.getShapes();
    }
}
