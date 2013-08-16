package com.msi.compute.actions;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.msi.compute.helper.CreateSecurityGroupLocalHelper;
import com.msi.compute.integration.AbstractBaseComputeTest;
import com.msi.tough.core.Appctx;
import com.msi.tough.query.ActionTestHelper;

/**
 * Test CreateSecurityGrouplocally (same VM, no webapp).
 *
 */


public class CreateSecurityGroupLocalTest extends AbstractBaseComputeTest {

    private static Logger logger = Appctx
            .getLogger(CreateSecurityGroupLocalTest.class.getName());

	private CreateSecurityGroupLocalHelper mHelper = null;

    @Autowired
    private final ActionTestHelper mActionHelper = null;

    @Before
    public void allocateSecurityGroup() throws Exception {
        mHelper = new CreateSecurityGroupLocalHelper();
        String groupId = mHelper.createSecurityGroup();
        logger.debug("Got GroupName: " + groupId);
    }

    @Test
    public void testGoodAllocation() {
        // Before & Afters create and delete the group; if they run, all is purportedly good.
    }

    @After
    public void cleanupCreated() throws Exception {
        mHelper.deleteAllSecurityGroups();
        mHelper = null;
    }

}
