package com.msi.compute.actions;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.msi.compute.helper.InstanceHelper;
import com.msi.compute.integration.AbstractBaseComputeTest;
import com.msi.tough.query.ActionTestHelper;

/**
 * Run instance locally (same VM, no webapp).
 *
 * @author jgardner
 *
 */
public class RunInstancesLocalTest extends AbstractBaseComputeTest {

    @Autowired
    private final ActionTestHelper actionHelper = null;

    @Before
    public void runInitialInstances() throws Exception {
        InstanceHelper.runInstance();
    }

    @Test
    public void testGoodLaunch() throws Exception {
        // Before & Afters allocate and release; if they run, it works.
    }

    @After
    public void cleanupCreated() throws Exception {
        InstanceHelper.terminateAllLaunchedInstances();
    }

}
