package com.msi.compute.actions;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.msi.compute.integration.AbstractBaseComputeTest;
import com.msi.tough.query.ActionTestHelper;

/**
 * Test allocate IP address locally (same VM, no webapp).
 *
 */
public class DisassociateAddressLocalTest extends AbstractBaseComputeTest {

    @Autowired
    private final ActionTestHelper mActionHelper = null;

//    @Before
//    public void allocateInitialResources() throws Exception {
//    	mHelper = new AssociateAddressLocalHelper();
//    	logger.warn("Note: Tests not yet available so not perfoming resource allocations.");
//    }
//
    @Test
    public void testGoodAssociation() {

    }

//    @After
//    public void cleanupCreated() throws Exception {
//        mHelper.disassociateAllAssociatedAddresses();
//        mHelper = null;
//    }

}
