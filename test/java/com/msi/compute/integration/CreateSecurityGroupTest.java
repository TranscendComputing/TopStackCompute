package com.msi.compute.integration;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.DeleteSecurityGroupRequest;
import com.msi.tough.core.Appctx;

public class CreateSecurityGroupTest extends AbstractBaseComputeTest {
    private static Logger logger = Appctx.getLogger(CreateSecurityGroupTest.class
            .getName());

    protected final SimpleDateFormat dateFormat = new SimpleDateFormat(
            "yyyy-MM-dd-HH-mm-ss-SSS");
    private final String baseName = dateFormat.format(new Date())
            + UUID.randomUUID().toString().substring(0, 3);

    String name1 = "c-crSec-" + baseName;

    private String desc = "This group should be removed soon";

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() throws Exception {
        DeleteSecurityGroupRequest req = new DeleteSecurityGroupRequest(name1);
        getComputeClientV2().deleteSecurityGroup(req );
    }

    @Test
    public void testGoodCreate() throws Exception {
        try {
            CreateSecurityGroupRequest req = new CreateSecurityGroupRequest(name1, desc);
            getComputeClientV2().createSecurityGroup(req);
        } catch (Exception e) {
            logger.error("Failed to create security group.", e);
        }
    }
}
