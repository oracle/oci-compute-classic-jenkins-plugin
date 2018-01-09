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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

import org.acegisecurity.Authentication;
import org.hamcrest.BaseMatcher;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.jmock.Expectations;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import com.oracle.cloud.compute.jenkins.client.ComputeCloudClient;
import com.oracle.cloud.compute.jenkins.client.ComputeCloudClientException;
import com.oracle.cloud.compute.jenkins.client.ComputeCloudClientFactory;
import com.oracle.cloud.compute.jenkins.client.ComputeCloudClientUnauthorizedException;
import com.oracle.cloud.compute.jenkins.model.InstanceOrchestration;
import com.oracle.cloud.compute.jenkins.ssh.SshConnector;
import com.trilead.ssh2.Connection;

import hudson.ProxyConfiguration;
import hudson.model.Node;
import hudson.model.Descriptor.FormException;
import hudson.model.labels.LabelAtom;
import hudson.security.ACL;
import hudson.security.AccessDeniedException2;
import hudson.security.Permission;
import hudson.slaves.Cloud;
import hudson.slaves.NodeProvisioner.PlannedNode;
import hudson.util.FormValidation;

public class ComputeCloudUnitTest {
    static { TestMessages.init(); }

    @Rule
    public final ComputeCloudMockery mockery = new ComputeCloudMockery();

    @Test
    public void testName() throws Exception {
        ComputeCloud cloud = new TestComputeCloud.Builder().cloudName("cn").build();
        Assert.assertEquals("cn", cloud.getCloudName());
        Assert.assertEquals("cn", cloud.getDisplayName());
        Assert.assertEquals("Oracle Cloud Infrastructure Compute Classic cn", cloud.getFullDisplayName());
    }

    @Test
    public void testGetNextTemplateId() {
        Assert.assertEquals(0, new TestComputeCloud().getNextTemplateId());
        Assert.assertEquals(1, new TestComputeCloud.Builder().nextTemplateId(1).build().getNextTemplateId());
    }

    @Test
    public void testGetTemplates() throws Exception {
        Assert.assertEquals(Collections.emptyList(), new TestComputeCloud.Builder().build().getTemplates());

        List<? extends ComputeCloudAgentTemplate> templates = new TestComputeCloud.Builder()
                .templates(Arrays.asList(new TestComputeCloudAgentTemplate.Builder().description("desc").build()))
                .build().getTemplates();
        Assert.assertEquals(1, templates.size());
        Assert.assertEquals("desc", templates.get(0).getDescription());
    }

    @Test
    public void testGetApiEndpoint() throws Exception {
        ComputeCloud cloud = new TestComputeCloud.Builder().apiEndpoint(ENDPOINT).build();
        Assert.assertEquals(ENDPOINT.toString(), cloud.getApiEndpoint());
        Assert.assertEquals(ENDPOINT, cloud.getApiEndpointUrl());
    }

    @Test
    public void testGetUser() throws Exception {
        ComputeCloud cloud = new TestComputeCloud.Builder().user(USER).build();
        Assert.assertEquals(USER.getIdentityDomainName(), cloud.getIdentityDomainName());
        Assert.assertEquals(USER.getUsername(), cloud.getUserName());
        Assert.assertEquals(USER, cloud.getUser());
    }

    @Test
    public void testGetPassword() {
        Assert.assertEquals(PASSWORD, new TestComputeCloud.Builder().password(PASSWORD).build().getPassword());
    }

    @Test(expected = IllegalStateException.class)
    public void testGetApiEndpointUrlError() {
        new TestComputeCloud.Builder().apiEndpoint(INVALID_ENDPOINT).build().getApiEndpointUrl();
    }

    @Test(expected = IllegalStateException.class)
    public void testGetUserError() {
        new TestComputeCloud.Builder().identityDomainName(INVALID_IDENTITY_DOMAIN_NAME).userName(INVALID_USER_NAME).build().getUser();
    }

    @Test
    public void testProvisionNoTemplates() {
        Assert.assertEquals(Collections.emptyList(), new TestComputeCloud.Builder().build().provision(null, 0));
        Assert.assertEquals(Collections.emptyList(), new TestComputeCloud.Builder().build().provision(null, 1));
        Assert.assertFalse(new TestComputeCloud.Builder().build().canProvision(null));
    }

    @Test
    public void testGetTemplateDisabled() {
        ComputeCloudAgentTemplate template = new TestComputeCloudAgentTemplate.Builder().mode(Node.Mode.NORMAL).build();
        ComputeCloud cloud = new TestComputeCloud.Builder().templates(Arrays.asList(template)).build();
        Assert.assertSame(template, cloud.getTemplate(null));
        for (int i = 0; i < ComputeCloudAgentTemplate.FAILURE_COUNT_LIMIT; i++) {
            template.increaseFailureCount("error");
        }
        template.increaseFailureCount("error");
    }

    private static final Matcher<String> AGENT_NAME_MATCHER = CoreMatchers.startsWith(ComputeCloud.AGENT_NAME_PREFIX);

