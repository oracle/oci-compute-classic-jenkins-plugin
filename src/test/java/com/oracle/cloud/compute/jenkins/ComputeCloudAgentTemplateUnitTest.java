package com.oracle.cloud.compute.jenkins;

import static com.oracle.cloud.compute.jenkins.ComputeCloudTestUtils.ENDPOINT;
import static com.oracle.cloud.compute.jenkins.ComputeCloudTestUtils.INVALID_ENDPOINT;
import static com.oracle.cloud.compute.jenkins.ComputeCloudTestUtils.INVALID_IDENTITY_DOMAIN_NAME;
import static com.oracle.cloud.compute.jenkins.ComputeCloudTestUtils.INVALID_USER_NAME;
import static com.oracle.cloud.compute.jenkins.ComputeCloudTestUtils.PASSWORD;
import static com.oracle.cloud.compute.jenkins.ComputeCloudTestUtils.USER;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.security.KeyPair;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.jmock.Expectations;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import com.oracle.cloud.compute.jenkins.client.ComputeCloudClient;
import com.oracle.cloud.compute.jenkins.client.ComputeCloudClientException;
import com.oracle.cloud.compute.jenkins.client.ComputeCloudClientFactory;
import com.oracle.cloud.compute.jenkins.model.ImageList;
import com.oracle.cloud.compute.jenkins.model.ImageListSourceType;
import com.oracle.cloud.compute.jenkins.model.SSHKey;
import com.oracle.cloud.compute.jenkins.model.SecurityList;
import com.oracle.cloud.compute.jenkins.model.Shape;

import hudson.model.Node;
import hudson.model.labels.LabelAtom;
import hudson.slaves.Cloud;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.bouncycastle.api.PEMEncodable;
import jenkins.bouncycastle.api.SecurityProviderInitializer;

public class ComputeCloudAgentTemplateUnitTest {
    static { TestMessages.init(); }
    static { new SecurityProviderInitializer(); }

    @Rule
    public final ComputeCloudMockery mockery = new ComputeCloudMockery();

    @Test
    public void testGetDisplayName() {
        Assert.assertEquals("null", new TestComputeCloudAgentTemplate().getDisplayName());
        Assert.assertEquals("d", new TestComputeCloudAgentTemplate.Builder().description("d").build().getDisplayName());
    }

    @Test
    public void testGetDescription() {
        Assert.assertNull(new TestComputeCloudAgentTemplate().getDescription());
        Assert.assertEquals("d", new TestComputeCloudAgentTemplate.Builder().description("d").build().getDescription());
    }

    @Test
    public void testGetOrchDescription() {
        Assert.assertNull(new TestComputeCloudAgentTemplate().getOrchDescription());
        Assert.assertEquals("", new TestComputeCloudAgentTemplate.Builder().orchDescription("").build().getOrchDescription());
        Assert.assertEquals("od", new TestComputeCloudAgentTemplate.Builder().orchDescription("od").build().getOrchDescription());
    }

    @Test
    public void testGetOrchDescriptionValue() {
        Assert.assertEquals("Jenkins agent", new TestComputeCloudAgentTemplate().getOrchDescriptionValue());
        Assert.assertEquals("Jenkins agent", new TestComputeCloudAgentTemplate.Builder().description("").build().getOrchDescriptionValue());

        Assert.assertEquals("Jenkins agent", new TestComputeCloudAgentTemplate.Builder().orchDescription("").build().getOrchDescriptionValue());
        Assert.assertEquals("Jenkins agent", new TestComputeCloudAgentTemplate.Builder().orchDescription("").description("").build().getOrchDescriptionValue());

        Assert.assertEquals("Jenkins agent: d", new TestComputeCloudAgentTemplate.Builder().description("d").build().getOrchDescriptionValue());
        Assert.assertEquals("Jenkins agent: d", new TestComputeCloudAgentTemplate.Builder().orchDescription("").description("d").build().getOrchDescriptionValue());

        Assert.assertEquals("od", new TestComputeCloudAgentTemplate.Builder().orchDescription("od").build().getOrchDescriptionValue());
        Assert.assertEquals("od", new TestComputeCloudAgentTemplate.Builder().orchDescription("od").description("").build().getOrchDescriptionValue());
        Assert.assertEquals("od", new TestComputeCloudAgentTemplate.Builder().orchDescription("od").description("d").build().getOrchDescriptionValue());
    }

    @Test
    public void testGetRemoteFS() {
        Assert.assertNull(new TestComputeCloudAgentTemplate().getRemoteFS());
        Assert.assertEquals("rfs", new TestComputeCloudAgentTemplate.Builder().remoteFS("rfs").build().getRemoteFS());
    }

    @Test
    public void testGetSshUser() {
        Assert.assertNull(new TestComputeCloudAgentTemplate().getSshUser());
        Assert.assertEquals("u", new TestComputeCloudAgentTemplate.Builder().sshUser("u").build().getSshUser());
    }

    @Test
    public void testGetSshUserValue() {
        String defaultSshUser = ComputeCloudAgentTemplate.DescriptorImpl.getDefaultSshUser();
        Assert.assertEquals(defaultSshUser, new TestComputeCloudAgentTemplate().getSshUserValue());
        Assert.assertEquals(defaultSshUser, new TestComputeCloudAgentTemplate.Builder().sshUser("").build().getSshUserValue());
        Assert.assertEquals(defaultSshUser, new TestComputeCloudAgentTemplate.Builder().sshUser(" ").build().getSshUserValue());
        Assert.assertEquals("u", new TestComputeCloudAgentTemplate.Builder().sshUser("u").build().getSshUserValue());
    }

    @Test
    public void testGetNumExecutors() {
        Assert.assertNull(new TestComputeCloudAgentTemplate().getNumExecutors());
        Assert.assertEquals("", new TestComputeCloudAgentTemplate.Builder().numExecutors("").build().getNumExecutors());
        Assert.assertEquals("-1", new TestComputeCloudAgentTemplate.Builder().numExecutors(-1).build().getNumExecutors());
        Assert.assertEquals("0", new TestComputeCloudAgentTemplate.Builder().numExecutors(0).build().getNumExecutors());
        Assert.assertEquals("2", new TestComputeCloudAgentTemplate.Builder().numExecutors(2).build().getNumExecutors());
        Assert.assertEquals("x", new TestComputeCloudAgentTemplate.Builder().numExecutors("x").build().getNumExecutors());
    }

    @Test
    public void testGetNumExecutorsValue() {
        int defaultNumExecutors = ComputeCloudAgentTemplate.DescriptorImpl.getDefaultNumExecutors();
        Assert.assertEquals(defaultNumExecutors, new TestComputeCloudAgentTemplate().getNumExecutorsValue());
        Assert.assertEquals(defaultNumExecutors, new TestComputeCloudAgentTemplate.Builder().numExecutors("").build().getNumExecutorsValue());
        Assert.assertEquals(defaultNumExecutors, new TestComputeCloudAgentTemplate.Builder().numExecutors(-1).build().getNumExecutorsValue());
        Assert.assertEquals(defaultNumExecutors, new TestComputeCloudAgentTemplate.Builder().numExecutors(0).build().getNumExecutorsValue());
        Assert.assertEquals(2, new TestComputeCloudAgentTemplate.Builder().numExecutors(2).build().getNumExecutorsValue());
        Assert.assertEquals(defaultNumExecutors, new TestComputeCloudAgentTemplate.Builder().numExecutors("x").build().getNumExecutorsValue());
    }

    @Test
    public void testGetMode() {
        Assert.assertNull(new TestComputeCloudAgentTemplate().getMode());
        Assert.assertSame(Node.Mode.NORMAL, new TestComputeCloudAgentTemplate.Builder().mode(Node.Mode.NORMAL).build().getMode());
    }

    @Test
    public void testGetLabelString() {
        Assert.assertNull(new TestComputeCloudAgentTemplate().getLabelString());
        Assert.assertEquals("a", new TestComputeCloudAgentTemplate.Builder().labelString("a").build().getLabelString());
    }

