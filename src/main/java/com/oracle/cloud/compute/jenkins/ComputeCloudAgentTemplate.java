package com.oracle.cloud.compute.jenkins;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.oracle.cloud.compute.jenkins.client.ComputeCloudClient;
import com.oracle.cloud.compute.jenkins.client.ComputeCloudClientException;
import com.oracle.cloud.compute.jenkins.client.ComputeCloudInstanceOrchestrationConfig;
import com.oracle.cloud.compute.jenkins.client.ComputeCloudObjectName;
import com.oracle.cloud.compute.jenkins.model.ImageList;
import com.oracle.cloud.compute.jenkins.model.ImageListEntry;
import com.oracle.cloud.compute.jenkins.model.ImageListSourceType;
import com.oracle.cloud.compute.jenkins.model.SSHKey;
import com.oracle.cloud.compute.jenkins.model.SecurityList;
import com.oracle.cloud.compute.jenkins.model.Shape;

import hudson.Extension;
import hudson.RelativePath;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.labels.LabelAtom;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.bouncycastle.api.PEMEncodable;

public class ComputeCloudAgentTemplate implements Describable<ComputeCloudAgentTemplate>, ComputeCloudInstanceOrchestrationConfig {
    private static final Logger LOGGER = Logger.getLogger(ComputeCloudAgentTemplate.class.getName());

    /**
     * The syntax required by the REST endpoint is "[0-9]+[bBkKmMgGtT]".
     * We also accept plain "[0-9]+", which is accepted by the cloud UI.
     * We also accept "[0-9]+[kKmMgGtT][bB]" since users are likely to add an extra "B".
     */
    private static final Pattern VOLUME_SIZE_PATTERN = Pattern.compile("([0-9]+)([bB]|[kKmMgGtT][bB]?)?");
    private static final String DEFAULT_SSH_USER = "opc";
    static final int FAILURE_COUNT_LIMIT = 3;

    private static FormValidationValue<String> checkVolumeSize(String value) {
        FormValidation fv = JenkinsUtil.validateRequired(value);
        if (fv.kind != FormValidation.Kind.OK) {
            return FormValidationValue.error(fv);
        }

        Matcher matcher = VOLUME_SIZE_PATTERN.matcher(value);
        if (!matcher.matches()) {
            return FormValidationValue.error(Messages.ComputeCloudAgentTemplate_volumeSize_invalid());
        }

        String size = matcher.group(1);
        String unit = matcher.group(2);

        if (unit == null) {
            unit = "G";
        } else if (unit.length() == 2) {
            // Change "gb" to "g".
            unit = unit.substring(0, 1);
        }

        return FormValidationValue.ok(size + unit);
    }

    private final String description;
    private final String labelString;
    private transient Collection<LabelAtom> labelAtoms;
    private final Node.Mode mode;
    private final String initScript;
    private final String numExecutors;
    private final String idleTerminationMinutes;
    private final int templateId;
    private final String orchDescription;
    private final String shapeName;
    private final List<String> securityListNames;
    private final ImageListSourceType imageListSource;
    private final String imageListName;
    private final String imageListEntry;
    private final boolean hypervisorPVEnabled;
    private final String volumeSize;
    private final String remoteFS;
    private final String sshUser;
    private final String sshConnectTimeoutSeconds;
    //the name of ssh public key located on target vm instance, which used for ssh connection;
    private final String sshKeyName;
    //the ssh private key set by user, which used for ssh connection;
    private final String privateKey;
    private final String startTimeoutSeconds;
    private final String initScriptTimeoutSeconds;

    private transient int failureCount;
    private transient String disableCause;

