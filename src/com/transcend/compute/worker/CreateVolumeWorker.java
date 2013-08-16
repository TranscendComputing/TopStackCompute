package com.transcend.compute.worker;

import java.util.Date;

import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.compute.ComputeServices;
import org.dasein.cloud.compute.VolumeCreateOptions;
import org.dasein.cloud.compute.VolumeSupport;
import org.dasein.util.uom.storage.Gigabyte;
import org.dasein.util.uom.storage.Storage;
import org.slf4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import com.msi.tough.core.Appctx;
import com.msi.tough.dasein.DaseinHelper;
import com.msi.tough.model.AccountBean;
import com.msi.tough.query.QueryFaults;
import com.msi.tough.query.ServiceRequestContext;
import com.msi.tough.workflow.core.AbstractWorker;
import com.transcend.compute.message.CreateVolumeMessage.CreateVolumeRequestMessage;
import com.transcend.compute.message.CreateVolumeMessage.CreateVolumeResponseMessage;

public class CreateVolumeWorker extends AbstractWorker<CreateVolumeRequestMessage, CreateVolumeResponseMessage>{

	private final Logger logger = Appctx
			.getLogger(CreateVolumeWorker.class.getName());
	
	/**
     * We need a local copy of this doWork to provide the transactional
     * annotation.  Transaction management is handled by the annotation, which
     * can only be on a concrete class.
     * @param req
     * @return
     * @throws Exception
     */
    @Transactional
    public CreateVolumeResponseMessage doWork(
    		CreateVolumeRequestMessage req) throws Exception {
        logger.debug("Performing work for CreateVolume.");
        return super.doWork(req, getSession());
    }
	
	@Override
	protected CreateVolumeResponseMessage doWork0(
			CreateVolumeRequestMessage request, ServiceRequestContext context)
			throws Exception {
		String volumeType = request.getVolumeType();
		if (volumeType != null && !volumeType.equals("standard")
				&& !volumeType.equals("io1") && !volumeType.equals("")) {
			throw QueryFaults.InvalidParameterValue();
		}
		if(volumeType == null || volumeType.equals("")){
			volumeType = "standard";
		}

		// call the service
		final AccountBean account = context.getAccountBean();
		final CloudProvider cloudProvider = DaseinHelper.getProvider(
				request.getAvailabilityZone(), account.getTenant(),
				account.getAccessKey(), account.getSecretKey());
		final ComputeServices comp = cloudProvider.getComputeServices();
		final VolumeSupport volserv = comp.getVolumeSupport();

		VolumeCreateOptions options = null;
		Storage<Gigabyte> storage = null;
		if (request.getSize() != 0) {
			storage = new Storage<Gigabyte>(request.getSize(), Storage.GIGABYTE);
		}
		if (request.getSnapshotId() != null && !"".equals(request.getSnapshotId())) {
			options = VolumeCreateOptions.getInstanceForSnapshot(
					request.getSnapshotId(), storage, "VOL", "VOL");
		} else {
			options = VolumeCreateOptions.getInstance(storage, "VOL", "VOL");
		}
		options.inDataCenter(request.getAvailabilityZone());
		final String volId = volserv.createVolume(options);
		final org.dasein.cloud.compute.Volume v = volserv.getVolume(volId);
		final CreateVolumeResponseMessage.Builder result = CreateVolumeResponseMessage.newBuilder();
		result.setAvailabilityZone(v.getProviderDataCenterId());
		result.setCreateTime(new Date(v.getCreationTimestamp()).toString());
		result.setIops(v.getIops());	
		result.setSize(v.getSizeInGigabytes());
		if(v.getProviderSnapshotId() != null){
			result.setSnapshotId(v.getProviderSnapshotId());	
		}
		final String state = v.getProviderVirtualMachineId() != null ? "attached"
				: v.getCurrentState().toString();
		result.setStatus(state);
		result.setVolumeId(volId);
		result.setVolumeType(volumeType);
		return result.buildPartial();
	}

}
