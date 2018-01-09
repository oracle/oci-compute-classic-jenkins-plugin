package com.oracle.cloud.compute.jenkins.client;

import java.util.List;

import com.oracle.cloud.compute.jenkins.model.ImageListSourceType;

public interface ComputeCloudInstanceOrchestrationConfig {
    String getOrchDescriptionValue();
    String getShapeName();
    List<String> getSecurityListNames();
    ImageListSourceType getImageListSource();
    String getImageListName();
    String getImageListEntry();
    String getVolumeSizeValue();
    String getSshKeyName();
    boolean isHypervisorPvEnabled();
}
