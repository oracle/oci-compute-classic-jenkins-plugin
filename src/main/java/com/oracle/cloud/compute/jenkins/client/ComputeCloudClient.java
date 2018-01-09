package com.oracle.cloud.compute.jenkins.client;

import java.util.Collection;

import com.oracle.cloud.compute.jenkins.model.ImageList;
import com.oracle.cloud.compute.jenkins.model.ImageListEntry;
import com.oracle.cloud.compute.jenkins.model.ImageListSourceType;
import com.oracle.cloud.compute.jenkins.model.InstanceOrchestration;
import com.oracle.cloud.compute.jenkins.model.SSHKey;
import com.oracle.cloud.compute.jenkins.model.SecurityList;
import com.oracle.cloud.compute.jenkins.model.Shape;

/**
 * Stateful connection to an endpoint server.  Implementations are not safe for
 * use by multiple threads by default.
 */
public interface ComputeCloudClient extends AutoCloseable {
    /**
     * Closes the client connection.
     */
    @Override
    void close();

    /**
     * Authenticates the client and stores an authentication token to allow
     * other methods to be called.
     *
     * @throws ComputeCloudClientUnauthorizedException if authentication failed
     * @throws ComputeCloudClientException if an error occurs communicating with
     * the endpoint server
     */
    void authenticate() throws ComputeCloudClientException;

    /**
     * Retrieves the CPU and memory details of all the available shapes.  The
     * resulting objects should be considered immutable.
     *
     * @return the collection of available {@link Shape} objects
     * @throws IllegalStateException if {@link #authenticate} was not called
     * @throws ComputeCloudClientUnauthorizedException if the authentication
     * token has timed out or the user is not authorized
     * @throws ComputeCloudClientException if an error occurs communicating with
     * the endpoint server
     */
    Collection<Shape> getShapes() throws ComputeCloudClientException;

    /**
     * Retrieve the list of imagelist according to source type
     * @param sourceType:
                   Oracle Public Image
                   Customer Private Image

     * @return the collection of {@link ImageList}
     * @throws ComputeCloudClientException if an error occurs communicating with
     * the endpoint server
     */
    Collection<ImageList> getImageLists(ImageListSourceType sourceType) throws ComputeCloudClientException;

    /**
     * Retrieves imagelist entries for a specified imagelist
     * @param imageListName ImageList name which used to get related versions
     *
     * @return the collection of {@link ImageListEntry}
     * @throws ComputeCloudClientException if an error occurs communicating with
     * the endpoint server
     */
    Collection<ImageListEntry> getImageListEntries(String imageListName) throws ComputeCloudClientException;

    /**
     * Retrieves the security lists
     *
     * @return the collection of {@link SecurityList}
     *
     * @throws IllegalStateException if {@link #authenticate} was not called
     * @throws ComputeCloudClientUnauthorizedException if the authentication
     * token has timed out or the user is not authorized
     * @throws ComputeCloudClientException if an error occurs communicating with
     * the endpoint server
     */
    Collection<SecurityList> getSecurityLists() throws ComputeCloudClientException;

    /**
     * Retrieves the SSH keys of the current user.
     *
     * @return the collection of {@link SSHKey}
     *
     * @throws IllegalStateException if {@link #authenticate} was not called
     * @throws ComputeCloudClientUnauthorizedException if the authentication
     * token has timed out or the user is not authorized
     * @throws ComputeCloudClientException if an error occurs communicating with
     * the endpoint server
     */
    Collection<SSHKey> getSSHKeys() throws ComputeCloudClientException;

    /**
     * Retrieves an SSH key of the current user.
     *
     * @param name of SSH key
     *
     * @return the SSH key, or null if not found
     * @throws IllegalStateException if {@link #authenticate} was not called
     * @throws ComputeCloudClientUnauthorizedException if the authentication
     * token has timed out or the user is not authorized
     * @throws ComputeCloudClientException if an error occurs communicating with
     * the endpoint server
     */
    SSHKey getSSHKey(String name) throws ComputeCloudClientException;

    /**
     * Creates an orchestration with the specified name that contains a
     * launchplan for an instance with the specified configuration.
     *
     * @param name the orchestration name
     * @param params the instance configuration
     * @throws ComputeCloudClientUnauthorizedException if the authentication
     * token has timed out or the user is not authorized
     * @throws ComputeCloudClientException if an error occurs communicating with
     * the endpoint server
     */
    void createInstanceOrchestration(String name, ComputeCloudInstanceOrchestrationConfig params) throws ComputeCloudClientException;

    /**
     * Gets a subset of properties from an instance orchestration created from
     * {@link #createInstanceOrchestration}
     *
     * @param name the orchestration name
     *
     * @return the {@link InstanceOrchestration} instance with the specified name
     * @throws ComputeCloudClientUnauthorizedException if the authentication
     * token has timed out or the user is not authorized
     * @throws ComputeCloudClientException if an error occurs communicating with
     * the endpoint server
     */
    InstanceOrchestration getInstanceOrchestration(String name) throws ComputeCloudClientException;

    /**
     * Starts an orchestration with the specified name.
     *
     * @param name the orchestration name
     * @throws ComputeCloudClientUnauthorizedException if the authentication
     * token has timed out or the user is not authorized
     * @throws ComputeCloudClientException if an error occurs communicating with
     * the endpoint server
     */
    void startOrchestration(String name) throws ComputeCloudClientException;

    /**
     * Stops an orchestration with the specified name.
     *
     * @param name the orchestration name
     * @throws ComputeCloudClientUnauthorizedException if the authentication
     * token has timed out or the user is not authorized
     * @throws ComputeCloudClientException if an error occurs communicating with
     * the endpoint server
     */
    void stopOrchestration(String name) throws ComputeCloudClientException;

    /**
     * Deletes an orchestration with the specified name.
     *
     * @param name the orchestration name
     * @throws ComputeCloudClientUnauthorizedException if the authentication
     * token has timed out or the user is not authorized
     * @throws ComputeCloudClientException if an error occurs communicating with
     * the endpoint server
     */
    void deleteOrchestration(String name) throws ComputeCloudClientException;
}