    @Test
    public void testGetLabelAtoms() {
        Assert.assertEquals(new LinkedHashSet<>(), new LinkedHashSet<>(new TestComputeCloudAgentTemplate().getLabelAtoms()));

        ComputeCloudAgentTemplate t = new TestComputeCloudAgentTemplate.Builder().labelString("a b").build();
        LinkedHashSet<LabelAtom> expected = new LinkedHashSet<>(Arrays.asList(new LabelAtom("a"), new LabelAtom("b")));
        Assert.assertEquals(expected, new LinkedHashSet<>(t.getLabelAtoms()));
        Assert.assertEquals(expected, new LinkedHashSet<>(t.getLabelAtoms()));
    }

    @Test
    public void testGetIdleTerminationMinutes() {
        Assert.assertNull(new TestComputeCloudAgentTemplate().getIdleTerminationMinutes());
        Assert.assertEquals("2", new TestComputeCloudAgentTemplate.Builder().idleTerminationMinutes("2").build().getIdleTerminationMinutes());
    }

    @Test
    public void testGetNextTemplateId() {
        Assert.assertEquals(0, new TestComputeCloudAgentTemplate().getTemplateId());
        Assert.assertEquals(1, new TestComputeCloudAgentTemplate.Builder().templateId(1).build().getTemplateId());
    }

    @Test
    public void testGetShapeName() throws Exception {
        Assert.assertNull(new TestComputeCloudAgentTemplate().getShapeName());
        Assert.assertEquals("sn", new TestComputeCloudAgentTemplate.Builder().shapeName("sn").build().getShapeName());
    }

    @Test
    public void testGetShapeNameWithFormFillFailureFallback() throws Exception {
        String formControlValue = FormFillFailureUnitTest.assumeGetErrorOption(FormFillFailure.errorWithValue("test", "sn")).value;
        Assert.assertEquals("sn", new TestComputeCloudAgentTemplate.Builder().shapeName(formControlValue).build().getShapeName());
    }

//    @Test
//    public void testGetSecurityListName() throws Exception {
//        Assert.assertNull(new TestComputeCloudAgentTemplate().getSecurityListName());
//        Assert.assertNull(new TestComputeCloudAgentTemplate.Builder().securityListNames("").build().getSecurityListName());
//        Assert.assertEquals("sln", new TestComputeCloudAgentTemplate.Builder().securityListNames("sln").build().getSecurityListName());
//    }

//    @Test
//    public void testGetSecurityListNameWithFormFillFailureFallback() throws Exception {
//        String formControlValue = FormFillFailureUnitTest.assumeGetErrorOption(FormFillFailure.errorWithValue("test", "sln")).value;
//        Assert.assertEquals("sln", new TestComputeCloudAgentTemplate.Builder().securityListNames(formControlValue).build().getSecurityListName());
//    }

    @Test
    public void testGetSecurityListNames() throws Exception {
        Assert.assertEquals(Collections.<String>emptyList(), new TestComputeCloudAgentTemplate().getSecurityListNames());
        Assert.assertEquals(Arrays.asList(""), new TestComputeCloudAgentTemplate.Builder().securityListNames("").build().getSecurityListNames());
        Assert.assertEquals(Arrays.asList("sln"), new TestComputeCloudAgentTemplate.Builder().securityListNames("sln").build().getSecurityListNames());
    }

//    @Test
//    public void testGetSecurityListNamesWithFormFillFailureFallback() throws Exception {
//        String formControlValue = FormFillFailureUnitTest.assumeGetErrorOption(FormFillFailure.errorWithValue("test", "sln")).value;
//        Assert.assertEquals(Arrays.asList("sln"), new TestComputeCloudAgentTemplate.Builder().securityListNames(formControlValue).build().getSecurityListNames());
//    }

    @Test
    public void testGetImageListName() throws Exception {
        Assert.assertNull(new TestComputeCloudAgentTemplate().getImageListName());
        Assert.assertEquals("iln", new TestComputeCloudAgentTemplate.Builder().imageListName("iln").build().getImageListName());
    }

    @Test
    public void testGetImageListNameWithFormFillFailureFallback() throws Exception {
        String formControlValue = FormFillFailureUnitTest.assumeGetErrorOption(FormFillFailure.errorWithValue("test", "iln")).value;
        Assert.assertEquals("iln", new TestComputeCloudAgentTemplate.Builder().imageListName(formControlValue).build().getImageListName());
    }

    @Test
    public void testGetVolumeSize() throws Exception {
        // Test invalid values.
        Assert.assertNull(new TestComputeCloudAgentTemplate().getVolumeSize());
        Assert.assertEquals("", new TestComputeCloudAgentTemplate.Builder().volumeSize("").build().getVolumeSize());
        Assert.assertEquals("x", new TestComputeCloudAgentTemplate.Builder().volumeSize("x").build().getVolumeSize());
        Assert.assertEquals("0x", new TestComputeCloudAgentTemplate.Builder().volumeSize("0x").build().getVolumeSize());
        Assert.assertEquals("0bb", new TestComputeCloudAgentTemplate.Builder().volumeSize("0bb").build().getVolumeSize());

        // Test valid values; they should be returned as-is.
        Assert.assertEquals("0", new TestComputeCloudAgentTemplate.Builder().volumeSize("0").build().getVolumeSize());
        Assert.assertEquals("0b", new TestComputeCloudAgentTemplate.Builder().volumeSize("0b").build().getVolumeSize());
        Assert.assertEquals("1B", new TestComputeCloudAgentTemplate.Builder().volumeSize("1B").build().getVolumeSize());
        Assert.assertEquals("22b", new TestComputeCloudAgentTemplate.Builder().volumeSize("22b").build().getVolumeSize());
        Assert.assertEquals("33B", new TestComputeCloudAgentTemplate.Builder().volumeSize("33B").build().getVolumeSize());
        for (String abbrev : new String[] { "k", "K", "m", "M", "g", "G", "t", "T" }) {
            Assert.assertEquals("4" + abbrev, new TestComputeCloudAgentTemplate.Builder().volumeSize("4" + abbrev).build().getVolumeSize());
            Assert.assertEquals("5" + abbrev + "b", new TestComputeCloudAgentTemplate.Builder().volumeSize("5" + abbrev + "b").build().getVolumeSize());
            Assert.assertEquals("6" + abbrev + "B", new TestComputeCloudAgentTemplate.Builder().volumeSize("6" + abbrev + "B").build().getVolumeSize());
        }
    }

    @Test
    public void testGetVolumeSizeValue() {
        // Test invalid values; they should be returned as-is.
        Assert.assertNull(new TestComputeCloudAgentTemplate().getVolumeSizeValue());
        Assert.assertEquals("", new TestComputeCloudAgentTemplate.Builder().volumeSize("").build().getVolumeSizeValue());
        Assert.assertEquals("x", new TestComputeCloudAgentTemplate.Builder().volumeSize("x").build().getVolumeSizeValue());
        Assert.assertEquals("0x", new TestComputeCloudAgentTemplate.Builder().volumeSize("0x").build().getVolumeSizeValue());
        Assert.assertEquals("0bb", new TestComputeCloudAgentTemplate.Builder().volumeSize("0bb").build().getVolumeSizeValue());

        // Test valid values; they should be normalized.
        Assert.assertEquals("0G", new TestComputeCloudAgentTemplate.Builder().volumeSize("0").build().getVolumeSizeValue());
        Assert.assertEquals("0b", new TestComputeCloudAgentTemplate.Builder().volumeSize("0b").build().getVolumeSizeValue());
        Assert.assertEquals("1B", new TestComputeCloudAgentTemplate.Builder().volumeSize("1B").build().getVolumeSizeValue());
        Assert.assertEquals("22b", new TestComputeCloudAgentTemplate.Builder().volumeSize("22b").build().getVolumeSizeValue());
        Assert.assertEquals("33B", new TestComputeCloudAgentTemplate.Builder().volumeSize("33B").build().getVolumeSizeValue());
        for (String abbrev : new String[] { "k", "K", "m", "M", "g", "G", "t", "T" }) {
            Assert.assertEquals("4" + abbrev, new TestComputeCloudAgentTemplate.Builder().volumeSize("4" + abbrev).build().getVolumeSizeValue());
            Assert.assertEquals("5" + abbrev, new TestComputeCloudAgentTemplate.Builder().volumeSize("5" + abbrev + "b").build().getVolumeSizeValue());
            Assert.assertEquals("6" + abbrev, new TestComputeCloudAgentTemplate.Builder().volumeSize("6" + abbrev + "B").build().getVolumeSizeValue());
        }
    }

