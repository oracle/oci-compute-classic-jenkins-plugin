package com.oracle.cloud.compute.jenkins.client;

/**
 * This exception and subclasses are thrown when an error occurs communicating
 * with the API endpoint server.
 */
@SuppressWarnings("serial")
public class ComputeCloudClientException extends Exception {
    public ComputeCloudClientException(String message) {
        super(message);
    }

    public ComputeCloudClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
