package com.msi.compute.integration;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.DeleteSecurityGroupRequest;

public class DeleteSecurityGroupTest extends AbstractBaseComputeTest {

    protected final SimpleDateFormat dateFormat = new SimpleDateFormat(
            "yyyy-MM-dd-HH-mm-ss-SSS");
    private final String baseName = dateFormat.format(new Date())
            + UUID.randomUUID().toString().substring(0, 4);

    String name1 = "del-sg-1-" + baseName;

    @Before
    public void setUp() throws Exception {
        CreateSecurityGroupRequest req = new CreateSecurityGroupRequest();
        req.setGroupName(name1);
        req.setDescription("This group should be removed soon.");
        getComputeClientV2().createSecurityGroup(req);
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testGoodCreate() {
        DeleteSecurityGroupRequest req = new DeleteSecurityGroupRequest(name1);
        getComputeClientV2().deleteSecurityGroup(req);
    }
}
