package com.msi.compute.integration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.msi.tough.core.Appctx;

public class DescribeSecurityGroupsTest extends AbstractBaseComputeTest{
	private static Logger logger = Appctx.getLogger(DescribeSecurityGroupsTest.class
            .getName());
	
	@Before
    public void setUp() throws Exception {
    	
    }

    @After
    public void tearDown() throws Exception {
        
    }

    @Test
    public void testGoodCreate() {
    	DescribeSecurityGroupsResult result = getComputeClientV2().describeSecurityGroups();
    	logger.info("Result: " + result.toString());
    }
}