    @DataBoundConstructor
    public ComputeCloudAgentTemplate(
            final String description,
            final String numExecutors,
            Node.Mode mode,
            final String labelString,
            final String idleTerminationMinutes,
            final int templateId,
            final String orchDescription,
            final String shapeName,
            final List<String> securityListNames,
            final String imageListSource,
            final String imageListName,
            final String imageListEntry,
            final boolean hypervisorPVEnabled,
            final String volumeSize,
            final String remoteFS,
            final String sshUser,
            final String sshConnectTimeoutSeconds,
            final String sshKeyName,
            final String privateKey,
            final String initScript,
            final String startTimeoutSeconds,
            final String initScriptTimeoutSeconds) {
        this.description = description;
        this.numExecutors = numExecutors;
        this.mode = mode;
        this.labelString = labelString;
        this.idleTerminationMinutes = idleTerminationMinutes;
        this.templateId = templateId;
        this.orchDescription = orchDescription;
        this.shapeName = FormFillFailure.getErrorValue(shapeName);
        this.securityListNames = securityListNames == null || securityListNames.isEmpty() ?
                    Collections.<String>emptyList() : securityListNames;
        this.imageListSource = ImageListSourceType.fromValue(imageListSource);
        this.imageListName = FormFillFailure.getErrorValue(imageListName);
        this.imageListEntry = imageListEntry;
        this.hypervisorPVEnabled = hypervisorPVEnabled;
        this.volumeSize = volumeSize;
        this.remoteFS = remoteFS;
        this.sshUser = sshUser;
        this.sshConnectTimeoutSeconds = sshConnectTimeoutSeconds;
        this.sshKeyName = FormFillFailure.getErrorValue(sshKeyName);
        this.privateKey = privateKey;
        this.initScript = initScript;
        this.startTimeoutSeconds = startTimeoutSeconds;
        this.initScriptTimeoutSeconds = initScriptTimeoutSeconds;
    }

    public String getDisplayName() {
        return String.valueOf(getDescription());
    }

    public String getDescription() {
        return description;
    }

    public String getNumExecutors() {
        return numExecutors;
    }

    private static FormValidationValue<Integer> checkNumExecutors(String value) {
        return FormValidationValue.validatePositiveInteger(value, 1);
    }

    public int getNumExecutorsValue() {
        return checkNumExecutors(numExecutors).getValue();
    }

    public Node.Mode getMode() {
        return mode;
    }

    public String getLabelString() {
        return labelString;
    }

    Collection<LabelAtom> parseLabels(String labels) {
        return Label.parse(labels);
    }

    public synchronized Collection<LabelAtom> getLabelAtoms() {
        Collection<LabelAtom> labelAtoms = this.labelAtoms;
        if (labelAtoms == null) {
            labelAtoms = parseLabels(labelString);
            this.labelAtoms = labelAtoms;
        }
        return labelAtoms;
    }

    public String getIdleTerminationMinutes() {
        return idleTerminationMinutes;
    }

    public int getTemplateId() {
        return templateId;
    }

    public String getOrchDescription() {
        return orchDescription;
    }

    @Override
    public String getOrchDescriptionValue() {
        if (orchDescription == null || orchDescription.isEmpty()) {
            String result = Messages.ComputeCloudAgentTemplate_orchDescription_default();
            if (description != null && !description.isEmpty()) {
                result += ": " + description;
            }
            return result;
        }
        return orchDescription;
    }

    @Override
    public String getShapeName() {
        return shapeName;
    }

    public String getSecurityListName() {
        return securityListNames.isEmpty() ? null : securityListNames.get(0);
    }

    @Override
    public List<String> getSecurityListNames() {
        return securityListNames;
    }

    @Override
    public ImageListSourceType getImageListSource() {
        return imageListSource;
    }

    @Override
    public String getImageListName() {
        return imageListName;
    }

    @Override
    public String getImageListEntry() {
        return imageListEntry;
    }

    @Override
    public boolean isHypervisorPvEnabled() {
        return hypervisorPVEnabled;
    }

    public String getVolumeSize() {
        return volumeSize;
    }

    @Override
    public String getVolumeSizeValue() {
        FormValidationValue<String> valid = checkVolumeSize(volumeSize);
        return valid.isOk() ? valid.getValue() : volumeSize;
    }

    public String getRemoteFS() {
        return remoteFS;
    }

