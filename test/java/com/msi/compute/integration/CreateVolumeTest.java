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

import com.amazonaws.services.ec2.model.CreateVolumeRequest;
import com.amazonaws.services.ec2.model.DeleteVolumeRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesRequest;
import com.amazonaws.services.ec2.model.Volume;
import com.msi.compute.helper.VolumeHelper;
import com.msi.tough.core.Appctx;

public class CreateVolumeTest extends AbstractBaseComputeTest {

    private static Logger logger = Appctx.getLogger(CreateVolumeTest.class
            .getName());

    public static final int MAX_SLEEP_SECS = 60;
    @Resource
    private VolumeHelper volumeHelper = null;

    private String volumeId = null;

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    	logger.debug("Checking to see if the target volume is available state for deletion.");
    	List<String> vols = new LinkedList<String>();
    	vols.add(volumeId);
        DeleteVolumeRequest req = new DeleteVolumeRequest(volumeId);
    	getComputeClientV2().deleteVolume(req);
    }

    @Test
    public void testGoodCreate() throws Exception {
        CreateVolumeRequest req = new CreateVolumeRequest(3, getDefaultAvailabilityZone());
		Volume volume  = getComputeClientV2().createVolume(req).getVolume();
        assertNotNull(volume);
        assertNotNull(volume.getVolumeId());
        assertEquals(volume.getSize(), new Integer(3));
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
    }
}
