package com.oracle.cloud.compute.jenkins;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import com.oracle.cloud.compute.jenkins.client.ComputeCloudClient;
import com.oracle.cloud.compute.jenkins.client.ComputeCloudClientException;
import com.oracle.cloud.compute.jenkins.client.ComputeCloudClientUnauthorizedException;
import com.oracle.cloud.compute.jenkins.client.ComputeCloudUser;
import com.oracle.cloud.compute.jenkins.model.InstanceOrchestration;
import com.oracle.cloud.compute.jenkins.ssh.SshConnector;
import com.trilead.ssh2.Connection;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.model.Computer;
import hudson.model.ComputerSet;
import hudson.model.Descriptor;
import hudson.model.Failure;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.Descriptor.FormException;
import hudson.slaves.AbstractCloudImpl;
import hudson.slaves.Cloud;
import hudson.slaves.NodeProvisioner.PlannedNode;
import hudson.util.FormValidation;
import hudson.util.HttpResponses;
import hudson.util.Secret;
import jenkins.model.Jenkins;

public class ComputeCloud extends AbstractCloudImpl {

    private static final Logger LOGGER = Logger.getLogger(ComputeCloud.class.getName());

    /**
     * The prefix to add to names provided by the user in the UI to ensure that
     * names of clouds in different plugins do not conflict.  We use the term
     * "name" to mean the full name with a prefix (to match Cloud.name), and we
     * use the term "cloud name" to be the short name without a prefix.
     *
     * @see #cloudNameToName
     * @see #nameToCloudName
     * @see #getCloudName
     * @see DescriptorImpl#doCheckCloudName
     */
    public static final String NAME_PREFIX = "oci-compute-classic-";

    static String cloudNameToName(String cloudName) {
        return NAME_PREFIX + cloudName.trim();
    }

    static String nameToCloudName(String name) {
        return name.substring(NAME_PREFIX.length());
    }

    /**
     * The prefix to add to the names of created orchestrations.
     */
    public static final String ORCHESTRATION_NAME_PREFIX = "jenkins-";

    /**
     * The prefix to add to the names of provisioned agents.
     */
    public static final String AGENT_NAME_PREFIX = "oci-compute-classic-";

    /** Time to sleep while polling if an orchestration has started. */
    private static final long POLL_SLEEP_MILLIS = TimeUnit.SECONDS.toMillis(5);

    /** Time to recycle cloud resources, throw exception if timeout */
    private static final long RECYCLE_TIMEOUT_NANOS = TimeUnit.MINUTES.toNanos(10);

    private final String apiEndpoint;
    private final String identityDomainName;
    private final String userName;
    private final String password;
    private final int nextTemplateId;
    private final List<? extends ComputeCloudAgentTemplate> templates;

    @DataBoundConstructor
    public ComputeCloud(
            String cloudName,
            String apiEndpoint,
            String identityDomainName,
            String userName,
            String password,
            String instanceCapStr,
            int nextTemplateId,
            List<? extends ComputeCloudAgentTemplate> templates) {
        super(cloudNameToName(cloudName), instanceCapStr);

        this.apiEndpoint = apiEndpoint;
        this.identityDomainName = identityDomainName;
        this.userName = userName;
        this.password = this.getEncryptedValue(password);

        this.nextTemplateId = nextTemplateId;
        if (templates == null) {
            this.templates = Collections.emptyList();
        } else {
            this.templates = templates;
        }

    }

    @Override
    public String getDisplayName() {
        return getCloudName();
    }

    /**
     * Called by {@code provision.jelly}.
     *
     * @return the full display name
     */
    public String getFullDisplayName() {
        return DescriptorImpl.DISPLAY_NAME + ' ' + getCloudName();
    }

    public String getCloudName() {
        return nameToCloudName(name);
    }

    public int getNextTemplateId() {
        return nextTemplateId;
    }

    public List<? extends ComputeCloudAgentTemplate> getTemplates() {
        return templates;
    }

    public String getApiEndpoint() {
        return apiEndpoint;
    }

