/*
 * TopStack (c) Copyright 2012-2013 Transcend Computing, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an AS IS BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.msi.compute.integration;

import javax.annotation.Resource;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import com.msi.tough.core.Appctx;
import com.msi.tough.helper.RunningInstanceHelper;

/**
 * A final test to clean up after all other tests run.  Our helpers create load
 * balancers and leave them up, since spinning up an LB takes multiple minutes.
 *
 * This will clean up instances, even from previous runs, since a file is
 * written.
 *
 * @author jgardner
 *
 */
public class ZTerminalIntegrationTest extends AbstractBaseComputeTest {

    private final static Logger logger = Appctx
            .getLogger(ZTerminalIntegrationTest.class.getName());

    @Resource
    RunningInstanceHelper runningInstanceHelper = null;

    private static ZTerminalIntegrationTest instance = null;

    @Before
    public void setUp() throws Exception {
        instance = this;
    }

    @Test
    public void noop() {

    }

    @AfterClass
    public static void cleanupCreated() throws Exception {
        logger.debug("Destroying: all tests complete.");
        instance.runningInstanceHelper.finalDestroy();
    }

}