    @Test
    public void testGetSshConnectTimeoutSeconds() {
        Assert.assertNull(new TestComputeCloudAgentTemplate().getSshConnectTimeoutSeconds());
        Assert.assertEquals("", new TestComputeCloudAgentTemplate.Builder().sshConnectTimeoutSeconds("").build().getSshConnectTimeoutSeconds());
        Assert.assertEquals("x", new TestComputeCloudAgentTemplate.Builder().sshConnectTimeoutSeconds("x").build().getSshConnectTimeoutSeconds());
        Assert.assertEquals("-1", new TestComputeCloudAgentTemplate.Builder().sshConnectTimeoutSeconds("-1").build().getSshConnectTimeoutSeconds());
        Assert.assertEquals("0", new TestComputeCloudAgentTemplate.Builder().sshConnectTimeoutSeconds("0").build().getSshConnectTimeoutSeconds());
        Assert.assertEquals("1", new TestComputeCloudAgentTemplate.Builder().sshConnectTimeoutSeconds("1").build().getSshConnectTimeoutSeconds());
    }

    @Test
    public void testGetSshConnectTimeoutNanos() {
        long defaultMillis = TimeUnit.SECONDS.toMillis(ComputeCloudAgentTemplate.DescriptorImpl.getDefaultSshConnectTimeoutSeconds());
        Assert.assertEquals(defaultMillis, new TestComputeCloudAgentTemplate().getSshConnectTimeoutMillis());
        Assert.assertEquals(defaultMillis, new TestComputeCloudAgentTemplate.Builder().sshConnectTimeoutSeconds("").build().getSshConnectTimeoutMillis());
        Assert.assertEquals(defaultMillis, new TestComputeCloudAgentTemplate.Builder().sshConnectTimeoutSeconds("x").build().getSshConnectTimeoutMillis());
        Assert.assertEquals(defaultMillis, new TestComputeCloudAgentTemplate.Builder().sshConnectTimeoutSeconds("-1").build().getSshConnectTimeoutMillis());
        Assert.assertEquals(0, new TestComputeCloudAgentTemplate.Builder().sshConnectTimeoutSeconds("0").build().getSshConnectTimeoutMillis());
        Assert.assertEquals(TimeUnit.SECONDS.toMillis(1), new TestComputeCloudAgentTemplate.Builder().sshConnectTimeoutSeconds("1").build().getSshConnectTimeoutMillis());
    }

    @Test
    public void testGetSSHKeyName() {
        Assert.assertNull(new TestComputeCloudAgentTemplate().getSshKeyName());
        Assert.assertEquals("skn", new TestComputeCloudAgentTemplate.Builder().sshKeyName("skn").build().getSshKeyName());
    }

    @Test
    public void testGetSSHKeyNameWithFormFillFailureFallback() throws Exception {
        String formControlValue = FormFillFailureUnitTest.assumeGetErrorOption(FormFillFailure.errorWithValue("test", "skn")).value;
        Assert.assertEquals("skn", new TestComputeCloudAgentTemplate.Builder().sshKeyName(formControlValue).build().getSshKeyName());
    }

    @Test
    public void testGetPrivateKey() {
        Assert.assertNull(new TestComputeCloudAgentTemplate().getPrivateKey());
        Assert.assertEquals("pk", new TestComputeCloudAgentTemplate.Builder().privateKey("pk").build().getPrivateKey());
    }

    @Test
    public void testGetInitScript() {
        Assert.assertNull(new TestComputeCloudAgentTemplate().getInitScript());
        Assert.assertEquals("is", new TestComputeCloudAgentTemplate.Builder().initScript("is").build().getInitScript());
    }

    @Test
    public void testGetStartTimeoutSeconds() {
        Assert.assertNull(new TestComputeCloudAgentTemplate().getStartTimeoutSeconds());
        Assert.assertEquals("", new TestComputeCloudAgentTemplate.Builder().startTimeoutSeconds("").build().getStartTimeoutSeconds());
        Assert.assertEquals("x", new TestComputeCloudAgentTemplate.Builder().startTimeoutSeconds("x").build().getStartTimeoutSeconds());
        Assert.assertEquals("-1", new TestComputeCloudAgentTemplate.Builder().startTimeoutSeconds("-1").build().getStartTimeoutSeconds());
        Assert.assertEquals("0", new TestComputeCloudAgentTemplate.Builder().startTimeoutSeconds("0").build().getStartTimeoutSeconds());
        Assert.assertEquals("1", new TestComputeCloudAgentTemplate.Builder().startTimeoutSeconds("1").build().getStartTimeoutSeconds());
    }

    @Test
    public void testGetStartTimeoutNanos() {
        long defaultNanos = TimeUnit.SECONDS.toNanos(ComputeCloudAgentTemplate.DescriptorImpl.getDefaultStartTimeoutSeconds());
        Assert.assertEquals(defaultNanos, new TestComputeCloudAgentTemplate().getStartTimeoutNanos());
        Assert.assertEquals(defaultNanos, new TestComputeCloudAgentTemplate.Builder().startTimeoutSeconds("").build().getStartTimeoutNanos());
        Assert.assertEquals(defaultNanos, new TestComputeCloudAgentTemplate.Builder().startTimeoutSeconds("x").build().getStartTimeoutNanos());
        Assert.assertEquals(defaultNanos, new TestComputeCloudAgentTemplate.Builder().startTimeoutSeconds("-1").build().getStartTimeoutNanos());
        Assert.assertEquals(0, new TestComputeCloudAgentTemplate.Builder().startTimeoutSeconds("0").build().getStartTimeoutNanos());
        Assert.assertEquals(TimeUnit.SECONDS.toNanos(1), new TestComputeCloudAgentTemplate.Builder().startTimeoutSeconds("1").build().getStartTimeoutNanos());
    }

    @Test
    public void testConfigMessages() throws Exception {
        for (Method method : ComputeCloudAgentTemplate.ConfigMessages.class.getMethods()) {
            if (Modifier.isStatic(method.getModifiers())) {
                Assert.assertNotNull(method.invoke(null));
            }
        }
    }

    @Test
    public void testGetDefaultNumExecutors() {
        Assert.assertEquals(1, ComputeCloudAgentTemplate.DescriptorImpl.getDefaultNumExecutors());
    }

    @Test
    public void testDoCheckNumExecutors() {
        Assert.assertEquals(FormValidation.Kind.OK, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckNumExecutors("1").kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckNumExecutors(null).kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckNumExecutors("").kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckNumExecutors("x").kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckNumExecutors("0").kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckNumExecutors("-1").kind);
    }

    private static TestComputeCloudAgentTemplate.TestDescriptor newDescriptor(ComputeCloudClientFactory factory) {
        ComputeCloudClientManager clientManager = new ComputeCloudClientManager(factory, Collections.<Cloud>emptyList());
        return new TestComputeCloudAgentTemplate.TestDescriptor.Builder()
                .cloudDescriptor(new TestComputeCloud.TestDescriptor.Builder().clientManager(clientManager).build())
                .build();
    }

    private TestComputeCloudAgentTemplate.TestDescriptor newDescriptor(final ComputeCloudClient client) {
        final ComputeCloudClientFactory factory = mockery.mock(ComputeCloudClientFactory.class);
        mockery.checking(new Expectations() {{ allowing(factory).createClient(ENDPOINT, USER, PASSWORD); will(returnValue(client)); }});
        return newDescriptor(factory);
    }

    @SuppressWarnings("serial")
    private static class TestListBoxModel extends ArrayList<TestListBoxModel.Option> {
        static class Option {
            final String displayName;
            final String value;
            boolean selected;

            Option(String displayName, String value, boolean selected) {
                this.displayName = displayName;
                this.value = value;
                this.selected = selected;
            }

            @Override
            public String toString() {
                StringBuilder b = new StringBuilder()
                        .append(getClass().getSimpleName())
                        .append('[').append(displayName)
                        .append('=').append(value);
                if (selected) {
                    b.append(", selected");
                }
                return b.append(']').toString();
            }

