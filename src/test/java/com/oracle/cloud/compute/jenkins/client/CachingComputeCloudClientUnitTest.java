package com.oracle.cloud.compute.jenkins.client;

import java.util.Arrays;
import java.util.Collections;

import org.jmock.Expectations;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import com.oracle.cloud.compute.jenkins.ComputeCloudMockery;
import com.oracle.cloud.compute.jenkins.model.ImageListSourceType;
import com.oracle.cloud.compute.jenkins.model.Shape;

public class CachingComputeCloudClientUnitTest {
    @Rule
    public final ComputeCloudMockery mockery = new ComputeCloudMockery();

    private void clearCaches(final ComputeCloudClient mockClient, CachingComputeCloudClient client) throws Exception {
        mockery.checking(new Expectations() {{ oneOf(mockClient).authenticate(); }});
        client.authenticate();
    }

    @Test
    public void testGetShapes() throws Exception {
        final ComputeCloudClient mockClient = mockery.mock(ComputeCloudClient.class);
        try (CachingComputeCloudClient client = new CachingComputeCloudClient(mockClient)) {
            mockery.checking(new Expectations() {{ oneOf(mockClient).getShapes(); will(returnValue(Collections.emptyList())); }});
            Assert.assertEquals(client.getShapes(), client.getShapes());

            clearCaches(mockClient, client);
            mockery.checking(new Expectations() {{ oneOf(mockClient).getShapes(); will(returnValue(Arrays.asList(new Shape()))); }});
            Assert.assertEquals(client.getShapes(), client.getShapes());

            mockery.checking(new Expectations() {{ oneOf(mockClient).close(); }});
        }
    }

    @Test
    public void testGetImageLists() throws Exception {
        final ComputeCloudClient mockClient = mockery.mock(ComputeCloudClient.class);
        try (CachingComputeCloudClient client = new CachingComputeCloudClient(mockClient)) {
            mockery.checking(new Expectations() {{ oneOf(mockClient).getImageLists(ImageListSourceType.ORACLE_PUBLIC_IMAGE); will(returnValue(Collections.emptyList())); }});
            Assert.assertEquals(client.getImageLists(ImageListSourceType.ORACLE_PUBLIC_IMAGE), client.getImageLists(ImageListSourceType.ORACLE_PUBLIC_IMAGE));

            clearCaches(mockClient, client);
            mockery.checking(new Expectations() {{ oneOf(mockClient).getImageLists(ImageListSourceType.ORACLE_PUBLIC_IMAGE); will(returnValue(Arrays.asList("iln"))); }});
            Assert.assertEquals(client.getImageLists(ImageListSourceType.ORACLE_PUBLIC_IMAGE), client.getImageLists(ImageListSourceType.ORACLE_PUBLIC_IMAGE));

            mockery.checking(new Expectations() {{ oneOf(mockClient).close(); }});
        }
    }
}
