package com.oracle.cloud.compute.jenkins.client;

import static com.oracle.cloud.compute.jenkins.ComputeCloudTestUtils.ENDPOINT;
import static com.oracle.cloud.compute.jenkins.ComputeCloudTestUtils.PASSWORD;
import static com.oracle.cloud.compute.jenkins.ComputeCloudTestUtils.USER;
import static com.oracle.cloud.compute.jenkins.client.JaxrsComputeCloudClient.createArrayBuilder;
import static com.oracle.cloud.compute.jenkins.client.JaxrsComputeCloudClient.createObjectBuilder;
import static com.oracle.cloud.compute.jenkins.client.JaxrsComputeCloudClient.entity;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriBuilder;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.jmock.Expectations;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import com.oracle.cloud.compute.jenkins.ComputeCloudMockery;
import com.oracle.cloud.compute.jenkins.TestComputeCloudAgentTemplate;
import com.oracle.cloud.compute.jenkins.TestMessages;
import com.oracle.cloud.compute.jenkins.model.ImageList;
import com.oracle.cloud.compute.jenkins.model.ImageListSourceType;
import com.oracle.cloud.compute.jenkins.model.InstanceOrchestration;
import com.oracle.cloud.compute.jenkins.model.SSHKey;
import com.oracle.cloud.compute.jenkins.model.SecurityList;
import com.oracle.cloud.compute.jenkins.model.Shape;

public class JaxrsComputeCloudClientUnitTest {
    static { TestMessages.init(); }

    @Rule
    public final ComputeCloudMockery mockery = new ComputeCloudMockery();

    private Client mockClient() {
        final Client client = mockery.mock(Client.class);
        mockery.checking(new Expectations() {{
            allowing(client).close();
        }});
        return client;
    }

    @Test
    public void test() {
        try (JaxrsComputeCloudClient c = new JaxrsComputeCloudClient(ENDPOINT, USER, PASSWORD, mockClient())) {
            c.toString();
        }
    }

    private static class ClientExpectations extends Expectations {
        UriBuilder withUriBuilder(final String s) {
            return with(new BaseMatcher<UriBuilder>() {
                @Override
                public boolean matches(Object item) {
                    return s.equals(((UriBuilder)item).build().toString());
                }

                @Override
                public void describeTo(Description description) {
                    description.appendText("UriBuilder.fromUri(").appendValue(s).appendText(")");
                }
            });
        }
    }

    private static abstract class ResponseBuilderImpl extends Response.ResponseBuilder {
        protected static ResponseBuilder newInstance() {
            return Response.ResponseBuilder.newInstance();
        }
    }

    private static Response.ResponseBuilder newResponseBuilder() {
        return ResponseBuilderImpl.newInstance();
    }

//    private static final String AUTHENTICATE_COOKIE_NAME = "nimbula";
    private static final String AUTHENTICATE_COOKIE_VALUE = "authenticated";

    private void allowingAuthenticate(Client client, URI endpoint, String user, String password) {
        final Invocation invocation = allowingAuthenticateBuildPost(client, endpoint, user, password);
        mockery.checking(new ClientExpectations() {{
            allowing(invocation).invoke(); will(returnValue(newResponseBuilder()
                    .status(Response.Status.OK)
                    .cookie(new NewCookie(TestJaxrsComputeCloudClient.AUTHENTICATE_COOKIE_NAME, AUTHENTICATE_COOKIE_VALUE, "/", null, NewCookie.DEFAULT_VERSION, null, (int)TimeUnit.MINUTES.toSeconds(30), null, false, false))
                    .build()));
        }});
    }

    private void allowingAuthenticate(Client client, URI endpoint, String user, String password, final Response.Status status) {
        final Invocation invocation = allowingAuthenticateBuildPost(client, endpoint, user, password);
        mockery.checking(new ClientExpectations() {{
            allowing(invocation).invoke(); will(returnValue(newResponseBuilder().status(status).build()));
        }});
    }

    private Invocation allowingAuthenticateBuildPost(final Client client, final URI endpoint, final String user, final String password) {
        final WebTarget target = mockery.mock(WebTarget.class);
        final Invocation.Builder builder = mockery.mock(Invocation.Builder.class);
        final Invocation invocation = mockery.mock(Invocation.class);
        mockery.checking(new ClientExpectations() {{
            allowing(client).target(withUriBuilder(endpoint + "/authenticate/")); will(returnValue(target));
            allowing(target).request(); will(returnValue(builder));

            allowing(builder).buildPost(entity(createObjectBuilder()
                    .add("user", user)
                    .add("password", password)
                    .build()));
            will(returnValue(invocation));
        }});

        return invocation;
    }

    @Test
    public void testAuthenticate() throws Exception {
        final Client client = mockClient();
        allowingAuthenticate(client, ENDPOINT, USER.getString(), PASSWORD);

        try (JaxrsComputeCloudClient c = new JaxrsComputeCloudClient(ENDPOINT, USER, PASSWORD, client)) {
            c.authenticate();
        }
    }

