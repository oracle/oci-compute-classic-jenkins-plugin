package com.oracle.cloud.compute.jenkins.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An image list is a collection of Oracle Cloud Infrastructure Compute Classic Service machine images. Each machine image in an image list is identified by a unique entry number.
 * See <a href="http://docs.oracle.com/cloud/latest/stcomputecs/STCSG/GUID-923D7B80-E55A-4706-A6D6-5492ABACA704.htm#STCSG-GUID-923D7B80-E55A-4706-A6D6-5492ABACA704">Maintaining Versions of Private Machine Images</a>
 * for additional information.
 */
public class ImageList {
    // Modified from swagger-codegen -l jaxrs-cxf-client
    // Modify ImageListResponse to ImageList
    // remove setDefault method
    private Integer _default = null;
    private String description = null;
    private List<ImageListEntry> entries = new ArrayList<ImageListEntry>();
    private String name = null;
    private String uri = null;

    public ImageList _default(Integer _default) {
      this._default = _default;
      return this;
    }

     /**
     * <p>The image list entry to be used, by default, when launching instances using this image list. If you don't specify this value, it is set to 1.
     * @return _default
    **/
    public Integer getDefault() {
      return _default;
    }

    public ImageList description(String description) {
      this.description = description;
      return this;
    }

     /**
     * <p>A description of this image list.
     * @return description
    **/
    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public ImageList entries(List<ImageListEntry> entries) {
      this.entries = entries;
      return this;
    }

    public ImageList addEntriesItem(ImageListEntry entriesItem) {
      this.entries.add(entriesItem);
      return this;
    }

     /**
     * Each machine image in an image list is identified by an image list entry.
     * @return entries
    **/
    public List<ImageListEntry> getEntries() {
      return entries;
    }

    public void setEntries(List<ImageListEntry> entries) {
      this.entries = entries;
    }

    public ImageList name(String name) {
      this.name = name;
      return this;
    }

     /**
     * The three-part name of the image list.
     * @return name
    **/
    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public ImageList uri(String uri) {
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
      ImageList ImageList = (ImageList) o;
      return Objects.equals(this._default, ImageList._default) &&
          Objects.equals(this.description, ImageList.description) &&
          Objects.equals(this.entries, ImageList.entries) &&
          Objects.equals(this.name, ImageList.name) &&
          Objects.equals(this.uri, ImageList.uri);
    }

    @Override
    public int hashCode() {
      return Objects.hash(_default, description, entries, name, uri);
    }


    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("class ImageList {\n");

      sb.append("    _default: ").append(toIndentedString(_default)).append("\n");
      sb.append("    description: ").append(toIndentedString(description)).append("\n");
      sb.append("    entries: ").append(toIndentedString(entries)).append("\n");
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
