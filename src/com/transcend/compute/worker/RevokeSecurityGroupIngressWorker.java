package com.transcend.compute.worker;

import java.util.Collection;
import java.util.List;

import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.network.FirewallRule;
import org.dasein.cloud.network.FirewallSupport;
import org.dasein.cloud.network.NetworkServices;
import org.dasein.cloud.network.Protocol;
import org.slf4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import com.msi.tough.core.Appctx;
import com.msi.tough.dasein.DaseinHelper;
import com.msi.tough.model.AccountBean;
import com.msi.tough.query.QueryFaults;
import com.msi.tough.query.ServiceRequestContext;
import com.msi.tough.workflow.core.AbstractWorker;
import com.transcend.compute.message.RevokeSecurityGroupIngressMessage.RevokeSecurityGroupIngressRequestMessage;
import com.transcend.compute.message.RevokeSecurityGroupIngressMessage.RevokeSecurityGroupIngressRequestMessage.IpPermission;
import com.transcend.compute.message.RevokeSecurityGroupIngressMessage.RevokeSecurityGroupIngressRequestMessage.IpPermission.Group;
import com.transcend.compute.message.RevokeSecurityGroupIngressMessage.RevokeSecurityGroupIngressRequestMessage.IpPermission.IpRange;
import com.transcend.compute.message.RevokeSecurityGroupIngressMessage.RevokeSecurityGroupIngressResponseMessage;
import com.transcend.compute.utils.ComputeFaults;
import com.transcend.compute.utils.SecurityGroupUtils;

public class RevokeSecurityGroupIngressWorker extends AbstractWorker<RevokeSecurityGroupIngressRequestMessage, RevokeSecurityGroupIngressResponseMessage>{
	private final static Logger logger = Appctx
			.getLogger(RevokeSecurityGroupIngressWorker.class.getName());

	/**
	 * We need a local copy of this doWork to provide the transactional
	 * annotation.  Transaction management is handled by the annotation, which
	 * can only be on a concrete class.
	 * @param req
	 * @return
	 * @throws Exception
	 */
	@Transactional
	public RevokeSecurityGroupIngressResponseMessage doWork(
			RevokeSecurityGroupIngressRequestMessage req) throws Exception {
		logger.debug("Performing work for RevokeSecurityGroupIngress.");
		return super.doWork(req, getSession());
	}

	@Override
	protected RevokeSecurityGroupIngressResponseMessage doWork0(
			RevokeSecurityGroupIngressRequestMessage request,
			ServiceRequestContext context) throws Exception {
		RevokeSecurityGroupIngressResponseMessage.Builder builder = RevokeSecurityGroupIngressResponseMessage.newBuilder();
		final AccountBean account = context.getAccountBean();

		final CloudProvider cloudProvider = DaseinHelper.getProvider(
				account.getDefZone(), account.getTenant(),
				account.getAccessKey(), account.getSecretKey());
		final NetworkServices network = cloudProvider.getNetworkServices();
		final FirewallSupport fs = network.getFirewallSupport();

		final String groupToModify = (request.getGroupId() == null || "".equals(request.getGroupId())) ?
				SecurityGroupUtils.getFirewallId(fs, request.getGroupName()) : request.getGroupId();

				if ("".equals(groupToModify) || fs.getFirewall(groupToModify) == null) {
					if (request.getGroupId() != null) {
						throw ComputeFaults.GroupIdDoesNotExist(request.getGroupId());
					} else {
						throw ComputeFaults.GroupNameDoesNotExist(request
								.getGroupName());
					}
				}

				checkParameters(fs, request);

				for (final IpPermission ip : request.getIpPermissionsList()) {
					ip.getIpProtocol();
					final int toPort = ip.getToPort();
					final int fromPort = ip.getFromPort();

					final List<IpRange> ipRanges = ip.getIpRangesList();
					boolean useIpRanges = (ipRanges == null || ipRanges.size() == 0) ? false : true;
					final List<Group> groups = ip.getGroupsList();
					boolean useGroups = (groups == null || groups.size() == 0) ? false : true;

					if(!useIpRanges && !useGroups){
						throw ComputeFaults.MissingParameter("Missing source specification: include source security group or CIDR information");
					}

					for (int m = 0; m < ipRanges.size(); m++) {
						// Check if IpPermissions.n.IpRanges and IpPermissions.n.Groups
						// are both empty/null. If so, end the loop.
						if (ipRanges.get(m) == null) {
							break;
						}
						// given an Ip Range
						else if (ipRanges.get(m) != null) {
							String cidrip = ipRanges.get(m).getCidrIp();
							if(cidrip != null){
								fs.revoke(groupToModify, cidrip, Protocol.TCP, fromPort,
										toPort);
							}
						}
					}
					for (int m = 0; m < groups.size(); ++m) {
						if(groups.get(m) == null || (groups.get(m).getGroupId() == null
								&& groups.get(m).getGroupName() == null && groups.get(m).getUserId() == null)){
							break;
						}
						final Group group = groups.get(m);
						// Get the group Id from the group name if given, otherwise
						// just set to group.getGroupId
						final String groupId = group.getGroupName() != null ? SecurityGroupUtils
								.getFirewallId(fs, group.getGroupName()) : group
								.getGroupId();
								fs.revoke(groupToModify, groupId,
										Protocol.TCP, fromPort, toPort);
					}
				}

				// Not 100% sure what AWS means this value to represent, but it looks
				// like it will never be false since an error will be thrown otherwise.
				logger.debug("Completed RevokeSecurityGroupIngress; returning the response message...");
				builder.setReturn(true);
				return builder.buildPartial();
	}