    private TestComputeCloud.Builder newProvisionComputeCloudBuilder() {
        final ExecutorService threadPool = mockery.mock(ExecutorService.class);
        mockery.checking(new Expectations() {{ allowing(threadPool); }});

        return new TestComputeCloud.Builder()
                .templates(Collections.<ComputeCloudAgentTemplate>emptyList())
                .nodes(Collections.<Node>emptyList())
                .threadPoolForRemoting(threadPool)
                .plannedNodeFactory(TestComputeCloud.TestPlannedNode.FACTORY);
    }

    private static Matcher<String> orchNameMatcher() {
        return new LazyEqualsMatcher<>(CoreMatchers.startsWith(ComputeCloud.ORCHESTRATION_NAME_PREFIX));
    }

    @Test
    public void testNewPlannedNode() {
        @SuppressWarnings("unchecked")
        Future<Node> future = mockery.mock(Future.class);
        PlannedNode plannedNode = new TestComputeCloud.Builder().build().newPlannedNode("dn", future, 123, null);
        Assert.assertEquals("dn", plannedNode.displayName);
        Assert.assertSame(future, plannedNode.future);
        Assert.assertEquals(123, plannedNode.numExecutors);
    }

    private void assertPlannedNode(PlannedNode plannedNode, ComputeCloudAgentTemplate t) {
        MatcherAssert.assertThat(plannedNode.displayName, orchNameMatcher());
        TestComputeCloud.TestPlannedNode testPlannedNode = (TestComputeCloud.TestPlannedNode)plannedNode;
        Assert.assertSame(t, testPlannedNode.template);
    }

    @Test
    public void testProvision() {
        TestComputeCloudAgentTemplate t = new TestComputeCloudAgentTemplate.Builder().mode(Node.Mode.NORMAL).build();
        TestComputeCloud cloud = newProvisionComputeCloudBuilder()
                .templates(Arrays.asList(t))
                .build();
        Assert.assertTrue(cloud.canProvision(null));

        Collection<PlannedNode> plannedNodes = cloud.provision(null, 1);
        Assert.assertEquals(1, plannedNodes.size());
        assertPlannedNode(plannedNodes.iterator().next(), t);
    }

    @Test
    public void testProvisionInvalidMode() {
        TestComputeCloud cloud = newProvisionComputeCloudBuilder()
                .templates(Arrays.asList(new TestComputeCloudAgentTemplate.Builder().build()))
                .build();
        Assert.assertFalse(cloud.canProvision(null));

        Collection<PlannedNode> plannedNodes = cloud.provision(null, 1);
        Assert.assertEquals(0, plannedNodes.size());
    }

    @Test
    public void testProvisionWithNegativeNumExecutors() {
        TestComputeCloudAgentTemplate t = new TestComputeCloudAgentTemplate.Builder()
                .mode(Node.Mode.NORMAL)
                .numExecutors(-1)
                .build();
        TestComputeCloud cloud = newProvisionComputeCloudBuilder()
                .templates(Arrays.asList(t))
                .build();
        Assert.assertTrue(cloud.canProvision(null));

        Collection<PlannedNode> plannedNodes = cloud.provision(null, 1);
        Assert.assertEquals(1, plannedNodes.size());
        assertPlannedNode(plannedNodes.iterator().next(), t);
    }

    @Test
    public void testProvision2() {
        TestComputeCloudAgentTemplate t = new TestComputeCloudAgentTemplate.Builder().mode(Node.Mode.NORMAL).build();
        TestComputeCloud cloud = newProvisionComputeCloudBuilder()
                .templates(Arrays.asList(t))
                .build();
        Assert.assertTrue(cloud.canProvision(null));

        Collection<PlannedNode> plannedNodes = cloud.provision(null, 2);
        Assert.assertEquals(2, plannedNodes.size());
        Iterator<PlannedNode> iter = plannedNodes.iterator();
        assertPlannedNode(iter.next(), t);
        assertPlannedNode(iter.next(), t);
    }

    @Test
    public void testProvision2WithInstanceCap() {
        TestComputeCloudAgentTemplate t = new TestComputeCloudAgentTemplate.Builder().mode(Node.Mode.NORMAL).build();
        TestComputeCloud cloud = newProvisionComputeCloudBuilder()
                .templates(Arrays.asList(t))
                .instanceCap(1)
                .build();
        Assert.assertTrue(cloud.canProvision(null));

        Collection<PlannedNode> plannedNodes = cloud.provision(null, 2);
        Assert.assertEquals(1, plannedNodes.size());
        assertPlannedNode(plannedNodes.iterator().next(), t);
    }

    @Test
    public void testProvision2WithInstanceCapAndExisting() {
        TestComputeCloud cloud = newProvisionComputeCloudBuilder()
                .templates(Arrays.asList(new TestComputeCloudAgentTemplate.Builder().mode(Node.Mode.NORMAL).build()))
                .instanceCap(1)
                .nodes(Arrays.<Node>asList(new TestCloudAgent.Builder().build(), new TestComputeCloudAgent.Builder().cloudName(ComputeCloud.NAME_PREFIX + "cloudName").build()))
                .build();
        Assert.assertTrue(cloud.canProvision(null));

        Collection<PlannedNode> plannedNodes = cloud.provision(null, 1);
        Assert.assertEquals(0, plannedNodes.size());
    }

