package com.msi.compute.integration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.DeleteSecurityGroupRequest;

public class DeleteSecurityGroupTest extends AbstractBaseComputeTest {
	private String grpName = "deleteSecGroupTest";


	@Before
    public void setUp() throws Exception {
    	CreateSecurityGroupRequest req = new CreateSecurityGroupRequest();
    	req.setGroupName(grpName);
    	req.setDescription("This group should be removed soon.");
		getComputeClientV2().createSecurityGroup(req);
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testGoodCreate() {
    	DeleteSecurityGroupRequest req = new DeleteSecurityGroupRequest(grpName);
		getComputeClientV2().deleteSecurityGroup(req);
    }
}
