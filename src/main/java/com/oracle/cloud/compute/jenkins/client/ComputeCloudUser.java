package com.oracle.cloud.compute.jenkins.client;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A two-part user of the form {@code /Compute-identityDomainName/user}.  The
 * special user {@code /oracle/public} is also accepted.
 */
public final class ComputeCloudUser {
    private static final String IDENTITY_DOMAIN_PREFIX = "Compute-";
    private static final Pattern IDENTITY_DOMAIN_NAME_PATTERN = Pattern.compile("[^/]+");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("[^/]+");
    private static final Pattern PATTERN = Pattern.compile("/(" + IDENTITY_DOMAIN_PREFIX + "(" + IDENTITY_DOMAIN_NAME_PATTERN + "))/(" + USERNAME_PATTERN + ")");

    private static String createString(String fullIdentityDomainName, String username) {
        return '/' + fullIdentityDomainName + '/' + username;
    }

    private static final String ORACLE_FULL_IDENTITY_DOMAIN_NAME = "oracle";
    private static final String ORACLE_PUBLIC_USERNAME = "public";
    public static final ComputeCloudUser ORACLE_PUBLIC =
            new ComputeCloudUser(createString(ORACLE_FULL_IDENTITY_DOMAIN_NAME, ORACLE_PUBLIC_USERNAME), ORACLE_FULL_IDENTITY_DOMAIN_NAME, null, ORACLE_PUBLIC_USERNAME);

    /**
     * Parse a two-part string.
     * @param user a string representing a user
     * @return the resultant {@link ComputeCloudUser} object
     *
     * @throws IllegalArgumentException if the string cannot be parsed.
     */
    public static ComputeCloudUser parse(String user) {
        if (user.equals(ORACLE_PUBLIC.getString())) {
            return ORACLE_PUBLIC;
        }

        Matcher matcher = PATTERN.matcher(Objects.requireNonNull(user, "user"));
        if (!matcher.matches()) {
            throw new IllegalArgumentException(user);
        }

        return new ComputeCloudUser(user, matcher.group(1), matcher.group(2), matcher.group(3));
    }

    /**
     * Creates a user from an identity domain name and a user name.
     *
     * @param identityDomainName identity domain name
     * @param username user name
     *
     * @return the {@link ComputeCloudUser} object created
     *
     * @throws IllegalArgumentException if the identity domain name or user name
     * is invalid
     */
    public static ComputeCloudUser valueOf(String identityDomainName, String username) {
        if (!IDENTITY_DOMAIN_NAME_PATTERN.matcher(identityDomainName).matches()) {
            throw new IllegalArgumentException("identityDomainName");
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException("username");
        }

        String fullIdentityDomainName = IDENTITY_DOMAIN_PREFIX + identityDomainName;
        String string = createString(fullIdentityDomainName, username);
        return new ComputeCloudUser(string, fullIdentityDomainName, identityDomainName, username);
    }

    private final String string;
    private final String fullIdentityDomainName;
    private final String identityDomainName;
    private final String username;

    private ComputeCloudUser(String string, String fullIdentityDomainName, String identityDomainName, String username) {
        this.string = string;
        this.fullIdentityDomainName = fullIdentityDomainName;
        this.identityDomainName = identityDomainName;
        this.username = username;
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

        ComputeCloudUser u = (ComputeCloudUser)o;
        return string.equals(u.string);
    }

    /**
     * The full user string in the form {@code /Compute-identityDomainName/user}.
     *
     * @return full user string
     */
    public String getString() {
        return string;
    }

    /**
     * The full identity domain name in the form {@code Compute-acme} for normal
     * accounts, or {@code oracle} for {@code #ORACLE_PUBLIC}.
     *
     * @return the full identity domain name
     */
    public String getFullIdentityDomainName() {
        return fullIdentityDomainName;
    }

    /**
     * The short identity domain name, or null for {@link #ORACLE_PUBLIC}.
     * For {@code Compute-acme}, this method returns {@code acme}.
     *
     * @return the short identity domain name or null for {@link #ORACLE_PUBLIC}
     */
    public String getIdentityDomainName() {
        return identityDomainName;
    }

    public String getUsername() {
        return username;
    }
}