    @Test
    public void testProvisionWithInstanceCapAndExistingOtherCloud() {
        TestComputeCloudAgentTemplate t = new TestComputeCloudAgentTemplate.Builder().mode(Node.Mode.NORMAL).build();
        TestComputeCloud cloud = newProvisionComputeCloudBuilder()
                .templates(Arrays.asList(t))
                .instanceCap(1)
                .nodes(Arrays.<Node>asList(new TestCloudAgent.Builder().build(), new TestComputeCloudAgent.Builder().cloudName(ComputeCloud.NAME_PREFIX + "cloudName1").build()))
                .build();
        Assert.assertTrue(cloud.canProvision(null));

        Collection<PlannedNode> plannedNodes = cloud.provision(null, 1);
        assertPlannedNode(plannedNodes.iterator().next(), t);
    }

    @Test
    public void testProvision2WithNumExecutors() {
        TestComputeCloudAgentTemplate t = new TestComputeCloudAgentTemplate.Builder()
                .mode(Node.Mode.NORMAL)
                .numExecutors(2)
                .build();
        TestComputeCloud cloud = newProvisionComputeCloudBuilder()
                .templates(Arrays.asList(t))
                .build();
        Assert.assertTrue(cloud.canProvision(null));

        Collection<PlannedNode> plannedNodes = cloud.provision(null, 2);
        Assert.assertEquals(1, plannedNodes.size());
        assertPlannedNode(plannedNodes.iterator().next(), t);
    }

    @Test
    public void testProvisionWithoutLabelNormal() {
        TestComputeCloudAgentTemplate t0 = new TestComputeCloudAgentTemplate.Builder()
                .mode(Node.Mode.NORMAL)
                .labelString("label0")
                .numExecutors(2)
                .build();
        TestComputeCloud cloud = newProvisionComputeCloudBuilder()
                .templates(Arrays.asList(t0))
                .build();
        Assert.assertTrue(cloud.canProvision(null));

        Collection<PlannedNode> plannedNodes = cloud.provision(null, 1);
        Assert.assertEquals(1, plannedNodes.size());
        assertPlannedNode(plannedNodes.iterator().next(), t0);
    }

    @Test
    public void testProvisionWithLabelNormal() {
        TestComputeCloudAgentTemplate t0 = new TestComputeCloudAgentTemplate.Builder()
                .mode(Node.Mode.NORMAL)
                .labelString("label0")
                .numExecutors(2)
                .build();
        TestComputeCloudAgentTemplate t1 = new TestComputeCloudAgentTemplate.Builder()
                .mode(Node.Mode.NORMAL)
                .labelString("label1")
                .numExecutors(2)
                .build();
        TestComputeCloud cloud = newProvisionComputeCloudBuilder()
                .templates(Arrays.asList(t0, t1))
                .build();
        LabelAtom label = new LabelAtom("label1");
        Assert.assertTrue(cloud.canProvision(label));

        Collection<PlannedNode> plannedNodes = cloud.provision(label, 1);
        Assert.assertEquals(1, plannedNodes.size());
        assertPlannedNode(plannedNodes.iterator().next(), t1);
    }

    @Test
    public void testProvisionWithoutLabelExclusive() {
        TestComputeCloudAgentTemplate t0 = new TestComputeCloudAgentTemplate.Builder()
                .mode(Node.Mode.EXCLUSIVE)
                .labelString("label0")
                .numExecutors(2)
                .build();
        TestComputeCloud cloud = newProvisionComputeCloudBuilder()
                .templates(Arrays.asList(t0))
                .build();
        Assert.assertFalse(cloud.canProvision(null));

        Collection<PlannedNode> plannedNodes = cloud.provision(null, 1);
        Assert.assertEquals(0, plannedNodes.size());
    }

    @Test
    public void testProvisionWithLabelExclusive() {
        TestComputeCloudAgentTemplate t0 = new TestComputeCloudAgentTemplate.Builder()
                .mode(Node.Mode.EXCLUSIVE)
                .labelString("label0")
                .numExecutors(2)
                .build();
        TestComputeCloudAgentTemplate t1 = new TestComputeCloudAgentTemplate.Builder()
                .mode(Node.Mode.EXCLUSIVE)
                .labelString("label1")
                .numExecutors(2)
                .build();
        TestComputeCloud cloud = newProvisionComputeCloudBuilder()
                .templates(Arrays.asList(t0, t1))
                .build();
        LabelAtom label = new LabelAtom("label1");
        Assert.assertTrue(cloud.canProvision(label));

        Collection<PlannedNode> plannedNodes = cloud.provision(label, 1);
        Assert.assertEquals(1, plannedNodes.size());
        assertPlannedNode(plannedNodes.iterator().next(), t1);
    }