            @Override
            public boolean equals(Object o) {
                if (o == null || o.getClass() != getClass()) {
                    return false;
                }

                Option option = (Option)o;
                return displayName.equals(option.displayName) &&
                        value.equals(option.value) &&
                        selected == option.selected;
            }
        }

        TestListBoxModel() {}

        TestListBoxModel(List<ListBoxModel.Option> list) {
            for (ListBoxModel.Option option : list) {
                add(option.name, option.value, option.selected);
            }
        }

        TestListBoxModel add(String displayName, String value, boolean selected) {
            add(new Option(displayName, value, selected));
            return this;
        }
    }

    private TestListBoxModel doFillShapeNameItems(ComputeCloudAgentTemplate.DescriptorImpl descriptor, String value) {
        try {
            return new TestListBoxModel(descriptor.doFillShapeNameItems(ENDPOINT.toString(), USER.getIdentityDomainName(), USER.getUsername(), PASSWORD, value));
        } catch (FormFillFailure e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void testDoFillShapeNameItems0() throws Exception {
        final ComputeCloudClient client = mockery.mock(ComputeCloudClient.class);
        mockery.checking(new Expectations() {{ allowing(client).getShapes(); will(returnValue(Collections.emptyList())); }});
        Assert.assertEquals(new TestListBoxModel().add("", "", false), doFillShapeNameItems(newDescriptor(client), ""));
    }

    @Test
    public void testDoFillShapeNameItems() throws Exception {
        final ComputeCloudClient client = mockery.mock(ComputeCloudClient.class);
        mockery.checking(new Expectations() {{
            allowing(client).getShapes(); will(returnValue(Arrays.asList(
                    new Shape().name("zero").cpus(BigDecimal.valueOf(0)).ram(0L).rootDiskSize(0L),
                    new Shape().name("zero2").cpus(BigDecimal.valueOf(0)).ram(0L).rootDiskSize(0L),

                    new Shape().name("cpuhalf").cpus(BigDecimal.valueOf(0.5)).ram(0L).rootDiskSize(0L),
                    new Shape().name("cpu").cpus(BigDecimal.valueOf(1)).ram(0L).rootDiskSize(0L),
                    new Shape().name("cpus").cpus(BigDecimal.valueOf(1.5)).ram(0L).rootDiskSize(0L),

                    new Shape().name("rammb").cpus(BigDecimal.ZERO).ram(1L).rootDiskSize(0L),
                    new Shape().name("rammbs").cpus(BigDecimal.ZERO).ram(2L).rootDiskSize(0L),
                    new Shape().name("ramgb").cpus(BigDecimal.ZERO).ram(1 * FileUtils.ONE_KB).rootDiskSize(0L),
                    new Shape().name("ramgbs").cpus(BigDecimal.ZERO).ram(2 * FileUtils.ONE_KB).rootDiskSize(0L),

                    new Shape().name("rdmb").cpus(BigDecimal.ZERO).ram(0L).rootDiskSize(FileUtils.ONE_MB),
                    new Shape().name("rdmbs").cpus(BigDecimal.ZERO).ram(0L).rootDiskSize(2 * FileUtils.ONE_MB),
                    new Shape().name("rdgb").cpus(BigDecimal.ZERO).ram(0L).rootDiskSize(FileUtils.ONE_GB),
                    new Shape().name("rdgbs").cpus(BigDecimal.ZERO).ram(0L).rootDiskSize(2 * FileUtils.ONE_GB)
            )));
        }});

        Assert.assertEquals(new TestListBoxModel()
                .add("", "", false)
                .add("zero - 0 CPUs, 0 bytes RAM", "zero", false)
                .add("zero2 - 0 CPUs, 0 bytes RAM", "zero2", false)
                .add("rdmb - 0 CPUs, 0 bytes RAM, 1 MB root disk", "rdmb", false)
                .add("rdmbs - 0 CPUs, 0 bytes RAM, 2 MB root disk", "rdmbs", false)
                .add("rdgb - 0 CPUs, 0 bytes RAM, 1 GB root disk", "rdgb", false)
                .add("rdgbs - 0 CPUs, 0 bytes RAM, 2 GB root disk", "rdgbs", false)
                .add("rammb - 0 CPUs, 1 MB RAM", "rammb", false)
                .add("rammbs - 0 CPUs, 2 MB RAM", "rammbs", false)
                .add("ramgb - 0 CPUs, 1 GB RAM", "ramgb", false)
                .add("ramgbs - 0 CPUs, 2 GB RAM", "ramgbs", false)
                .add("cpuhalf - 0.5 CPUs, 0 bytes RAM", "cpuhalf", false)
                .add("cpu - 1 CPU, 0 bytes RAM", "cpu", false)
                .add("cpus - 1.5 CPUs, 0 bytes RAM", "cpus", false),
                doFillShapeNameItems(newDescriptor(client), ""));
    }

    @Test
    public void testDoFillShapeNameItemsSelected() throws Exception {
        final ComputeCloudClient client = mockery.mock(ComputeCloudClient.class);
        mockery.checking(new Expectations() {{ allowing(client).getShapes(); will(returnValue(Arrays.asList(new Shape().name("zero").cpus(BigDecimal.ZERO).ram(0L).rootDiskSize(0L)))); }});

        Assert.assertEquals(new TestListBoxModel()
                .add("", "", false)
                .add("zero - 0 CPUs, 0 bytes RAM", "zero", true),
                doFillShapeNameItems(newDescriptor(client), "zero"));
    }

    @Test
    public void testDoFillShapeNameItemsSelectedWithFormFillFailureFallback() throws Exception {
        final ComputeCloudClient client = mockery.mock(ComputeCloudClient.class);
        mockery.checking(new Expectations() {{ allowing(client).getShapes(); will(returnValue(Arrays.asList(new Shape().name("zero").cpus(BigDecimal.ZERO).ram(0L).rootDiskSize(0L)))); }});

        Assert.assertEquals(new TestListBoxModel()
                .add("", "", false)
                .add("zero - 0 CPUs, 0 bytes RAM", "zero", true),
                doFillShapeNameItems(newDescriptor(client), FormFillFailureUnitTest.assumeGetErrorOption(FormFillFailure.errorWithValue("erron", "zero")).value));
    }

    private void assertDoFillShapeNameItemsError(ComputeCloudAgentTemplate.DescriptorImpl descriptor, String apiEndpoint, String identityDomainName, String userName, String password) {
        try {
            descriptor.doFillShapeNameItems(apiEndpoint, identityDomainName, userName, password, "sn");
            Assert.fail();
        } catch (FormFillFailure e) {
            ListBoxModel.Option errorOption = FormFillFailureUnitTest.getErrorOption(e);
            if (errorOption != null) {
                Assert.assertEquals("sn", FormFillFailure.getErrorValue(errorOption.value));
            }
        }
    }

    @Test
    public void testDoFillShapeNameItemsFormValidation() throws Exception {
        ComputeCloudClientFactory factory = mockery.mock(ComputeCloudClientFactory.class);
        assertDoFillShapeNameItemsError(newDescriptor(factory), INVALID_ENDPOINT, USER.getIdentityDomainName(), USER.getUsername(), PASSWORD);
        assertDoFillShapeNameItemsError(newDescriptor(factory), ENDPOINT.toString(), INVALID_IDENTITY_DOMAIN_NAME, USER.getUsername(), PASSWORD);
        assertDoFillShapeNameItemsError(newDescriptor(factory), ENDPOINT.toString(), USER.getIdentityDomainName(), INVALID_USER_NAME, PASSWORD);
    }

    @Test
    public void testDoFillShapeNameItemsGetShapesError() throws Exception {
        final ComputeCloudClient client = mockery.mock(ComputeCloudClient.class);
        mockery.checking(new Expectations() {{ allowing(client).getShapes(); will(throwException(new ComputeCloudClientException("test"))); }});
        assertDoFillShapeNameItemsError(newDescriptor(client), ENDPOINT.toString(), USER.getIdentityDomainName(), USER.getUsername(), PASSWORD);
    }

    @Test
    public void testDoCheckShapeName() throws Exception {
        Assert.assertEquals(FormValidation.Kind.OK, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckShapeName("x").kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckShapeName(null).kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckShapeName("").kind);
    }

    @Test
    public void testDoCheckShapeNameWithFormFillFailureFallback() throws Exception {
        Assert.assertEquals(FormValidation.Kind.ERROR, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckShapeName(
                FormFillFailureUnitTest.assumeGetErrorOption(FormFillFailure.errorWithValue("e", "x")).value).kind);
    }

    private TestListBoxModel doFillSecurityListNameItems(ComputeCloudAgentTemplate.DescriptorImpl descriptor, String value) {
        try {
            return new TestListBoxModel(descriptor.doFillSecurityListNameItems(ENDPOINT.toString(), USER.getIdentityDomainName(), USER.getUsername(), PASSWORD, value));
        } catch (FormFillFailure e) {
            throw new AssertionError(e);
        }
    }

    private static final String SECURITY_LIST_STRING = USER.getString() + "/seclist";
    private static final String IMAGE_LIST_STRING = "/oracle/public/testimagelist";

    @Test
    public void testDoFillSecurityListNameItems0() throws Exception {
        final ComputeCloudClient client = mockery.mock(ComputeCloudClient.class);
        mockery.checking(new Expectations() {{ allowing(client).getSecurityLists(); will(returnValue(Collections.emptyList())); }});
        Assert.assertEquals(new TestListBoxModel().add("", "", false), doFillSecurityListNameItems(newDescriptor(client), ""));
    }

    @Test
    public void testDoFillSecurityListNameItems() throws Exception {
        final ComputeCloudClient client = mockery.mock(ComputeCloudClient.class);
        mockery.checking(new Expectations() {{
            allowing(client).getSecurityLists(); will(returnValue(Arrays.asList(
                    new SecurityList().name(SECURITY_LIST_STRING + 1),
                    new SecurityList().name(SECURITY_LIST_STRING + 0)
            )));
        }});

        Assert.assertEquals(new TestListBoxModel()
                .add("", "", false)
                .add("seclist0", SECURITY_LIST_STRING + 0, false)
                .add("seclist1", SECURITY_LIST_STRING + 1, false),
                doFillSecurityListNameItems(newDescriptor(client), ""));
    }

    @Test
    public void testDoFillSecurityListNameItemsSelected() throws Exception {
        final ComputeCloudClient client = mockery.mock(ComputeCloudClient.class);
        mockery.checking(new Expectations() {{ allowing(client).getSecurityLists(); will(returnValue(Arrays.asList(new SecurityList().name(SECURITY_LIST_STRING)))); }});

        Assert.assertEquals(new TestListBoxModel()
                .add("", "", false)
                .add("seclist", SECURITY_LIST_STRING, true),
                doFillSecurityListNameItems(newDescriptor(client), SECURITY_LIST_STRING));
    }

    @Test
    public void testDoFillSecurityListNameItemsSelectedWithFormFillFailureFallback() throws Exception {
        final ComputeCloudClient client = mockery.mock(ComputeCloudClient.class);
        mockery.checking(new Expectations() {{ allowing(client).getSecurityLists(); will(returnValue(Arrays.asList(new SecurityList().name(SECURITY_LIST_STRING)))); }});

        Assert.assertEquals(new TestListBoxModel()
                .add("", "", false)
                .add("seclist", SECURITY_LIST_STRING, true),
                doFillSecurityListNameItems(newDescriptor(client), FormFillFailureUnitTest.assumeGetErrorOption(FormFillFailure.errorWithValue("erron", SECURITY_LIST_STRING)).value));
    }

    private void assertDoFillSecurityListNameItemsError(ComputeCloudAgentTemplate.DescriptorImpl descriptor, String apiEndpoint, String identityDomainName, String userName, String password) {
        try {
            descriptor.doFillSecurityListNameItems(apiEndpoint, identityDomainName, userName, password, SECURITY_LIST_STRING);
            Assert.fail();
        } catch (FormFillFailure e) {
            ListBoxModel.Option errorOption = FormFillFailureUnitTest.getErrorOption(e);
            if (errorOption != null) {
                Assert.assertEquals(SECURITY_LIST_STRING, FormFillFailure.getErrorValue(errorOption.value));
            }
        }
    }

    @Test
    public void testDoFillSecurityListNameItemsFormValidation() throws Exception {
        ComputeCloudClientFactory factory = mockery.mock(ComputeCloudClientFactory.class);
        assertDoFillSecurityListNameItemsError(newDescriptor(factory), INVALID_ENDPOINT, USER.getIdentityDomainName(), USER.getUsername(), PASSWORD);
        assertDoFillSecurityListNameItemsError(newDescriptor(factory), ENDPOINT.toString(), INVALID_IDENTITY_DOMAIN_NAME, USER.getUsername(), PASSWORD);
        assertDoFillSecurityListNameItemsError(newDescriptor(factory), ENDPOINT.toString(), USER.getIdentityDomainName(), INVALID_USER_NAME, PASSWORD);
    }

    @Test
    public void testDoFillSecurityListNameItemsGetSecurityListsError() throws Exception {
        final ComputeCloudClient client = mockery.mock(ComputeCloudClient.class);
        mockery.checking(new Expectations() {{ allowing(client).getSecurityLists(); will(throwException(new ComputeCloudClientException("test"))); }});
        assertDoFillSecurityListNameItemsError(newDescriptor(client), ENDPOINT.toString(), USER.getIdentityDomainName(), USER.getUsername(), PASSWORD);
    }

    @Test
    public void testDoCheckSecurityListName() throws Exception {
        Assert.assertEquals(FormValidation.Kind.OK, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckSecurityListName("x").kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckSecurityListName(null).kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckSecurityListName("").kind);
    }

    @Test
    public void testDoCheckSecurityListNameWithFormFillFailureFallback() throws Exception {
        Assert.assertEquals(FormValidation.Kind.ERROR, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckSecurityListName(
                FormFillFailureUnitTest.assumeGetErrorOption(FormFillFailure.errorWithValue("e", "x")).value).kind);
    }

    private TestListBoxModel doFillImageListNameItems(ComputeCloudAgentTemplate.DescriptorImpl descriptor, String sourceTypeString, String value) {
        try {
            return new TestListBoxModel(descriptor.doFillImageListNameItems(ENDPOINT.toString(), USER.getIdentityDomainName(), USER.getUsername(), PASSWORD, sourceTypeString, value));
        } catch (FormFillFailure e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void testDoFillImageListNameItems0() throws Exception {
        final ComputeCloudClient client = mockery.mock(ComputeCloudClient.class);
        mockery.checking(new Expectations() {{ allowing(client).getImageLists(ImageListSourceType.ORACLE_PUBLIC_IMAGE); will(returnValue(Collections.emptyList())); }});
        Assert.assertEquals(new TestListBoxModel().add("", "", false), doFillImageListNameItems(newDescriptor(client), ImageListSourceType.ORACLE_PUBLIC_IMAGE.toString(), ""));
    }

    @Test
    public void testDoFillImageListNameItems() throws Exception {
        final ComputeCloudClient client = mockery.mock(ComputeCloudClient.class);
        mockery.checking(new Expectations() {{ allowing(client).getImageLists(ImageListSourceType.ORACLE_PUBLIC_IMAGE); will(returnValue(Arrays.asList(new ImageList().name("/oracle/public/iln0"), new ImageList().name("/oracle/public/iln1")))); }});

        Assert.assertEquals(new TestListBoxModel()
                .add("", "", false)
                .add("iln0", "/oracle/public/iln0", false)
                .add("iln1", "/oracle/public/iln1", false),
                doFillImageListNameItems(newDescriptor(client), ImageListSourceType.ORACLE_PUBLIC_IMAGE.toString(), ""));
    }

    @Test
    public void testDoFillImageListNameItemsSelected() throws Exception {
        final ComputeCloudClient client = mockery.mock(ComputeCloudClient.class);
        mockery.checking(new Expectations() {{ allowing(client).getImageLists(ImageListSourceType.ORACLE_PUBLIC_IMAGE); will(returnValue(Arrays.asList(new ImageList().name("/oracle/public/iln")))); }});

        Assert.assertEquals(new TestListBoxModel()
                .add("", "", false)
                .add("iln", "/oracle/public/iln", true),
                doFillImageListNameItems(newDescriptor(client), ImageListSourceType.ORACLE_PUBLIC_IMAGE.toString(), "/oracle/public/iln"));
    }

    @Test
    public void testDoFillImageListNameItemsSelectedWithFormFillFailureFallback() throws Exception {
        final ComputeCloudClient client = mockery.mock(ComputeCloudClient.class);
        mockery.checking(new Expectations() {{ allowing(client).getImageLists(ImageListSourceType.ORACLE_PUBLIC_IMAGE); will(returnValue(Arrays.asList(new ImageList().name("/oracle/public/iln")))); }});

        Assert.assertEquals(new TestListBoxModel()
                .add("", "", false)
                .add("iln", "/oracle/public/iln", true),
                doFillImageListNameItems(newDescriptor(client), ImageListSourceType.ORACLE_PUBLIC_IMAGE.toString(), FormFillFailureUnitTest.assumeGetErrorOption(FormFillFailure.errorWithValue("error", "/oracle/public/iln")).value));
    }

    private void assertDoFillImageListNameItemsError(ComputeCloudAgentTemplate.DescriptorImpl descriptor, String apiEndpoint, String identityDomainName, String userName, String password) {
        try {
            descriptor.doFillImageListNameItems(apiEndpoint, identityDomainName, userName, password, ImageListSourceType.ORACLE_PUBLIC_IMAGE.toString(), "/oracle/public/iln");
            Assert.fail();
        } catch (FormFillFailure e) {
            ListBoxModel.Option errorOption = FormFillFailureUnitTest.getErrorOption(e);
            if (errorOption != null) {
                Assert.assertEquals("/oracle/public/iln", FormFillFailure.getErrorValue(errorOption.value));
            }
        }
    }

    @Test
    public void testDoFillImageListNameItemsFormValidation() throws Exception {
        ComputeCloudClientFactory factory = mockery.mock(ComputeCloudClientFactory.class);
        assertDoFillImageListNameItemsError(newDescriptor(factory), INVALID_ENDPOINT, USER.getIdentityDomainName(), USER.getUsername(), PASSWORD);
        assertDoFillImageListNameItemsError(newDescriptor(factory), ENDPOINT.toString(), INVALID_IDENTITY_DOMAIN_NAME, USER.getUsername(), PASSWORD);
        assertDoFillImageListNameItemsError(newDescriptor(factory), ENDPOINT.toString(), USER.getIdentityDomainName(), INVALID_USER_NAME, PASSWORD);
    }

    @Test
    public void testDoFillImageListNameItemsGetOraclePublicImageListNamesError() throws Exception {
        final ComputeCloudClient client = mockery.mock(ComputeCloudClient.class);
        mockery.checking(new Expectations() {{ allowing(client).getImageLists(ImageListSourceType.ORACLE_PUBLIC_IMAGE); will(throwException(new ComputeCloudClientException("test"))); }});
        assertDoFillImageListNameItemsError(newDescriptor(client), ENDPOINT.toString(), USER.getIdentityDomainName(), USER.getUsername(), PASSWORD);
    }

    @Test
    public void testDoCheckImageListName() throws Exception {
        Assert.assertEquals(FormValidation.Kind.OK, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckImageListName("x").kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckImageListName(null).kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckImageListName("").kind);
    }

    @Test
    public void testDoCheckImageListNameWithFormFillFailureFallback() throws Exception {
        Assert.assertEquals(FormValidation.Kind.ERROR, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckImageListName(
                FormFillFailureUnitTest.assumeGetErrorOption(FormFillFailure.errorWithValue("e", "x")).value).kind);
    }

    @Test
    public void testDoCheckVolumeSize() throws Exception {
        Assert.assertEquals(FormValidation.Kind.ERROR, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckVolumeSize("").kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckVolumeSize("x").kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckVolumeSize("0x").kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckVolumeSize("0bb").kind);

        Assert.assertEquals(FormValidation.Kind.OK, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckVolumeSize("0").kind);
        Assert.assertEquals(FormValidation.Kind.OK, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckVolumeSize("0b").kind);
        Assert.assertEquals(FormValidation.Kind.OK, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckVolumeSize("1B").kind);
        Assert.assertEquals(FormValidation.Kind.OK, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckVolumeSize("22b").kind);
        Assert.assertEquals(FormValidation.Kind.OK, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckVolumeSize("33B").kind);
        for (String abbrev : new String[] { "k", "K", "m", "M", "g", "G", "t", "T" }) {
            Assert.assertEquals(FormValidation.Kind.OK, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckVolumeSize("4" + abbrev).kind);
            Assert.assertEquals(FormValidation.Kind.OK, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckVolumeSize("5" + abbrev + "b").kind);
            Assert.assertEquals(FormValidation.Kind.OK, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckVolumeSize("6" + abbrev + "B").kind);
        }
    }

    @Test
    public void testDoCheckLabelString() {
        Assert.assertEquals(FormValidation.Kind.OK, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckLabelString(null, Node.Mode.NORMAL).kind);
        Assert.assertEquals(FormValidation.Kind.WARNING, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckLabelString(null, Node.Mode.EXCLUSIVE).kind);

        Assert.assertEquals(FormValidation.Kind.OK, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckLabelString("", Node.Mode.NORMAL).kind);
        Assert.assertEquals(FormValidation.Kind.WARNING, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckLabelString("", Node.Mode.EXCLUSIVE).kind);

        Assert.assertEquals(FormValidation.Kind.OK, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckLabelString(" ", Node.Mode.NORMAL).kind);
        Assert.assertEquals(FormValidation.Kind.WARNING, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckLabelString(" ", Node.Mode.EXCLUSIVE).kind);

        Assert.assertEquals(FormValidation.Kind.OK, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckLabelString("a", Node.Mode.NORMAL).kind);
        Assert.assertEquals(FormValidation.Kind.OK, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckLabelString("a", Node.Mode.EXCLUSIVE).kind);
    }

    @Test
    public void testDoCheckIdleTerminationMinutes() {
        Assert.assertEquals(FormValidation.Kind.OK, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckIdleTerminationMinutes(null).kind);
        Assert.assertEquals(FormValidation.Kind.OK, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckIdleTerminationMinutes("").kind);
        Assert.assertEquals(FormValidation.Kind.OK, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckIdleTerminationMinutes(" ").kind);
        Assert.assertEquals(FormValidation.Kind.OK, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckIdleTerminationMinutes("1").kind);
        Assert.assertEquals(FormValidation.Kind.OK, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckIdleTerminationMinutes("0").kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckIdleTerminationMinutes("-1").kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckIdleTerminationMinutes("x").kind);
    }

    private TestListBoxModel doFillSshKeyNameItems(ComputeCloudAgentTemplate.DescriptorImpl descriptor, String value) {
        try {
            return new TestListBoxModel(descriptor.doFillSshKeyNameItems(ENDPOINT.toString(), USER.getIdentityDomainName(), USER.getUsername(), PASSWORD, value));
        } catch (FormFillFailure e) {
            throw new AssertionError(e);
        }
    }

    private static final String SSHKEY_NAME = "sshkey";
    private static final String SSHKEY_NAME_STRING = USER.getString() + '/' + SSHKEY_NAME;

    @Test
    public void testDoFillSshKeyNameItems0() throws Exception {
        final ComputeCloudClient client = mockery.mock(ComputeCloudClient.class);
        mockery.checking(new Expectations() {{ allowing(client).getSSHKeys(); will(returnValue(Collections.emptyList())); }});
        Assert.assertEquals(new TestListBoxModel().add("", "", false), doFillSshKeyNameItems(newDescriptor(client), ""));
    }

    @Test
    public void testDoFillSshKeyNameItems() throws Exception {
        final ComputeCloudClient client = mockery.mock(ComputeCloudClient.class);
        mockery.checking(new Expectations() {{
            allowing(client).getSSHKeys(); will(returnValue(Arrays.asList(
                    new SSHKey().name(SSHKEY_NAME_STRING + 1),
                    new SSHKey().name(SSHKEY_NAME_STRING + 0)
            )));
        }});

        Assert.assertEquals(new TestListBoxModel()
                .add("", "", false)
                .add("sshkey0", SSHKEY_NAME_STRING + 0, false)
                .add("sshkey1", SSHKEY_NAME_STRING + 1, false),
                doFillSshKeyNameItems(newDescriptor(client), ""));
    }

    @Test
    public void testDoFillSshKeyNameItemsSelected() throws Exception {
        final ComputeCloudClient client = mockery.mock(ComputeCloudClient.class);
        mockery.checking(new Expectations() {{ allowing(client).getSSHKeys(); will(returnValue(Arrays.asList(new SSHKey().name(SSHKEY_NAME_STRING)))); }});

        Assert.assertEquals(new TestListBoxModel()
                .add("", "", false)
                .add("sshkey", SSHKEY_NAME_STRING, true),
                doFillSshKeyNameItems(newDescriptor(client), SSHKEY_NAME_STRING));
    }

    @Test
    public void testDoFillSshKeyNameItemsSelectedWithFormFillFailureFallback() throws Exception {
        final ComputeCloudClient client = mockery.mock(ComputeCloudClient.class);
        mockery.checking(new Expectations() {{ allowing(client).getSSHKeys(); will(returnValue(Arrays.asList(new SSHKey().name(SSHKEY_NAME_STRING)))); }});

        Assert.assertEquals(new TestListBoxModel()
                .add("", "", false)
                .add("sshkey", SSHKEY_NAME_STRING, true),
                doFillSshKeyNameItems(newDescriptor(client), FormFillFailureUnitTest.assumeGetErrorOption(FormFillFailure.errorWithValue("erron", SSHKEY_NAME_STRING)).value));
    }

    private void assertDoFillSshKeyNameItemsError(ComputeCloudAgentTemplate.DescriptorImpl descriptor, String apiEndpoint, String identityDomainName, String userName, String password) {
        try {
            descriptor.doFillSshKeyNameItems(apiEndpoint, identityDomainName, userName, password, SSHKEY_NAME_STRING);
            Assert.fail();
        } catch (FormFillFailure e) {
            ListBoxModel.Option errorOption = FormFillFailureUnitTest.getErrorOption(e);
            if (errorOption != null) {
                Assert.assertEquals(SSHKEY_NAME_STRING, FormFillFailure.getErrorValue(errorOption.value));
            }
        }
    }

    @Test
    public void testDoFillSshKeyNameItemsFormValidation() throws Exception {
        ComputeCloudClientFactory factory = mockery.mock(ComputeCloudClientFactory.class);
        assertDoFillSshKeyNameItemsError(newDescriptor(factory), INVALID_ENDPOINT, USER.getIdentityDomainName(), USER.getUsername(), PASSWORD);
        assertDoFillSshKeyNameItemsError(newDescriptor(factory), ENDPOINT.toString(), INVALID_IDENTITY_DOMAIN_NAME, USER.getUsername(), PASSWORD);
        assertDoFillSshKeyNameItemsError(newDescriptor(factory), ENDPOINT.toString(), USER.getIdentityDomainName(), INVALID_USER_NAME, PASSWORD);
    }

    @Test
    public void testDoFillSshKeyNameItemsGetSecurityListsError() throws Exception {
        final ComputeCloudClient client = mockery.mock(ComputeCloudClient.class);
        mockery.checking(new Expectations() {{ allowing(client).getSSHKeys(); will(throwException(new ComputeCloudClientException("test"))); }});
        assertDoFillSshKeyNameItemsError(newDescriptor(client), ENDPOINT.toString(), USER.getIdentityDomainName(), USER.getUsername(), PASSWORD);
    }

    @Test
    public void testGetDefaultSshConnectTimeoutSeconds() {
        Assert.assertEquals(30, ComputeCloudAgentTemplate.DescriptorImpl.getDefaultSshConnectTimeoutSeconds());
    }

    @Test
    public void testDoCheckSshConnectTimeoutSeconds() {
        Assert.assertEquals(FormValidation.Kind.ERROR, new TestComputeCloudAgentTemplate.DescriptorImpl().doCheckSshConnectTimeoutSeconds(null).kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new TestComputeCloudAgentTemplate.DescriptorImpl().doCheckSshConnectTimeoutSeconds("").kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new TestComputeCloudAgentTemplate.DescriptorImpl().doCheckSshConnectTimeoutSeconds("x").kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new TestComputeCloudAgentTemplate.DescriptorImpl().doCheckSshConnectTimeoutSeconds("-1").kind);
        Assert.assertEquals(FormValidation.Kind.OK, new TestComputeCloudAgentTemplate.DescriptorImpl().doCheckSshConnectTimeoutSeconds("0").kind);
        Assert.assertEquals(FormValidation.Kind.OK, new TestComputeCloudAgentTemplate.DescriptorImpl().doCheckSshConnectTimeoutSeconds("1").kind);
    }

    @Test
    public void testDoCheckSshKeyName() throws Exception {
        Assert.assertEquals(FormValidation.Kind.OK, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckSshKeyName("x").kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckSshKeyName(null).kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckSshKeyName("").kind);
    }

    @Test
    public void testDoCheckSshKeyNameWithFormFillFailureFallback() throws Exception {
        Assert.assertEquals(FormValidation.Kind.ERROR, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckSshKeyName(
                FormFillFailureUnitTest.assumeGetErrorOption(FormFillFailure.errorWithValue("e", "x")).value).kind);
    }

    // Randomly generated with: ssh-keygen -t rsa -b 768
    private static final String TEST_PRIVATE_KEY_PEM =
            "-----BEGIN RSA PRIVATE KEY-----\n" +
            "MIIBywIBAAJhAKmEwj68Ssf3v5tkolZzwANvDs/PDGBSxC8A1FqsXQ+hrGa/j/JB\n" +
            "/R+xXPSvr/a1KWaPilXqhALt8+7LIfg4TbUxdhXVdVJupha7JwBUCBH87DFVQzc5\n" +
            "wqJJ7J6iIGZNNwIDAQABAmARLCi9UDfHIBrh9ATZ+ynVbzex54iabWgAVvYsJU/c\n" +
            "GIWtdvRvFy48OqxvASkzNdDMlI5QxpD92cfoykxFd/U4lPjcgKpInm7CkGVvJFtC\n" +
            "Qr2MG87iILNAuQWHwlljyuECMQDcJzo9u1+ue9wlcBUjhtfo7nCKxoEg9xsTRIWn\n" +
            "JnrWiGw6oyy/AIKxw/pSxN1d3DECMQDFHuYKp3VKMo4kz+J2XdXeYKg/iZ8ebeNu\n" +
            "id8tiTtiUtgoAA5znMwM5JAhh7EALecCMQCjunjSGFwcg/lBzo2qEkrY7Ru92cuH\n" +
            "HL+CIN/VZATPMD5tjZVlp5eLZVjx3X9UosECMQCjf/CBD8r6gxphsEh/s29MZ1HG\n" +
            "eckQfUcyjYsfAv/NmzeNXhaekISzgPWHyjvnESsCMD0vJJ8DsP01Zi4CGnN3Cw1t\n" +
            "D5T/rai8O9b0G4JOOXRTjv8v68ajYDotjolRwTCULw==\n" +
            "-----END RSA PRIVATE KEY-----\n";
    // Extracted from private key: ssh-keygen -y -f privkey.pem
    private static final String TEST_PUBLIC_KEY_SSH = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAAAYQCphMI+vErH97+bZKJWc8ADbw7PzwxgUsQvANRarF0Poaxmv4/yQf0fsVz0r6/2tSlmj4pV6oQC7fPuyyH4OE21MXYV1XVSbqYWuycAVAgR/OwxVUM3OcKiSeyeoiBmTTc=";

    @Test
    public void testDoCheckPrivateKey() throws Exception {
        Assert.assertEquals(FormValidation.Kind.ERROR, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckPrivateKey(null).kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckPrivateKey("").kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckPrivateKey("x").kind);
        Assert.assertEquals(FormValidation.Kind.OK, new ComputeCloudAgentTemplate.DescriptorImpl().doCheckPrivateKey(TEST_PRIVATE_KEY_PEM).kind);
    }

    @Test
    public void testDoVerifySshKeyPairFormValidationError() {
        ComputeCloudClientFactory factory = mockery.mock(ComputeCloudClientFactory.class);
        Assert.assertEquals(FormValidation.Kind.ERROR, newDescriptor(factory).doVerifySshKeyPair("", "idm", "u", "p", "skn", TEST_PRIVATE_KEY_PEM).kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, newDescriptor(factory).doVerifySshKeyPair("https://x", "", "u", "p", "skn", TEST_PRIVATE_KEY_PEM).kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, newDescriptor(factory).doVerifySshKeyPair("https://x", "idm", "", "p", "skn", TEST_PRIVATE_KEY_PEM).kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, newDescriptor(factory).doVerifySshKeyPair("https://x", "idm", "u", "", "skn", TEST_PRIVATE_KEY_PEM).kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, newDescriptor(factory).doVerifySshKeyPair("https://x", "idm", "u", "p", "", TEST_PRIVATE_KEY_PEM).kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, newDescriptor(factory).doVerifySshKeyPair("https://x", "idm", "u", "p", "skn", "").kind);
    }

    private static FormValidation doTestConnection(final ComputeCloudClientFactory factory, String sshKeyName, String privateKey) throws Exception {
        return newDescriptor(factory).doVerifySshKeyPair(ENDPOINT.toString(), USER.getIdentityDomainName(), USER.getUsername(), PASSWORD, sshKeyName, privateKey);
    }

    @Test
    @Ignore
    public void testDoVerifySshKeyPair() throws Exception {
        final ComputeCloudClientFactory factory = mockery.mock(ComputeCloudClientFactory.class);
        final ComputeCloudClient client = mockery.mock(ComputeCloudClient.class);
        mockery.checking(new Expectations() {{
            allowing(factory).createClient(ENDPOINT, USER, PASSWORD); will(returnValue(client));
            allowing(client).getSSHKey(SSHKEY_NAME); will(returnValue(new SSHKey().key(TEST_PUBLIC_KEY_SSH)));
        }});

        Assert.assertEquals(FormValidation.Kind.OK, doTestConnection(factory, SSHKEY_NAME_STRING, TEST_PRIVATE_KEY_PEM).kind);
    }

    private static FormValidation doVerifySshKeyPair(TestComputeCloudAgentTemplate.TestDescriptor.PEMDecoder pemDecoder) {
        return new TestComputeCloudAgentTemplate.TestDescriptor.Builder()
                .pemDecoder(pemDecoder)
                .build().doVerifySshKeyPair("https://x", "idm", "u", "p", "skn", TEST_PRIVATE_KEY_PEM);
    }

    @Test
    public void testDoVerifySshKeyPairUnrecoverableKeyException() throws Exception {
        Assert.assertEquals(FormValidation.Kind.ERROR, doVerifySshKeyPair(new TestComputeCloudAgentTemplate.TestDescriptor.PEMDecoder() {
            @Override
            public PEMEncodable decode(String pem) throws UnrecoverableKeyException {
                throw new UnrecoverableKeyException();
            }
        }).kind);
    }

    @Test
    public void testDoVerifySshKeyPairIOException() throws Exception {
        Assert.assertEquals(FormValidation.Kind.ERROR, doVerifySshKeyPair(new TestComputeCloudAgentTemplate.TestDescriptor.PEMDecoder() {
            @Override
            public PEMEncodable decode(String pem) throws IOException {
                throw new IOException();
            }
        }).kind);
    }

    @Test
    public void testDoVerifySshKeyPairNullPointerException() throws Exception {
        Assert.assertEquals(FormValidation.Kind.ERROR, doVerifySshKeyPair(new TestComputeCloudAgentTemplate.TestDescriptor.PEMDecoder() {
            @Override
            public PEMEncodable decode(String pem) throws IOException {
                // Simulate https://issues.jenkins-ci.org/browse/JENKINS-41978
                throw new NullPointerException();
            }
        }).kind);
    }

    @Test
    public void testDoVerifySshKeyPairNotRSAPublicKey() throws Exception {
        Assert.assertEquals(FormValidation.Kind.ERROR, doVerifySshKeyPair(new TestComputeCloudAgentTemplate.TestDescriptor.PEMDecoder() {
            @Override
            public PEMEncodable decode(String pem) throws IOException {
                return PEMEncodable.create(new KeyPair(null, null));
            }
        }).kind);
    }

    @Test
    public void testDoVerifySshKeyPairKeyNotFound() throws Exception {
        final ComputeCloudClientFactory factory = mockery.mock(ComputeCloudClientFactory.class);
        final ComputeCloudClient client = mockery.mock(ComputeCloudClient.class);
        mockery.checking(new Expectations() {{
            allowing(factory).createClient(ENDPOINT, USER, PASSWORD); will(returnValue(client));
            allowing(client).getSSHKey(SSHKEY_NAME); will(throwException(new ComputeCloudClientException("test")));
        }});

        Assert.assertEquals(FormValidation.Kind.ERROR, doTestConnection(factory, SSHKEY_NAME_STRING, TEST_PRIVATE_KEY_PEM).kind);
    }

    @Test
    public void testDoVerifySshKeyPairMismatch() throws Exception {
        final ComputeCloudClientFactory factory = mockery.mock(ComputeCloudClientFactory.class);
        final ComputeCloudClient client = mockery.mock(ComputeCloudClient.class);
        mockery.checking(new Expectations() {{
            allowing(factory).createClient(ENDPOINT, USER, PASSWORD); will(returnValue(client));
            allowing(client).getSSHKey(SSHKEY_NAME); will(returnValue(new SSHKey().key("mismatch")));
        }});

        Assert.assertEquals(FormValidation.Kind.ERROR, doTestConnection(factory, SSHKEY_NAME_STRING, TEST_PRIVATE_KEY_PEM).kind);
    }

    @Test
    public void testGetDefaultStartTimeoutSeconds() {
        Assert.assertEquals(TimeUnit.MINUTES.toSeconds(5), ComputeCloudAgentTemplate.DescriptorImpl.getDefaultStartTimeoutSeconds());
    }

    @Test
    public void testDoCheckStartTimeoutSeconds() {
        Assert.assertEquals(FormValidation.Kind.ERROR, new TestComputeCloudAgentTemplate.DescriptorImpl().doCheckStartTimeoutSeconds(null).kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new TestComputeCloudAgentTemplate.DescriptorImpl().doCheckStartTimeoutSeconds("").kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new TestComputeCloudAgentTemplate.DescriptorImpl().doCheckStartTimeoutSeconds("x").kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new TestComputeCloudAgentTemplate.DescriptorImpl().doCheckStartTimeoutSeconds("-1").kind);
        Assert.assertEquals(FormValidation.Kind.OK, new TestComputeCloudAgentTemplate.DescriptorImpl().doCheckStartTimeoutSeconds("0").kind);
        Assert.assertEquals(FormValidation.Kind.OK, new TestComputeCloudAgentTemplate.DescriptorImpl().doCheckStartTimeoutSeconds("1").kind);
    }

    @Test
    public void testDisableCause() {
        TestComputeCloudAgentTemplate template = new TestComputeCloudAgentTemplate();
        Assert.assertNull(template.getDisableCause());

        for (int i = 0; i < ComputeCloudAgentTemplate.FAILURE_COUNT_LIMIT - 1; i++) {
            template.increaseFailureCount("error");
        }
        Assert.assertNull(template.getDisableCause());

        template.increaseFailureCount("error");
        Assert.assertNotNull(template.getDisableCause());
    }

    @Test
    public void testResetFailureCause() {
        TestComputeCloudAgentTemplate template = new TestComputeCloudAgentTemplate();
        Assert.assertNull(template.getDisableCause());

        for (int i = 0; i < ComputeCloudAgentTemplate.FAILURE_COUNT_LIMIT - 1; i++) {
            template.increaseFailureCount("error");
        }
        template.resetFailureCount();
        template.increaseFailureCount("error");
        Assert.assertNull(template.getDisableCause());

        for (int i = 0; i < ComputeCloudAgentTemplate.FAILURE_COUNT_LIMIT; i++) {
            template.increaseFailureCount("error");
        }
        Assert.assertNotNull(template.getDisableCause());
        template.resetFailureCount();
        Assert.assertNull(template.getDisableCause());
    }
}
