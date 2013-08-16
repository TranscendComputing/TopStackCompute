package com.transcend.compute.worker;

import org.dasein.cloud.CloudException;
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
import com.transcend.compute.message.DeleteVolumeMessage.DeleteVolumeRequestMessage;
import com.transcend.compute.message.DeleteVolumeMessage.DeleteVolumeResponseMessage;
import com.transcend.compute.utils.ComputeFaults;

public class DeleteVolumeWorker extends AbstractWorker<DeleteVolumeRequestMessage, DeleteVolumeResponseMessage>{
	private final Logger logger = Appctx.getLogger(DeleteVolumeWorker.class
			.getName());
	
	/**
     * We need a local copy of this doWork to provide the transactional
     * annotation.  Transaction management is handled by the annotation, which
     * can only be on a concrete class.
     * @param req
     * @return
     * @throws Exception
     */
    @Transactional
    public DeleteVolumeResponseMessage doWork(
    		DeleteVolumeRequestMessage req) throws Exception {
        logger.debug("Performing work for DeleteVolume.");
        return super.doWork(req, getSession());
    }
	
	@Override
	protected DeleteVolumeResponseMessage doWork0(
			DeleteVolumeRequestMessage request, ServiceRequestContext context)
			throws Exception {
		final AccountBean account = context.getAccountBean();

		final CloudProvider cloudProvider = DaseinHelper.getProvider(
				account.getDefZone(), account.getTenant(),
				account.getAccessKey(), account.getSecretKey());
		final ComputeServices comp = cloudProvider.getComputeServices();
		final VolumeSupport volServ = comp.getVolumeSupport();

		final String volId = request.getVolumeId();

		if (volServ.getVolume(volId) == null) {
			throw ComputeFaults.VolumeDoesNotExist(volId);
		} else {
			try {
				volServ.remove(volId);
			} catch (CloudException e) {
				throw ComputeFaults.VolumeCannotBeDeleted(volId);
			}
		}
		
		DeleteVolumeResponseMessage.Builder builder = DeleteVolumeResponseMessage.newBuilder();
		// Not 100% sure what AWS means this value to represent, but it looks
		// like it will never be false since an error will be thrown otherwise.
		builder.setReturn(true);
		return builder.buildPartial();
	}
}
