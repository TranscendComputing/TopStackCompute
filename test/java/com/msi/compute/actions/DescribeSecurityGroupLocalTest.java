package com.msi.compute.actions;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.msi.compute.integration.AbstractBaseComputeTest;
import com.msi.tough.core.Appctx;
import com.msi.tough.query.ActionTestHelper;
import com.transcend.compute.message.DescribeSecurityGroupsMessage.DescribeSecurityGroupsRequestMessage;
import com.transcend.compute.message.DescribeSecurityGroupsMessage.DescribeSecurityGroupsResponseMessage;
import com.transcend.compute.worker.DescribeSecurityGroupsWorker;

/**
 * Test describing instances locally.
 *
 * @author jgardner
 *
 */
public class DescribeSecurityGroupLocalTest extends AbstractBaseComputeTest {

    private static Logger logger = Appctx.getLogger(DescribeSecurityGroupLocalTest.class
            .getName());

    @Autowired
    private ActionTestHelper actionHelper = null;

    @Autowired
    private DescribeSecurityGroupsWorker describeGroupsWorker = null;

    /**
     */
    @Test
    public void testDescribeSecurityGroups() throws Exception {
        DescribeSecurityGroupsResponseMessage result = null;
        DescribeSecurityGroupsRequestMessage.Builder builder =
                DescribeSecurityGroupsRequestMessage.newBuilder();
        builder.setTypeId(true);
        builder.setCallerAccessKey(getCreds().getAWSAccessKeyId());
        builder.setRequestId("test");
        result = describeGroupsWorker.doWork(builder.build());
        assertNotNull(result);
        logger.debug("Got results:" +result.getSecurityGroupInfoList());
        assertTrue("Expect some security groups.",
                result.getSecurityGroupInfoList().size() > 0);

    }
}