    @Test
    public void testAuthenticateProcessingException() throws Exception {
        final Client client = mockClient();
        final Invocation invocation = allowingAuthenticateBuildPost(client, ENDPOINT, USER.getString(), PASSWORD);
        final Throwable cause = new ProcessingException("test");
        mockery.checking(new Expectations() {{
            allowing(invocation).invoke(); will(throwException(cause));
        }});

        try (JaxrsComputeCloudClient c = new JaxrsComputeCloudClient(ENDPOINT, USER, PASSWORD, client)) {
            c.authenticate();
        } catch (ComputeCloudClientException e) {
            Assert.assertEquals("test", e.getMessage());
            Assert.assertSame(cause, e.getCause());
        }
    }

    @Test
    public void testAuthenticateProcessingIOException() throws Exception {
        final Client client = mockClient();
        final Invocation invocation = allowingAuthenticateBuildPost(client, ENDPOINT, USER.getString(), PASSWORD);
        final Throwable cause = new ProcessingException(new IOException());
        mockery.checking(new Expectations() {{
            allowing(invocation).invoke(); will(throwException(cause));
        }});

        try (JaxrsComputeCloudClient c = new JaxrsComputeCloudClient(ENDPOINT, USER, PASSWORD, client)) {
            c.authenticate();
        } catch (ComputeCloudClientException e) {
            Assert.assertSame(cause, e.getCause());
        }
    }

    @Test
    public void testAuthenticateProcessingJsonException() throws Exception {
        final Client client = mockClient();
        final Invocation invocation = allowingAuthenticateBuildPost(client, ENDPOINT, USER.getString(), PASSWORD);
        final Throwable cause = new ProcessingException(new JsonException("test"));
        mockery.checking(new Expectations() {{
            allowing(invocation).invoke(); will(throwException(cause));
        }});

        try (JaxrsComputeCloudClient c = new JaxrsComputeCloudClient(ENDPOINT, USER, PASSWORD, client)) {
            c.authenticate();
            Assert.fail();
        } catch (ComputeCloudClientException e) {
            Assert.assertSame(cause, e.getCause());
        }
    }

    @Test
    public void testAuthenticateProcessingJsonIOException() throws Exception {
        final Client client = mockClient();
        final Invocation invocation = allowingAuthenticateBuildPost(client, ENDPOINT, USER.getString(), PASSWORD);
        final Throwable cause = new ProcessingException(new JsonException("test", new IOException("cause")));
        mockery.checking(new Expectations() {{
            allowing(invocation).invoke(); will(throwException(cause));
        }});

        try (JaxrsComputeCloudClient c = new JaxrsComputeCloudClient(ENDPOINT, USER, PASSWORD, client)) {
            c.authenticate();
            Assert.fail();
        } catch (ComputeCloudClientException e) {
            Assert.assertEquals("cause", e.getMessage());
            Assert.assertSame(cause, e.getCause());
        }
    }

    @Test(expected = ComputeCloudClientException.class)
    public void testAuthenticateInternalServerError() throws Exception {
        final Client client = mockClient();
        allowingAuthenticate(client, ENDPOINT, USER.getString(), PASSWORD, Response.Status.INTERNAL_SERVER_ERROR);

        try (JaxrsComputeCloudClient c = new JaxrsComputeCloudClient(ENDPOINT, USER, PASSWORD, client)) {
            c.authenticate();
        }
    }

    @Test(expected = ComputeCloudClientUnauthorizedException.class)
    public void testAuthenticateUnauthorized() throws Exception {
        final Client client = mockClient();
        allowingAuthenticate(client, ENDPOINT, USER.getString(), PASSWORD, Response.Status.UNAUTHORIZED);

        try (JaxrsComputeCloudClient c = new JaxrsComputeCloudClient(ENDPOINT, USER, PASSWORD, client)) {
            c.authenticate();
        }
    }

    private static class TestJaxrsComputeCloudClient extends JaxrsComputeCloudClient {
        static final String READ_ENTITY_HEADER = TestJaxrsComputeCloudClient.class.getName() + ".readEntity";

        static <T> Response.ResponseBuilder addReadEntity(Response.ResponseBuilder builder, Class<T> entityType, T entity) {
            return builder.header(READ_ENTITY_HEADER, Collections.singletonMap(entityType, entity));
        }

        public TestJaxrsComputeCloudClient(URI apiEndpoint, ComputeCloudUser user, String password, Client client) {
            super(apiEndpoint, user, password, client);
        }

        @Override
        <T> T readEntity(Response response, Class<T> entityType) {
            // We don't want to implement our own Response subclass, so we use
            // newResponseBuilder(), but that creates OutboundJaxrsResponse,
            // which does not support readEntity, so we override this method to
            // read the entities stored by addReadEntity.
            Map<?, ?> entities = (Map<?, ?>)response.getHeaders().getFirst(READ_ENTITY_HEADER);
            return entityType.cast(entities.get(entityType));
        }
    }

    private static Response createResponse(JsonObject entity) {
        return createResponse(Response.Status.OK, entity);
    }

