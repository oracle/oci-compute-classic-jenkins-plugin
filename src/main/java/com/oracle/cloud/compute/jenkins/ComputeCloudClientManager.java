package com.oracle.cloud.compute.jenkins;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.oracle.cloud.compute.jenkins.client.ComputeCloudClient;
import com.oracle.cloud.compute.jenkins.client.ComputeCloudClientFactory;
import com.oracle.cloud.compute.jenkins.client.ComputeCloudUser;
import com.oracle.cloud.compute.jenkins.client.ProxyComputeCloudClient;

import hudson.slaves.Cloud;
import jenkins.model.Jenkins;

/**
 * This manages a cache of ComputeCloudClient instances.  Instances are created
 * and added to the cache on demand, and instances are lazily removed from the
 * cache when there is no active ComputeCloud instance using it.
 */
public class ComputeCloudClientManager implements AutoCloseable, ComputeCloudClientFactory {
    static class ClientKey {
        final URI apiEndpoint;
        final ComputeCloudUser user;
        final String password;

        ClientKey(URI apiEndpoint, ComputeCloudUser user, String password) {
            this.apiEndpoint = apiEndpoint;
            this.user = user;
            this.password = password;
        }

        @Override
        public String toString() {
            return super.toString() + '[' + apiEndpoint + ", " + user + ", #" + Integer.toHexString(password.hashCode()) + ']';
        }

        @Override
        public int hashCode() {
            int result = 1;
            result = 31 * result + apiEndpoint.hashCode();
            result = 31 * result + user.hashCode();
            result = 31 * result + password.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || o.getClass() != getClass()) {
                return false;
            }

            ClientKey k = (ClientKey)o;
            return apiEndpoint.equals(k.apiEndpoint) &&
                    user.equals(k.user) &&
                    password.equals(k.password);
        }
    }

    private static class ClientData {
        final ComputeCloudClient client;
        private int numReferences = 1;

        public ClientData(ComputeCloudClient client) {
            this.client = client;
        }

        synchronized void addReference() {
            numReferences++;
        }

        synchronized void removeReference() {
            numReferences--;
            if (numReferences == 0) {
                client.close();
            }
        }
    }

    /**
     * A client implementation that calls {@link ClientData#decrement}
     * when {@link #close} is called.
     */
    private static class ManagedComputeCloudClient extends ProxyComputeCloudClient {
        private final ClientData clientData;
        private final AtomicBoolean closed = new AtomicBoolean();

        public ManagedComputeCloudClient(ClientData clientData) {
            super(clientData.client);
            this.clientData = clientData;
            clientData.addReference();
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                clientData.removeReference();
            }
        }
    }

    private final ComputeCloudClientFactory factory;
    private final Collection<Cloud> activeClouds;
    private final Map<ClientKey, ClientData> clients = new LinkedHashMap<>();

    /**
     * @param factory the client factory
     * @param activeClouds the list of active clouds, typically
     * {@link Jenkins#clouds}
     */
    public ComputeCloudClientManager(ComputeCloudClientFactory factory, Collection<Cloud> activeClouds) {
        this.factory = Objects.requireNonNull(factory, "factory");
        this.activeClouds = Objects.requireNonNull(activeClouds, "activeClouds");
    }

    @Override
    public String toString() {
        return super.toString() + '[' + factory + ", " + clients + ']';
    }

    @Override
    public synchronized void close() {
        for (ClientData clientData : clients.values()) {
            clientData.client.close();
        }
        clients.clear();
    }

    private static ClientKey createKey(ComputeCloud cloud) {
        return new ClientKey(cloud.getApiEndpointUrl(), cloud.getUser(), cloud.getPassword());
    }

    /**
     * Remove cached clients that are not used by any active ComputeCloud's.
     */
    private void removeUnusedClients() {
        Set<ClientKey> used = new HashSet<>();
        for (Cloud cloud : activeClouds) {
            if (cloud instanceof ComputeCloud) {
                ComputeCloud computeCloud = (ComputeCloud)cloud;
                try {
                    used.add(createKey(computeCloud));
                } catch (IllegalStateException e) {
                    // This cloud instance has an invalid apiEndpoint,
                    // identityDomainName, or userName, so just ignore it.
                }
            }
        }

        for (Iterator<Map.Entry<ClientKey, ClientData>> iter = clients.entrySet().iterator(); iter.hasNext();) {
            Map.Entry<ClientKey, ClientData> entry = iter.next();
            if (!used.contains(entry.getKey())) {
                iter.remove();
                entry.getValue().removeReference();
            }
        }
    }

    private synchronized ComputeCloudClient createClient(ClientKey key) {
        ClientData clientData = clients.get(key);
        if (clientData == null) {
            removeUnusedClients();
            clientData = new ClientData(factory.createClient(key.apiEndpoint, key.user, key.password));
            clients.put(key, clientData);
        }

        return new ManagedComputeCloudClient(clientData);
    }

    public ComputeCloudClient createClient(ComputeCloud cloud) {
        return createClient(createKey(cloud));
    }

    @Override
    public ComputeCloudClient createClient(URI apiEndpoint, ComputeCloudUser user, String password) {
        return createClient(new ClientKey(apiEndpoint, user, password));
    }
}