    private interface CallableFuture<T> extends Future<T> {
        Callable<?> getCallable();
    }

    private interface NewAgentCallback {
        ComputeCloudAgent newComputeCloudAgent(
                final String name,
                final ComputeCloudAgentTemplate template,
                final String cloudName,
                final String orchName,
                final String host);
    }

    private static final String TEST_CLOUD_NAME = new TestComputeCloud.Builder().build().name;

    private NewAgentCallback mockNewAgentCallback(
            final ComputeCloudAgentTemplate template,
            final Matcher<String> orchNameMatcher,
            final String host) {
        final NewAgentCallback callback = mockery.mock(NewAgentCallback.class);
        mockery.checking(new Expectations() {{
            allowing(callback).newComputeCloudAgent(
                    with(AGENT_NAME_MATCHER),
                    with(template),
                    with(TEST_CLOUD_NAME),
                    with(orchNameMatcher),
                    with(host));
            will(returnValue(new TestComputeCloudAgent.Builder().build()));
        }});
        return callback;
    }

    private ComputeCloudAgent provision(ComputeCloudClient client, final ComputeCloudAgentTemplate template, final NewAgentCallback callback) throws Exception {
        final ExecutorService threadPoolForRemoting = mockery.mock(ExecutorService.class);
        mockery.checking(new Expectations() {{
            @SuppressWarnings({ "unchecked", "rawtypes" })
            Matcher<Callable<?>> anyCallable = (Matcher)any(Callable.class);
            oneOf(threadPoolForRemoting).submit(with(anyCallable)); will(new CustomAction("submit") {
                @Override
                public Object invoke(Invocation invocation) throws Throwable {
                    final Callable<?> callable = (Callable<?>)invocation.getParameter(0);
                    final CallableFuture<?> future = mockery.mock(CallableFuture.class);
                    mockery.checking(new Expectations() {{ allowing(future).getCallable(); will(returnValue(callable)); }});
                    return future;
                }
            });
        }});

        final boolean[] connectCalledRef = new boolean[1];
        SshConnector sshConnector = new SshConnector() {
            @Override
            public ProxyConfiguration getProxyConfiguration() {
                return null;
            }

            @Override
            public void connect(Connection conn, int timeoutMillis) throws IOException {
                connectCalledRef[0] = true;
                Assert.assertEquals(timeoutMillis, template.getSshConnectTimeoutMillis());
            }
        };

        ComputeCloud cloud = new TestComputeCloud(new TestComputeCloud.Builder()
                .nodes(Collections.<Node>emptyList())
                .templates(Arrays.asList(template))
                .client(client)
                .threadPoolForRemoting(threadPoolForRemoting)
                .clock(new TestClock())
                .sshConnector(sshConnector)) {
            @Override
            ComputeCloudAgent newComputeCloudAgent(
                    String name,
                    ComputeCloudAgentTemplate template,
                    String cloudName,
                    String orchName,
                    String host) throws IOException, FormException {
                return callback.newComputeCloudAgent(name, template, cloudName, orchName, host);
            }
        };
        Collection<PlannedNode> plannedNodes = cloud.provision(null, 1);
        Assert.assertEquals(1, plannedNodes.size());
        CallableFuture<?> future = (CallableFuture<?>)plannedNodes.iterator().next().future;
        Callable<?> callable = future.getCallable();
        ComputeCloudAgent agent = (ComputeCloudAgent)callable.call();
        Assert.assertTrue(connectCalledRef[0]);
        return agent;
    }

    private static class LazyEqualsMatcher<T> extends BaseMatcher<T> {
        private final Matcher<T> matcher;
        private T value;

        LazyEqualsMatcher(Matcher<T> matcher) {
            this.matcher = Objects.requireNonNull(matcher);
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean matches(Object item) {
            if (value == null) {
                value = (T)item;
                return matcher.matches(item);
            }
            return Objects.equals(item, value);
        }

        @Override
        public void describeTo(Description description) {
            if (value == null) {
                matcher.describeTo(description);
            } else {
                description.appendValue(value);
            }
        }
    }

    @Test
    public void testProvisionSubmit() throws Exception {
        final ComputeCloudClient client = mockery.mock(ComputeCloudClient.class);
        final ComputeCloudAgentTemplate template = new TestComputeCloudAgentTemplate.Builder().mode(Node.Mode.NORMAL).build();
        final Matcher<String> orchNameMatcher = orchNameMatcher();
        final NewAgentCallback callback = mockNewAgentCallback(template, orchNameMatcher, "ip");
        mockery.checking(new Expectations() {{
            oneOf(client).createInstanceOrchestration(with(orchNameMatcher), with(template));
            oneOf(client).startOrchestration(with(orchNameMatcher));
            oneOf(client).getInstanceOrchestration(with(orchNameMatcher)); will(returnValue(new InstanceOrchestration().status(InstanceOrchestration.Status.ready).ip("ip")));
            oneOf(client).close();
        }});

        provision(client, template, callback);
    }

