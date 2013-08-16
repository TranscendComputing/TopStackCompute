package com.msi.compute.actions;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.msi.compute.integration.AbstractBaseComputeTest;
import com.msi.tough.core.Appctx;
import com.msi.tough.query.ActionTestHelper;

/**
 * Test allocate IP address locally (same VM, no webapp).
 *
 */
@Ignore
public class AssociateAddressLocalTest extends AbstractBaseComputeTest {

    private static Logger logger = Appctx
            .getLogger(AssociateAddressLocalTest.class.getName());

    @Autowired
    private final ActionTestHelper mActionHelper = null;

    @Before
    public void allocateInitialResources() throws Exception {
    	/* TODO: get address to associate */
    	/* TODO: get a valid instance on which to associate the address */
    	/* TODO: now associate */
    	logger.warn("Note: Tests not yet available so not perfoming resource allocations.");
    }

    @Test
    public void testGoodAssociation() {
        // Before & Afters do the work; if they run then we presume all works.
    }

    @After
    public void cleanupCreated() throws Exception {
        // mHelper.disassociateAllAssociatedAddresses();
        // mHelper = null;
    }

}
