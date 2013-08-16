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
import com.msi.compute.helper.CreateSecurityGroupLocalHelper;
import com.msi.tough.core.Appctx;

public class RevokeSecurityGroupIngressTest extends AbstractBaseComputeTest {
    private static Logger logger = Appctx
            .getLogger(RevokeSecurityGroupIngressTest.class.getName());

    private CreateSecurityGroupLocalHelper mHelper = null;

    private String groupId = null;

    @Before
    public void setUp() throws Exception {
        mHelper = new CreateSecurityGroupLocalHelper();
        groupId = mHelper.createSecurityGroup();
        logger.debug("Setting up the security group ingresses ");
        AuthorizeSecurityGroupIngressRequest req = new AuthorizeSecurityGroupIngressRequest();
        req.setGroupId(groupId);
        Collection<IpPermission> ipPermissions = new LinkedList<IpPermission>();
        IpPermission ip = new IpPermission();
        ip.setFromPort(1010);
        ip.setToPort(1010);
        ip.setIpProtocol("tcp");

        Collection<String> ranges = new LinkedList<String>();
        ranges.add("1.2.3.4/16");
        ip.setIpRanges(ranges);

        ipPermissions.add(ip);

        IpPermission ip2 = new IpPermission();
        ip2.setFromPort(1012);
        ip2.setToPort(1012);
        ip2.setIpProtocol("tcp");

        Collection<UserIdGroupPair> groups = new LinkedList<UserIdGroupPair>();
        UserIdGroupPair group = new UserIdGroupPair();
        group.setGroupName("default");
        groups.add(group);
        ip2.setUserIdGroupPairs(groups);

        ipPermissions.add(ip2);

        req.setIpPermissions(ipPermissions);
        getComputeClientV2().authorizeSecurityGroupIngress(req);
        logger.debug("Setting up the test resources completed!");
    }

    @After
    public void cleanupCreated() throws Exception {
        mHelper.deleteAllSecurityGroups();
        mHelper = null;
    }

    @Test
    public void testRevokeCidrIngress() {
        logger.debug("testRevokeCidrIngress is to be tested.");
        RevokeSecurityGroupIngressRequest req = new RevokeSecurityGroupIngressRequest();

        req.setGroupId(groupId);
        Collection<IpPermission> ipPermissions = new LinkedList<IpPermission>();
        IpPermission ip = new IpPermission();
        ip.setFromPort(1010);
        ip.setToPort(1010);
        ip.setIpProtocol("tcp");

        Collection<String> ranges = new LinkedList<String>();
        ranges.add("1.2.3.4/16");
        ip.setIpRanges(ranges);

        ipPermissions.add(ip);
        req.setIpPermissions(ipPermissions);

        getComputeClientV2().revokeSecurityGroupIngress(req);
        logger.debug("testRevokeCidrIngress test completed!");
    }

    @Test
    public void testRevokeGroupIngress() {
        logger.debug("testRevokeGroupIngress is to be tested.");
        RevokeSecurityGroupIngressRequest req = new RevokeSecurityGroupIngressRequest();

        req.setGroupId(groupId);
        Collection<IpPermission> ipPermissions = new LinkedList<IpPermission>();
        IpPermission ip = new IpPermission();
        ip.setFromPort(1012);
        ip.setToPort(1012);
        ip.setIpProtocol("tcp");

        Collection<UserIdGroupPair> groups = new LinkedList<UserIdGroupPair>();
        UserIdGroupPair group = new UserIdGroupPair();
        group.setGroupName("default");
        groups.add(group);
        ip.setUserIdGroupPairs(groups);

        ipPermissions.add(ip);
        req.setIpPermissions(ipPermissions);

        getComputeClientV2().revokeSecurityGroupIngress(req);
        logger.debug("testRevokeGroupIngress test completed!");
    }
}
