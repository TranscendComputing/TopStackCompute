package com.msi.compute.integration;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.slf4j.Logger;

import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.msi.tough.core.Appctx;

public class DescribeInstancesTest extends AbstractBaseComputeTest {

    private static Logger logger = Appctx.getLogger(DescribeInstancesTest.class
            .getName());

    /**
     */
    @Test
    public void testDescribe() {
        DescribeInstancesResult result = null;
        logger.info("Describing Instances");
        final DescribeInstancesRequest request = new DescribeInstancesRequest();
        result = getComputeClientV2().describeInstances(request);
        assertNotNull(result);
        // There may be no instances running.
        //assertTrue(result.getReservations().size() > 0);
        logger.debug("Got instances: " + result);
    }

}
