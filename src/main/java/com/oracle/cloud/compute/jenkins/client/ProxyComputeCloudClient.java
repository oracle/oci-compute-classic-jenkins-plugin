package com.oracle.cloud.compute.jenkins.client;

import java.util.Collection;

import com.oracle.cloud.compute.jenkins.model.ImageList;
import com.oracle.cloud.compute.jenkins.model.ImageListEntry;
import com.oracle.cloud.compute.jenkins.model.ImageListSourceType;
import com.oracle.cloud.compute.jenkins.model.InstanceOrchestration;
import com.oracle.cloud.compute.jenkins.model.SSHKey;
import com.oracle.cloud.compute.jenkins.model.SecurityList;
import com.oracle.cloud.compute.jenkins.model.Shape;

public class ProxyComputeCloudClient implements ComputeCloudClient {
    protected final ComputeCloudClient client;

    public ProxyComputeCloudClient(ComputeCloudClient client) {
        this.client = client;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '[' + client + ']';
    }

    protected void preInvoke() throws ComputeCloudClientException {}

    @Override
    public void close() {
        client.close();
    }

    @Override
    public void authenticate() throws ComputeCloudClientException {
        preInvoke();
        client.authenticate();
    }

    @Override
    public Collection<Shape> getShapes() throws ComputeCloudClientException {
        preInvoke();
        return client.getShapes();
    }

    @Override
    public Collection<SecurityList> getSecurityLists() throws ComputeCloudClientException {
        preInvoke();
        return client.getSecurityLists();
    }

    @Override
    public Collection<SSHKey> getSSHKeys() throws ComputeCloudClientException {
        preInvoke();
        return client.getSSHKeys();
    }

    @Override
    public SSHKey getSSHKey(String name) throws ComputeCloudClientException {
        preInvoke();
        return client.getSSHKey(name);
    }

    @Override
    public Collection<ImageList> getImageLists(ImageListSourceType sourceType) throws ComputeCloudClientException {
        preInvoke();
        return client.getImageLists(sourceType);
    }

    @Override
    public Collection<ImageListEntry> getImageListEntries(String imageListName) throws ComputeCloudClientException {
        preInvoke();
        return client.getImageListEntries(imageListName);
    }

    @Override
    public void createInstanceOrchestration(String name, ComputeCloudInstanceOrchestrationConfig params) throws ComputeCloudClientException {
        preInvoke();
        client.createInstanceOrchestration(name, params);
    }

    @Override
    public InstanceOrchestration getInstanceOrchestration(String name) throws ComputeCloudClientException {
        preInvoke();
        return client.getInstanceOrchestration(name);
    }

    @Override
    public void startOrchestration(String name) throws ComputeCloudClientException {
        preInvoke();
        client.startOrchestration(name);
    }

    @Override
    public void stopOrchestration(String name) throws ComputeCloudClientException {
        preInvoke();
        client.stopOrchestration(name);
    }

    @Override
    public void deleteOrchestration(String name) throws ComputeCloudClientException {
        preInvoke();
        client.deleteOrchestration(name);
    }
}