    @Test(expected = ComputeCloudClientException.class)
    public void testProvisionSubmitCreateError() throws Exception {
        final ComputeCloudClient client = mockery.mock(ComputeCloudClient.class);
        final ComputeCloudAgentTemplate template = new TestComputeCloudAgentTemplate.Builder().mode(Node.Mode.NORMAL).build();
        mockery.checking(new Expectations() {{
            oneOf(client).createInstanceOrchestration(with(orchNameMatcher()), with(template)); will(throwException(new ComputeCloudClientException("test")));
            oneOf(client).close();
        }});

        provision(client, template, null);
    }

    @Test(expected = ComputeCloudClientException.class)
    public void testProvisionSubmitGetStatusErrorAndRecycleSuccess() throws Exception {
        final ComputeCloudClient client = mockery.mock(ComputeCloudClient.class);
        final ComputeCloudAgentTemplate template = new TestComputeCloudAgentTemplate.Builder().mode(Node.Mode.NORMAL).build();
        mockery.checking(new Expectations() {{
            Matcher<String> orchNameMatcher = orchNameMatcher();
            oneOf(client).createInstanceOrchestration(with(orchNameMatcher), with(template));
            oneOf(client).startOrchestration(with(orchNameMatcher));
            oneOf(client).getInstanceOrchestration(with(orchNameMatcher)); will(throwException(new ComputeCloudClientException("test")));
            oneOf(client).getInstanceOrchestration(with(orchNameMatcher)); will(throwException(new ComputeCloudClientException("test")));
            oneOf(client).close();
        }});

        provision(client, template, null);
    }

    @Test(expected = IOException.class)
    public void testProvisionSubmitStatusErrorAndRecycleSuccess() throws Exception {
        final ComputeCloudClient client = mockery.mock(ComputeCloudClient.class);
        final ComputeCloudAgentTemplate template = new TestComputeCloudAgentTemplate.Builder().mode(Node.Mode.NORMAL).build();
        mockery.checking(new Expectations() {{
            Matcher<String> orchNameMatcher = orchNameMatcher();
            oneOf(client).createInstanceOrchestration(with(orchNameMatcher), with(template));
            oneOf(client).startOrchestration(with(orchNameMatcher));
            oneOf(client).getInstanceOrchestration(with(orchNameMatcher)); will(returnValue(new InstanceOrchestration().status(InstanceOrchestration.Status.error)));
            oneOf(client).getInstanceOrchestration(with(orchNameMatcher)); will(returnValue(new InstanceOrchestration().status(InstanceOrchestration.Status.error)));
            oneOf(client).stopOrchestration(with(orchNameMatcher));
            oneOf(client).getInstanceOrchestration(with(orchNameMatcher)); will(returnValue(new InstanceOrchestration().status(InstanceOrchestration.Status.stopped)));
            oneOf(client).deleteOrchestration(with(orchNameMatcher));
            oneOf(client).close();
        }});

        provision(client, template, null);
    }

    @Test
    public void testProvisionSubmitSlow() throws Exception {
        final ComputeCloudClient client = mockery.mock(ComputeCloudClient.class);
        final ComputeCloudAgentTemplate template = new TestComputeCloudAgentTemplate.Builder().mode(Node.Mode.NORMAL).build();
        final Matcher<String> orchNameMatcher = orchNameMatcher();
        final NewAgentCallback callback = mockNewAgentCallback(template, orchNameMatcher, "ip");
        mockery.checking(new Expectations() {{
            oneOf(client).createInstanceOrchestration(with(orchNameMatcher), with(template));
            oneOf(client).startOrchestration(with(orchNameMatcher));
            oneOf(client).getInstanceOrchestration(with(orchNameMatcher)); will(returnValue(new InstanceOrchestration().status(InstanceOrchestration.Status.starting)));
            oneOf(client).getInstanceOrchestration(with(orchNameMatcher)); will(returnValue(new InstanceOrchestration().status(InstanceOrchestration.Status.ready).ip("ip")));
            oneOf(client).close();
        }});

        provision(client, template, callback);
    }

    @Test
    public void testProvisionSubmitCreateConsecutiveErrors() throws Exception {
        final ComputeCloudClient client = mockery.mock(ComputeCloudClient.class);
        final ComputeCloudAgentTemplate template = new TestComputeCloudAgentTemplate.Builder().mode(Node.Mode.NORMAL).build();
        for (int i = 0; i < ComputeCloudAgentTemplate.FAILURE_COUNT_LIMIT; i++) {
                mockery.checking(new Expectations() {{
                oneOf(client).createInstanceOrchestration(with(orchNameMatcher()), with(template)); will(throwException(new ComputeCloudClientException("test")));
                oneOf(client).close();
                }});
            try {
                provision(client, template, null);
                Assert.fail("expect to fail");
            } catch (ComputeCloudClientException e) {}
        }
        Assert.assertNotNull("template is expected to be disabled", template.getDisableCause());
    }

