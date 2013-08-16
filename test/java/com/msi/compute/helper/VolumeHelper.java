package com.msi.compute.helper;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateVolumeRequest;
import com.amazonaws.services.ec2.model.CreateVolumeResult;
import com.amazonaws.services.ec2.model.DeleteVolumeRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.amazonaws.services.ec2.model.Volume;
import com.msi.tough.query.ActionTestHelper;

@Component
public class VolumeHelper {

    private Map<String,Volume> volumes =
            new HashMap<String,Volume>();

    @Resource
    private ActionTestHelper actionTestHelper = null;

    @Resource
    private String defaultAvailabilityZone = null;

    @Resource
    private AmazonEC2Client computeClient;

    /**
     * Construct a minimal valid create volume request.
     *
     * @param size
     * @return
     */
    public CreateVolumeRequest createVolumeRequest(int size) {
        final CreateVolumeRequest request = new CreateVolumeRequest();
        request.setSize(size);
        request.setAvailabilityZone(defaultAvailabilityZone);
        return request;
    }


    /**
     * Create a volume.
     *
     * @param size
     * @return
     * @return
     */
    public Volume createVolume(int size) throws Exception {
        CreateVolumeResult result = null;
        final CreateVolumeRequest request =
                createVolumeRequest(size);
        result = computeClient.createVolume(request);
        volumes.put(result.getVolume().getVolumeId(), result.getVolume());
        return result.getVolume();
    }

    /**
     * Construct a describe volume request.
     *
     * @param volumeId
     * @return
     */
    public DescribeVolumesRequest describeVolumeRequest(String volumeId) {
        final DescribeVolumesRequest request = new DescribeVolumesRequest()
            .withVolumeIds(volumeId);
        return request;
    }

    /**
     * Describe an account with the given access key.
     *
     * @param volumeId
     */
    public Volume describeVolume(String volumeId) throws Exception {
        DescribeVolumesRequest describeVolumeRequest = describeVolumeRequest(volumeId);
        DescribeVolumesResult result =
                computeClient.describeVolumes(describeVolumeRequest);
        return result.getVolumes().get(0);
    }

    /**
     * Construct a delete volume request.
     *
     * @param volumeId
     * @return
     */
    public DeleteVolumeRequest deleteVolumeRequest(String volumeId) {
        final DeleteVolumeRequest request = new DeleteVolumeRequest();
        request.setVolumeId(volumeId);
        return request;
    }

    /**
     * Delete an account with the given access key.
     *
     * @param volumeId
     */
    public void deleteVolume(String volumeId) throws Exception {
        DeleteVolumeRequest deleteVolumeRequest = deleteVolumeRequest(volumeId);
        computeClient.deleteVolume(deleteVolumeRequest);
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
