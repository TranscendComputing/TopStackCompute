package com.msi.compute.integration;

import org.junit.Test;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.model.DisassociateAddressRequest;

public class DisassociateAddressTest extends AbstractBaseComputeTest {

    @Test
    public void testDisassociateAddress() {
        // See AssociateAddressTest; teardown for that does Disassociate
    }

    @Test(expected=AmazonServiceException.class)
    public void testDisassociateBadAddress() {
        final DisassociateAddressRequest request = new DisassociateAddressRequest();
        request.setPublicIp("127.0.0.1");
        getComputeClientV2().disassociateAddress(request);
    }

}
