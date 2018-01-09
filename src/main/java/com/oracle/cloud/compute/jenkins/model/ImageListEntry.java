package com.oracle.cloud.compute.jenkins.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An imagelist entry is a specified version of image.
 * See <a href="http://docs.oracle.com/cloud/latest/stcomputecs/STCSG/GUID-923D7B80-E55A-4706-A6D6-5492ABACA704.htm#STCSG-GUID-923D7B80-E55A-4706-A6D6-5492ABACA704">Maintaining Versions of Private Machine Images</a>
 * for additional information.
 */
public class ImageListEntry {
    // Modified from swagger-codegen -l jaxrs-cxf-client
    // Modify ImageListEntryResponse to ImageListEntry
    private Object attributes = null;
    private String imagelist = null;
    private List<String> machineimages = new ArrayList<String>();
    private String uri = null;
    private Integer version = null;

    public ImageListEntry attributes(Object attributes) {
      this.attributes = attributes;
      return this;
    }

     /**
     * <p>User-defined parameters, in JSON format, that can be passed to an instance of this machine image when it is launched. This field can be used, for example, to specify the location of a database server and login details. Instance metadata, including user-defined data is available at http://192.0.0.192/ within an instance. See <a target="_blank" href="http://www.oracle.com/pls/topic/lookup?ctx=stcomputecs&id=STCSG-GUID-268FE284-E5A0-4A18-BA58-345660925FB7">Retrieving User-Defined Instance Attributes</a> in <em>Using Oracle Cloud Infrastructure Compute Classic Service (IaaS)</em>.
     * @return attributes
    **/
    public Object getAttributes() {
      return attributes;
    }

    public void setAttributes(Object attributes) {
      this.attributes = attributes;
    }

    public ImageListEntry imagelist(String imagelist) {
      this.imagelist = imagelist;
      return this;
    }

     /**
     * Name of the imagelist.
     * @return imagelist
    **/
    public String getImagelist() {
      return imagelist;
    }

    public void setImagelist(String imagelist) {
      this.imagelist = imagelist;
    }

    public ImageListEntry machineimages(List<String> machineimages) {
      this.machineimages = machineimages;
      return this;
    }

    public ImageListEntry addMachineimagesItem(String machineimagesItem) {
      this.machineimages.add(machineimagesItem);
      return this;
    }

     /**
     * <p>A list of machine images. Specify the three-part name of each machine image.
     * @return machineimages
    **/
    public List<String> getMachineimages() {
      return machineimages;
    }

    public void setMachineimages(List<String> machineimages) {
      this.machineimages = machineimages;
    }

    public ImageListEntry uri(String uri) {
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

    public ImageListEntry version(Integer version) {
      this.version = version;
      return this;
    }

     /**
     * Version number of these machineImages in the imagelist.
     * @return version
    **/
    public Integer getVersion() {
      return version;
    }

    public void setVersion(Integer version) {
      this.version = version;
    }


    @Override
    public boolean equals(java.lang.Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      ImageListEntry ImageListEntry = (ImageListEntry) o;
      return Objects.equals(this.attributes, ImageListEntry.attributes) &&
          Objects.equals(this.imagelist, ImageListEntry.imagelist) &&
          Objects.equals(this.machineimages, ImageListEntry.machineimages) &&
          Objects.equals(this.uri, ImageListEntry.uri) &&
          Objects.equals(this.version, ImageListEntry.version);
    }

    @Override
    public int hashCode() {
      return Objects.hash(attributes, imagelist, machineimages, uri, version);
    }


    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("class ImageListEntry {\n");

      sb.append("    attributes: ").append(toIndentedString(attributes)).append("\n");
      sb.append("    imagelist: ").append(toIndentedString(imagelist)).append("\n");
      sb.append("    machineimages: ").append(toIndentedString(machineimages)).append("\n");
      sb.append("    uri: ").append(toIndentedString(uri)).append("\n");
      sb.append("    version: ").append(toIndentedString(version)).append("\n");
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
