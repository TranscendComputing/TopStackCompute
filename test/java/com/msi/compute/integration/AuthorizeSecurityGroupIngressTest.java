package com.msi.compute.integration;

import java.util.Collection;
import java.util.LinkedList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.RevokeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.UserIdGroupPair;
import com.msi.tough.core.Appctx;

public class AuthorizeSecurityGroupIngressTest extends AbstractBaseComputeTest {
    private static Logger logger = Appctx.getLogger(AuthorizeSecurityGroupIngressTest.class
            .getName());

    private IpPermission ipPermission = null;

    @Before
    public void setUp() throws Exception {
		// nothing to do here
    }

	@After
    public void tearDown() throws Exception {
		logger.debug("Cleaning up the security group ingresses ");
		RevokeSecurityGroupIngressRequest req = new RevokeSecurityGroupIngressRequest();

		req.setGroupName("transcend-topstack");
		Collection<IpPermission> ipPermissions = new LinkedList<IpPermission>();
		ipPermissions.add(ipPermission);
		req.setIpPermissions(ipPermissions);

		getComputeClientV2().revokeSecurityGroupIngress(req);
		logger.debug("Cleaning up the test resources completed!");
	}

	@Test
	public void testAuthCidr(){
		logger.debug("AuthorizeSecurityGroupIngress is to be tested.");
		AuthorizeSecurityGroupIngressRequest req = new AuthorizeSecurityGroupIngressRequest();
		req.setGroupName("transcend-topstack");
		Collection<IpPermission> ipPermissions = new LinkedList<IpPermission>();
		ipPermission = new IpPermission();
		ipPermission.setFromPort(80);
		ipPermission.setToPort(80);
		ipPermission.setIpProtocol("tcp");

		Collection<String> ranges = new LinkedList<String>();
		ranges.add("0.0.0.0/0");
		ipPermission.setIpRanges(ranges);

		ipPermissions.add(ipPermission);
		req.setIpPermissions(ipPermissions);
		getComputeClientV2().authorizeSecurityGroupIngress(req);
		logger.debug("testAuthCidr test completed!");

		// TODO call DescribeSecurityGroups to check if authorization was successful
	}

    @Test
    public void testAuthGroup(){

        logger.debug("AuthorizeSecurityGroupIngress is to be tested.");
        AuthorizeSecurityGroupIngressRequest req = new AuthorizeSecurityGroupIngressRequest();
        req.setGroupName("transcend-topstack");
        Collection<IpPermission> ipPermissions = new LinkedList<IpPermission>();
        ipPermission = new IpPermission();
        ipPermission.setFromPort(82);
        ipPermission.setToPort(82);
        ipPermission.setIpProtocol("tcp");

        Collection<UserIdGroupPair> groups = new LinkedList<UserIdGroupPair>();
        UserIdGroupPair group = new UserIdGroupPair();
        group.setGroupName("default");
        groups.add(group);
        ipPermission.setUserIdGroupPairs(groups);

        ipPermissions.add(ipPermission);
        req.setIpPermissions(ipPermissions);

        getComputeClientV2().authorizeSecurityGroupIngress(req);
        logger.debug("testAuthGroup test completed!");

        // TODO call DescribeSecurityGroups to check if authorization was successful
    }
}
