package com.oracle.cloud.compute.jenkins.client;

import java.util.Collection;

import com.oracle.cloud.compute.jenkins.model.ImageList;
import com.oracle.cloud.compute.jenkins.model.ImageListSourceType;
import com.oracle.cloud.compute.jenkins.model.Shape;

/**
 * An implementation of ComputeCloudClient that caches infrequently changing
 * data from the server.
 */
public class CachingComputeCloudClient extends ProxyComputeCloudClient {
    public CachingComputeCloudClient(ComputeCloudClient client) {
        super(client);
    }

    private Collection<Shape> shapes;
    private Collection<ImageList> oraclePublicImageLists;
    private Collection<ImageList> customerPrivateImageLists;

    private synchronized void clearCaches() {
        shapes = null;
        oraclePublicImageLists = null;
        customerPrivateImageLists = null;
    }

    @Override
    public void authenticate() throws ComputeCloudClientException {
        clearCaches();
        super.authenticate();
    }

    @Override
    public synchronized Collection<Shape> getShapes() throws ComputeCloudClientException {
        Collection<Shape> shapes = this.shapes;
        if (shapes == null) {
            shapes = super.getShapes();
            this.shapes = shapes;
        }
        return shapes;
    }

    @Override
    public synchronized Collection<ImageList> getImageLists(ImageListSourceType sourceType) throws ComputeCloudClientException {
        Collection<ImageList> imageLists = null;
        if (sourceType.equals(ImageListSourceType.ORACLE_PUBLIC_IMAGE)) {
            imageLists = this.oraclePublicImageLists;
        } else if (sourceType.equals(ImageListSourceType.PRIVATE_IAMGE)) {
            imageLists = this.customerPrivateImageLists;
        }

        if (imageLists == null) {
            imageLists = super.getImageLists(sourceType);
            if (sourceType.equals(ImageListSourceType.ORACLE_PUBLIC_IMAGE)) {
                this.oraclePublicImageLists = imageLists;
            } else if (sourceType.equals(ImageListSourceType.PRIVATE_IAMGE)) {
                this.customerPrivateImageLists = imageLists;
            }
        }
        return imageLists;
    }
}
