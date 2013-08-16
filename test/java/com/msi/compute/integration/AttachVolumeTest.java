package com.msi.compute.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.LinkedList;
import java.util.List;

import javax.annotation.Resource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import com.amazonaws.services.ec2.model.AttachVolumeRequest;
import com.amazonaws.services.ec2.model.AttachVolumeResult;
import com.amazonaws.services.ec2.model.CreateVolumeRequest;
import com.amazonaws.services.ec2.model.DeleteVolumeRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesRequest;
import com.amazonaws.services.ec2.model.Volume;
import com.amazonaws.services.ec2.model.VolumeAttachment;
import com.msi.compute.helper.InstanceHelper;
import com.msi.compute.helper.VolumeHelper;
import com.msi.tough.core.Appctx;

public class AttachVolumeTest extends AbstractBaseComputeTest {

    private static Logger logger = Appctx.getLogger(AttachVolumeTest.class
            .getName());

    public static final int MAX_SLEEP_SECS = 60;
    @Resource
    private VolumeHelper volumeHelper = null;

    private String volumeId = null;

    private String instanceId = null;

    @Before
    public void setUp() throws Exception {
        CreateVolumeRequest req = new CreateVolumeRequest(3, getDefaultAvailabilityZone());
        Volume volume = getComputeClientV2().createVolume(req).getVolume();
        volumeId = volume.getVolumeId();
        logger.debug("Successfully created volume " + volumeId);
        List<String> vols = new LinkedList<String>();
        vols.add(volumeId);
        DescribeVolumesRequest dReq = new DescribeVolumesRequest(vols);
        for (int secs = 0; secs < MAX_SLEEP_SECS;) {
            Volume v = getComputeClientV2().describeVolumes(dReq).getVolumes().get(0);
            logger.debug("Vol "+v.getVolumeId()+" has status \"" + v.getState() + "\"");
            if ("available".equals(v.getState())) {
                secs = 60;
                break;
            }
            Thread.sleep(1000 * 10);
            secs += 10;
        }
        logger.info("Creating instance.");
        instanceId = InstanceHelper.runInstance();
        logger.info("Created instance with ID " + instanceId);
        assertEquals("running", InstanceHelper.waitForState(instanceId, "running"));
    }

    @After
    public void tearDown() throws Exception {
        // Terminate the instance so the attachment will be eliminated
        InstanceHelper.terminateInstance(instanceId);
        assertEquals("terminated", InstanceHelper.waitForState(instanceId, "terminated"));

        List<String> vols = new LinkedList<String>();
        vols.add(volumeId);
        logger.debug("Deleting the volume with volume id, " + volumeId + ".");
        DeleteVolumeRequest req = new DeleteVolumeRequest(volumeId);
        getComputeClientV2().deleteVolume(req);
    }

    @Test
    public void testGoodAttach() throws Exception {
        AttachVolumeRequest attachVolumeRequest =
                new AttachVolumeRequest(volumeId, instanceId, "/dev/xdz");

        AttachVolumeResult result =
                getComputeClientV2().attachVolume(attachVolumeRequest);
        assertNotNull("Expect good result.", result);
        VolumeAttachment attachment = result.getAttachment();
        assertNotNull("Expect good volume attachment.", attachment);
        logger.debug("Got attachment: " + attachment.toString());
    }
}
