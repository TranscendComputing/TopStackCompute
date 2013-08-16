package com.msi.compute.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Placement;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.msi.tough.core.Appctx;

public class RunInstanceTest extends AbstractBaseComputeTest {

    private static Logger logger = Appctx.getLogger(RunInstanceTest.class
            .getName());

    private static final int MAX_WAIT_SECS = 60;
    private static final int WAIT_SECS = 2;

    @Resource
    protected String testInstanceType = null;

    @Resource
    protected String baseImageId = null;

    private Instance createdInstance = null;

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
        if (createdInstance == null) {
            return;
        }
        final TerminateInstancesRequest terminateRequest = new TerminateInstancesRequest();
        {
            final List<String> instanceIds = new ArrayList<String>();
            instanceIds.add(createdInstance.getInstanceId());
            terminateRequest.setInstanceIds(instanceIds);
        }
        // delete instance
        getComputeClientV2().terminateInstances(terminateRequest);
    }

    @Test
    public void testCompleteRunInstance() throws Exception {
        logger.info("Creating Instance");
        final RunInstancesRequest request = new RunInstancesRequest();
        Placement place = new Placement();
        place.setAvailabilityZone(getDefaultAvailabilityZone());
        request.setPlacement(place);
        request.setImageId(baseImageId);
        request.setInstanceType(testInstanceType);
        request.setMinCount(1);
        request.setMaxCount(1);
        final RunInstancesResult runResult = getComputeClientV2().runInstances(
                request);

        // Only 1 instance created, so just get first from list of instances
        createdInstance = runResult.getReservation()
                .getInstances().get(0);
        assertNotNull("Expect created instance.", createdInstance);
        // check details from the result
        logger.info("Created instance with ID "
                + createdInstance.getInstanceId());
        logger.info("Complete RunInstances result: " + runResult);
        assertEquals(testInstanceType, createdInstance.getInstanceType());
        assertEquals(baseImageId,
                createdInstance.getImageId());
        assertEquals(getDefaultAvailabilityZone(), createdInstance.getPlacement()
                .getAvailabilityZone());
        //assertEquals("pending", createdInstance.getState().getName());

        // describe the instance just created to check details
        final DescribeInstancesRequest describe = new DescribeInstancesRequest();
        {
            final List<String> instanceIds = new ArrayList<String>();
            instanceIds.add(createdInstance.getInstanceId());
            describe.setInstanceIds(instanceIds);
        }

        // sleep to wait for instance to spin up
        boolean started = false;
        int count;
        for (count = 0; count < MAX_WAIT_SECS; count += WAIT_SECS) {
            final DescribeInstancesResult describeResult = getComputeClientV2()
                    .describeInstances(describe);
            final Instance describedInstance = describeResult.getReservations()
                    .get(0).getInstances().get(0);
            logger.info("Complete DescribeInstances result:" + describeResult);
            if (describedInstance.getState().getName().equals("running")) {
                started = true;
                break;
            }
            Thread.sleep(1000 * WAIT_SECS);
        }
        assertTrue("Expect instance to start.", started);

        logger.info("Instance started in " + count + " seconds.");

    }

}