    public URI getApiEndpointUrl() {
        try {
            return new URI(apiEndpoint);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    public String getIdentityDomainName() {
        return identityDomainName;
    }

    public String getUserName() {
        return userName;
    }

    public ComputeCloudUser getUser() {
        try {
            return ComputeCloudUser.valueOf(identityDomainName, userName);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(e);
        }
    }

    protected String getEncryptedValue(String str) {
        return Secret.fromString(str).getEncryptedValue();
    }

    protected String getPlainText(String str) {
        return  str == null ? null : Secret.decrypt(str).getPlainText();
    }

    public final String getPassword() {
        return getPlainText(password);
    }

    ExecutorService getThreadPoolForRemoting() {
        return Computer.threadPoolForRemoting;
    }

    PlannedNode newPlannedNode(String displayName, Future<Node> future, int numExecutors, ComputeCloudAgentTemplate template) {
        return new PlannedNode(displayName, future, numExecutors);
    }

    @Override
    public Collection<PlannedNode> provision(final Label label, int excessWorkload) {
        final ComputeCloudAgentTemplate t = getTemplate(label);
        if (t == null) {
            return Collections.emptyList();
        }

        int numAgents = countCurrentComputeCloudAgents();

        List<PlannedNode> r = new ArrayList<>();
        for (; excessWorkload > 0 && numAgents < getInstanceCap(); numAgents++) {
            Provisioner provisioner = new Provisioner(t);
            String displayName = provisioner.getPlannedNodeDisplayName();
            Future<Node> future = getThreadPoolForRemoting().submit(provisioner);

            int numExecutors = provisioner.numExecutors;
            r.add(newPlannedNode(displayName, future, numExecutors, t));
            excessWorkload -= numExecutors;
        }
        return r;
    }

    private class Provisioner implements Callable<Node> {
        final ComputeCloudAgentTemplate template;
        final int numExecutors;
        final String name;
        final String orchName;

        Provisioner(ComputeCloudAgentTemplate template) {
            this.template = template;
            this.numExecutors = template.getNumExecutorsValue();

            UUID uuid = UUID.randomUUID();
            this.name = AGENT_NAME_PREFIX + uuid;
            this.orchName = ORCHESTRATION_NAME_PREFIX + uuid;
        }

        public String getPlannedNodeDisplayName() {
            return orchName;
        }

        @Override
        public Node call() throws Exception {
            return provision(name, template, orchName);
        }
    }

    ComputeCloudAgent newComputeCloudAgent(
            final String name,
            final ComputeCloudAgentTemplate template,
            final String cloudName,
            final String orchName,
            final String host) throws IOException, FormException {
        return new ComputeCloudAgent(name, template, cloudName, orchName, host);
    }

    // Stop and delete orchestration related cloud resources
    protected void stopAndDeleteOrchestration(ComputeCloudClient client, long timeoutNanos, String orchName) throws ComputeCloudClientException, InterruptedException, IOException {

        TimeoutHelper timeoutHelper = new TimeoutHelper(getClock(), timeoutNanos, POLL_SLEEP_MILLIS);
        do {
            InstanceOrchestration instanceOrch = client.getInstanceOrchestration(orchName);

            InstanceOrchestration.Status status = instanceOrch.getStatus();

            if (status == InstanceOrchestration.Status.error
                    && instanceOrch.getErrors() != null && instanceOrch.getErrors().size() > 0) {
                StringBuilder errMsgBuilder = new StringBuilder();
                for (String msg : instanceOrch.getErrors()) {
                    errMsgBuilder.append("\n");
                    errMsgBuilder.append(msg);
                }
                LOGGER.warning("Orchestration " + orchName + " in error status would be recycled, error messages:" + errMsgBuilder.toString());
            }

            if (status != InstanceOrchestration.Status.stopped && status != InstanceOrchestration.Status.stopping) {
                client.stopOrchestration(orchName);
            }

            if (status == InstanceOrchestration.Status.stopped) {
                client.deleteOrchestration(orchName);
                return;
            }
        } while (timeoutHelper.sleep());

        throw new IOException("Provision node: " + orchName + " failed, AND CREATED RESOURCES FAILED TO RECYCLE, REQUIRE MANUAL OPERATION!!!");
    }

    /**
     * recycle orchestration related cloud resources with specified orchestration name
     *
     * @param orchName the name of orchestration to be recycled.
     * @throws InterruptedException if thread is interrupted.
     * @throws IOException if request hit IO exception.
     */
    public void recycleCloudResources(String orchName) throws InterruptedException, IOException {
        try (ComputeCloudClient client = createClient()) {
            stopAndDeleteOrchestration(client, RECYCLE_TIMEOUT_NANOS, orchName);
        } catch (ComputeCloudClientException e) {
            throw new IOException(e);
        }
    }

    private ComputeCloudAgent provision(String name, ComputeCloudAgentTemplate template, String orchName) throws Exception {
        LOGGER.info("Provisioning new node with Oracle Cloud Infrastructure Compute Classic orchestration " + orchName);

        try (ComputeCloudClient client = createClient()) {
            client.createInstanceOrchestration(orchName, template);

            TimeoutHelper timeoutHelper = new TimeoutHelper(getClock(), template.getStartTimeoutNanos(), POLL_SLEEP_MILLIS);

            // If the start/wait fails, delete the orchestration.
            // However, if the orchestration is in error status, extract the error message from the orchestration
            // and include it in the exception message or else the user will have no way to diagnose the failure after we delete the orchestration.
            InstanceOrchestration instance;
            try {
                instance = startInstanceAndAwait(client, orchName, timeoutHelper);
                String ip = instance.getIp();
                LOGGER.info("Provisioned orchestration " + orchName + " with public ip " + ip);
                awaitInstanceSshAvailable(ip, template.getSshConnectTimeoutMillis(), timeoutHelper);
                template.resetFailureCount();

                return newComputeCloudAgent(name, template, this.name, orchName, ip);
            } catch (Exception e) {
                try {
                    stopAndDeleteOrchestration(client, template.getStartTimeoutNanos(), orchName);
                    LOGGER.log(Level.WARNING, "Provision node: " + orchName + " failed, and created resources have been recycled.", e);
                } catch (Exception ex) {
                    LOGGER.log(Level.WARNING, "Provision node: " + orchName + " failed, and failed to recycle node " + orchName, ex);
                }
                throw e;
            }
        } catch (Exception e) {
            template.increaseFailureCount(e.getMessage());
            throw e;
        }
    }

    Clock getClock() {
        return Clock.INSTANCE;
    }

    /**
     * Start instance and await its status to be running before return this instance
     */
    private InstanceOrchestration startInstanceAndAwait(ComputeCloudClient client, String orchName, TimeoutHelper timeoutHelper) throws Exception {
        client.startOrchestration(orchName);

        do {
            InstanceOrchestration instanceOrch = client.getInstanceOrchestration(orchName);

            InstanceOrchestration.Status status = instanceOrch.getStatus();
            if (status == InstanceOrchestration.Status.ready) {
                return instanceOrch;
            }

            if (status != InstanceOrchestration.Status.starting) {
                throw new IOException("Instance " + orchName + " has status " + status + " rather than starting or ready");
            }
        } while (timeoutHelper.sleep());

        IOException ex = new IOException("Timed out waiting for orchestration to have ready status");
        // in case exception would be override by stopAndDeleteInstance exception
        LOGGER.log(Level.WARNING, "Timed out waiting for orchestration to have ready status", ex);
        throw ex;
    }

    SshConnector getSshConnector() {
        return SshConnector.INSTANCE;
    }

    private void awaitInstanceSshAvailable(String host, int connectTimeoutMillis, TimeoutHelper timeoutHelper) throws IOException, InterruptedException {
        SshConnector sshConnector = getSshConnector();
        do {
            Connection conn = sshConnector.createConnection(host);
            try {
                sshConnector.connect(conn, connectTimeoutMillis);
                return;
            } catch (IOException e) {
                LOGGER.log(Level.FINER, "Ignoring exception connecting to SSH during privision", e);
            } finally {
                conn.close();
            }
        } while (timeoutHelper.sleep());

        throw new IOException("Timed out connecting to SSH");
    }

    List<Node> getNodes() {
        return JenkinsUtil.getJenkinsInstance().getNodes();
    }

    public int countCurrentComputeCloudAgents() {
        int r = 0;
        for (Node n : getNodes())
            if (n instanceof ComputeCloudAgent) {
                ComputeCloudAgent agent = (ComputeCloudAgent)n;
                if (name.equals(agent.getCloudName())) {
                    r++;
                }
            }
        return r;
    }

    public ComputeCloudAgentTemplate getTemplate(Label label) {
        for (ComputeCloudAgentTemplate t : templates) {
            if (t.getDisableCause() != null) {
                continue;
            }
            if (t.getMode() == Node.Mode.NORMAL) {
                if (label == null || label.matches(t.getLabelAtoms())) {
                    return t;
                }
            } else if (t.getMode() == Node.Mode.EXCLUSIVE) {
                if (label != null && label.matches(t.getLabelAtoms())) {
                    return t;
                }
            }
        }
        return null;
    }

    static final String PROVISION_ATTR_AGENT_NAME = ComputeCloud.class.getName() + ".name";
    static final String PROVISION_ATTR_NUM_EXECUTORS = ComputeCloud.class.getName() + ".numExecutors";

    /**
     * Called by {@code computerSet.jelly} when explicitly provisioning a new
     * node via the nodes page.
     *
     * @param templateId template id
     * @param req request
     * @param rsp response
     *
     * @throws ServletException if a servlet exception occurs
     * @throws IOException if a IO error occurs
     */
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    public void doProvision(
            @QueryParameter int templateId,
            StaplerRequest req,
            StaplerResponse rsp) throws ServletException, IOException {
        checkPermission(PROVISION);

        ComputeCloudAgentTemplate template = getTemplateById(templateId);
        if (template == null) {
            sendError(Messages.ComputeCloud_provision_templateNotFound(), req, rsp);
            return;
        }
        if (template.getDisableCause() != null) {
            sendError(Messages.ComputeCloud_provision_templateDisabled(), req, rsp);
            return;
        }

        // Note that this will directly add a new node without involving
        // NodeProvisioner, so that class will not be aware that a node is being
        // provisioned until ExplicitProvisioner adds it.
        ExplicitProvisioner provisioner = new ExplicitProvisioner(template);
        getThreadPoolForRemoting().submit(provisioner);

        req.setAttribute(PROVISION_ATTR_AGENT_NAME, provisioner.name);
        req.setAttribute(PROVISION_ATTR_NUM_EXECUTORS, provisioner.numExecutors);
        req.getView(this, "provision").forward(req, rsp);
    }

    void addNode(Node node) throws IOException {
        JenkinsUtil.getJenkinsInstance().addNode(node);
    }

    private class ExplicitProvisioner extends Provisioner {
        ExplicitProvisioner(ComputeCloudAgentTemplate template) {
            super(template);
        }

        @Override
        public Node call() throws Exception {
            // Simulate NodeProvisioner.update.
            String displayName = getPlannedNodeDisplayName();
            try {
                addNode(super.call());
                LOGGER.log(Level.INFO, "{0} provisioning successfully completed", displayName);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Provisioned slave " + displayName + " failed!", e);
            }

            // doProvision does not use the Future.
            return null;
        }
    }

    private ComputeCloudAgentTemplate getTemplateById(int templateId) {
        for (ComputeCloudAgentTemplate t : templates) {
            if (t.getTemplateId() == templateId) {
                return t;
            }
        }
        return null;
    }

    /**
     * Called by {@code provision.jelly} to show a sidepanel.
     *
     * @return provision side panel class
     */
    public Class<?> getProvisionSidePanelClass() {
        return ComputerSet.class;
    }

    /**
     * Display a provisioning message based on request attributes set by
     * {@link #doProvision}.
     *
     * @param req request
     *
     * @return provision started message
     */
    public String getProvisionStartedMessage(HttpServletRequest req) {
        String name = (String)req.getAttribute(PROVISION_ATTR_AGENT_NAME);
        Integer numExecutors = (Integer)req.getAttribute(PROVISION_ATTR_NUM_EXECUTORS);
        return Messages.ComputeCloud_provision_started(name, numExecutors);
    }

    /**
     * The breadcrumb on the {@code provision.jelly} page contains a link to
     * this object.  We have no data to display, so redirect the user to the
     * computer set page.
     *
     * @return the http response
     *
     * @throws IOException if an IO error occurs
     */
    public HttpResponse doIndex() throws IOException {
        return HttpResponses.redirectTo("../../computer/");
    }

    public ComputeCloudClient createClient() {
        return ComputeCloudPlugin.CLIENT_MANAGER.createClient(this);
    }

    @Override
    public boolean canProvision(Label label) {
        return getTemplate(label) != null;
    }

    static class ConfigMessages {
        static final DynamicResourceBundleHolder holder = DynamicResourceBundleHolder.get(ComputeCloud.class, "config");

        public static String apiEndpoint() {
            return holder.format("apiEndpoint");
        }

        public static String identityDomainName() {
            return holder.format("identityDomainName");
        }

        public static String userName() {
            return holder.format("userName");
        }

        public static String password() {
            return holder.format("password");
        }
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<Cloud> {
        public static final String DISPLAY_NAME = "Oracle Cloud Infrastructure Compute Classic";

        @Override
        public String getDisplayName() {
            return DISPLAY_NAME;
        }

        List<? extends Cloud> getClouds() {
            return JenkinsUtil.getJenkinsInstance().clouds;
        }

        public FormValidation doCheckCloudName(@QueryParameter String value) {
            value = value.trim();
            try {
                Jenkins.checkGoodName(value);
            } catch (Failure e) {
                return FormValidation.error(e.getMessage());
            }

            String name = cloudNameToName(value);
            int found = 0;
            for (Cloud c : getClouds()) {
                if (c.name.equals(name)) {
                    found++;
                }
            }

            // We don't know whether the user is adding a new cloud or updating
            // an existing one.  If they are adding a new cloud, then found==0,
            // but if they are updating an existing cloud, then found==1, and we
            // do not want to give an error, so we check found>1.  This means
            // this error is only given after the user has already saved a new
            // cloud with a duplicate name and has reopened the configuration.
            if (found > 1) {
                return FormValidation.error(Messages.ComputeCloud_cloudName_duplicate(value));
            }

            return FormValidation.ok();
        }

        public static FormValidation withContext(FormValidation fv, String context) {
            return FormValidation.error(JenkinsUtil.unescape(fv.getMessage()) + ": " + context);
        }

        private FormValidationValue<URI> checkApiEndpoint(String value, boolean withContext) {
            FormValidation fv = JenkinsUtil.validateRequired(value);
            if (fv.kind != FormValidation.Kind.OK) {
                return FormValidationValue.error(withContext ? withContext(fv, ConfigMessages.apiEndpoint()) : fv);
            }

            URI uri;
            try {
                uri = new URI(value);
            } catch (URISyntaxException e) {
                return FormValidationValue.error(Messages.ComputeCloud_apiEndpoint_invalidUrl());
            }

            if (!uri.isAbsolute() || !uri.getScheme().startsWith("http")) {
                return FormValidationValue.error(Messages.ComputeCloud_apiEndpoint_invalidUrlScheme());
            }

            return FormValidationValue.ok(uri);
        }

        public FormValidation doCheckApiEndpoint(@QueryParameter String value) {
            return checkApiEndpoint(value, false).getFormValidation();
        }

        private boolean isValidUser(String identityDomainName, String userName) {
            try {
                ComputeCloudUser.valueOf(identityDomainName, userName);
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }

        private FormValidation checkIdentityDomainName(String value, boolean withContext) {
            FormValidation fv = JenkinsUtil.validateRequired(value);
            if (fv.kind != FormValidation.Kind.OK) {
                return withContext ? withContext(fv, ConfigMessages.identityDomainName()) : fv;
            }
            if (!isValidUser(value, "x")) {
                return FormValidation.error(Messages.ComputeCloud_identityDomainName_invalid());
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckIdentityDomainName(@QueryParameter String value) {
            return checkIdentityDomainName(value, false);
        }

        public FormValidation checkUserName(String value, boolean withContext) {
            FormValidation fv = JenkinsUtil.validateRequired(value);
            if (fv.kind != FormValidation.Kind.OK) {
                return withContext ? withContext(fv, ConfigMessages.userName()) : fv;
            }
            if (!isValidUser("x", value)) {
                return FormValidation.error(Messages.ComputeCloud_userName_invalid());
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckUserName(@QueryParameter String value) {
            return checkUserName(value, false);
        }

        private FormValidation checkPassword(String value, boolean withContext) {
            FormValidation fv = JenkinsUtil.validateRequired(value);
            if (fv.kind != FormValidation.Kind.OK) {
                return withContext ? withContext(fv, ConfigMessages.password()) : fv;
            }
            return fv;
        }

        ComputeCloudClientManager getComputeCloudClientManager() {
            return ComputeCloudPlugin.CLIENT_MANAGER;
        }

        private FormValidation newUnableToConnectException(FormValidation fv) {
            return FormValidation.error(Messages.ComputeCloud_testConnection_unable(JenkinsUtil.unescape(fv.getMessage())));
        }

        public ComputeCloudClient createClient(String apiEndpoint, String identityDomainName, String userName, String password) throws FormValidation {
            FormValidationValue<URI> apiEndpointValid = checkApiEndpoint(apiEndpoint, true);
            if (!apiEndpointValid.isOk()) {
                throw newUnableToConnectException(apiEndpointValid.getFormValidation());
            }

            FormValidation fv = checkIdentityDomainName(identityDomainName, true);
            if (fv.kind != FormValidation.Kind.OK) {
                throw newUnableToConnectException(fv);
            }

            fv = checkUserName(userName, true);
            if (fv.kind != FormValidation.Kind.OK) {
                throw newUnableToConnectException(fv);
            }

            fv = checkPassword(password, true);
            if (fv.kind != FormValidation.Kind.OK) {
                throw newUnableToConnectException(fv);
            }

            URI apiEndpointUrl = apiEndpointValid.getValue();
            ComputeCloudUser user = ComputeCloudUser.valueOf(identityDomainName, userName);
            return getComputeCloudClientManager().createClient(apiEndpointUrl, user, password);
        }

        public static FormValidation toFormValidation(ComputeCloudClientException e) {
            LOGGER.log(Level.FINE, "Failed to connect to Oracle Cloud Infrastructure Compute Classic", e);
            if (e instanceof ComputeCloudClientUnauthorizedException) {
                return FormValidation.error(Messages.ComputeCloud_testConnection_unauthorized());
            }
            return FormValidation.error(Messages.ComputeCloud_testConnection_error(e.getMessage()));
        }

        /**
         * Test connection from configuration page.
         *
         * @param apiEndpoint api endpoint
         * @param identityDomainName identitiy domain name
         * @param userName user name
         * @param password password
         *
         * @return a {@link FormValidation} object containing the details about success or error
         */
        public FormValidation doTestConnection(
                @QueryParameter String apiEndpoint,
                @QueryParameter String identityDomainName,
                @QueryParameter String userName,
                @QueryParameter String password) {
            try (ComputeCloudClient client = createClient(apiEndpoint, identityDomainName, userName, password)) {
                client.authenticate();
                return FormValidation.ok(Messages.ComputeCloud_testConnection_success());
            } catch (FormValidation fv) {
                return fv;
            } catch (ComputeCloudClientException e) {
                return toFormValidation(e);
            }
        }

    }
}
