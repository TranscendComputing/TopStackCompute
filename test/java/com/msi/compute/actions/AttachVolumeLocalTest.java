package com.msi.compute.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.LinkedList;
import java.util.List;

import javax.annotation.Resource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import com.msi.compute.helper.InstanceHelper;
import com.msi.compute.helper.VolumeLocalHelper;
import com.msi.compute.integration.AbstractBaseComputeTest;
import com.msi.tough.core.Appctx;
import com.transcend.compute.message.AttachVolumeMessage.AttachVolumeRequest;
import com.transcend.compute.message.AttachVolumeMessage.AttachVolumeResponse;
import com.transcend.compute.message.VolumeMessage.AttachStatus;
import com.transcend.compute.message.VolumeMessage.Volume;
import com.transcend.compute.message.VolumeMessage.VolumeStatus;
import com.transcend.compute.worker.AttachVolumeWorker;

public class AttachVolumeLocalTest extends AbstractBaseComputeTest {

    private static Logger logger = Appctx.getLogger(AttachVolumeLocalTest.class
            .getName());

    public static final int MAX_SLEEP_SECS = 60;
    public static final int SLEEP_SECS = 10;

    private static int reqCounter = 0;

    @Resource
    private VolumeLocalHelper volumeLocalHelper = null;

    @Resource
    private AttachVolumeWorker attachVolumeWorker = null;

    private String volumeId = null;

    private String instanceId = null;

    @Before
    public void setUp() throws Exception {
        Volume volume = volumeLocalHelper.createVolume(1);
        volumeId = volume.getVolumeId();
        logger.debug("Successfully created volume " + volumeId);
        List<String> vols = new LinkedList<String>();
        vols.add(volumeId);
        for (int secs = 0; secs < MAX_SLEEP_SECS;) {
            Volume foundVolume = volumeLocalHelper.describeVolume(volumeId);
            logger.debug("Vol "+foundVolume.getVolumeId()+" has status \"" +
                    foundVolume.getStatus() + "\"");
            if (VolumeStatus.AVAILABLE.equals(foundVolume.getStatus())) {
                break;
            }
            Thread.sleep(1000 * SLEEP_SECS);
            secs += SLEEP_SECS;
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
        volumeLocalHelper.deleteVolume(volumeId);
    }

    @Test
    public void testGoodAttach() throws Exception {
        AttachVolumeRequest.Builder attachVolumeRequest =
                AttachVolumeRequest.newBuilder();
        attachVolumeRequest.setTypeId(true);
        attachVolumeRequest.setCallerAccessKey(getCreds().getAWSAccessKeyId());
        attachVolumeRequest.setRequestId("atVol-" + reqCounter++);
        attachVolumeRequest.setVolumeId(volumeId);
        attachVolumeRequest.setInstanceId(instanceId);
        attachVolumeRequest.setDevice("/dev/xdz");

        AttachVolumeResponse result =
                attachVolumeWorker.doWork(attachVolumeRequest.build());
        assertNotNull("Expect good result.", result);
        assertNotNull("Expect good volume attachment.", result.getStatus());
        logger.debug("Got attachment: " + result.toString());
        assertEquals(AttachStatus.ATTACHING, result.getStatus());
        AttachStatus actual = volumeLocalHelper.waitForState(volumeId,
                AttachStatus.ATTACHED);
        assertEquals(AttachStatus.ATTACHED, actual);
    }
}
