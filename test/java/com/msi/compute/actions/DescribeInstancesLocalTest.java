package com.msi.compute.actions;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.msi.compute.integration.AbstractBaseComputeTest;
import com.msi.tough.core.Appctx;
import com.msi.tough.query.ActionTestHelper;
import com.transcend.compute.message.DescribeInstancesMessage.DescribeInstancesRequestMessage;
import com.transcend.compute.message.DescribeInstancesMessage.DescribeInstancesResponseMessage;
import com.transcend.compute.worker.DescribeInstancesWorker;

/**
 * Test describing instances locally.
 *
 * @author jgardner
 *
 */
public class DescribeInstancesLocalTest extends AbstractBaseComputeTest {

    private static Logger logger = Appctx.getLogger(DescribeInstancesLocalTest.class
            .getName());

    @Autowired
    private ActionTestHelper actionHelper = null;

    @Autowired
    private DescribeInstancesWorker describeInstancesWorker = null;

    /**
     * This test assumes there's always some instances running.
     */
    @Test
    public void testDescribeInstances() throws Exception {
        DescribeInstancesResponseMessage result = null;
        DescribeInstancesRequestMessage.Builder builder =
                DescribeInstancesRequestMessage.newBuilder();
        builder.setTypeId(true);
        builder.setCallerAccessKey(getCreds().getAWSAccessKeyId());
        builder.setRequestId("test");
        result = describeInstancesWorker.doWork(builder.build());
        assertNotNull(result);
        logger.debug("Got results:" +result.getReservationsList());
        assertTrue("Expect some instances to be running.",
                result.getReservationsList().size() > 0);

    }
}
