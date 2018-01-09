package com.oracle.cloud.compute.jenkins.client;

import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.ClientBuilder;

import org.glassfish.jersey.logging.LoggingFeature;

public class JaxrsComputeCloudClientFactory implements ComputeCloudClientFactory {
    public static final ComputeCloudClientFactory INSTANCE = new JaxrsComputeCloudClientFactory();

    private static final String CLASS_NAME = JaxrsComputeCloudClientFactory.class.getName();
    static final boolean DEBUG = Boolean.getBoolean(CLASS_NAME + ".debug");

    private static final ClientBuilder BUILDER = ClientBuilder.newBuilder();
    static {
        if (DEBUG) {
            // We pass maxEntitySize=null, which defaults to
            // LoggingFeature.DEFAULT_MAX_ENTITY_SIZE (8192), but
            // https://java.net/jira/browse/JERSEY-3035 causes the response to
            // be truncated at fewer bytes anyway.
            BUILDER.register(new LoggingFeature(Logger.getLogger(CLASS_NAME), Level.INFO, LoggingFeature.Verbosity.PAYLOAD_ANY, null));
        }
    }

    private JaxrsComputeCloudClientFactory() {}

    @Override
    public ComputeCloudClient createClient(URI endpoint, ComputeCloudUser user, String password) {
        return new JaxrsComputeCloudClient(endpoint, user, password, BUILDER.build());
    }
}