    @Test
    public void testProvisionSubmitCreateErrorsAndSucceed() throws Exception {
        final ComputeCloudClient client = mockery.mock(ComputeCloudClient.class);
        final ComputeCloudAgentTemplate template = new TestComputeCloudAgentTemplate.Builder().mode(Node.Mode.NORMAL).build();
        final Matcher<String> orchNameMatcher = orchNameMatcher();
        final NewAgentCallback callback = mockNewAgentCallback(template, orchNameMatcher, "ip");

        // Let provision succeed before it reaches the limit, and then check whether template disalbeCause is reset
        for (int i = 0; i < ComputeCloudAgentTemplate.FAILURE_COUNT_LIMIT + 1; i++) {
            if (i == ComputeCloudAgentTemplate.FAILURE_COUNT_LIMIT - 1) {
                mockery.checking(new Expectations() {{
                    oneOf(client).createInstanceOrchestration(with(orchNameMatcher), with(template));
                    oneOf(client).startOrchestration(with(orchNameMatcher));
                    oneOf(client).getInstanceOrchestration(with(orchNameMatcher)); will(returnValue(new InstanceOrchestration().status(InstanceOrchestration.Status.ready).ip("ip")));
                    oneOf(client).close();
                }});
                System.out.println(i);
                provision(client, template, callback);
            } else {
                mockery.checking(new Expectations() {{
                    oneOf(client).createInstanceOrchestration(with(orchNameMatcher()), with(template)); will(throwException(new ComputeCloudClientException("test")));
                    oneOf(client).close();
                }});
                try {
                    System.out.println(i);
                    provision(client, template, null);
                    Assert.fail("expect to fail");
                } catch (ComputeCloudClientException e) {}
            }
        }
        Assert.assertNull("template disable not reset", template.getDisableCause());
    }

    private static class TestACL extends ACL {
        private final Permission permission;
        private final boolean hasPermission;

        TestACL(Permission permission, boolean hasPermission) {
            this.permission = permission;
            this.hasPermission = hasPermission;
        }

        @Override
        public boolean hasPermission(Authentication a, Permission permission) {
            Assert.assertSame(this.permission, permission);
            return hasPermission;
        }
    }

    private void allowingGetProvisionView(final ComputeCloud cloud, final StaplerRequest req, final StaplerResponse rsp) throws Exception {
        final RequestDispatcher rd = mockery.mock(RequestDispatcher.class);
        mockery.checking(new Expectations() {{
            allowing(req).setAttribute(with(ComputeCloud.PROVISION_ATTR_AGENT_NAME), with(AGENT_NAME_MATCHER));
            allowing(req).setAttribute(ComputeCloud.PROVISION_ATTR_NUM_EXECUTORS, 1);
            allowing(req).getView(cloud, "provision"); will(returnValue(rd));
            allowing(rd).forward(req, rsp);
        }});
    }

    @Test
    public void testDoProvision() throws Exception {
        final StaplerRequest req = mockery.mock(StaplerRequest.class);
        final StaplerResponse rsp = mockery.mock(StaplerResponse.class);
        ComputeCloud cloud = new TestComputeCloud.Builder().acl(new TestACL(Cloud.PROVISION, true)).build();
        allowingGetProvisionView(cloud, req, rsp);
    }

    private void allowingSendError(final Object it, final StaplerRequest req, final StaplerResponse rsp) throws Exception {
        mockery.checking(new Expectations() {{
            allowing(req).setAttribute(with(any(String.class)), with(any(Object.class)));
            allowing(rsp).forward(it, "error", req);
        }});
    }

    @Test
    public void testDoProvisionUnknownTemplate() throws Exception {
        final StaplerRequest req = mockery.mock(StaplerRequest.class);
        final StaplerResponse rsp = mockery.mock(StaplerResponse.class);
        ComputeCloud cloud = new TestComputeCloud.Builder()
                .templates(Arrays.asList(new TestComputeCloudAgentTemplate()))
                .acl(new TestACL(Cloud.PROVISION, true))
                .build();
        allowingSendError(cloud, req, rsp);
        cloud.doProvision(1, req, rsp);
    }

    @Test
    public void testDoProvisionDisabledTemplate() throws Exception {
        final StaplerRequest req = mockery.mock(StaplerRequest.class);
        final StaplerResponse rsp = mockery.mock(StaplerResponse.class);
        final TestComputeCloudAgentTemplate template = new TestComputeCloudAgentTemplate();
        for (int i = 0; i < ComputeCloudAgentTemplate.FAILURE_COUNT_LIMIT; i++) {
            template.increaseFailureCount("error");
        }
        ComputeCloud cloud = new TestComputeCloud.Builder()
                .templates(Arrays.asList(template))
                .acl(new TestACL(Cloud.PROVISION, true))
                .build();
        allowingSendError(cloud, req, rsp);
        cloud.doProvision(template.getTemplateId(), req, rsp);
    }

    @Test(expected = AccessDeniedException2.class)
    public void testDoProvisionNoPermission() throws Exception {
        new TestComputeCloud.Builder().acl(new TestACL(Cloud.PROVISION, false)).build().doProvision(0, null, null);
    }