	// Check all parameters for anything that will cause failure such as
	// nonexistant groups, rules not existing, etc.
	private void checkParameters(final FirewallSupport fs,
			final RevokeSecurityGroupIngressRequestMessage request) throws Exception {
		final String groupToModify = (request.getGroupId() == null || "".equals(request.getGroupId())) ?
				SecurityGroupUtils.getFirewallId(fs, request.getGroupName()) : request.getGroupId();

				if ("".equals(groupToModify) || fs.getFirewall(groupToModify) == null) {
					if (request.getGroupId() != null) {
						throw ComputeFaults.GroupIdDoesNotExist(request.getGroupId());
					} else {
						throw ComputeFaults.GroupNameDoesNotExist(request
								.getGroupName());
					}
				}
				for (final IpPermission ip : request.getIpPermissionsList()) {
					ip.getIpProtocol();
					final int toPort = ip.getToPort();
					final int fromPort = ip.getFromPort();

					// TODO: validation for protocol
					if (fromPort > toPort) {
						throw QueryFaults
						.InvalidParameterCombination("FromPort cannot be higher than toPort");
					}
					final List<IpRange> ipRanges = ip.getIpRangesList();
					final List<Group> groups = ip.getGroupsList();
					final Collection<FirewallRule> rules = fs.getRules(groupToModify);
					for (int m = 0; m < ipRanges.size(); m++) {
						// Check if IpPermissions.n.IpRanges and IpPermissions.n.Groups
						// are both empty/null. If so, end the loop since there are no
						// more IpPermissions
						if (ipRanges.get(m) == null || ipRanges.get(m).getCidrIp() == null) {
							break;
						}

						// given cidrip, check cidrip ingress doesn't already exist
						// TODO: validation for cidrip
						else if (ipRanges.get(m) != null) {
							String cidrip = ipRanges.get(m).getCidrIp();
							boolean ruleFound = false;
							for (final FirewallRule rule : rules) {
								if (cidrip.equals(rule.getSourceEndpoint().getCidr())
										&& "ingress".equals(rule.getDirection()
												.toString().toLowerCase())
												&& toPort == rule.getEndPort()
												&& fromPort == rule.getStartPort()) {
									ruleFound = true;
									break;
								}
							}
							if (!ruleFound) {
								throw ComputeFaults
								.RuleDoesNotExists("The ingress for CidrIp "
										+ ipRanges.get(m)
										+ " is not a rule for the group "
										+ fs.getFirewall(groupToModify)
										.getName() + "(Id:"
										+ groupToModify + ")");
							}
						}
						// given groupName/Id, check group exists and that the rule for
						// that group doesn't already exist

					}
					for(int n = 0;n < groups.size(); ++n){
						if (groups.get(n) == null || (groups.get(n).getGroupId() == null
								&& groups.get(n).getGroupName() == null && groups.get(n).getUserId() == null)) {
							break;
						}
						else {
							final Group group = groups.get(n);
							// Get the group Id from the group name if given, otherwise
							// just set to group.getGroupId. Will be empty string if
							// groupName doens't refer to existing group
							final String groupId = group.getGroupName() != null ? SecurityGroupUtils
									.getFirewallId(fs, group.getGroupName()) : group
									.getGroupId();
									if ("".equals(groupId) || fs.getFirewall(groupId) == null) {
										if (group.getGroupId() != null) {
											throw ComputeFaults.GroupIdDoesNotExist(group
													.getGroupId());
										} else {
											throw ComputeFaults.GroupNameDoesNotExist(group
													.getGroupName());
										}
									}
									boolean ruleFound = false;
									for (final FirewallRule rule : rules) {
										if (groupId.equals(rule.getSourceEndpoint()
												.getProviderFirewallId())
												&& "ingress".equals(rule.getDirection()
														.toString().toLowerCase())
														&& toPort == rule.getEndPort()
														&& fromPort == rule.getStartPort()) {
											ruleFound = true;
											break;
										}
									}
									if (!ruleFound) {
										throw ComputeFaults
										.RuleDoesNotExists("The ingress for group "
												+ fs.getFirewall(groupId).getName()
												+ "(Id:"
												+ groupId
												+ ")"
												+ " is not a rule in the group "
												+ fs.getFirewall(groupToModify)
												.getName() + "(Id:"
												+ groupToModify + ")");
									}
						}
					}
				}
	}

}
