package com.msi.compute.helper;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.msi.tough.core.Appctx;
import com.msi.tough.query.ActionTestHelper;
import com.transcend.compute.message.DescribeInstancesMessage.DescribeInstancesRequestMessage;
import com.transcend.compute.message.DescribeInstancesMessage.DescribeInstancesResponseMessage;
import com.transcend.compute.message.InstanceMessage.Instance;
import com.transcend.compute.message.InstanceMessage.Instance.Placement;
import com.transcend.compute.message.RunInstancesMessage.RunInstancesRequestMessage;
import com.transcend.compute.message.RunInstancesMessage.RunInstancesResponseMessage;
import com.transcend.compute.message.TerminateInstancesMessage.TerminateInstancesRequestMessage;
import com.transcend.compute.utils.ComputeFaults.InstanceDoesNotExist;
import com.transcend.compute.worker.DescribeInstancesWorker;
import com.transcend.compute.worker.RunInstancesWorker;
import com.transcend.compute.worker.TerminateInstancesWorker;

/**
 * Instance helper for non-web tests (using actions in-VM).
 *
 * @author jgardner
 *
 */
@Component
public class InstanceHelper {
    private static Logger logger = Appctx.getLogger(InstanceHelper.class
            .getName());

    private static final int MAX_WAIT_SECS = 60;
    private static final int WAIT_SECS = 2;

    private static List<String> instances = new ArrayList<String>();

    private static ActionTestHelper actionHelper = null;

    private static RunInstancesWorker runInstancesWorker = null;

    private static DescribeInstancesWorker describeInstancesWorker = null;

    private static TerminateInstancesWorker terminateInstancesWorker = null;

    private static String defaultAvailabilityZone = null;

    private static String baseImageId = null;

    private static String defaultFlavor = null;

    private static int reqCounter = 0;

    /**
     * Construct a minimal valid address request.
     *
     * @param addressName
     * @return
     */
    public static RunInstancesRequestMessage runInstancesRequest() {
        final RunInstancesRequestMessage.Builder builder =
                RunInstancesRequestMessage.newBuilder();
        builder.setTypeId(true);
        builder.setCallerAccessKey(actionHelper.getAccessKey());
        builder.setRequestId("run-"+baseImageId);
        Placement.Builder place = Placement.newBuilder();
        place.setAvailabilityZone(defaultAvailabilityZone);
        builder.setPlacement(place);
        builder.setImageId(baseImageId);
        builder.setInstanceType(defaultFlavor);
        return builder.build();
    }

    public static TerminateInstancesRequestMessage terminateInstanceRequest(String instanceId) {
        final TerminateInstancesRequestMessage.Builder builder =
                TerminateInstancesRequestMessage.newBuilder();
        builder.setTypeId(true);
        builder.setCallerAccessKey(actionHelper.getAccessKey());
        builder.setRequestId("term-"+instanceId);
        builder.addInstanceIds(instanceId);
        return builder.build();
    }

    /**
     * Run an instance (with arbitrary image ID).
     *
     */
    public static String runInstance() throws Exception {
        RunInstancesRequestMessage req = runInstancesRequest();
        RunInstancesResponseMessage result = null;
        result = runInstancesWorker.doWork(req);
        assertTrue(result.getReservation().getInstanceCount() > 0);
        instances.add(result.getReservation().getInstanceList().get(0).getInstanceId());
        return instances.get(instances.size()-1);
    }

    /**
     * Describe an instance.
     *
     */
    public static Instance describeInstance(String instanceId) throws Exception {
        DescribeInstancesResponseMessage result = null;
        DescribeInstancesRequestMessage.Builder builder =
                DescribeInstancesRequestMessage.newBuilder();
        builder.setTypeId(true);
        builder.setCallerAccessKey(actionHelper.getAccessKey());
        builder.setRequestId("desc-"+instanceId+reqCounter++);
        builder.addInstanceIds(instanceId);
        result = describeInstancesWorker.doWork(builder.build());

        return result.getReservations(0).getInstanceList().get(0);
    }

    /**
     * Wait until instance is running (or some other state).
     *
     * @param instanceId
     * @param state desired run state
     */
    public static String waitForState(String instanceId, String state) throws Exception {
        boolean done = false;
        int count;
        Instance describedInstance = null;
        for (count = 0; count < MAX_WAIT_SECS; count += WAIT_SECS) {
            try {
                describedInstance = describeInstance(instanceId);
            } catch (InstanceDoesNotExist idne) {
                // if terminating, may not see "terminated" before gone.
                if ("terminated".equals(state)) {
                    return state;
                } else {
                    throw idne;
                }
            }
            logger.info("instance state:" + describedInstance.getState());
            if (describedInstance.getState().getName().equals(state)) {
                done = true;
                break;
            }
            Thread.sleep(1000 * WAIT_SECS);
        }
        if (!done) {
            return describedInstance.getState().getName();
        }
        logger.info("Instance is " + state + " after " + count + " seconds.");
        return state;
    }

    /**
     * Terminate an instance.
     *
     * @param instanceId
     * @param client
     */
    public static void terminateInstance(String instanceId) throws Exception {
        final TerminateInstancesRequestMessage request =
                terminateInstanceRequest(instanceId);
        terminateInstancesWorker.doWork(request);
    }

    public static void terminateAllLaunchedInstances() throws Exception {
        for (String instanceId : instances) {
            final TerminateInstancesRequestMessage request =
                    terminateInstanceRequest(instanceId);
            terminateInstancesWorker.doWork(request);
        }
        instances.clear();
    }

    @Autowired(required=true)
    public void setActionTestHelper(ActionTestHelper actionTestHelper) {
        InstanceHelper.actionHelper = actionTestHelper;
    }

    @Autowired(required=true)
    public void setRunInstancesWorker(RunInstancesWorker runInstancesWorker) {
        InstanceHelper.runInstancesWorker = runInstancesWorker;
    }

    @Autowired(required=true)
    public void setDescribeInstancesWorker(DescribeInstancesWorker describeInstancesWorker) {
        InstanceHelper.describeInstancesWorker = describeInstancesWorker;
    }

    @Autowired(required=true)
    public void setTerminateInstancesWorker(TerminateInstancesWorker terminateInstancesWorker) {
        InstanceHelper.terminateInstancesWorker = terminateInstancesWorker;
    }

    @Autowired(required=true)
    public void setDefaultAvailabilityZone(String defaultAvailabilityZone) {
        InstanceHelper.defaultAvailabilityZone = defaultAvailabilityZone;
    }

    @Autowired(required=true)
    public void setBaseImageId(String baseImageId) {
        InstanceHelper.baseImageId = baseImageId;
    }

    @Autowired(required=true)
    public void setDefaultFlavor(String defaultFlavor) {
        InstanceHelper.defaultFlavor = defaultFlavor;
    }
}
