package com.oracle.cloud.compute.jenkins.client;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A three-part name of the form {@code /Compute-identityDomainName/user/name}.
 */
public class ComputeCloudObjectName {
    private static final Pattern NAME_PATTERN = Pattern.compile(".+");
    private static final Pattern PATTERN = Pattern.compile("(/[^/]+/[^/]+)/(" + NAME_PATTERN + ")");

    /**
     * Parses a three-part string.
     *
     * @param string a three-part string
     *
     * @return the resultant {@link ComputeCloudObjectName} object
     *
     * @throws IllegalArgumentException if the string cannot be parsed.
     */
    public static ComputeCloudObjectName parse(String string) {
        Matcher matcher = PATTERN.matcher(string);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(string);
        }

        ComputeCloudUser user = ComputeCloudUser.parse(matcher.group(1));
        String name = matcher.group(2);
        return new ComputeCloudObjectName(string, user, name);
    }

    /**
     * Creates an object name from a user and name.
     *
     * @param user the user
     * @param name the name
     *
     * @return the created {@link ComputeCloudObjectName} object
     *
     * @throws IllegalArgumentException if the name is invalid
     */
    public static ComputeCloudObjectName valueOf(ComputeCloudUser user, String name) {
        Objects.requireNonNull(user, "user");
        if (!NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("name");
        }

        String string = user.getString() + '/' + name;
        return new ComputeCloudObjectName(string, user, name);
    }

    private final String string;
    private final ComputeCloudUser user;
    private final String name;

    private ComputeCloudObjectName(String string, ComputeCloudUser user, String name) {
        this.string = string;
        this.user = user;
        this.name = name;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '[' + string + ']';
    }

    @Override
    public int hashCode() {
        return string.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || o.getClass() != getClass()) {
            return false;
        }

        ComputeCloudObjectName n = (ComputeCloudObjectName)o;
        return string.equals(n.string);
    }

    /**
     * Returns the three-part string.
     *
     * @return the three-part string
     */
    public String getString() {
        return string;
    }

    public ComputeCloudUser getUser() {
        return user;
    }

    public String getName() {
        return name;
    }
}