    private static Response createResponse(Response.Status status, JsonObject entity) {
        return createResponse(status, Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE), JsonObject.class);
    }

    private static <T> Response createResponse(Response.Status status, Entity<T> entity, Class<T> klass) {
        Response.ResponseBuilder builder = newResponseBuilder().status(status).type(entity.getMediaType());
        return TestJaxrsComputeCloudClient.addReadEntity(builder, klass, entity.getEntity()).build();
    }

    private static Response createResultResponse(JsonValue result) {
        return createResponse(createObjectBuilder().add("result", result).build());
    }

    private Invocation.Builder allowingRequest(final Client client, final String endpoint) {
        final WebTarget target = mockery.mock(WebTarget.class);
        final Invocation.Builder builder = mockery.mock(Invocation.Builder.class);
        mockery.checking(new ClientExpectations() {{
            allowing(client).target(withUriBuilder(endpoint)); will(returnValue(target));
            oneOf(target).request(); will(returnValue(builder));
        }});
        return builder;
    }

    @Test
    public void testAuthenticateUnauthorizedInvalidMessage() throws Exception {
        final Client client = mockClient();
        final Invocation invocation = allowingAuthenticateBuildPost(client, ENDPOINT, USER.getString(), PASSWORD);
        mockery.checking(new ClientExpectations() {{
            allowing(invocation).invoke(); will(returnValue(createResponse(Response.Status.UNAUTHORIZED, createObjectBuilder().build())));
        }});

        try (JaxrsComputeCloudClient c = new TestJaxrsComputeCloudClient(ENDPOINT, USER, PASSWORD, client)) {
            c.authenticate();
            Assert.fail();
        } catch (ComputeCloudClientException e) {
            Assert.assertEquals(e.getMessage(), "HTTP 401 Unauthorized");
        }
    }

    @Test
    public void testAuthenticateUnauthorizedMessage() throws Exception {
        final Client client = mockClient();
        final Invocation invocation = allowingAuthenticateBuildPost(client, ENDPOINT, USER.getString(), PASSWORD);
        mockery.checking(new ClientExpectations() {{
            allowing(invocation).invoke(); will(returnValue(createResponse(Response.Status.UNAUTHORIZED, createObjectBuilder().add("message", "error").build())));
        }});

        try (JaxrsComputeCloudClient c = new TestJaxrsComputeCloudClient(ENDPOINT, USER, PASSWORD, client)) {
            c.authenticate();
            Assert.fail();
        } catch (ComputeCloudClientException e) {
            Assert.assertEquals(e.getMessage(), "HTTP 401 Unauthorized: error");
        }
    }

    private Invocation.Builder allowingAuthenticatedRequest(final Client client, final String endpoint) {
        final Invocation.Builder builder = allowingRequest(client, endpoint);
        mockery.checking(new ClientExpectations() {{
            oneOf(builder).cookie(TestJaxrsComputeCloudClient.AUTHENTICATE_COOKIE_NAME, AUTHENTICATE_COOKIE_VALUE); will(returnValue(builder));
        }});
        return builder;
    }

    private void allowingGetShapes(final Client client, final URI endpoint, final JsonArray shapes) {
        final Invocation.Builder builder = allowingAuthenticatedRequest(client, endpoint + "/shape/");
        final Invocation invocation = mockery.mock(Invocation.class);
        mockery.checking(new ClientExpectations() {{
            oneOf(builder).accept(JaxrsComputeCloudClient.ORACLE_COMPUTE_V3_MEDIA_TYPE); will(returnValue(builder));
            oneOf(builder).buildGet(); will(returnValue(invocation));
            oneOf(invocation).invoke(); will(returnValue(createResultResponse(shapes)));
        }});
    }

    @Test(expected = IllegalStateException.class)
    public void testGetShapesUnauthenticated() throws Exception {
        final Client client = mockClient();
        allowingRequest(client, ENDPOINT + "/shape/");

        try (JaxrsComputeCloudClient c = new JaxrsComputeCloudClient(ENDPOINT, USER, PASSWORD, client)) {
            c.getShapes();
        }
    }

    @Test
    public void testGetShapes0() throws Exception {
        final Client client = mockClient();
        allowingAuthenticate(client, ENDPOINT, USER.getString(), PASSWORD);
        allowingGetShapes(client, ENDPOINT, createArrayBuilder().build());

        try (JaxrsComputeCloudClient c = new TestJaxrsComputeCloudClient(ENDPOINT, USER, PASSWORD, client)) {
            c.authenticate();
            Assert.assertEquals(Collections.emptyList(), c.getShapes());
        }
    }

    @Test
    public void testGetShapes() throws Exception {
        final Client client = mockClient();
        allowingAuthenticate(client, ENDPOINT, USER.getString(), PASSWORD);
        allowingGetShapes(client, ENDPOINT, createArrayBuilder()
                .add(createObjectBuilder())
                .add(createObjectBuilder()
                        .add("cpus", 1)
                        .add("gpus", 2)
                        .add("io", 3)
                        .add("is_root_ssd", true)
                        .add("name", "nameValue")
                        .add("nds_iops_limit", 4)
                        .add("placement_requirements", createArrayBuilder().add("pr0").add("pr1"))
                        .add("ram", 5)
                        .add("root_disk_size", 6)
                        .add("ssd_data_size", 7)
                        .add("uri", "uriValue")
                        .build())
                .build());

        try (JaxrsComputeCloudClient c = new TestJaxrsComputeCloudClient(ENDPOINT, USER, PASSWORD, client)) {
            c.authenticate();
            Assert.assertEquals(Arrays.asList(
                            new Shape(),
                            new Shape()
                                    .cpus(BigDecimal.valueOf(1))
                                    .gpus(2L)
                                    .io(3L)
                                    .isRootSsd(true)
                                    .name("nameValue")
                                    .ndsIopsLimit(4L)
                                    .placementRequirements(Arrays.asList("pr0", "pr1"))
                                    .ram(5L)
                                    .rootDiskSize(6L)
                                    .ssdDataSize(7L)
                                    .uri("uriValue")
                    ),
                    c.getShapes());
        }
    }

    private void allowingGetSecurityLists(final Client client, final URI endpoint, ComputeCloudUser user, final JsonArray securityLists) {
        final Invocation.Builder builder = allowingAuthenticatedRequest(client, endpoint + "/seclist" + user.getString() + '/');
        final Invocation invocation = mockery.mock(Invocation.class);
        mockery.checking(new ClientExpectations() {{
            oneOf(builder).accept(JaxrsComputeCloudClient.ORACLE_COMPUTE_V3_MEDIA_TYPE); will(returnValue(builder));
            oneOf(builder).buildGet(); will(returnValue(invocation));
            oneOf(invocation).invoke(); will(returnValue(createResultResponse(securityLists)));
        }});
    }

    @Test(expected = IllegalStateException.class)
    public void testGetSecurityListsUnauthenticated() throws Exception {
        final Client client = mockClient();
        allowingRequest(client, ENDPOINT + "/seclist" + USER.getString() + '/');

        try (JaxrsComputeCloudClient c = new JaxrsComputeCloudClient(ENDPOINT, USER, PASSWORD, client)) {
            c.getSecurityLists();
        }
    }

    @Test
    public void testGetSecurityLists0() throws Exception {
        final Client client = mockClient();
        allowingAuthenticate(client, ENDPOINT, USER.getString(), PASSWORD);
        allowingGetSecurityLists(client, ENDPOINT, USER, createArrayBuilder().build());

        try (JaxrsComputeCloudClient c = new TestJaxrsComputeCloudClient(ENDPOINT, USER, PASSWORD, client)) {
            c.authenticate();
            Assert.assertEquals(Collections.emptyList(), c.getSecurityLists());
        }
    }

    @Test
    public void testGetSecurityLists() throws Exception {
        final Client client = mockClient();
        allowingAuthenticate(client, ENDPOINT, USER.getString(), PASSWORD);
        allowingGetSecurityLists(client, ENDPOINT, USER, createArrayBuilder()
                .add(createObjectBuilder())
                .add(createObjectBuilder()
                        .add("account", "/Compute-a4343010/default")
                        .add("description", "description")
                        .add("outbound_cidr_policy", "PERMIT")
                        .add("policy", "DENY")
                        .add("group_id", "10930")
                        .add("id", "97e95649-18e3-45c4-a840-690397b2cac2")
                        .add("name", "/Compute-a4343010/wang.danian@oracle.com/danian_sec_list")
                        .build())
                .build());

        try (JaxrsComputeCloudClient c = new TestJaxrsComputeCloudClient(ENDPOINT, USER, PASSWORD, client)) {
            c.authenticate();
            Assert.assertEquals(Arrays.asList(
                            new SecurityList(),
                            new SecurityList()
                                    .account("/Compute-a4343010/default")
                                    .description("description")
                                    .outboundCidrPolicy("PERMIT")
                                    .policy("DENY")
                                    .groupId("10930")
                                    .id("97e95649-18e3-45c4-a840-690397b2cac2")
                                    .name("/Compute-a4343010/wang.danian@oracle.com/danian_sec_list")
                    ),
                    c.getSecurityLists());
        }
    }

    private void allowingGetSSHKeys(final Client client, final URI endpoint, ComputeCloudUser user, final JsonArray sshKeys) {
        final Invocation.Builder builder = allowingAuthenticatedRequest(client, endpoint + "/sshkey" + user.getString() + '/');
        final Invocation invocation = mockery.mock(Invocation.class);
        mockery.checking(new ClientExpectations() {{
            oneOf(builder).accept(JaxrsComputeCloudClient.ORACLE_COMPUTE_V3_MEDIA_TYPE); will(returnValue(builder));
            oneOf(builder).buildGet(); will(returnValue(invocation));
            oneOf(invocation).invoke(); will(returnValue(createResultResponse(sshKeys)));
        }});
    }

    @Test(expected = IllegalStateException.class)
    public void testGetSSHKeysUnauthenticated() throws Exception {
        final Client client = mockClient();
        allowingRequest(client, ENDPOINT + "/sshkey" + USER.getString() + '/');

        try (JaxrsComputeCloudClient c = new JaxrsComputeCloudClient(ENDPOINT, USER, PASSWORD, client)) {
            c.getSSHKeys();
        }
    }

    @Test
    public void testGetSSHKeys0() throws Exception {
        final Client client = mockClient();
        allowingAuthenticate(client, ENDPOINT, USER.getString(), PASSWORD);
        allowingGetSSHKeys(client, ENDPOINT, USER, createArrayBuilder().build());

        try (JaxrsComputeCloudClient c = new TestJaxrsComputeCloudClient(ENDPOINT, USER, PASSWORD, client)) {
            c.authenticate();
            Assert.assertEquals(Collections.emptyList(), c.getSSHKeys());
        }
    }

    @Test
    public void testGetSSHKeys() throws Exception {
        final Client client = mockClient();
        allowingAuthenticate(client, ENDPOINT, USER.getString(), PASSWORD);
        allowingGetSSHKeys(client, ENDPOINT, USER, createArrayBuilder()
                .add(createObjectBuilder())
                .add(createObjectBuilder()
                        .add("enabled", true)
                        .add("uri", "https://ucf5.external.dc1.c9qa132.oraclecorp.com/sshkey/Compute-a4343010/wang.danian%40oracle.com/danian_ssh_key1")
                        .add("key", "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDDe7nKZalsdjflakjdsflkj")
                        .add("name", "/Compute-a4343010/wang.danian@oracle.com/danian_sec_list")
                        .build())
                .build());

        try (JaxrsComputeCloudClient c = new TestJaxrsComputeCloudClient(ENDPOINT, USER, PASSWORD, client)) {
            c.authenticate();
            Assert.assertEquals(Arrays.asList(
                            new SSHKey(),
                            new SSHKey()
                                    .enabled(true)
                                    .uri("https://ucf5.external.dc1.c9qa132.oraclecorp.com/sshkey/Compute-a4343010/wang.danian%40oracle.com/danian_ssh_key1")
                                    .key("ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDDe7nKZalsdjflakjdsflkj")
                                    .name("/Compute-a4343010/wang.danian@oracle.com/danian_sec_list")
                    ),
                    c.getSSHKeys());
        }
    }

    private void allowingGetSSHKey(final Client client, final URI endpoint, ComputeCloudUser user, String name, final JsonObject sshKey) {
        final Invocation.Builder builder = allowingAuthenticatedRequest(client, endpoint + "/sshkey" + user.getString() + '/' + name);
        final Invocation invocation = mockery.mock(Invocation.class);
        mockery.checking(new ClientExpectations() {{
            oneOf(builder).accept(JaxrsComputeCloudClient.ORACLE_COMPUTE_V3_MEDIA_TYPE); will(returnValue(builder));
            oneOf(builder).buildGet(); will(returnValue(invocation));
            oneOf(invocation).invoke(); will(returnValue(createResponse(sshKey)));
        }});
    }

    @Test(expected = IllegalStateException.class)
    public void testGetSSHKeyUnauthenticated() throws Exception {
        final Client client = mockClient();
        allowingRequest(client, ENDPOINT + "/sshkey" + USER.getString() + "/testsshkey");

        try (JaxrsComputeCloudClient c = new JaxrsComputeCloudClient(ENDPOINT, USER, PASSWORD, client)) {
            c.getSSHKey("testsshkey");
        }
    }

    @Test
    public void testGetSSHKeyNotFound() throws Exception {
        final Client client = mockClient();
        allowingAuthenticate(client, ENDPOINT, USER.getString(), PASSWORD);
        allowingGetSSHKey(client, ENDPOINT, USER, "testsshkey", createObjectBuilder()
                .add("enabled", true)
                .add("uri", "u")
                .add("key", "k")
                .add("name", "n")
                .build());

        try (JaxrsComputeCloudClient c = new TestJaxrsComputeCloudClient(ENDPOINT, USER, PASSWORD, client)) {
            c.authenticate();
            Assert.assertEquals(new SSHKey().enabled(true).uri("u").key("k").name("n"), c.getSSHKey("testsshkey"));
        }
    }

    private void allowingGetImageLists(final Client client, final URI endpoint, ComputeCloudUser user, final JsonArray securityLists) {
        final Invocation.Builder builder = allowingAuthenticatedRequest(client, endpoint + "/imagelist/oracle/public/");
        final Invocation invocation = mockery.mock(Invocation.class);
        mockery.checking(new ClientExpectations() {{
            oneOf(builder).accept(JaxrsComputeCloudClient.ORACLE_COMPUTE_V3_MEDIA_TYPE); will(returnValue(builder));
            oneOf(builder).buildGet(); will(returnValue(invocation));
            oneOf(invocation).invoke(); will(returnValue(createResultResponse(securityLists)));
        }});
    }

    @Test(expected = IllegalStateException.class)
    public void testGetImageListsUnauthenticated() throws Exception {
        final Client client = mockClient();
        allowingRequest(client, ENDPOINT + "/imagelist/oracle/public/");

        try (JaxrsComputeCloudClient c = new JaxrsComputeCloudClient(ENDPOINT, USER, PASSWORD, client)) {
            c.getImageLists(ImageListSourceType.ORACLE_PUBLIC_IMAGE);
        }
    }

    @Test
    public void testGetImageLists0() throws Exception {
        final Client client = mockClient();
        allowingAuthenticate(client, ENDPOINT, USER.getString(), PASSWORD);
        allowingGetImageLists(client, ENDPOINT, USER, createArrayBuilder().build());

        try (JaxrsComputeCloudClient c = new TestJaxrsComputeCloudClient(ENDPOINT, USER, PASSWORD, client)) {
            c.authenticate();
            Assert.assertEquals(Collections.emptyList(), c.getImageLists(ImageListSourceType.ORACLE_PUBLIC_IMAGE));
        }
    }

    @Test
    public void testGetImageLists() throws Exception {
        final Client client = mockClient();
        allowingAuthenticate(client, ENDPOINT, USER.getString(), PASSWORD);
        allowingGetImageLists(client, ENDPOINT, USER, createArrayBuilder()
                .add(createObjectBuilder().add("name", "iln0"))
                .add(createObjectBuilder().add("name", "iln1"))
                .build());

        try (JaxrsComputeCloudClient c = new TestJaxrsComputeCloudClient(ENDPOINT, USER, PASSWORD, client)) {
            c.authenticate();
            Assert.assertEquals(Arrays.asList(new ImageList().name("iln0"), new ImageList().name("iln1")), c.getImageLists(ImageListSourceType.ORACLE_PUBLIC_IMAGE));
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateInstanceOrchestrationUnauthenticated() throws Exception {
        final Client client = mockClient();
        allowingRequest(client, ENDPOINT + "/orchestration/");

        try (JaxrsComputeCloudClient c = new JaxrsComputeCloudClient(ENDPOINT, USER, PASSWORD, client)) {
            c.createInstanceOrchestration("name", new TestComputeCloudAgentTemplate.Builder().build());
        }
    }

    private void allowingCreateInstanceOrchestration(final Client client, final URI endpoint, final Matcher<Entity<JsonObject>> entityMatcher) {
        final Invocation.Builder builder = allowingAuthenticatedRequest(client, ENDPOINT + "/orchestration/");
        final Invocation invocation = mockery.mock(Invocation.class);
        mockery.checking(new ClientExpectations() {{
            oneOf(builder).accept(JaxrsComputeCloudClient.ORACLE_COMPUTE_V3_MEDIA_TYPE); will(returnValue(builder));
            oneOf(builder).buildPost(with(entityMatcher)); will(returnValue(invocation));
            oneOf(invocation).invoke(); will(returnValue(createResponse(createObjectBuilder().build())));
        }});
    }

    private static class CreateInstanceOrchestrationEntityMatcher extends BaseMatcher<Entity<JsonObject>> {
        private final ComputeCloudObjectName name;

        CreateInstanceOrchestrationEntityMatcher(ComputeCloudObjectName name) {
            this.name = name;
        }

        @Override
        public boolean matches(Object o) {
            @SuppressWarnings("unchecked")
            Entity<JsonObject> entity = (Entity<JsonObject>)o;
            JsonObject orch = entity.getEntity();
            return name.equals(ComputeCloudObjectName.parse(orch.getString("name"))) && matchesEntity(orch);
        }

        static JsonObject getSingleObject(JsonArray objects) {
            Assert.assertEquals(1, objects.size());
            return objects.getJsonObject(0);
        }

        static JsonObject getOplanObjectJson(JsonObject orchJson, String label) {
            for (JsonObject oplanJson : orchJson.getJsonArray("oplans").getValuesAs(JsonObject.class)) {
                if (oplanJson.getString("label").equals(label)) {
                    return getSingleObject(oplanJson.getJsonArray("objects"));
                }
            }
            throw new AssertionError();
        }

        static JsonObject getVolumeJson(JsonObject orchJson) {
            return getOplanObjectJson(orchJson, JaxrsComputeCloudClient.STORAGE_VOLUME_LABEL);
        }

        static JsonObject getInstanceJson(JsonObject orchJson) {
            return getSingleObject(getOplanObjectJson(orchJson, JaxrsComputeCloudClient.LAUNCHPLAN_LABEL).getJsonArray("instances"));
        }

        static JsonArray getInstanceSecurityListNamesJson(JsonObject instanceJson) {
            return instanceJson.getJsonObject("networking").getJsonObject("eth0").getJsonArray("seclists");
        }

        protected boolean matchesEntity(JsonObject orch) {
            return true;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("expected orchestration JSON");
        }
    }

    @Test
    public void testCreateInstanceOrchestrationDefault() throws Exception {
        final Client client = mockClient();
        allowingAuthenticate(client, ENDPOINT, USER.getString(), PASSWORD);

        final String name = "n";
        allowingCreateInstanceOrchestration(client, ENDPOINT, new CreateInstanceOrchestrationEntityMatcher(ComputeCloudObjectName.valueOf(USER, name)) {
            @Override
            protected boolean matchesEntity(JsonObject orch) {
                JsonObject volumeJson = getVolumeJson(orch);
                JsonObject instanceJson = getInstanceJson(orch);
                return orch.getString("description").equals("Jenkins agent") &&
                        volumeJson.getString("imagelist").isEmpty() &&
                        volumeJson.getString("size").isEmpty() &&
                        instanceJson.getString("shape").isEmpty() &&
                        instanceJson.getJsonArray("sshkeys").getString(0).isEmpty() &&
                        getInstanceSecurityListNamesJson(instanceJson).isEmpty();
            }
        });

        try (JaxrsComputeCloudClient c = new TestJaxrsComputeCloudClient(ENDPOINT, USER, PASSWORD, client)) {
            c.authenticate();
            c.createInstanceOrchestration(name, new TestComputeCloudAgentTemplate.Builder().build());
        }
    }

    @Test
    public void testCreateInstanceOrchestration() throws Exception {
        final Client client = mockClient();
        allowingAuthenticate(client, ENDPOINT, USER.getString(), PASSWORD);

        final String name = "n";
        allowingCreateInstanceOrchestration(client, ENDPOINT, new CreateInstanceOrchestrationEntityMatcher(ComputeCloudObjectName.valueOf(USER, name)) {
            @Override
            protected boolean matchesEntity(JsonObject orch) {
                JsonObject volumeJson = getVolumeJson(orch);
                JsonObject instanceJson = getInstanceJson(orch);
                return orch.getString("description").equals("od") &&
                        volumeJson.getString("imagelist").equals("iln") &&
                        volumeJson.getString("size").equals("vs") &&
                        instanceJson.getString("shape").equals("sn") &&
                        instanceJson.getJsonArray("sshkeys").equals(createArrayBuilder().add("ssh_key").build()) &&
                        getInstanceSecurityListNamesJson(instanceJson).equals(createArrayBuilder().add("sln0").build());
            }
        });

        try (JaxrsComputeCloudClient c = new TestJaxrsComputeCloudClient(ENDPOINT, USER, PASSWORD, client)) {
            c.authenticate();
            c.createInstanceOrchestration(name, new TestComputeCloudAgentTemplate.Builder()
                    .orchDescription("od")
                    .shapeName("sn")
                    .securityListNames("sln0")
                    .imageListName("iln")
                    .volumeSize("vs")
                    .sshKeyName("ssh_key")
                    .build());
        }
    }

    private static String getOrchestrationEndpoint(URI endpoint, ComputeCloudUser user, String name) {
        return endpoint + "/orchestration" + ComputeCloudObjectName.valueOf(user, name).getString();
    }

    @Test(expected = IllegalStateException.class)
    public void testGetInstanceOrchestrationUnauthenticated() throws Exception {
        final Client client = mockClient();
        allowingRequest(client, getOrchestrationEndpoint(ENDPOINT, USER, "n"));

        try (JaxrsComputeCloudClient c = new JaxrsComputeCloudClient(ENDPOINT, USER, PASSWORD, client)) {
            c.getInstanceOrchestration("n");
        }
    }

    private void allowingGetInstanceOrchestration(final Client client, final String endpoint, final JsonObject entity) {
        final Invocation.Builder builder = allowingAuthenticatedRequest(client, endpoint);
        final Invocation invocation = mockery.mock(Invocation.class);
        mockery.checking(new ClientExpectations() {{
            oneOf(builder).accept(JaxrsComputeCloudClient.ORACLE_COMPUTE_V3_MEDIA_TYPE); will(returnValue(builder));
            oneOf(builder).buildGet(); will(returnValue(invocation));
            oneOf(invocation).invoke(); will(returnValue(createResponse(entity)));
        }});
    }

    @Test(expected = ComputeCloudClientException.class)
    public void testGetInstanceOrchestrationMissingIpreservationOplan() throws Exception {
        final Client client = mockClient();
        allowingAuthenticate(client, ENDPOINT, USER.getString(), PASSWORD);
        allowingGetInstanceOrchestration(client, getOrchestrationEndpoint(ENDPOINT, USER, "n"), createObjectBuilder()
                .add("oplans", createArrayBuilder()
                        .add(createObjectBuilder().add("label", "doesnotmatch"))
                        .build())
                .build());

        try (JaxrsComputeCloudClient c = new TestJaxrsComputeCloudClient(ENDPOINT, USER, PASSWORD, client)) {
            c.authenticate();
            c.getInstanceOrchestration("n");
        }
    }

    @Test(expected = ComputeCloudClientException.class)
    public void testGetInstanceOrchestrationEmptyIpReservationObjects() throws Exception {
        final Client client = mockClient();
        allowingAuthenticate(client, ENDPOINT, USER.getString(), PASSWORD);
        allowingGetInstanceOrchestration(client, getOrchestrationEndpoint(ENDPOINT, USER, "n"), createObjectBuilder()
                .add("oplans", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("label", JaxrsComputeCloudClient.IP_RESERVATION_LABEL)
                                .add("objects", createArrayBuilder())
                                .build())
                        .build())
                .build());

        try (JaxrsComputeCloudClient c = new TestJaxrsComputeCloudClient(ENDPOINT, USER, PASSWORD, client)) {
            c.authenticate();
            c.getInstanceOrchestration("n");
        }
    }

    @Test
    public void testGetInstanceOrchestrationStopped() throws Exception {
        final Client client = mockClient();
        allowingAuthenticate(client, ENDPOINT, USER.getString(), PASSWORD);
        allowingGetInstanceOrchestration(client, getOrchestrationEndpoint(ENDPOINT, USER, "n"), createObjectBuilder()
                .add("status", InstanceOrchestration.Status.stopped.name())
                .add("oplans", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("label", JaxrsComputeCloudClient.IP_RESERVATION_LABEL)
                                .add("objects", createArrayBuilder()
                                        .add(createObjectBuilder())
                                        .build())
                                .build())
                        .build())
                .build());

        try (JaxrsComputeCloudClient c = new TestJaxrsComputeCloudClient(ENDPOINT, USER, PASSWORD, client)) {
            c.authenticate();
            Assert.assertEquals(
                    new InstanceOrchestration().status(InstanceOrchestration.Status.stopped),
                    c.getInstanceOrchestration("n"));
        }
    }

    @Test
    public void testGetInstanceOrchestrationReady() throws Exception {
        final Client client = mockClient();
        allowingAuthenticate(client, ENDPOINT, USER.getString(), PASSWORD);
        allowingGetInstanceOrchestration(client, getOrchestrationEndpoint(ENDPOINT, USER, "n"), createObjectBuilder()
                .add("status", InstanceOrchestration.Status.ready.name())
                .add("oplans", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("label", JaxrsComputeCloudClient.IP_RESERVATION_LABEL)
                                .add("objects", createArrayBuilder()
                                        .add(createObjectBuilder()
                                                .add("ip", "1.1.1.1"))
                                        .build())
                                .build())
                        .build())
                .build());

        try (JaxrsComputeCloudClient c = new TestJaxrsComputeCloudClient(ENDPOINT, USER, PASSWORD, client)) {
            c.authenticate();
            Assert.assertEquals(
                    new InstanceOrchestration()
                            .status(InstanceOrchestration.Status.ready)
                            .ip("1.1.1.1"),
                    c.getInstanceOrchestration("n"));
        }
    }

    private static String getOrchestrationActionEndpoint(URI endpoint, ComputeCloudUser user, String name, String action) {
        return getOrchestrationEndpoint(endpoint, user, name) + "?action=" + action;
    }

    private static String getStartOrchestrationEndpoint(URI endpoint, ComputeCloudUser user, String name) {
        return getOrchestrationActionEndpoint(endpoint, user, name, "START");
    }

    private static String getStopOrchestrationEndpoint(URI endpoint, ComputeCloudUser user, String name) {
        return getOrchestrationActionEndpoint(endpoint, user, name, "STOP");
    }

    @Test(expected = IllegalStateException.class)
    public void testStartOrchestrationUnauthenticated() throws Exception {
        final Client client = mockClient();
        allowingRequest(client, getStartOrchestrationEndpoint(ENDPOINT, USER, "n"));

        try (JaxrsComputeCloudClient c = new JaxrsComputeCloudClient(ENDPOINT, USER, PASSWORD, client)) {
            c.startOrchestration("n");
        }
    }

    private void allowingOrchestrationAction(Client client, String endpoint) {
        final Builder builder = allowingAuthenticatedRequest(client, endpoint);
        final Invocation invocation = mockery.mock(Invocation.class);
        mockery.checking(new ClientExpectations() {{
            oneOf(builder).accept(JaxrsComputeCloudClient.ORACLE_COMPUTE_V3_MEDIA_TYPE); will(returnValue(builder));
            oneOf(builder).buildPut(entity(createObjectBuilder().build())); will(returnValue(invocation));
            oneOf(invocation).invoke(); will(returnValue(createResponse(createObjectBuilder().build())));
        }});
    }

    @Test
    public void testStartOrchestration() throws Exception {
        final Client client = mockClient();
        allowingAuthenticate(client, ENDPOINT, USER.getString(), PASSWORD);
        allowingOrchestrationAction(client, getStartOrchestrationEndpoint(ENDPOINT, USER, "n"));

        try (JaxrsComputeCloudClient c = new TestJaxrsComputeCloudClient(ENDPOINT, USER, PASSWORD, client)) {
            c.authenticate();
            c.startOrchestration("n");
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testStopOrchestrationUnauthenticated() throws Exception {
        final Client client = mockClient();
        allowingRequest(client, getStopOrchestrationEndpoint(ENDPOINT, USER, "n"));

        try (JaxrsComputeCloudClient c = new JaxrsComputeCloudClient(ENDPOINT, USER, PASSWORD, client)) {
            c.stopOrchestration("n");
        }
    }

    @Test
    public void testStopOrchestration() throws Exception {
        final Client client = mockClient();
        allowingAuthenticate(client, ENDPOINT, USER.getString(), PASSWORD);
        allowingOrchestrationAction(client, getStopOrchestrationEndpoint(ENDPOINT, USER, "n"));

        try (JaxrsComputeCloudClient c = new TestJaxrsComputeCloudClient(ENDPOINT, USER, PASSWORD, client)) {
            c.authenticate();
            c.stopOrchestration("n");
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testDeleteOrchestrationUnauthenticated() throws Exception {
        final Client client = mockClient();
        allowingRequest(client, getOrchestrationEndpoint(ENDPOINT, USER, "n"));

        try (JaxrsComputeCloudClient c = new JaxrsComputeCloudClient(ENDPOINT, USER, PASSWORD, client)) {
            c.deleteOrchestration("n");
        }
    }

    private void allowingDeleteOrchestration(final Client client, final String endpoint) {
        final Invocation.Builder builder = allowingAuthenticatedRequest(client, endpoint);
        final Invocation invocation = mockery.mock(Invocation.class);
        mockery.checking(new ClientExpectations() {{
            oneOf(builder).buildDelete(); will(returnValue(invocation));
            oneOf(invocation).invoke(); will(returnValue(newResponseBuilder().status(Response.Status.NO_CONTENT).build()));
        }});
    }

    @Test
    public void testDeleteOrchestration() throws Exception {
        final Client client = mockClient();
        allowingAuthenticate(client, ENDPOINT, USER.getString(), PASSWORD);
        allowingDeleteOrchestration(client, getOrchestrationEndpoint(ENDPOINT, USER, "n"));

        try (JaxrsComputeCloudClient c = new TestJaxrsComputeCloudClient(ENDPOINT, USER, PASSWORD, client)) {
            c.authenticate();
            c.deleteOrchestration("n");
        }
    }
}