    private boolean doProvision(ComputeCloudClient client, ComputeCloudAgentTemplate template, final NewAgentCallback callback) throws Exception {
        final ExecutorService threadPoolForRemoting = mockery.mock(ExecutorService.class);
        final boolean[] addNodeCalled = new boolean[1];
        mockery.checking(new Expectations() {{
            @SuppressWarnings({ "unchecked", "rawtypes" })
            Matcher<Callable<?>> anyCallable = (Matcher)any(Callable.class);
            oneOf(threadPoolForRemoting).submit(with(anyCallable)); will(new CustomAction("submit") {
                @Override
                public Object invoke(Invocation invocation) throws Throwable {
                    Callable<?> callable = (Callable<?>)invocation.getParameter(0);
                    try {
                        callable.call();
                    } catch (Error e) {
                        Assert.assertFalse(addNodeCalled[0]);
                        throw e;
                    }
                    return null;
                }
            });
        }});

        SshConnector sshConnector = new SshConnector() {
            @Override
            public ProxyConfiguration getProxyConfiguration() {
                return null;
            }

            @Override
            public void connect(Connection conn, int timeoutMillis) throws IOException {}
        };

        final ComputeCloud cloud = new TestComputeCloud(new TestComputeCloud.Builder()
                .nodes(Collections.<Node>emptyList())
                .templates(Arrays.asList(template))
                .client(client)
                .threadPoolForRemoting(threadPoolForRemoting)
                .clock(new TestClock())
                .sshConnector(sshConnector)
                .acl(new TestACL(Cloud.PROVISION, true))) {
            @Override
            ComputeCloudAgent newComputeCloudAgent(
                    String name,
                    ComputeCloudAgentTemplate template,
                    String cloudName,
                    String orchName,
                    String host) throws IOException, FormException {
                return callback.newComputeCloudAgent(name, template, cloudName, orchName, host);
            }

            @Override
            void addNode(Node node) throws IOException {
                Assert.assertNotNull(node);
                addNodeCalled[0] = true;
            }
        };
        final StaplerRequest req = mockery.mock(StaplerRequest.class);
        final StaplerResponse rsp = mockery.mock(StaplerResponse.class);
        allowingGetProvisionView(cloud, req, rsp);

        cloud.doProvision(template.getTemplateId(), req, rsp);
        return addNodeCalled[0];
    }

    @Test
    public void testDoProvisionSubmit() throws Exception {
        final ComputeCloudClient client = mockery.mock(ComputeCloudClient.class);
        final Matcher<String> orchNameMatcher = orchNameMatcher();
        final TestComputeCloudAgentTemplate template = new TestComputeCloudAgentTemplate();
        mockery.checking(new Expectations() {{
            oneOf(client).createInstanceOrchestration(with(orchNameMatcher), with(template));
            oneOf(client).startOrchestration(with(orchNameMatcher));
            oneOf(client).getInstanceOrchestration(with(orchNameMatcher)); will(returnValue(new InstanceOrchestration().status(InstanceOrchestration.Status.ready).ip("ip")));
            oneOf(client).close();
        }});

        Assert.assertTrue(doProvision(client, template, mockNewAgentCallback(template, orchNameMatcher(), "ip")));
    }

    @Test
    public void testDoProvisionSubmitComputeCloudClientException() throws Exception {
        final ComputeCloudClient client = mockery.mock(ComputeCloudClient.class);
        final Matcher<String> orchNameMatcher = orchNameMatcher();
        final TestComputeCloudAgentTemplate template = new TestComputeCloudAgentTemplate();
        mockery.checking(new Expectations() {{
            oneOf(client).createInstanceOrchestration(with(orchNameMatcher), with(template)); will(throwException(new ComputeCloudClientException("test")));
            oneOf(client).close();
        }});

        Assert.assertFalse(doProvision(client, template, mockNewAgentCallback(template, orchNameMatcher(), "ip")));
    }

    @Test
    public void testGetProvisionStartedMessage() {
        final HttpServletRequest req = mockery.mock(HttpServletRequest.class);
        mockery.checking(new Expectations() {{
            allowing(req).getAttribute(ComputeCloud.PROVISION_ATTR_AGENT_NAME); will(returnValue("an"));
            allowing(req).getAttribute(ComputeCloud.PROVISION_ATTR_NUM_EXECUTORS); will(returnValue(1));
        }});
        new TestComputeCloud().getProvisionStartedMessage(req);
    }

    @Test
    public void testConfigMessages() throws Exception {
        for (Method method : ComputeCloud.ConfigMessages.class.getMethods()) {
            if (Modifier.isStatic(method.getModifiers())) {
                Assert.assertNotNull(method.invoke(null));
            }
        }
    }

