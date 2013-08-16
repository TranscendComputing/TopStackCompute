package com.msi.compute.integration;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;

import com.amazonaws.services.ec2.model.AllocateAddressRequest;
import com.amazonaws.services.ec2.model.AllocateAddressResult;
import com.amazonaws.services.ec2.model.AssociateAddressRequest;
import com.amazonaws.services.ec2.model.AssociateAddressResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DisassociateAddressRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.ReleaseAddressRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.msi.compute.helper.InstanceHelper;
import com.msi.tough.core.Appctx;

public class AssociateAddressTest extends AbstractBaseComputeTest {

    private static Logger logger = Appctx.getLogger(AssociateAddressTest.class
            .getName());
    private String createdInstanceId = null;
    private String publicIp = null;
    private static final int MAX_WAIT_SECS = 60;

    @Before
    public void setUp() throws Exception {

        // allocate the address to associate
        AllocateAddressResult allocateResult = null;
        logger.info("Creating Address");
        final AllocateAddressRequest aaReq = new AllocateAddressRequest();
        allocateResult = getComputeClientV2().allocateAddress(aaReq);
        publicIp = allocateResult.getPublicIp();
        logger.info(allocateResult.getPublicIp()
                + " has been allocated for this test.");

        // get a valid instance on which to associate the address
        logger.info("Creating Instance");
        createdInstanceId = InstanceHelper.runInstance();

        assertNotNull("Expect created instance.", createdInstanceId);
        // check details from the result
        logger.info("Created instance with ID " + createdInstanceId);

        // describe the instance just created to check details
        final DescribeInstancesRequest describe = new DescribeInstancesRequest();
        {
            final List<String> instanceIds = new ArrayList<String>();
            instanceIds.add(createdInstanceId);
            describe.setInstanceIds(instanceIds);
        }

        // sleep to wait for instance to spin up
        boolean started = false;
        int count;
        for (count = 0; count < MAX_WAIT_SECS; count += 5) {
            final DescribeInstancesResult describeResult = getComputeClientV2()
                    .describeInstances(describe);
            final Instance describedInstance = describeResult.getReservations()
                    .get(0).getInstances().get(0);
            logger.info("Complete DescribeInstances result:" + describeResult);
            if (describedInstance.getState().getName().equals("running")) {
                started = true;
                break;
            }
            Thread.sleep(5000);
        }
        assertTrue("Expect instance to start.", started);
        logger.info("Instance started in " + count + " seconds.");

    }

    @After
    public void tearDown() throws Exception {
        final DisassociateAddressRequest request = new DisassociateAddressRequest();
        request.setPublicIp(publicIp);
        getComputeClientV2().disassociateAddress(request);

        TerminateInstancesRequest tReq = new TerminateInstancesRequest();
        Collection<String> instanceIds = new LinkedList<String>();
        instanceIds.add(createdInstanceId);
        tReq.setInstanceIds(instanceIds);
        getComputeClientV2().terminateInstances(tReq);

        ReleaseAddressRequest rReq = new ReleaseAddressRequest(publicIp);
        getComputeClientV2().releaseAddress(rReq);
    }

    @Test
    public void testGoodAssociation() throws InterruptedException {
        logger.debug("Associating " + createdInstanceId
                + " with " + publicIp);
        AssociateAddressRequest req = new AssociateAddressRequest(
                createdInstanceId, publicIp);
        AssociateAddressResult result = getComputeClientV2().associateAddress(
                req);
        logger.debug("Association has been completed: " + result.toString());
    }

    @Ignore
    @Test
    public void testBadAssociation() {
        try {
            AssociateAddressRequest req = new AssociateAddressRequest(
                    "35ed170c-35d7-4697-8a35-e9b3403200e7", "invalid_cidrip");
            AssociateAddressResult result = getComputeClientV2()
                    .associateAddress(req);
            logger.debug("Association has been completed: " + result.toString());
        } catch (Exception e) {
            // logger.debug(e.getStackTrace());
            logger.info(e.getMessage());
        }
    }
}