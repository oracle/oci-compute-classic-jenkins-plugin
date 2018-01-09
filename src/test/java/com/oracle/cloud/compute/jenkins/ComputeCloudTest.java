package com.oracle.cloud.compute.jenkins;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.slaves.Cloud;

public class ComputeCloudTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    @Ignore
    public void testConfigRoundtrip() throws Exception {
        ComputeCloud orig = new TestComputeCloud.Builder().cloudName("foo").build();
        r.jenkins.clouds.add(orig);
        r.submit(r.createWebClient().goTo("configure").getFormByName("config"));

        Cloud actual = r.jenkins.clouds.iterator().next();
        r.assertEqualBeans(orig, actual, "nimbulaUrl,domainName,nimbulaUser,nimbulaPassword");

    }

}