    @Test
    public void testDoCheckCloudName() {
        List<Cloud> emptyClouds = Collections.<Cloud>emptyList();
        Assert.assertEquals(FormValidation.Kind.OK, new TestComputeCloud.TestDescriptor.Builder().clouds(emptyClouds).build().doCheckCloudName("x").kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new TestComputeCloud.TestDescriptor.Builder().clouds(emptyClouds).build().doCheckCloudName("").kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new TestComputeCloud.TestDescriptor.Builder().clouds(emptyClouds).build().doCheckCloudName(" ").kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new TestComputeCloud.TestDescriptor.Builder().clouds(emptyClouds).build().doCheckCloudName(".").kind);

        TestCloud other = new TestCloud("x");
        TestCloud match = new TestCloud(ComputeCloud.cloudNameToName("x"));
        Assert.assertEquals(FormValidation.Kind.OK, new TestComputeCloud.TestDescriptor.Builder().clouds(other, match).build().doCheckCloudName("x").kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new TestComputeCloud.TestDescriptor.Builder().clouds(other, match, match).build().doCheckCloudName("x").kind);
    }

    @Test
    public void testDoCheckApiEndpoint() throws Exception {
        Assert.assertEquals(FormValidation.Kind.OK, new ComputeCloud.DescriptorImpl().doCheckApiEndpoint("https://x").kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new ComputeCloud.DescriptorImpl().doCheckApiEndpoint("").kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new ComputeCloud.DescriptorImpl().doCheckApiEndpoint("x").kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new ComputeCloud.DescriptorImpl().doCheckApiEndpoint("ftp://x").kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new ComputeCloud.DescriptorImpl().doCheckApiEndpoint(INVALID_ENDPOINT).kind);
    }

    @Test
    public void testDoCheckIdentityDomainName() {
        Assert.assertEquals(FormValidation.Kind.OK, new ComputeCloud.DescriptorImpl().doCheckIdentityDomainName("a").kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new ComputeCloud.DescriptorImpl().doCheckIdentityDomainName("").kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new ComputeCloud.DescriptorImpl().doCheckIdentityDomainName("a/b").kind);
    }

    @Test
    public void testDoCheckUserName() {
        Assert.assertEquals(FormValidation.Kind.OK, new ComputeCloud.DescriptorImpl().doCheckUserName("a").kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new ComputeCloud.DescriptorImpl().doCheckUserName("").kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new ComputeCloud.DescriptorImpl().doCheckUserName("a/b").kind);
    }

    @Test
    public void testDoTestConnectionFormValidationError() {
        Assert.assertEquals(FormValidation.Kind.ERROR, new ComputeCloud.DescriptorImpl().doTestConnection("", "idm", "u", "p").kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new ComputeCloud.DescriptorImpl().doTestConnection("https://x", "", "u", "p").kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new ComputeCloud.DescriptorImpl().doTestConnection("https://x", "idm", "", "p").kind);
        Assert.assertEquals(FormValidation.Kind.ERROR, new ComputeCloud.DescriptorImpl().doTestConnection("https://x", "idm", "u", "").kind);
    }

    private static FormValidation doTestConnection(final ComputeCloudClientFactory factory) throws Exception {
        return new TestComputeCloud.TestDescriptor.Builder().clientManager(new ComputeCloudClientManager(factory, Collections.<Cloud>emptyList())).build()
                .doTestConnection(ENDPOINT.toString(), USER.getIdentityDomainName(), USER.getUsername(), PASSWORD);
    }

    @Test
    public void testDoTestConnection() throws Exception {
        final ComputeCloudClientFactory factory = mockery.mock(ComputeCloudClientFactory.class);
        final ComputeCloudClient client = mockery.mock(ComputeCloudClient.class);
        mockery.checking(new Expectations() {{
            allowing(factory).createClient(ENDPOINT, USER, PASSWORD); will(returnValue(client));
            allowing(client).authenticate();
        }});

        Assert.assertEquals(FormValidation.Kind.OK, doTestConnection(factory).kind);
    }

    @Test
    public void testDoTestConnectionUnauthorized() throws Exception {
        final ComputeCloudClientFactory factory = mockery.mock(ComputeCloudClientFactory.class);
        final ComputeCloudClient client = mockery.mock(ComputeCloudClient.class);
        mockery.checking(new Expectations() {{
            allowing(factory).createClient(ENDPOINT, USER, PASSWORD); will(returnValue(client));
            allowing(client).authenticate(); will(throwException(new ComputeCloudClientUnauthorizedException("test")));
        }});

        Assert.assertEquals(FormValidation.Kind.ERROR, doTestConnection(factory).kind);
    }

    @Test
    public void testDoTestConnectionError() throws Exception {
        final ComputeCloudClientFactory factory = mockery.mock(ComputeCloudClientFactory.class);
        final ComputeCloudClient client = mockery.mock(ComputeCloudClient.class);
        mockery.checking(new Expectations() {{
            allowing(factory).createClient(ENDPOINT, USER, PASSWORD); will(returnValue(client));
            allowing(client).authenticate(); will(throwException(new ComputeCloudClientException("test")));
        }});

        Assert.assertEquals(FormValidation.Kind.ERROR, doTestConnection(factory).kind);
    }
}
