package com.transcend.compute.worker;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.compute.ComputeServices;
import org.dasein.cloud.compute.VolumeSupport;
import org.slf4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import com.msi.tough.core.Appctx;
import com.msi.tough.dasein.DaseinHelper;
import com.msi.tough.model.AccountBean;
import com.msi.tough.query.ServiceRequestContext;
import com.msi.tough.workflow.core.AbstractWorker;
import com.transcend.compute.message.DescribeVolumesMessage.DescribeVolumesRequestMessage;
import com.transcend.compute.message.DescribeVolumesMessage.DescribeVolumesResponseMessage;
import com.transcend.compute.message.VolumeMessage.AttachStatus;
import com.transcend.compute.message.VolumeMessage.Volume;
import com.transcend.compute.message.VolumeMessage.Volume.Attachment;
import com.transcend.compute.message.VolumeMessage.VolumeStatus;
import com.transcend.compute.utils.ComputeFaults;
import com.transcend.compute.utils.VolumeUtils;

public class DescribeVolumesWorker extends AbstractWorker<DescribeVolumesRequestMessage, DescribeVolumesResponseMessage>{

	private final static Logger logger = Appctx
			.getLogger(DescribeVolumesWorker.class.getName());

	/**
	 * We need a local copy of this doWork to provide the transactional
	 * annotation.  Transaction management is handled by the annotation, which
	 * can only be on a concrete class.
	 * @param req
	 * @return
	 * @throws Exception
	 */
	@Transactional
	public DescribeVolumesResponseMessage doWork(
			DescribeVolumesRequestMessage req) throws Exception {
		logger.debug("Performing work for DescribeVolumes.");
		return super.doWork(req, getSession());
	}

	@Override
	protected DescribeVolumesResponseMessage doWork0(
			DescribeVolumesRequestMessage request, ServiceRequestContext context)
					throws Exception {
		logger.debug("Request message - BEGIN\n" + request.toString() +"\n END");
		final DescribeVolumesResponseMessage.Builder result = DescribeVolumesResponseMessage.newBuilder();

		final AccountBean account = context.getAccountBean();
		final CloudProvider cloudProvider = DaseinHelper.getProvider(
				account.getDefZone(), account.getTenant(),
				account.getAccessKey(), account.getSecretKey());
		final ComputeServices comp = cloudProvider.getComputeServices();
		final VolumeSupport volserv = comp.getVolumeSupport();

		for (String volId : request.getVolumeIdList()) {
			if (volserv.getVolume(volId) == null) {
				throw ComputeFaults.VolumeDoesNotExist(volId);
			}
		}
		final Map<String, String> filterNameMap = createFilterNameMap();

		for (org.dasein.cloud.compute.Volume v : volserv.listVolumes()) {
			if (request.getVolumeIdList() != null
					&& request.getVolumeIdList().size() > 0) {
				if (!request.getVolumeIdList().contains(v.getProviderVolumeId())) {
					continue;
				}
			}
			if (!VolumeUtils.checkFilters(v, request.getFilterList(),
					filterNameMap)) {
				continue;
			}

			final Volume.Builder volume = Volume.newBuilder();
			//final ArrayList<Attachment> attachments = new ArrayList<Attachment>();

			volume.setAvailabilityZone(v.getProviderDataCenterId());
			volume.setCreateTime(new Date(v.getCreationTimestamp()).toString());
			volume.setIops(v.getIops());
			volume.setSize(v.getSizeInGigabytes());
			if(v.getProviderSnapshotId() != null){
				volume.setSnapshotId(v.getProviderSnapshotId());
			}
			String vmId = v.getProviderVirtualMachineId();

            VolumeStatus state = VolumeStatus.ERROR;
			// TODO: Still need values for deleteOnTermination, current state,
			// and attach time. Where do these values come from?
			if (vmId != null && !"".equals(vmId)) {
				state = VolumeStatus.IN_USE;
				Attachment.Builder attachment = Attachment.newBuilder();
				attachment.setInstanceId(vmId);
				attachment.setDevice(v.getDeviceId());
				// TODO: Placeholder status. Assuming whenever there is an
				// attachment to be shown, the volume is attached.
				attachment.setStatus(AttachStatus.ATTACHED);
				attachment.setVolumeId(v.getName());
				//attachments.add(attachment.buildPartial());
				//volume.setAttachments(attachments);
				volume.addAttachment(attachment.buildPartial());
			}
			switch (v.getCurrentState()) {
			    case PENDING:
			        state = VolumeStatus.IN_USE;
			        break;
			    case AVAILABLE:
			        state = VolumeStatus.AVAILABLE;
			        break;
			    case DELETED:
			        state = VolumeStatus.DELETED;
			        break;
			}
			volume.setStatus(state);
			volume.setVolumeId(v.getName());
			if(v.getType() == null){
				volume.setVolumeType("standard");
			}else{
				volume.setVolumeType(v.getType().toString());
			}
			result.addVolumes(volume.buildPartial());
		}
		return result.buildPartial();
	}


	private Map<String, String> createFilterNameMap() {
		final Map<String, String> filterNameMap = new HashMap<String, String>();
		filterNameMap.put("availability-zone", "providerDataCenterId");
		filterNameMap.put("create-time", "creationTimestamp");
		filterNameMap.put("size", "size");
		filterNameMap.put("snapshot-id", "providerSnapshotId");
		filterNameMap.put("status", "currentState");
		filterNameMap.put("volume-id", "name");
		// filterNameMap.put("volume-type","volumeType");
		// filterNameMap.put("attachment.attach-time", "");
		// filterNameMap.put("attachment.delete-on-termination", "")
		filterNameMap.put("attachment.device", "deviceId");
		// filterNameMap.put("attachment.status", "");
		return filterNameMap;
	}
}