    public String getSshUser() {
        return sshUser;
    }

    public String getSshUserValue() {
        return sshUser == null || sshUser.trim().isEmpty() ? DEFAULT_SSH_USER : sshUser;
    }

    public String getSshConnectTimeoutSeconds() {
        return sshConnectTimeoutSeconds;
    }

    private static FormValidationValue<Integer> checkSshConnectTimeoutSeconds(String value) {
        return FormValidationValue.validateNonNegativeInteger(value, 30);
    }

    public int getSshConnectTimeoutMillis() {
        return (int)TimeUnit.SECONDS.toMillis(checkSshConnectTimeoutSeconds(sshConnectTimeoutSeconds).getValue());
    }

    public int getInitScriptTimeoutSeconds() {
        return (int)TimeUnit.SECONDS.toSeconds(checkInitScriptTimeoutSeconds(initScriptTimeoutSeconds).getValue());
    }

    private static FormValidationValue<Integer> checkInitScriptTimeoutSeconds(String value){
        return FormValidationValue.validateNonNegativeInteger(value, 120);
    }


    @Override
    public String getSshKeyName() {
        return this.sshKeyName;
    }

    public String getPrivateKey() {
        return this.privateKey;
    }

    public String getInitScript() {
        return initScript;
    }

    public String getStartTimeoutSeconds() {
        return startTimeoutSeconds;
    }

    private static FormValidationValue<Integer> checkStartTimeoutSeconds(String value) {
        return FormValidationValue.validateNonNegativeInteger(value, (int)TimeUnit.MINUTES.toSeconds(5));
    }

    public long getStartTimeoutNanos() {
        return TimeUnit.SECONDS.toNanos(checkStartTimeoutSeconds(startTimeoutSeconds).getValue());
    }

    public synchronized void increaseFailureCount(String cause) {
        if (++failureCount >= FAILURE_COUNT_LIMIT) {
            LOGGER.warning("Agent template " + getDisplayName() + " disabled due to error: " + cause);
            disableCause = cause;
        }
    }

    public synchronized void resetFailureCount() {
        if (failureCount > 0) {
            failureCount = 0;
            if (LOGGER.isLoggable(Level.FINE)) LOGGER.fine("Agent template " + getDisplayName() + " is reset");
        }
        if (disableCause != null) {
            disableCause = null;
            LOGGER.info("Agent template " + getDisplayName() + " is re-enabled");
        }
    }

    public synchronized String getDisableCause() {
        return disableCause;
    }

    @Override
    public Descriptor<ComputeCloudAgentTemplate> getDescriptor() {
        return JenkinsUtil.getDescriptorOrDie(getClass());
    }

    static class ConfigMessages {
        static final DynamicResourceBundleHolder holder = DynamicResourceBundleHolder.get(ComputeCloudAgentTemplate.class, "config");

        public static String sshKeyName() {
            return holder.format("sshKeyName");
        }

