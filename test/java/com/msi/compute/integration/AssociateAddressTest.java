package com.msi.compute.integration;

import static org.junit.Assert.assertNotNull;

import javax.annotation.Resource;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;

import com.amazonaws.services.ec2.model.AllocateAddressRequest;
import com.amazonaws.services.ec2.model.AllocateAddressResult;
import com.amazonaws.services.ec2.model.AssociateAddressRequest;
import com.amazonaws.services.ec2.model.AssociateAddressResult;
import com.amazonaws.services.ec2.model.DisassociateAddressRequest;
import com.amazonaws.services.ec2.model.ReleaseAddressRequest;
import com.msi.tough.core.Appctx;
import com.msi.tough.helper.RunningInstanceHelper;

public class AssociateAddressTest extends AbstractBaseComputeTest {

    private static Logger logger = Appctx.getLogger(AssociateAddressTest.class
            .getName());
    private String createdInstanceId = null;
    private String publicIp = null;

    @Resource
    RunningInstanceHelper runningInstanceHelper = null;

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
        createdInstanceId = runningInstanceHelper.
                getOrCreateInstance("associateAddressTest");

        assertNotNull("Expect created instance.", createdInstanceId);
        // check details from the result
        logger.info("Created instance with ID " + createdInstanceId);

        runningInstanceHelper.waitForState(createdInstanceId, "running");
    }

    @After
    public void tearDown() throws Exception {
        final DisassociateAddressRequest request = new DisassociateAddressRequest();
        request.setPublicIp(publicIp);
        getComputeClientV2().disassociateAddress(request);

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
