package com.msi.compute.actions;

import java.util.HashSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.msi.compute.helper.AddressLocalHelper;
import com.msi.compute.integration.AbstractBaseComputeTest;
import com.msi.tough.core.Appctx;
import com.msi.tough.query.ActionTestHelper;

/**
 * Test allocate IP address locally (same VM, no webapp).
 *
 * @author jgardner
 *
 */
public class AllocateAddressLocalTest extends AbstractBaseComputeTest {

    private static Logger logger = Appctx
            .getLogger(AllocateAddressLocalTest.class.getName());

    @Autowired
    private final ActionTestHelper actionHelper = null;

    private HashSet<String> remaining = new HashSet<String>();

    @Before
    public void allocateInitialAddresses() throws Exception {
        String ip = AddressLocalHelper.allocateAddress();
        remaining.add(ip);
        logger.debug("Got IP: " + ip);
        ip = AddressLocalHelper.allocateAddress();
        remaining.add(ip);
        logger.debug("Got IP: " + ip);
    }

    @Test
    public void testGoodAllocation() {
        // Before & Afters allocate and release; if they run, it works.
    }

    /**
     * This test assumes limited quota account in the database
     */
    // @Test(expected = ErrorResponse.class)
    @Test
    public void testQuotaError() throws Exception {
        // AllocateAddressResult result = null;
        // final ActionRequest request = new ActionRequest();
        // AllocateAddress action = new AllocateAddress();
        // result = actionHelper.invokeProcessWithLimitedQuota(action,
        // request.getRequest(), request.getResponse(), request.getMap());
        // assertNotNull(result);
        // logger.debug("Got results:" + result.getAllocationId());
    }

    @After
    public void cleanupCreated() throws Exception {
        AddressLocalHelper.releaseAllAllocatedAddresses();
    }

}
