package com.oracle.cloud.compute.jenkins.model;

import java.util.Objects;
/**
 *A security list is a group of Oracle Cloud Infrastructure Compute Classic Service instances that
  you can specify as the source or destination in one or more security rules.
  The instances in a security list can communicate fully, on all ports,
  with other instances in the same security list.
  An instance could be added to one or more security lists, which may have
  different policies, the most restrictive policy is applicable to the instance.
  See the
  <a href="http://docs.oracle.com/cloud/latest/computecs_common/OCSUG/GUID-C94EF16D-DD88-46D9-B37E-20C2A3F62E6F.htm#OCSUG176">About Security Lists</a>
  documentation for additional information.
 *
 */
public class SecurityList {
    // Modified from swagger-codegen -l jaxrs-cxf-client
    // Modify SecListResponse to Security
    // Add groupId and id according to response object

    private String account;
    private String description;
    private String name;
    private String outboundCidrPolicy;
    private String policy;
    private String uri;
    private String groupId;
    private String id; // TODO: check whether id could be used as the identify as security object

    public SecurityList account(String account) {
        this.account = account;
        return this;
    }

    public SecurityList description(String description) {
        this.description = description;
        return this;
    }

    public SecurityList name(String name) {
        this.name = name;
        return this;
    }

    public SecurityList outboundCidrPolicy(String outboundCidrPolicy) {
        this.outboundCidrPolicy = outboundCidrPolicy;
        return this;
    }

    public SecurityList policy(String policy) {
        this.policy = policy;
        return this;
    }

    public SecurityList uri(String uri) {
        this.uri = uri;
        return this;
    }

    public SecurityList groupId(String groupId) {
        this.groupId = groupId;
        return this;
    }

    public SecurityList id(String id) {
        this.id = id;
        return this;
    }

    @Override
    public boolean equals(java.lang.Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      SecurityList securityList = (SecurityList) o;
      return Objects.equals(this.account, securityList.account) &&
          Objects.equals(this.description, securityList.description) &&
          Objects.equals(this.name, securityList.name) &&
          Objects.equals(this.outboundCidrPolicy, securityList.outboundCidrPolicy) &&
          Objects.equals(this.policy, securityList.policy) &&
          Objects.equals(this.uri, securityList.uri) &&
          Objects.equals(this.groupId, securityList.groupId) &&
          Objects.equals(this.id, securityList.id);
    }

    @Override
    public int hashCode() {
      return Objects.hash(account, description, name, outboundCidrPolicy, policy, uri, groupId, id);
    }


    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("class SecurityList {\n");

      sb.append("    account: ").append(toIndentedString(account)).append("\n");
      sb.append("    description: ").append(toIndentedString(description)).append("\n");
      sb.append("    name: ").append(toIndentedString(name)).append("\n");
      sb.append("    outboundCidrPolicy: ").append(toIndentedString(outboundCidrPolicy)).append("\n");
      sb.append("    policy: ").append(toIndentedString(policy)).append("\n");
      sb.append("    uri: ").append(toIndentedString(uri)).append("\n");
      sb.append("    groupId: ").append(toIndentedString(groupId)).append("\n");
      sb.append("    id: ").append(toIndentedString(id)).append("\n");
      sb.append("}");
      return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(java.lang.Object o) {
      if (o == null) {
        return "null";
      }
      return o.toString().replace("\n", "\n    ");
    }

    /**
     * @return the account
     */
    public String getAccount() {
        return account;
    }

    /**
     * @param account the account to set
     */
    public void setAccount(String account) {
        this.account = account;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the outboundCidrPolicy
     */
    public String getOutboundCidrPolicy() {
        return outboundCidrPolicy;
    }

    /**
     * @param outboundCidrPolicy the outboundCidrPolicy to set
     */
    public void setOutboundCidrPolicy(String outboundCidrPolicy) {
        this.outboundCidrPolicy = outboundCidrPolicy;
    }

    /**
     * @return the policy
     */
    public String getPolicy() {
        return policy;
    }

    /**
     * @param policy the policy to set
     */
    public void setPolicy(String policy) {
        this.policy = policy;
    }

    /**
     * @return the uri
     */
    public String getUri() {
        return uri;
    }

    /**
     * @param uri the uri to set
     */
    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * @return the groupId
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * @param groupId the groupId to set
     */
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }


}
