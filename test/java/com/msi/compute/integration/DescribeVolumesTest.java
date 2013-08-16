package com.msi.compute.integration;

import java.util.Collection;
import java.util.LinkedList;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;

import com.amazonaws.services.ec2.model.DescribeVolumesRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.msi.tough.core.Appctx;

@Ignore
public class DescribeVolumesTest extends AbstractBaseComputeTest{
	
	private static Logger logger = Appctx.getLogger(DescribeVolumesTest.class
            .getName());
	
	@Before
    public void setUp() throws Exception {
    	
    }

    @After
    public void tearDown() throws Exception {
        
    }

    @Test
    public void testGoodCreate() {
    	
    	// Just run the basic test to describe all volumes till issue #800 is fixed
    	
    	DescribeVolumesRequest req = new DescribeVolumesRequest();
    	Collection<String> volumeIds = new LinkedList<String>();
    	volumeIds.add("542");
		req.setVolumeIds(volumeIds);
    	
		/*Collection<Filter> filters = new LinkedList<Filter>();
		List<String> values = new LinkedList<String>();
		values.add("6");
		filters.add(new Filter("size", values));
		req.setFilters(filters);
		*/
		DescribeVolumesResult result = getComputeClientV2().describeVolumes(req);
		logger.info("Result: " + result.toString());
    }
}
