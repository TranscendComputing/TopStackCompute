package com.transcend.compute.worker;

import java.util.Collection;
import java.util.List;

import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.network.Firewall;
import org.dasein.cloud.network.FirewallRule;
import org.dasein.cloud.network.FirewallSupport;
import org.dasein.cloud.network.NetworkServices;
import org.dasein.cloud.network.RuleTarget;
import org.slf4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import com.msi.tough.core.Appctx;
import com.msi.tough.dasein.DaseinHelper;
import com.msi.tough.model.AccountBean;
import com.msi.tough.query.ServiceRequestContext;
import com.msi.tough.workflow.core.AbstractWorker;
import com.transcend.compute.message.DescribeSecurityGroupsMessage.DescribeSecurityGroupsRequestMessage;
import com.transcend.compute.message.DescribeSecurityGroupsMessage.DescribeSecurityGroupsRequestMessage.Filter;
import com.transcend.compute.message.DescribeSecurityGroupsMessage.DescribeSecurityGroupsResponseMessage;
import com.transcend.compute.message.DescribeSecurityGroupsMessage.DescribeSecurityGroupsResponseMessage.SecurityGroupInfo;
import com.transcend.compute.message.DescribeSecurityGroupsMessage.DescribeSecurityGroupsResponseMessage.SecurityGroupInfo.IpPermission;
import com.transcend.compute.message.DescribeSecurityGroupsMessage.DescribeSecurityGroupsResponseMessage.SecurityGroupInfo.IpRange;

public class DescribeSecurityGroupsWorker
        extends
        AbstractWorker<DescribeSecurityGroupsRequestMessage, DescribeSecurityGroupsResponseMessage> {
    private final Logger logger = Appctx
            .getLogger(DescribeSecurityGroupsWorker.class.getName());

    /**
     * We need a local copy of this doWork to provide the transactional
     * annotation. Transaction management is handled by the annotation, which
     * can only be on a concrete class.
     *
     * @param req
     * @return
     * @throws Exception
     */
    @Transactional
    public DescribeSecurityGroupsResponseMessage doWork(
            DescribeSecurityGroupsRequestMessage req) throws Exception {
        logger.debug("Performing work for AllocateAddress.");
        return super.doWork(req, getSession());
    }

    @Override
    protected DescribeSecurityGroupsResponseMessage doWork0(
            DescribeSecurityGroupsRequestMessage request,
            ServiceRequestContext context) throws Exception {

        final DescribeSecurityGroupsResponseMessage.Builder result = DescribeSecurityGroupsResponseMessage
                .newBuilder();

        final AccountBean account = context.getAccountBean();
        final CloudProvider cloudProvider = DaseinHelper.getProvider(
                account.getDefZone(), account.getTenant(),
                account.getAccessKey(), account.getSecretKey());
        final NetworkServices network = cloudProvider.getNetworkServices();
        final FirewallSupport fs = network.getFirewallSupport();

        for (final Firewall fw : fs.list()) {
            final SecurityGroupInfo.Builder sg = SecurityGroupInfo.newBuilder();
            sg.setGroupDescription(fw.getDescription());
            sg.setGroupId(fw.getProviderFirewallId());
            sg.setGroupName(fw.getName());

            Collection<FirewallRule> rules = null;
            rules = fs.getRules(fw.getProviderFirewallId());

            for (final FirewallRule r : rules) {
                final IpPermission.Builder p = IpPermission.newBuilder();
                p.setFromPort(r.getStartPort());
                p.setToPort(r.getEndPort());
                RuleTarget sourceEndpoint = r.getSourceEndpoint();
                switch (sourceEndpoint.getRuleTargetType()) {
                case CIDR:
                    IpRange.Builder ipRange = IpRange.newBuilder();
                    ipRange.setCidrIp(sourceEndpoint.getCidr());
                    p.addIpRange(ipRange);
                    p.setIpProtocol(r.getProtocol().toString());
                    sg.addIpPermission(p.buildPartial());
                    break;
                default:
                    logger.warn("Unhandled source type:"
                            + sourceEndpoint.getRuleTargetType());
                }
            }

            // sg.setIpPermissionsEgress(ipPermissionsEgress);
            sg.setOwnerId(account.getTenant());
            // sg.setTags(tags);
            // sg.setVpcId(vpcId);

            if (request.getGroupIdList() != null
                    && request.getGroupIdList().size() > 0) {
                boolean sw = false;
                for (final String i : request.getGroupIdList()) {
                    if (i.equals("" + fw.getProviderFirewallId())) {
                        sw = true;
                        break;
                    }
                }
                if (!sw) {
                    continue;
                }
            }

            if (request.getGroupNameList() != null
                    && request.getGroupNameList().size() > 0) {
                boolean sw = false;
                for (final String i : request.getGroupNameList()) {
                    if (i.equals(fw.getName())) {
                        sw = true;
                        break;
                    }
                }
                if (!sw) {
                    continue;
                }
            }

            if (request.getFilterList() != null
                    && request.getFilterList().size() > 0) {
                boolean sw = true;
                for (final Filter f : request.getFilterList()) {
                    if (!sw) {
                        break;
                    }
                    final String nm = f.getName();
                    final List<String> vs = f.getValueList();
                    if (nm.equals("ip-permission.cidr")) {
                        for (final IpPermission p : sg.getIpPermissionList()) {
                            if (!vs.contains(p.getIpRangeList().get(0))) {
                                sw = false;
                                break;
                            }
                        }
                    }
                }
                if (!sw) {
                    continue;
                }
            }

            // sgs.add(sg);
            result.addSecurityGroupInfo(sg);
        }
        // result.setSecurityGroups(sgs);
        return result.buildPartial();
    }
}
