package com.msi.compute.helper;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.msi.tough.core.Appctx;
import com.msi.tough.query.ActionTestHelper;
import com.transcend.compute.message.CreateVolumeMessage.CreateVolumeRequestMessage;
import com.transcend.compute.message.CreateVolumeMessage.CreateVolumeResponseMessage;
import com.transcend.compute.message.DeleteVolumeMessage.DeleteVolumeRequestMessage;
import com.transcend.compute.message.DescribeVolumesMessage.DescribeVolumesRequestMessage;
import com.transcend.compute.message.DescribeVolumesMessage.DescribeVolumesResponseMessage;
import com.transcend.compute.message.VolumeMessage.AttachStatus;
import com.transcend.compute.message.VolumeMessage.Volume;
import com.transcend.compute.message.VolumeMessage.VolumeStatus;
import com.transcend.compute.worker.CreateVolumeWorker;
import com.transcend.compute.worker.DeleteVolumeWorker;
import com.transcend.compute.worker.DescribeVolumesWorker;

@Component
public class VolumeLocalHelper {
    private static Logger logger = Appctx.getLogger(VolumeLocalHelper.class
            .getName());

    private static final int MAX_WAIT_SECS = 60;
    private static final int WAIT_SECS = 2;

    private Map<String,Volume> volumes =
            new HashMap<String,Volume>();

    @Resource
    private ActionTestHelper actionTestHelper = null;

    @Resource
    private String defaultAvailabilityZone = null;

    @Resource
    private CreateVolumeWorker createVolumeWorker = null;

    @Resource
    private DescribeVolumesWorker describeVolumesWorker = null;

    @Resource
    private DeleteVolumeWorker deleteVolumeWorker = null;

    private static int reqCounter = 0;

    /**
     * Construct a minimal valid create volume request.
     *
     * @param size
     * @return
     */
    public CreateVolumeRequestMessage createVolumeRequest(int size) {
        final CreateVolumeRequestMessage.Builder request =
                CreateVolumeRequestMessage.newBuilder();
        request.setTypeId(true);
        request.setCallerAccessKey(actionTestHelper.getAccessKey());
        request.setRequestId("crVol-" + reqCounter++);
        request.setSize(size);
        request.setAvailabilityZone(defaultAvailabilityZone);
        return request.build();
    }


    /**
     * Create a volume.
     *
     * @param size
     * @return
     * @return
     */
    public Volume createVolume(int size) throws Exception {
        CreateVolumeResponseMessage result = null;
        final CreateVolumeRequestMessage request =
                createVolumeRequest(size);
        result = createVolumeWorker.doWork(request);
        Volume.Builder volume = Volume.newBuilder();
        volume.setVolumeId(result.getVolumeId());
        volume.setVolumeType(result.getVolumeType());
        volume.setSize(result.getSize());
        volume.setCreateTime(result.getCreateTime());
        volume.setStatus(VolumeStatus.CREATING);
        volume.setAvailabilityZone(result.getAvailabilityZone());
        volumes.put(result.getVolumeId(), volume.build());
        return volume.build();
    }

    /**
     * Construct a describe volume request.
     *
     * @param volumeId
     * @return
     */
    public DescribeVolumesRequestMessage describeVolumeRequest(String volumeId) {
        final DescribeVolumesRequestMessage.Builder request =
                DescribeVolumesRequestMessage.newBuilder();
        request.setTypeId(true);
        request.setCallerAccessKey(actionTestHelper.getAccessKey());
        request.setRequestId("dsVol-" + reqCounter++);
        request.addVolumeId(volumeId);
        return request.build();
    }

    /**
     * Describe a volume.
     *
     * @param volumeId
     */
    public Volume describeVolume(String volumeId) throws Exception {
        DescribeVolumesRequestMessage describeVolumeRequest =
                describeVolumeRequest(volumeId);
        DescribeVolumesResponseMessage result =
                describeVolumesWorker.doWork(describeVolumeRequest);
        return result.getVolumes(0);
    }

    /**
     * Wait until volume reaches some expected state.
     *
     * @param volumeId
     * @param state desired state
     */
    public AttachStatus waitForState(String volumeId, AttachStatus state) throws Exception {
        boolean done = false;
        int count;
        Volume describedVolume = null;
        for (count = 0; count < MAX_WAIT_SECS; count += WAIT_SECS) {
            describedVolume = describeVolume(volumeId);
            logger.info("describeVolume result:" + describedVolume);
            if (describedVolume.getAttachmentCount() > 0 &&
                    describedVolume.getAttachment(0).getStatus().equals(state)) {
                done = true;
                break;
            }
            Thread.sleep(1000 * WAIT_SECS);
        }
        if (!done) {
            if (describedVolume.getAttachmentCount() == 0) {
                return AttachStatus.DETACHED;
            }
            return describedVolume.getAttachment(0).getStatus();
        }
        logger.info("Instance is " + state + " after " + count + " seconds.");
        return state;
    }
    /**
     * Construct a delete volume request.
     *
     * @param volumeId
     * @return
     */
    public DeleteVolumeRequestMessage deleteVolumeRequest(String volumeId) {
        final DeleteVolumeRequestMessage.Builder request =
                DeleteVolumeRequestMessage.newBuilder();
        request.setTypeId(true);
        request.setCallerAccessKey(actionTestHelper.getAccessKey());
        request.setRequestId("dlVol-" + reqCounter++);
        request.setVolumeId(volumeId);
        return request.build();
    }

    /**
     * Delete an account with the given access key.
     *
     * @param volumeId
     */
    public void deleteVolume(String volumeId) throws Exception {
        DeleteVolumeRequestMessage deleteVolumeRequest = deleteVolumeRequest(volumeId);
        deleteVolumeWorker.doWork(deleteVolumeRequest);
        volumes.remove(volumeId);
    }

    /**
     * Delete all accounts created by tests (for test-end cleanup).
     */
    public void deleteAllCreatedVolumes() throws Exception {
        for (String volumeId : volumes.keySet()) {
            deleteVolume(volumeId);
        }
        volumes.clear();
    }
}
