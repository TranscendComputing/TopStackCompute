package com.transcend.compute.worker;

import java.util.Date;

import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.compute.ComputeServices;
import org.dasein.cloud.compute.VolumeSupport;
import org.slf4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import com.amazonaws.util.DateUtils;
import com.msi.tough.core.Appctx;
import com.msi.tough.dasein.DaseinHelper;
import com.msi.tough.model.AccountBean;
import com.msi.tough.query.ServiceRequestContext;
import com.msi.tough.workflow.core.AbstractWorker;
import com.transcend.compute.message.AttachVolumeMessage.AttachVolumeRequest;
import com.transcend.compute.message.AttachVolumeMessage.AttachVolumeResponse;
import com.transcend.compute.message.VolumeMessage;
import com.transcend.compute.utils.ComputeFaults;

public class AttachVolumeWorker extends
        AbstractWorker<AttachVolumeRequest,
        AttachVolumeResponse> {
    private final Logger logger = Appctx.getLogger(AttachVolumeWorker.class
            .getName());

    private DateUtils dateUtils = new DateUtils();

    /**
     * We need a local copy of this doWork to provide the transactional
     * annotation. Transaction management is handled by the annotation, which
     * can only be on a concrete class.
     * @param req
     * @return
     * @throws Exception
     */
    @Transactional
    public AttachVolumeResponse doWork(
            AttachVolumeRequest req) throws Exception {
        logger.debug("Performing work for AttachVolume.");
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
    protected AttachVolumeResponse doWork0(AttachVolumeRequest req,
            ServiceRequestContext context) throws Exception {

        final AttachVolumeResponse.Builder result =
                AttachVolumeResponse.newBuilder();
        final AccountBean account = context.getAccountBean();

        final CloudProvider cloudProvider = DaseinHelper.getProvider(
                account.getDefZone(), account.getTenant(),
                account.getAccessKey(), account.getSecretKey());

        final String instanceId = req.getInstanceId();

        // Check if instance id refers to existing instance
        if (cloudProvider.getComputeServices().getVirtualMachineSupport()
                .getVirtualMachine(instanceId) == null) {
            throw ComputeFaults.instanceDoesNotExist(instanceId);
        }
        final ComputeServices comp = cloudProvider.getComputeServices();
        final VolumeSupport volumeSupport = comp.getVolumeSupport();

        volumeSupport.attach(req.getVolumeId(),
                req.getInstanceId(), req.getDevice());

        result.setInstanceId(instanceId);
        result.setVolumeId(req.getVolumeId());
        result.setDevice(req.getDevice());
        result.setStatus(VolumeMessage.AttachStatus.ATTACHING);
        result.setAttachTime(dateUtils.formatIso8601Date(new Date()));
        return result.buildPartial();
    }
}