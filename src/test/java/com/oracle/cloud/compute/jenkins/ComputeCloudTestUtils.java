package com.oracle.cloud.compute.jenkins;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.TreeSet;

import com.oracle.cloud.compute.jenkins.client.ComputeCloudUser;

import hudson.model.labels.LabelAtom;
import hudson.util.QuotedStringTokenizer;
import jenkins.model.Jenkins;

public class ComputeCloudTestUtils {
    public static final String INVALID_ENDPOINT = ":";
    public static final URI ENDPOINT;
    public static final URI ENDPOINT2;
    static {
        try {
            ENDPOINT = new URI("http://doesnotexist");
            ENDPOINT2 = new URI("http://doesnotexist2");
        } catch (URISyntaxException e) {
            throw new Error(e);
        }
    }

    public static final String INVALID_IDENTITY_DOMAIN_NAME = "/";
    public static final String INVALID_USER_NAME = "/";
    public static final ComputeCloudUser USER = ComputeCloudUser.parse("/Compute-acme/jack.jones@example.com");
    public static final ComputeCloudUser USER2 = ComputeCloudUser.parse("/Compute-acme/jack.jones2@example.com");
    public static final String PASSWORD = "password";
    public static final String PASSWORD2 = "password2";

    /**
     * Similar to {@link hudson.model.Label#parse} but does not use
     * {@link Jenkins#getInstance}.
     */
    public static Collection<LabelAtom> parseLabels(String labels) {
        Collection<LabelAtom> result = new TreeSet<>();
        if (labels != null && !labels.isEmpty()) {
            for (QuotedStringTokenizer tok = new QuotedStringTokenizer(labels); tok.hasMoreTokens();) {
                result.add(new LabelAtom(tok.nextToken()));
            }
        }
        return result;
    }
}
