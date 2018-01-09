package com.oracle.cloud.compute.jenkins.client;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonException;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.spi.JsonProvider;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.StatusType;
import javax.ws.rs.core.UriBuilder;

import com.oracle.cloud.compute.jenkins.model.ImageList;
import com.oracle.cloud.compute.jenkins.model.ImageListEntry;
import com.oracle.cloud.compute.jenkins.model.ImageListSourceType;
import com.oracle.cloud.compute.jenkins.model.InstanceOrchestration;
import com.oracle.cloud.compute.jenkins.model.InstanceOrchestration.Status;
import com.oracle.cloud.compute.jenkins.model.SSHKey;
import com.oracle.cloud.compute.jenkins.model.SecurityList;
import com.oracle.cloud.compute.jenkins.model.Shape;

/**
 * An implementation of ComputeCloudClient using the JAX-RS client API.
 */
public class JaxrsComputeCloudClient implements ComputeCloudClient {
    private static final Logger LOGGER = Logger.getLogger(JaxrsComputeCloudClient.class.getName());
    static final MediaType ORACLE_COMPUTE_V3_MEDIA_TYPE = new MediaType("application", "oracle-compute-v3+json");
    private static final JsonProvider JSON_PROVIDER = JsonProvider.provider();
    static final String AUTHENTICATE_COOKIE_NAME = "nimbula";

    private final URI apiEndpoint;
    private final ComputeCloudUser user;
    private final String password;
    private final Client client;

    private volatile Collection<NewCookie> authenticationCookies;

    public JaxrsComputeCloudClient(URI apiEndpoint, ComputeCloudUser user, String password, Client client) {
        this.apiEndpoint = apiEndpoint;
        this.user = user;
        this.password = password;
        this.client = client;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '[' + apiEndpoint + ", " + user + ", #" + password.hashCode() + ']';
    }

    @Override
    public void close() {
        client.close();
    }

    private UriBuilder newUriBuilder() {
        return UriBuilder.fromUri(apiEndpoint);
    }

    private UriBuilder newUriBuilder(String path) {
        return newUriBuilder().path(path);
    }

    static JsonObjectBuilder createObjectBuilder() {
        return JSON_PROVIDER.createObjectBuilder();
    }

    static JsonArrayBuilder createArrayBuilder() {
        return JSON_PROVIDER.createArrayBuilder();
    }

    static <T extends JsonStructure> Entity<T> entity(T json) {
        return Entity.entity(json, ORACLE_COMPUTE_V3_MEDIA_TYPE);
    }

    <T> T readEntity(Response response, Class<T> entityType) {
        return response.readEntity(entityType);
    }

    private Response invoke(Invocation inv) throws ComputeCloudClientException {
        Response response;
        try {
            response = inv.invoke();
        } catch (ProcessingException e) {
            String message = e.getMessage();
            for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
                // JsonException will be thrown if an underlying I/O error
                // occurs when JsonGenerator.flush is called.
                if (cause instanceof JsonException) {
                    Throwable jsonCause = cause.getCause();
                    if (jsonCause != null) {
                        message = jsonCause.getMessage();
                    }
                    break;
                }
            }

            throw new ComputeCloudClientException(message, e);
        }

        StatusType statusInfo = response.getStatusInfo();
        if (statusInfo.getFamily() != Response.Status.Family.SUCCESSFUL) {
            StringBuilder messageBuilder = new StringBuilder()
                    .append("HTTP ").append(statusInfo.getStatusCode())
                    .append(' ').append(statusInfo.getReasonPhrase());

            if (MediaType.APPLICATION_JSON_TYPE.equals(response.getMediaType())) {
                try {
                    JsonObject entity = readEntity(response, JsonObject.class);
                    String message = entity.getString("message");

                    messageBuilder.append(": ").append(message);
                } catch (Exception e) {
                    LOGGER.log(Level.FINE, "Error reading error message", e);
                }
            }

            String message = messageBuilder.toString();
            if (Response.Status.fromStatusCode(statusInfo.getStatusCode()) == Response.Status.UNAUTHORIZED) {
                throw new ComputeCloudClientUnauthorizedException(message);
            }
            throw new ComputeCloudClientException(message);
        }

