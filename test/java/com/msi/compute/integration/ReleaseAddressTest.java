package com.msi.compute.integration;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import com.amazonaws.services.ec2.model.AllocateAddressRequest;
import com.amazonaws.services.ec2.model.AllocateAddressResult;
import com.amazonaws.services.ec2.model.ReleaseAddressRequest;
import com.msi.tough.core.Appctx;

public class ReleaseAddressTest extends AbstractBaseComputeTest {

    private static Logger logger = Appctx.getLogger(ReleaseAddressTest.class
            .getName());

    private Set<String> publicIps = new HashSet<String>();

    @Before
    public void setUp() throws Exception {
    }

    
    @Test
    public void tearDown() throws Exception {
        for (String publicIp : publicIps) {
            final ReleaseAddressRequest request = new ReleaseAddressRequest();
            request.withPublicIp(publicIp);
            getComputeClientV2().releaseAddress(request);
        }
    }

    @Before
    public void testGoodCreate() {
        AllocateAddressResult allocateResult = null;
        logger.info("Creating Address");
        final AllocateAddressRequest request = new AllocateAddressRequest();
        allocateResult = getComputeClientV2().allocateAddress(request);
        publicIps.add(allocateResult.getPublicIp());
        String address = allocateResult.getPublicIp();
        logger.debug("Got resulting IP: " + address);
    }
}
