package com.transcend.compute.worker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.compute.ComputeServices;
import org.dasein.cloud.compute.VirtualMachineSupport;
import org.slf4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import com.msi.tough.core.Appctx;
import com.msi.tough.dasein.DaseinHelper;
import com.msi.tough.model.AccountBean;
import com.msi.tough.query.ServiceRequestContext;
import com.msi.tough.workflow.core.AbstractWorker;
import com.transcend.compute.message.TerminateInstancesMessage.TerminateInstancesRequestMessage;
import com.transcend.compute.message.TerminateInstancesMessage.TerminateInstancesResponseMessage;
import com.transcend.compute.message.TerminateInstancesMessage.TerminateInstancesResponseMessage.InstanceStateChange;
import com.transcend.compute.message.TerminateInstancesMessage.TerminateInstancesResponseMessage.InstanceStateChange.InstanceState;
import com.transcend.compute.utils.ComputeFaults;
import com.transcend.compute.utils.InstanceUtils;
import com.yammer.metrics.core.Meter;

public class TerminateInstancesWorker
        extends
        AbstractWorker<TerminateInstancesRequestMessage, TerminateInstancesResponseMessage> {
    private final Logger logger = Appctx
            .getLogger(TerminateInstancesWorker.class.getName());

    private static Map<String, Meter> meters = initMeter("Compute",
            "TerminateInstances");

    @Override
    protected void mark(TerminateInstancesResponseMessage ret, Exception e) {
        markStandard(meters, e);
    }


    /**
     * We need a local copy of this doWork to provide the transactional
     * annotation. Transaction management is handled by the annotation, which
     * can only be on a concrete class.
     *
     * @param req
     * @return
     * @throws Exception
     */
    @Transactional
    public TerminateInstancesResponseMessage doWork(
            TerminateInstancesRequestMessage req) throws Exception {
        return super.doWork(req, getSession());
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.msi.tough.workflow.core.AbstractWorker#doWork0(com.google.protobuf
     * .Message, com.msi.tough.query.ServiceRequestContext)
     */
    @Override
    protected TerminateInstancesResponseMessage doWork0(
            TerminateInstancesRequestMessage req, ServiceRequestContext context)
            throws Exception {
        final AccountBean account = context.getAccountBean();

        final CloudProvider cloudProvider = DaseinHelper.getProvider(
                account.getDefZone(), account.getTenant(),
                account.getAccessKey(), account.getSecretKey());

        final TerminateInstancesResponseMessage.Builder result = TerminateInstancesResponseMessage
                .newBuilder();

        final ComputeServices comp = cloudProvider.getComputeServices();
        final VirtualMachineSupport vmServ = comp.getVirtualMachineSupport();

        for (String vmId : req.getInstanceIdsList()) {
            if (vmServ.getVirtualMachine(vmId) == null) {
                throw ComputeFaults.instanceDoesNotExist(vmId);
            }
        }

        final List<InstanceStateChange> terminatingInstances = new ArrayList<InstanceStateChange>();
        for (String vmId : req.getInstanceIdsList()) {
            final InstanceStateChange.Builder stateChange =
                    InstanceStateChange.newBuilder();
            final InstanceState.Builder currentState = InstanceState.newBuilder();
            final InstanceState.Builder previousState = InstanceState.newBuilder();
            logger.info("Attempting to shut down instance " + vmId);

            previousState.setName(vmServ.getVirtualMachine(vmId)
                    .getCurrentState().toString().toLowerCase());
            previousState.setCode(InstanceUtils.getStateCode(previousState
                    .getName()));
            vmServ.terminate(vmId);

            // Status should always be shutting-down, but there may be a reason
            // to not hard code it like this.
            // At least in Openstack, termination happens so quickly, it's hard
            // to get the status between "running" and when the instance is shut
            // down.
            currentState.setName("shutting-down");
            currentState.setCode(32);

            stateChange.setCurrentState(currentState);
            stateChange.setPreviousState(previousState);
            stateChange.setInstanceId(vmId);
            terminatingInstances.add(stateChange.build());
        }
        result.addAllTerminatingInstances(terminatingInstances);
        return result.buildPartial();
    }

}