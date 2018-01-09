package com.oracle.cloud.compute.jenkins.model;

import java.util.Objects;

public class SSHKey {
    // Modified from swagger-codegen -l jaxrs-cxf-client
    // - renamed from SSHKeyResponse to SSHKey

    private Boolean enabled = null;
    private String key = null;
    private String name = null;
    private String uri = null;

    public SSHKey enabled(Boolean enabled) {
      this.enabled = enabled;
      return this;
    }

     /**
     * Indicates whether the key is enabled (<code>true</code>) or disabled.
     * @return enabled
    **/
    public Boolean getEnabled() {
      return enabled;
    }

    public void setEnabled(Boolean enabled) {
      this.enabled = enabled;
    }

    public SSHKey key(String key) {
      this.key = key;
      return this;
    }

     /**
     * <p>The SSH public key value.
     * @return key
    **/
    public String getKey() {
      return key;
    }

    public void setKey(String key) {
      this.key = key;
    }

    public SSHKey name(String name) {
      this.name = name;
      return this;
    }

     /**
     * <p>The three-part name of the object
     * @return name
    **/
    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public SSHKey uri(String uri) {
      this.uri = uri;
      return this;
    }

     /**
     * Uniform Resource Identifier
     * @return uri
    **/
    public String getUri() {
      return uri;
    }

    public void setUri(String uri) {
      this.uri = uri;
    }


    @Override
    public boolean equals(java.lang.Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      SSHKey ssHKeyResponse = (SSHKey) o;
      return Objects.equals(this.enabled, ssHKeyResponse.enabled) &&
          Objects.equals(this.key, ssHKeyResponse.key) &&
          Objects.equals(this.name, ssHKeyResponse.name) &&
          Objects.equals(this.uri, ssHKeyResponse.uri);
    }

    @Override
    public int hashCode() {
      return Objects.hash(enabled, key, name, uri);
    }


    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("class SSHKey {\n");

      sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
      sb.append("    key: ").append(toIndentedString(key)).append("\n");
      sb.append("    name: ").append(toIndentedString(name)).append("\n");
      sb.append("    uri: ").append(toIndentedString(uri)).append("\n");
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
}
