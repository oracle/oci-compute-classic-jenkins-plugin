package com.oracle.cloud.compute.jenkins.client;

import java.util.concurrent.TimeUnit;

import com.oracle.cloud.compute.jenkins.Clock;

/**
 * An implementation of ComputeCloudClient that automatically authenticates
 * when a method is called and automatically reauthenticates when the
 * authentication tokens expire.
 */
public class AutoAuthComputeCloudClient extends ProxyComputeCloudClient {
    /**
     * The
     * <a href="http://docs.oracle.com/cloud/latest/stcomputecs/STCSA/Authentication.html">documented</a>
     * duration of an authentication token.
     */
    static final long AUTHENTICATION_TIMEOUT_NANOS = TimeUnit.MINUTES.toNanos(30);

    /**
     * The time before an authentication timeout when this client will
     * proactively reauthenticate.
     */
    static final long REAUTHENTICATE_SLACK_NANOS = TimeUnit.MINUTES.toNanos(5);

    private final Clock clock;
    private boolean authenticated;
    private long authenticatedNanoTime;

    public AutoAuthComputeCloudClient(ComputeCloudClient client) {
        this(client, Clock.INSTANCE);
    }

    public AutoAuthComputeCloudClient(ComputeCloudClient client, Clock clock) {
        super(client);
        this.clock = clock;
    }

    @Override
    public void close() {
        client.close();
    }

    private synchronized void authenticate(long nanoTime) throws ComputeCloudClientException {
        client.authenticate();
        authenticated = true;
        authenticatedNanoTime = nanoTime;
    }



    /**
     * {@inheritDoc}
     * This will always reauthenticate even if there is a cached token.
     */
    @Override
    public void authenticate() throws ComputeCloudClientException {
        authenticate(clock.nanoTime());
    }

    @Override
    protected synchronized void preInvoke() throws ComputeCloudClientException {
        long nanoTime = clock.nanoTime();
        if (authenticated && nanoTime - authenticatedNanoTime < AUTHENTICATION_TIMEOUT_NANOS - REAUTHENTICATE_SLACK_NANOS) {
            return;
        }

        authenticate(nanoTime);
    }
}