        public static String privateKey() {
            return holder.format("privateKey");
        }
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ComputeCloudAgentTemplate> {

        @Override
        public String getHelpFile(String fieldName) {
            if (fieldName.equals("privateKey")) {
                fieldName = "sshKeyName";
            }

            String p = super.getHelpFile(fieldName);
            if (p == null) {
                p = JenkinsUtil.getJenkinsInstance().getDescriptor(ComputeCloudAgent.class).getHelpFile(fieldName);
            }

            return p;
        }

        public static String getDefaultSshUser() {
            return DEFAULT_SSH_USER;
        }

        public static int getDefaultNumExecutors() {
            return checkNumExecutors(null).getValue();
        }

        public FormValidation doCheckNumExecutors(@QueryParameter String value) {
            return checkNumExecutors(value).getFormValidation();
        }

        ComputeCloud.DescriptorImpl getComputeCloudDescriptor() {
            return JenkinsUtil.getDescriptorOrDie(ComputeCloud.class, ComputeCloud.DescriptorImpl.class);
        }

        private ComputeCloudClient createClient(String apiEndpoint, String identityDomainName, String userName, String password) throws FormValidation {
            return getComputeCloudDescriptor().createClient(apiEndpoint, identityDomainName, userName, password);
        }

        private ComputeCloudClient createFormFillClient(String apiEndpoint, String identityDomainName, String userName, String password, String value) throws FormFillFailure {
            try {
                return createClient(apiEndpoint, identityDomainName, userName, password);
            } catch (FormValidation fv) {
                throw FormFillFailure.errorWithValue(fv, value);
            }
        }

        private static FormFillFailure toFormFillFailure(ComputeCloudClientException e, String value) {
            return FormFillFailure.errorWithValue(ComputeCloud.DescriptorImpl.toFormValidation(e), value);
        }

        private static final Comparator<Shape> SHAPE_COMPARATOR = new Comparator<Shape>() {
            @Override
            public int compare(Shape shape1, Shape shape2) {
                int diff = shape1.getCpus().compareTo(shape2.getCpus());
                if (diff == 0) {
                    diff = shape1.getRam().compareTo(shape2.getRam());
                    if (diff == 0) {
                        diff = shape1.getRootDiskSize().compareTo(shape2.getRootDiskSize());
                        if (diff == 0) {
                            shape1.getName().equals(shape2.getName());
                        }
                    }
                }
                return diff;
            }
        };

        public ListBoxModel doFillShapeNameItems(
                @QueryParameter @RelativePath("..") String apiEndpoint,
                @QueryParameter @RelativePath("..") String identityDomainName,
                @QueryParameter @RelativePath("..") String userName,
                @QueryParameter @RelativePath("..") String password,
                @QueryParameter String shapeName) throws FormFillFailure {
            shapeName = FormFillFailure.getErrorValue(shapeName);
            try (ComputeCloudClient client = createFormFillClient(apiEndpoint, identityDomainName, userName, password, shapeName)) {
                List<Shape> shapes;
                try {
                    shapes = new ArrayList<>(client.getShapes());
                } catch (ComputeCloudClientException e) {
                    throw toFormFillFailure(e, shapeName);
                }

                ListBoxModel model = new ListBoxModel().add("");

                Collections.sort(shapes, SHAPE_COMPARATOR);
                for (Shape shape : shapes) {
                    String name = shape.getName();
                    BigDecimal cpus = shape.getCpus();

                    StringBuilder displayName = new StringBuilder()
                            .append(name)
                            .append(" - ")
                            .append(Messages.ComputeCloudAgentTemplate_shapeName_cpu(cpus, cpus.doubleValue()))
                            .append(", ")
                            .append(Messages.ComputeCloudAgentTemplate_shapeName_ram(FileUtils.byteCountToDisplaySize(shape.getRam() * FileUtils.ONE_MB)));

                    long rootDiskSize = shape.getRootDiskSize();
                    if (rootDiskSize != 0) {
                        displayName.append(", ")
                        .append(Messages.ComputeCloudAgentTemplate_shapeName_rootDiskSize(FileUtils.byteCountToDisplaySize(rootDiskSize)));
                    }

                    model.add(new ListBoxModel.Option(displayName.toString(), name, name.equals(shapeName)));
                }

                return model;
            }
        }

        public FormValidation doCheckShapeName(@QueryParameter String value) {
            return FormFillFailure.validateRequired(value);
        }

        private static final Comparator<SecurityList> SECURITY_LIST_COMPARATOR = new Comparator<SecurityList>() {
            @Override
            public int compare(SecurityList security1, SecurityList security2) {
                return security1.getName().compareTo(security2.getName());
            }
        };

        public ListBoxModel doFillSecurityListNameItems(
                @QueryParameter @RelativePath("..") String apiEndpoint,
                @QueryParameter @RelativePath("..") String identityDomainName,
                @QueryParameter @RelativePath("..") String userName,
                @QueryParameter @RelativePath("..") String password,
                @QueryParameter String securityListName) throws FormFillFailure {
            securityListName = FormFillFailure.getErrorValue(securityListName);
            try (ComputeCloudClient client = createFormFillClient(apiEndpoint, identityDomainName, userName, password, securityListName)) {
                List<SecurityList> securityLists;
                try {
                    securityLists = new ArrayList<>(client.getSecurityLists());
                } catch (ComputeCloudClientException e) {
                    throw toFormFillFailure(e, securityListName);
                }

                ListBoxModel model = new ListBoxModel().add("");

                Collections.sort(securityLists, SECURITY_LIST_COMPARATOR);
                for (SecurityList securityList : securityLists) {
                    String value = securityList.getName();
                    String displayName = ComputeCloudObjectName.parse(value).getName();

                    model.add(new ListBoxModel.Option(displayName, value, value.equals(securityListName)));
                }

                return model;
            }
        }

        public FormValidation doCheckSecurityListName(@QueryParameter String value) {
            return FormFillFailure.validateRequired(value);
        }

        public ListBoxModel doFillImageListSourceItems(@QueryParameter String imageListSource)  {
            imageListSource = FormFillFailure.getErrorValue(imageListSource);
            ListBoxModel model = new ListBoxModel().add("");
            model.add(new ListBoxModel.Option(ImageListSourceType.ORACLE_PUBLIC_IMAGE.toString(), ImageListSourceType.ORACLE_PUBLIC_IMAGE.toString(), imageListSource.equals(ImageListSourceType.ORACLE_PUBLIC_IMAGE.toString())));
            model.add(new ListBoxModel.Option(ImageListSourceType.PRIVATE_IAMGE.toString(), ImageListSourceType.PRIVATE_IAMGE.toString(), imageListSource.equals(ImageListSourceType.PRIVATE_IAMGE.toString())));
            return model;
        }

        public ListBoxModel doFillImageListNameItems(
                @QueryParameter @RelativePath("..") String apiEndpoint,
                @QueryParameter @RelativePath("..") String identityDomainName,
                @QueryParameter @RelativePath("..") String userName,
                @QueryParameter @RelativePath("..") String password,
                @QueryParameter String imageListSource,
                @QueryParameter String imageListName) throws FormFillFailure {
            imageListName = FormFillFailure.getErrorValue(imageListName);
            ListBoxModel model = new ListBoxModel().add("");
            if (imageListSource == null || imageListSource.isEmpty())
                return model;

            try (ComputeCloudClient client = createFormFillClient(apiEndpoint, identityDomainName, userName, password, imageListName)) {
                List<ImageList> imageLists;
                try {
                    imageLists = new ArrayList<>(client.getImageLists(ImageListSourceType.fromValue(imageListSource)));
                } catch (ComputeCloudClientException e) {
                    throw toFormFillFailure(e, imageListName);
                }

                for (ImageList imageList : imageLists) {
                    String value = imageList.getName();
                    String displayName = ComputeCloudObjectName.parse(value).getName();
                    model.add(new ListBoxModel.Option(displayName, value, value.equals(imageListName)));
                }

                return model;
            }
        }

        public ListBoxModel doFillImageListEntryItems(
                @QueryParameter @RelativePath("..") String apiEndpoint,
                @QueryParameter @RelativePath("..") String identityDomainName,
                @QueryParameter @RelativePath("..") String userName,
                @QueryParameter @RelativePath("..") String password,
                @QueryParameter String imageListName,
                @QueryParameter String imageListEntry) throws FormFillFailure {
            imageListEntry = FormFillFailure.getErrorValue(imageListEntry);
            ListBoxModel model = new ListBoxModel().add("");
            if (imageListName == null || imageListName.isEmpty())
                return model;

            try (ComputeCloudClient client = createFormFillClient(apiEndpoint, identityDomainName, userName, password, imageListEntry)) {
                List<ImageListEntry> imageListEntries;
                try {
                    imageListEntries = new ArrayList<>(client.getImageListEntries(imageListName));
                } catch (ComputeCloudClientException e) {
                    throw toFormFillFailure(e, imageListEntry);
                }


                for (ImageListEntry entry : imageListEntries) {
                    String value = entry.getVersion().toString();
                    String displayName = value;
                    model.add(new ListBoxModel.Option(displayName, value, value.equals(imageListEntry)));
                }

                return model;
            }
        }

        public FormValidation doCheckImageListSource(@QueryParameter String value) {
            return FormFillFailure.validateRequired(value);
        }

        public FormValidation doCheckImageListName(@QueryParameter String value) {
            return FormFillFailure.validateRequired(value);
        }

        public FormValidation doCheckVolumeSize(@QueryParameter String value) {
            return checkVolumeSize(value).getFormValidation();
        }

        public FormValidation doCheckLabelString(@QueryParameter String value, @QueryParameter Node.Mode mode) {
            if (mode == Node.Mode.EXCLUSIVE && (value == null || value.trim().isEmpty())) {
                return FormValidation.warning(Messages.ComputeCloudAgentTemplate_labelString_exclusiveEmpty());
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckIdleTerminationMinutes(@QueryParameter String value) {
            if (value == null || value.trim().isEmpty()) {
                return FormValidation.ok();
            }
            return FormValidation.validateNonNegativeInteger(value);
        }

        public static int getDefaultSshConnectTimeoutSeconds() {
            return checkSshConnectTimeoutSeconds(null).getValue();
        }

        public FormValidation doCheckSshConnectTimeoutSeconds(@QueryParameter String value) {
            return checkSshConnectTimeoutSeconds(value).getFormValidation();
        }

        private static final Comparator<SSHKey> SSHKEY_COMPARATOR = new Comparator<SSHKey>() {
            @Override
            public int compare(SSHKey sshKey1, SSHKey sshKey2) {
                return sshKey1.getName().compareTo(sshKey2.getName());
            }
        };

        public ListBoxModel doFillSshKeyNameItems(
                @QueryParameter @RelativePath("..") String apiEndpoint,
                @QueryParameter @RelativePath("..") String identityDomainName,
                @QueryParameter @RelativePath("..") String userName,
                @QueryParameter @RelativePath("..") String password,
                @QueryParameter String sshKeyName) throws FormFillFailure {
            sshKeyName = FormFillFailure.getErrorValue(sshKeyName);
            try (ComputeCloudClient client = createFormFillClient(apiEndpoint, identityDomainName, userName, password, sshKeyName)) {
                List<SSHKey> sshKeys;
                try {
                    sshKeys = new ArrayList<>(client.getSSHKeys());
                } catch (ComputeCloudClientException e) {
                    throw toFormFillFailure(e, sshKeyName);
                }

                ListBoxModel model = new ListBoxModel().add("");

                Collections.sort(sshKeys, SSHKEY_COMPARATOR);
                for (SSHKey sshkey : sshKeys) {
                    String value = sshkey.getName();
                    String displayName = ComputeCloudObjectName.parse(value).getName();

                    model.add(new ListBoxModel.Option(displayName, value, value.equals(sshKeyName)));
                }

                return model;
            }
        }

        private FormValidation checkSshKeyName(String value, boolean withContext) {
            FormValidation fv = FormFillFailure.validateRequired(value);
            if (fv.kind != FormValidation.Kind.OK) {
                return withContext ? ComputeCloud.DescriptorImpl.withContext(fv, ConfigMessages.sshKeyName()) : fv;
            }
            return fv;
        }

        public FormValidation doCheckSshKeyName(@QueryParameter String value) {
            return checkSshKeyName(value, false);
        }

        PEMEncodable decodePEM(String pem) throws UnrecoverableKeyException, IOException {
            return PEMEncodable.decode(pem);
        }

        private FormValidationValue<RSAPublicKey> checkPrivateKey(String value, boolean withContext) {
            FormValidation fv = JenkinsUtil.validateRequired(value);
            if (fv.kind != FormValidation.Kind.OK) {
                return FormValidationValue.error(withContext ? ComputeCloud.DescriptorImpl.withContext(fv, ConfigMessages.privateKey()) : fv);
            }

            PEMEncodable encodable;
            try {
                encodable = decodePEM(value);
            } catch (NullPointerException e) {
                // Workaround https://issues.jenkins-ci.org/browse/JENKINS-41978
                LOGGER.log(Level.FINE, "Failed to parse private key", e);
                return FormValidationValue.error(Messages.ComputeCloudAgentTemplate_privateKey_invalid());
            } catch (UnrecoverableKeyException e) {
                LOGGER.log(Level.FINE, "Failed to parse private key", e);
                return FormValidationValue.error(Messages.ComputeCloudAgentTemplate_privateKey_unable(e.toString()));
            } catch (IOException e) {
                LOGGER.log(Level.FINE, "Failed to parse private key", e);
                return FormValidationValue.error(Messages.ComputeCloudAgentTemplate_privateKey_unable(e.getMessage()));
            }

            KeyPair keyPair = encodable.toKeyPair();
            if (keyPair == null) {
                LOGGER.log(Level.FINE, "toKeyPair returned null for {0}", encodable.getRawObject());
                return FormValidationValue.error(Messages.ComputeCloudAgentTemplate_privateKey_invalid());
            }

            PublicKey publicKey = keyPair.getPublic();
            if (!(publicKey instanceof RSAPublicKey)) {
                LOGGER.log(Level.FINE, "getPublic returned non-RSAPublicKey {0} for {1}",
                        new Object[] { publicKey, encodable.getRawObject() });
                return FormValidationValue.error(Messages.ComputeCloudAgentTemplate_privateKey_invalid());
            }

            RSAPublicKey rsaPublicKey = (RSAPublicKey)publicKey;
            return FormValidationValue.ok(rsaPublicKey);
        }

        public FormValidation doCheckPrivateKey(@QueryParameter String value) {
            return checkPrivateKey(value, false).getFormValidation();
        }

        private FormValidation newUnableToVerifySshKeyPairFormValidation(FormValidation fv) {
            return FormValidation.error(Messages.ComputeCloudAgentTemplate_verifySshKeyPair_unable(JenkinsUtil.unescape(fv.getMessage())));
        }

        public FormValidation doVerifySshKeyPair(
                @QueryParameter String apiEndpoint,
                @QueryParameter String identityDomainName,
                @QueryParameter String userName,
                @QueryParameter String password,
                @QueryParameter String sshKeyName,
                @QueryParameter String privateKey) {
            FormValidation fv = checkSshKeyName(sshKeyName, true);
            if (fv.kind != FormValidation.Kind.OK) {
                return newUnableToVerifySshKeyPairFormValidation(fv);
            }

            FormValidationValue<RSAPublicKey> privateKeyValid = checkPrivateKey(privateKey, true);
            if (!privateKeyValid.isOk()) {
                return newUnableToVerifySshKeyPairFormValidation(privateKeyValid.getFormValidation());
            }

            SSHKey sshKey;
            try (ComputeCloudClient client = createClient(apiEndpoint, identityDomainName, userName, password)) {
                sshKey = client.getSSHKey(ComputeCloudObjectName.parse(sshKeyName).getName());
            } catch (FormValidation fv2) {
                return fv2;
            } catch (ComputeCloudClientException e) {
                return ComputeCloud.DescriptorImpl.toFormValidation(e);
            }

            String sshString = SshKeyUtil.toSshString(privateKeyValid.getValue());
            if (!sshString.equals(sshKey.getKey())) {
                return FormValidation.error(Messages.ComputeCloudAgentTemplate_verifySshKeyPair_mismatch());
            }

            return FormValidation.ok(Messages.ComputeCloudAgentTemplate_verifySshKeyPair_success());
        }

        public static int getDefaultStartTimeoutSeconds() {
            return checkStartTimeoutSeconds(null).getValue();
        }

        public FormValidation doCheckStartTimeoutSeconds(@QueryParameter String value) {
            return checkStartTimeoutSeconds(value).getFormValidation();
        }
    }
}
