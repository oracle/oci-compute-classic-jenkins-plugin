package com.oracle.cloud.compute.jenkins.model;

/**
 * According to the source of machine image, two imagelist type are defined:
 *  Oracle Public Image which provided by oracle.
 *  Private Image which created by users.
 *
 */
public enum ImageListSourceType {

    ORACLE_PUBLIC_IMAGE("Oracle Public Image"),
    PRIVATE_IAMGE("Private Image");

    private String value;

    private ImageListSourceType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }

    /**
     * Use this in place of valueOf.
     *
     * @param value
     *        real value
     * @return InstanceType corresponding to the value
     */
    public static ImageListSourceType fromValue(String value) {
        if (value == null || "".equals(value)) {
            throw new IllegalArgumentException("Value cannot be null or empty!");
        }

        for (ImageListSourceType enumEntry : ImageListSourceType.values()) {
            if (enumEntry.toString().equals(value)) {
                return enumEntry;
            }
        }

        throw new IllegalArgumentException("Cannot create enum from " + value + " value!");
    }
}
