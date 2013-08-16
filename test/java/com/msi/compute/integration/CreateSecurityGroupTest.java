package com.msi.compute.integration;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.DeleteSecurityGroupRequest;

public class CreateSecurityGroupTest extends AbstractBaseComputeTest {

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
        CreateSecurityGroupRequest req = new CreateSecurityGroupRequest(name1, desc);
        getComputeClientV2().createSecurityGroup(req);
    }
}
