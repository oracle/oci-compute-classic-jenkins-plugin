package com.oracle.cloud.compute.jenkins.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public class Shape {
    // Modified from swagger-codegen -l jaxrs-cxf-client
    // - renamed from ShapeResult to Shape
    // - removed ApiModelProperty
    // - replaced Integer with Long (root_disk_size overflow)
    // - normalized whitespace and comments
    // - simplified toString, used lineSeparator
    // - removed initializion of placementRequirements
    // - removed addPlacementRequirementsItem
    // - added equals

    private BigDecimal cpus;
    private Long gpus;
    private Long io;
    private Boolean isRootSsd;
    private String name;
    private Long ndsIopsLimit;
    private List<String> placementRequirements;
    private Long ram;
    private Long rootDiskSize;
    private Long ssdDataSize;
    private String uri;

    /**
     * Number of CPUs or partial CPUs allocated to instances of this shape.
     *
     * @return cpus
     */
    public BigDecimal getCpus() {
        return cpus;
    }

    public void setCpus(BigDecimal cpus) {
        this.cpus = cpus;
    }

    public Shape cpus(BigDecimal cpus) {
        this.cpus = cpus;
        return this;
    }

    /**
     * Number of gpu devices allocated to instances of this shape
     *
     * @return gpus
     */
    public Long getGpus() {
        return gpus;
    }

    public void setGpus(Long gpus) {
        this.gpus = gpus;
    }

    public Shape gpus(Long gpus) {
        this.gpus = gpus;
        return this;
    }

    /**
     * IO share allocated to instances of this shape.
     *
     * @return io
     */
    public Long getIo() {
        return io;
    }

    public void setIo(Long io) {
        this.io = io;
    }

    public Shape io(Long io) {
        this.io = io;
        return this;
    }

    /**
     * Store the root disk image on SSD storage.
     *
     * @return isRootSsd
     */
    public Boolean getIsRootSsd() {
        return isRootSsd;
    }

    public void setIsRootSsd(Boolean isRootSsd) {
        this.isRootSsd = isRootSsd;
    }

    public Shape isRootSsd(Boolean isRootSsd) {
        this.isRootSsd = isRootSsd;
        return this;
    }

    /**
     * Name of this shape.
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Shape name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Limits the rate of IO for NDS storage volumes.
     *
     * @return ndsIopsLimit
     */
    public Long getNdsIopsLimit() {
        return ndsIopsLimit;
    }

    public void setNdsIopsLimit(Long ndsIopsLimit) {
        this.ndsIopsLimit = ndsIopsLimit;
    }

    public Shape ndsIopsLimit(Long ndsIopsLimit) {
        this.ndsIopsLimit = ndsIopsLimit;
        return this;
    }

    /**
     * A list of strings specifying arbitrary tags on nodes to be matched on placement.
     *
     * @return placementRequirements
     */
    public List<String> getPlacementRequirements() {
        return placementRequirements;
    }

    public void setPlacementRequirements(List<String> placementRequirements) {
        this.placementRequirements = placementRequirements;
    }

    public Shape placementRequirements(List<String> placementRequirements) {
        this.placementRequirements = placementRequirements;
        return this;
    }

    /**
     * Number of megabytes of memory allocated to instances of this shape.
     *
     * @return ram
     */
    public Long getRam() {
        return ram;
    }

    public void setRam(Long ram) {
        this.ram = ram;
    }

    public Shape ram(Long ram) {
        this.ram = ram;
        return this;
    }

    /**
     * Size of the root disk in bytes.
     *
     * @return rootDiskSize
     */
    public Long getRootDiskSize() {
        return rootDiskSize;
    }

    public void setRootDiskSize(Long rootDiskSize) {
        this.rootDiskSize = rootDiskSize;
    }

    public Shape rootDiskSize(Long rootDiskSize) {
        this.rootDiskSize = rootDiskSize;
        return this;
    }

    /**
     * Size of the local SSD data disk in bytes.
     *
     * @return ssdDataSize
     */
    public Long getSsdDataSize() {
        return ssdDataSize;
    }

    public void setSsdDataSize(Long ssdDataSize) {
        this.ssdDataSize = ssdDataSize;
    }

    public Shape ssdDataSize(Long ssdDataSize) {
        this.ssdDataSize = ssdDataSize;
        return this;
    }

    /**
     * Uniform Resource Identifier
     *
     * @return uri
     */
    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Shape uri(String uri) {
        this.uri = uri;
        return this;
    }

    @Override
    public String toString() {
        return "class Shape {" + System.lineSeparator() +
                "    cpus: " + cpus + System.lineSeparator() +
                "    gpus: " + gpus + System.lineSeparator() +
                "    io: " + io + System.lineSeparator() +
                "    isRootSsd: " + isRootSsd + System.lineSeparator() +
                "    name: " + name + System.lineSeparator() +
                "    ndsIopsLimit: " + ndsIopsLimit + System.lineSeparator() +
                "    placementRequirements: " + placementRequirements + System.lineSeparator() +
                "    ram: " + ram + System.lineSeparator() +
                "    rootDiskSize: " + rootDiskSize + System.lineSeparator() +
                "    ssdDataSize: " + ssdDataSize + System.lineSeparator() +
                "    uri: " + uri + System.lineSeparator() +
                "}"
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || o.getClass() != getClass()) {
            return false;
        }

        Shape s = (Shape)o;
        return Objects.equals(cpus, s.cpus) &&
                Objects.equals(gpus, s.gpus) &&
                Objects.equals(io, s.io) &&
                Objects.equals(isRootSsd, s.isRootSsd) &&
                Objects.equals(name, s.name) &&
                Objects.equals(ndsIopsLimit, s.ndsIopsLimit) &&
                Objects.equals(placementRequirements, s.placementRequirements) &&
                Objects.equals(ram, s.ram) &&
                Objects.equals(rootDiskSize, s.rootDiskSize) &&
                Objects.equals(ssdDataSize, s.ssdDataSize) &&
                Objects.equals(uri, s.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cpus, gpus, io, isRootSsd, name, ndsIopsLimit, placementRequirements, ram,
               rootDiskSize, ssdDataSize, uri);
    }
}
