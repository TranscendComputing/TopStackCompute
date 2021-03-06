package com.transcend.compute.actions;

import java.util.Map;

import org.slf4j.Logger;

import com.generationjava.io.xml.XMLNode;
import com.msi.tough.core.Appctx;
import com.msi.tough.query.AbstractQueuedAction;
import com.msi.tough.query.ErrorResponse;
import com.msi.tough.query.QueryFaults;
import com.msi.tough.query.QueryUtil;
import com.msi.tough.query.ServiceRequest;
import com.msi.tough.query.ServiceRequestContext;
import com.msi.tough.query.ServiceResponse;
import com.transcend.compute.message.RevokeSecurityGroupIngressMessage.RevokeSecurityGroupIngressRequestMessage;
import com.transcend.compute.message.RevokeSecurityGroupIngressMessage.RevokeSecurityGroupIngressRequestMessage.IpPermission.Group;
import com.transcend.compute.message.RevokeSecurityGroupIngressMessage.RevokeSecurityGroupIngressRequestMessage.IpPermission.IpRange;
import com.transcend.compute.message.RevokeSecurityGroupIngressMessage.RevokeSecurityGroupIngressResponseMessage;
import com.yammer.metrics.core.Meter;

public class RevokeSecurityGroupIngress extends AbstractQueuedAction<RevokeSecurityGroupIngressRequestMessage, RevokeSecurityGroupIngressResponseMessage> {

	private final Logger logger = Appctx.getLogger(RevokeSecurityGroupIngress.class
			.getName());

	private static Map<String, Meter> meters = initMeter("Compute",
			"RevokeSecurityGroupIngress");

	@Override
	protected void mark(Object ret, Exception e) {
		markStandard(meters, e);
	}


	public String marshall(final ServiceResponse in,
			final RevokeSecurityGroupIngressResponseMessage result) {
		final XMLNode root = new XMLNode("RevokeSecurityGroupResponse");
		root.addAttr("xmlns", "http://ec2.amazonaws.com/doc/2012-10-01/");
		QueryUtil.addNode(root, "requestId", result.getRequestId());
		QueryUtil.addNode(root, "result", result.getReturn());
		return root.toString();
	}

	private RevokeSecurityGroupIngressRequestMessage unmarshall(
			final Map<String, String[]> mapIn) {
		if (!mapIn.containsKey("GroupId") && !mapIn.containsKey("GroupName")) {
			throw QueryFaults
					.MissingParameter("You must specify GroupId or GroupName");
		}
		if (mapIn.containsKey("GroupId") && mapIn.containsKey("GroupName")) {
			throw QueryFaults
					.InvalidParameterCombination("GroupId and GroupName should not be specified together.");
		}

		RevokeSecurityGroupIngressRequestMessage.Builder builder = RevokeSecurityGroupIngressRequestMessage.newBuilder();
		String groupId = QueryUtil.getString(mapIn, "GroupId");
		if(groupId != null){
			builder.setGroupId(groupId);
		}
		String groupName = QueryUtil.getString(mapIn, "GroupName");
		if(groupName != null){
			builder.setGroupName(groupName);
		}

		final String s = "IpPermissions.";
		for (int i = 1;;++i) {
			logger.debug("Looking for " + i + "th IpPermission to unmarshall.");
			if (!mapIn.containsKey(s + i + ".IpProtocol")) {
				break;
			}
			RevokeSecurityGroupIngressRequestMessage.IpPermission.Builder ipPermissionBuilder = RevokeSecurityGroupIngressRequestMessage.IpPermission.newBuilder();
			ipPermissionBuilder.setIpProtocol(QueryUtil.getString(mapIn, s + i + ".IpProtocol"));
			ipPermissionBuilder.setFromPort(QueryUtil.getInt(mapIn, s + i + ".FromPort"));
			ipPermissionBuilder.setToPort(QueryUtil.getInt(mapIn, s + i + ".ToPort"));

			for(int j = 1;;++j){
				if (!mapIn.containsKey(s + i + ".Groups." + j + ".UserId") && !mapIn.containsKey(s + i + ".Groups." + j + ".GroupName")
						&& !mapIn.containsKey(s + i + ".Groups." + j + ".GroupId")) {
					break;
				}
				Group.Builder gBuilder = RevokeSecurityGroupIngressRequestMessage.IpPermission.Group.newBuilder();
				String ip_groupId = QueryUtil.getString(mapIn, s + i + ".Groups." + j + ".GroupId");
				if(ip_groupId != null){
					gBuilder.setGroupId(ip_groupId);
				}
				String ip_groupName = QueryUtil.getString(mapIn, s + i + ".Groups." + j + ".GroupName");
				if(ip_groupName != null){
					gBuilder.setGroupName(ip_groupName);
				}
				String ip_userId = QueryUtil.getString(mapIn, s + i + ".Groups." + j + ".UserId");
				if(ip_userId != null){
					gBuilder.setUserId(ip_userId);
				}
				ipPermissionBuilder.addGroups(gBuilder);
			}

			for(int k = 1;;++k){
				if (!mapIn.containsKey(s + i + ".IpRanges." + k + ".CidrIp")) {
					break;
				}
				IpRange.Builder iprBuilder = RevokeSecurityGroupIngressRequestMessage.IpPermission.IpRange.newBuilder();
				String ip_range_cidrip = QueryUtil.getString(mapIn, s + i + ".IpRanges." + k + ".CidrIp");
				if(ip_range_cidrip != null){
					iprBuilder.setCidrIp(ip_range_cidrip);
				}
				ipPermissionBuilder.addIpRanges(iprBuilder);
			}
			builder.addIpPermissions(ipPermissionBuilder);
		}

		return builder.buildPartial();
	}

	@Override
	public ServiceResponse buildResponse(ServiceResponse resp,
			RevokeSecurityGroupIngressResponseMessage message) {
		resp.setPayload(marshall(resp, message));
		return resp;
	}

	@Override
	public RevokeSecurityGroupIngressRequestMessage handleRequest(
			ServiceRequest req, ServiceRequestContext context)
			throws ErrorResponse {
		return unmarshall(req
                .getParameterMap());
	}
}