        return response;
    }

    @Override
    public void authenticate() throws ComputeCloudClientException {
        Invocation inv = client.target(newUriBuilder("authenticate/")).request()
                .buildPost(entity(createObjectBuilder()
                        .add("user", user.getString())
                        .add("password", password)
                        .build()));
        authenticationCookies = invoke(inv).getCookies().values();
    }

    private Invocation.Builder addAuthenticationCookies(Invocation.Builder builder) {
        if (authenticationCookies == null) {
            throw new IllegalStateException("Not authenticated");
        }

        for (Cookie cookie : authenticationCookies) {
            if (cookie.getName() != null && cookie.getName().equalsIgnoreCase(AUTHENTICATE_COOKIE_NAME))
                builder.cookie(cookie.getName(), cookie.getValue());
        }

        return builder;
    }

    private Invocation.Builder request(UriBuilder uriBuilder) {
        return addAuthenticationCookies(client.target(uriBuilder).request());
    }

    private Invocation.Builder request(String path) {
        return request(newUriBuilder(path));
    }

    private JsonObject invokeAndReadJsonObject(Invocation inv) throws ComputeCloudClientException {
        return readEntity(invoke(inv), JsonObject.class);
    }

    private JsonArray invokeAndGetResultArray(Invocation inv) throws ComputeCloudClientException {
        return invokeAndReadJsonObject(inv).getJsonArray("result");
    }

    private static Boolean getBoolean(JsonObject o, String key) {
        Boolean b = o.containsKey(key) ? o.getBoolean(key) : null;
        return b;
    }

    private static Long getLong(JsonObject o, String key) {
        JsonNumber n = o.getJsonNumber(key);
        return n == null ? null : n.longValueExact();
    }

    private static Integer getInteger(JsonObject o, String key) {
        JsonNumber n = o.getJsonNumber(key);
        return n == null ? null : n.intValueExact();
    }

    private static BigDecimal getBigDecimal(JsonObject o, String key) {
        JsonNumber n = o.getJsonNumber(key);
        return n == null ? null : n.bigDecimalValue();
    }

    private static String getString(JsonObject o, String key) {
        return o.getString(key, null);
    }

    private static List<String> toStringList(JsonArray arr) {
        List<String> result = new ArrayList<>(arr.size());
        for (JsonString stringJson : arr.getValuesAs(JsonString.class)) {
            result.add(stringJson.getString());
        }
        return result;
    }

    private static List<String> getStringList(JsonObject o, String key) {
        JsonArray arr = o.getJsonArray(key);
        return arr == null ? null : toStringList(arr);
    }

    @SuppressWarnings("serial")
    private static List<ImageListEntry> toImageListEntryList(JsonObject o, String key) throws ComputeCloudClientException {
        final JsonArray arr = o.getJsonArray(key);
        return new ArrayList<ImageListEntry>() {
            {
                for (JsonObject entryJson : arr.getValuesAs(JsonObject.class)) {
                    add(toImageListEntry(entryJson));
                }
            }
        };
    }

    @Override
    public Collection<Shape> getShapes() throws ComputeCloudClientException {
        Collection<Shape> result = new ArrayList<>();
        Invocation inv = request("shape/").accept(ORACLE_COMPUTE_V3_MEDIA_TYPE).buildGet();
        for (JsonObject shapeJson : invokeAndGetResultArray(inv).getValuesAs(JsonObject.class)) {
            result.add(toShape(shapeJson));
        }
        return result;
    }

    private Shape toShape(JsonObject shapeJson) throws ComputeCloudClientException {
        return new Shape()
                .cpus(getBigDecimal(shapeJson, "cpus"))
                .gpus(getLong(shapeJson, "gpus"))
                .io(getLong(shapeJson, "io"))
                .isRootSsd(getBoolean(shapeJson, "is_root_ssd"))
                .name(getString(shapeJson, "name"))
                .ndsIopsLimit(getLong(shapeJson, "nds_iops_limit"))
                .placementRequirements(getStringList(shapeJson, "placement_requirements"))
                .ram(getLong(shapeJson, "ram"))
                .rootDiskSize(getLong(shapeJson, "root_disk_size"))
                .ssdDataSize(getLong(shapeJson, "ssd_data_size"))
                .uri(getString(shapeJson, "uri"))
                ;
    }

    @Override
    public Collection<SecurityList> getSecurityLists() throws ComputeCloudClientException {
        Collection<SecurityList> result = new ArrayList<>();
        Invocation inv = request("seclist" + user.getString() + '/').accept(ORACLE_COMPUTE_V3_MEDIA_TYPE).buildGet();
        for (JsonObject securityJson : invokeAndGetResultArray(inv).getValuesAs(JsonObject.class)) {
            result.add(toSecurityList(securityJson));
        }
        return result;
    }

    private SecurityList toSecurityList(JsonObject securityJson) throws ComputeCloudClientException {
        return new SecurityList()
                .account(getString(securityJson, "account"))
                .description(getString(securityJson, "description"))
                .uri(getString(securityJson, "uri"))
                .outboundCidrPolicy(getString(securityJson, "outbound_cidr_policy"))
                .policy(getString(securityJson, "policy"))
                .groupId(getString(securityJson, "group_id"))
                .id(getString(securityJson, "id"))
                .name(getString(securityJson, "name"))
                ;
     }

    private SSHKey toSSHKey(JsonObject sshKeyJson) throws ComputeCloudClientException {
        return new SSHKey()
                .enabled(getBoolean(sshKeyJson, "enabled"))
                .uri(getString(sshKeyJson, "uri"))
                .key(getString(sshKeyJson, "key"))
                .name(getString(sshKeyJson, "name"));
    }

    @Override
    public Collection<SSHKey> getSSHKeys() throws ComputeCloudClientException {
        Collection<SSHKey> result = new ArrayList<>();
        Invocation inv = request("sshkey" + user.getString() + '/').accept(ORACLE_COMPUTE_V3_MEDIA_TYPE).buildGet();
        for (JsonObject sshkey : invokeAndGetResultArray(inv).getValuesAs(JsonObject.class)) {
            result.add(toSSHKey(sshkey));
        }
        return result;
    }

    @Override
    public SSHKey getSSHKey(String name) throws ComputeCloudClientException {
        Invocation inv = request("sshkey" + user.getString() + '/' + name).accept(ORACLE_COMPUTE_V3_MEDIA_TYPE).buildGet();
        return toSSHKey(invokeAndReadJsonObject(inv));
    }

    private static ImageListEntry toImageListEntry(JsonObject imageListEntryJson) throws ComputeCloudClientException {
        return new ImageListEntry()
                .attributes(imageListEntryJson.get("attributes"))
                .machineimages(getStringList(imageListEntryJson, "machineimages"))
                .uri(getString(imageListEntryJson, "uri"))
                .version(getInteger(imageListEntryJson, "version"));
    }

    private ImageList toImageList(JsonObject imageListJson) throws ComputeCloudClientException {
        return new ImageList()
                ._default(getInteger(imageListJson, "default"))
                .description(getString(imageListJson, "description"))
                //TODO: skip entries when generate imagelists, as only name is needed.
                //.entries(toImageListEntryList(imageListJson, "entries"))
                .name(getString(imageListJson, "name"))
                .uri(getString(imageListJson, "uri"));
    }

    @Override
    public Collection<ImageList> getImageLists(ImageListSourceType sourceType) throws ComputeCloudClientException {
        Collection<ImageList> result = new ArrayList<>();
        String uri = "imagelist";
        if (sourceType.equals(ImageListSourceType.ORACLE_PUBLIC_IMAGE))
            uri += "/oracle/public/";
        else
            uri += user.getString() + '/';

        Invocation inv = request(uri).accept(ORACLE_COMPUTE_V3_MEDIA_TYPE).buildGet();
        for (JsonObject imageListJson : invokeAndGetResultArray(inv).getValuesAs(JsonObject.class)) {
            result.add(toImageList(imageListJson));
        }
        return result;
    }

    @Override
    public Collection<ImageListEntry> getImageListEntries(String imageListName) throws ComputeCloudClientException {
        Collection<ImageListEntry> result = new ArrayList<>();
        String uri = "imagelist" + imageListName;

        Invocation inv = request(uri).accept(ORACLE_COMPUTE_V3_MEDIA_TYPE).buildGet();
        for (ImageListEntry imageListEntry : toImageListEntryList(invokeAndReadJsonObject(inv), "entries")) {
            result.add(imageListEntry);
        }
        return result;
    }

    private static JsonObject createDependsRelationshipJson(String from, String to) {
        return createObjectBuilder()
                .add("oplan", from)
                .add("to_oplan", to)
                .add("type", "depends")
                .build();
    }

    static final String IP_RESERVATION_LABEL = "ip-reservation";
    static final String STORAGE_VOLUME_LABEL = "storage-volume";
    static final String LAUNCHPLAN_LABEL = "launchplan";

    private String getAttributeNameString(ComputeCloudObjectName attributeName) {
        return attributeName.getString();
    }

    private JsonObject buildSingleInstanceJsonObject(ComputeCloudObjectName objectName, ComputeCloudInstanceOrchestrationConfig params) {

        ComputeCloudObjectName instanceName = objectName;
        String volumeNameString = getAttributeNameString(objectName);
        String ipReservationNameString = getAttributeNameString(objectName);

        String shapeName = params.getShapeName();
        String sshKeyName = params.getSshKeyName();

        JsonArrayBuilder securityListNamesJson = createArrayBuilder();
        for (String securityName : params.getSecurityListNames()) {
            securityListNamesJson.add(securityName);
        }

        JsonObjectBuilder instanceObjBuilder = createObjectBuilder()
                .add("shape", shapeName == null ? "" : shapeName)
                .add("name", instanceName.getString())
                .add("storage_attachments", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("index", 1)
                                .add("volume", volumeNameString)
                                .build())
                        .build())
                .add("networking", createObjectBuilder()
                        .add("eth0", createObjectBuilder()
                                .add("seclists", securityListNamesJson)
                                .add("nat", "ipreservation:" + ipReservationNameString)
                                .build())
                        .build())
                .add("boot_order", createArrayBuilder().add(1))
                .add("sshkeys", createArrayBuilder()
                        .add(sshKeyName == null ? "" : sshKeyName)
                        .build());


        if (params.isHypervisorPvEnabled()) {
            instanceObjBuilder.add("hypervisor", createObjectBuilder().add("mode", "pv"));
        }
        return instanceObjBuilder.build();
    }

    @Override
    public void createInstanceOrchestration(String name, ComputeCloudInstanceOrchestrationConfig params) throws ComputeCloudClientException {
        ComputeCloudObjectName objectName = ComputeCloudObjectName.valueOf(user, name);
        String volumeNameString = getAttributeNameString(objectName);
        String ipReservationNameString = getAttributeNameString(objectName);
        String description = params.getOrchDescriptionValue();
        String imageListName = params.getImageListName();
        String imageListEntry = params.getImageListEntry();
        String volumeSize = params.getVolumeSizeValue();

        JsonObject instanceJsonObject = buildSingleInstanceJsonObject(objectName, params);


        Invocation inv = request("orchestration/").accept(ORACLE_COMPUTE_V3_MEDIA_TYPE).buildPost(entity(createObjectBuilder()
                // Top-Level Attributes in Orchestrations
                // https://docs.oracle.com/cloud/latest/stcomputecs/STCSG/GUID-6D65F452-AC5C-4A3A-ABB4-0690602CC1C8.htm
                .add("name", objectName.getString())
                .add("description", description)
                .add("oplans", createArrayBuilder()
                        // Object Plan Attributes
                        // https://docs.oracle.com/cloud/latest/stcomputecs/STCSG/GUID-E86DD6AD-A54B-4A8B-A1DC-3AB99FB471D8.htm
                        .add(createObjectBuilder()
                                .add("label", IP_RESERVATION_LABEL)
                                .add("obj_type", "ip/reservation")
                                .add("objects", createArrayBuilder()
                                        // Orchestration Attributes for ip/reservation
                                        // https://docs.oracle.com/cloud/latest/stcomputecs/STCSG/GUID-F55F2BE3-5400-4105-91C4-01C0342EEAA1.htm
                                        .add(createObjectBuilder()
                                                .add("name", ipReservationNameString)
                                                .add("parentpool", "/oracle/public/ippool")
                                                .add("permanent", true)
                                                .build())
                                        .build())
                                .build())
                        .add(createObjectBuilder()
                                .add("label", STORAGE_VOLUME_LABEL)
                                .add("obj_type", "storage/volume")
                                .add("objects", createArrayBuilder()
                                        // Orchestration Attributes for storage/volume
                                        // https://docs.oracle.com/cloud/latest/stcomputecs/STCSG/GUID-3C22B1C6-8ED6-4C9A-88E7-45FE13A3BC37.htm
                                        .add(createObjectBuilder()
                                                .add("name", volumeNameString)
                                                .add("bootable", true)
                                                .add("imagelist", imageListName == null ? "" : imageListName)
                                                // TODO: configurable?
                                                .add("imagelist_entry", imageListEntry == null ? "" : imageListEntry)
                                                .add("size", volumeSize == null ? "" : volumeSize)
                                                .add("properties", createArrayBuilder().add("/oracle/public/storage/default"))
                                                .build())
                                        .build())
                                .build())
                        .add(createObjectBuilder()
                                .add("label", LAUNCHPLAN_LABEL)
                                .add("obj_type", "launchplan")
                                // TODO: Do we actually want the instance to be recreated automatically if it stops
                                // unexpectedly?  The Jenkins slave won't be restarted automatically anyway.
                                .add("ha_policy", "active")
                                .add("objects", createArrayBuilder()
                                        // Orchestration Attributes for launchplan
                                        // https://docs.oracle.com/cloud/latest/stcomputecs/STCSG/GUID-DEBE9723-82C2-4BC6-BE2C-8FAECAE1B943.htm
                                        .add(createObjectBuilder()
                                                .add("instances", createArrayBuilder()
                                                        // Orchestration Attributes for instances
                                                        // https://docs.oracle.com/cloud/latest/stcomputecs/STCSG/GUID-1E557077-D859-4F7E-ADA6-988B5CC1072A.htm
                                                        .add(instanceJsonObject)
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build())
                .add("relationships", createArrayBuilder()
                        .add(createDependsRelationshipJson(LAUNCHPLAN_LABEL, IP_RESERVATION_LABEL))
                        .add(createDependsRelationshipJson(LAUNCHPLAN_LABEL, STORAGE_VOLUME_LABEL))
                        .build())
                .build()));

        invokeAndReadJsonObject(inv);
    }

    private UriBuilder newOrchestrationUriBuilder(String name) {
        return newUriBuilder().path("orchestration" + ComputeCloudObjectName.valueOf(user, name).getString());
    }

    private JsonObject findOplanObjectJson(JsonArray oplansJson, String label) throws ComputeCloudClientException {
        for (JsonObject oplanJson : oplansJson.getValuesAs(JsonObject.class)) {
            if (oplanJson.getString("label").equals(label)) {
                JsonArray objects = oplanJson.getJsonArray("objects");
                if (objects.size() != 1) {
                    throw new ComputeCloudClientException("expected 1 object for the " + label + " oplan, found " + objects.size());
                }
                return objects.getJsonObject(0);
            }
        }

        throw new ComputeCloudClientException("oplan not found: " + label);
    }

    private List<String> findErrorObjectsJson(JsonArray oplansJson) throws ComputeCloudClientException {
        List<String> errors = new ArrayList<String>();
        for (JsonObject oplanJson : oplansJson.getValuesAs(JsonObject.class)) {
            if (oplanJson.getString("status") != null
                    && oplanJson.getString("status").equals(Status.error.toString())
                    && oplanJson.getJsonObject("info") != null) {
                String opanLabel = oplanJson.getString("label");
                JsonObject infoObject = oplanJson.getJsonObject("info");
                if (infoObject.getJsonObject("errors") == null) {
                    errors.add(opanLabel + " in error status with unknown error!");
                } else {
                    for (JsonValue errorVal : infoObject.getJsonObject("errors").values()) {
                        errors.add("<------ OPlan " + opanLabel + " in error status with message: " + errorVal.toString() + "------>");
                    }
                }
            }
        }

        return errors;
    }

    @Override
    public InstanceOrchestration getInstanceOrchestration(String name) throws ComputeCloudClientException {
        JsonObject orchJson = invokeAndReadJsonObject(request(newOrchestrationUriBuilder(name))
                .accept(ORACLE_COMPUTE_V3_MEDIA_TYPE)
                .buildGet());
        JsonArray oplansJson = orchJson.getJsonArray("oplans");
        JsonObject ipReservationJson = findOplanObjectJson(oplansJson, IP_RESERVATION_LABEL);

        InstanceOrchestration instanceOrch = new InstanceOrchestration()
                .status(Status.valueOf(orchJson.getString("status")))
                .ip(ipReservationJson.getString("ip", null));

        if (orchJson.getString("status") != null && orchJson.getString("status").equals(Status.error.toString())) {
            List<String> errors = findErrorObjectsJson(oplansJson);
            instanceOrch = instanceOrch.errors(errors);
        }

        return instanceOrch;
    }

    private void updateOrchestrationState(String name, String action) throws ComputeCloudClientException {
        UriBuilder uriBuilder = newOrchestrationUriBuilder(name)
                .queryParam("action", action);
        invokeAndReadJsonObject(request(uriBuilder)
                .accept(ORACLE_COMPUTE_V3_MEDIA_TYPE)
                .buildPut(entity(createObjectBuilder().build())));
    }

    @Override
    public void startOrchestration(String name) throws ComputeCloudClientException {
        updateOrchestrationState(name, "START");
    }

    @Override
    public void stopOrchestration(String name) throws ComputeCloudClientException {
        updateOrchestrationState(name, "STOP");
    }

    @Override
    public void deleteOrchestration(String name) throws ComputeCloudClientException {
        invoke(request(newOrchestrationUriBuilder(name))
                .buildDelete());
    }
}
