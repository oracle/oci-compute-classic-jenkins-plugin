package com.oracle.cloud.compute.jenkins.client;

/**
 * This exception is thrown when an authentication or authorization error occurs
 * while communicating with the API endpoint server.
 */
@SuppressWarnings("serial")
public class ComputeCloudClientUnauthorizedException extends ComputeCloudClientException {
    public ComputeCloudClientUnauthorizedException(String message) {
        super(message);
    }
}
