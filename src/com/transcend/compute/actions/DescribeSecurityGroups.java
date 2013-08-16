package com.transcend.compute.actions;

import java.util.List;
import java.util.Map;

import com.generationjava.io.xml.XMLNode;
import com.msi.tough.query.AbstractQueuedAction;
import com.msi.tough.query.ErrorResponse;
import com.msi.tough.query.QueryUtil;
import com.msi.tough.query.ServiceRequest;
import com.msi.tough.query.ServiceRequestContext;
import com.msi.tough.query.ServiceResponse;
import com.transcend.compute.message.DescribeInstancesMessage.DescribeInstancesRequestMessage.Filter;
import com.transcend.compute.message.DescribeSecurityGroupsMessage.DescribeSecurityGroupsRequestMessage;
import com.transcend.compute.message.DescribeSecurityGroupsMessage.DescribeSecurityGroupsResponseMessage;
import com.transcend.compute.message.DescribeSecurityGroupsMessage.DescribeSecurityGroupsResponseMessage.SecurityGroupInfo;
import com.transcend.compute.utils.SecurityGroupUtils;
import com.yammer.metrics.core.Meter;

public class DescribeSecurityGroups extends
		AbstractQueuedAction<DescribeSecurityGroupsRequestMessage, DescribeSecurityGroupsResponseMessage> {

	private static Map<String, Meter> meters = initMeter("Compute",
			"DescribeSecurityGroups");

	@Override
	protected void mark(Object ret, Exception e) {
		markStandard(meters, e);
	}

	public String marshall(ServiceResponse resp,
            DescribeSecurityGroupsResponseMessage result) {
		final List<SecurityGroupInfo> grps = result.getSecurityGroupInfoList();
		final XMLNode root = new XMLNode("DescribeSecurityGroupsResponse");
		root.addAttr("xmlns", "http://ec2.amazonaws.com/doc/2012-10-01/");
		QueryUtil.addNode(root, "requestId", result.getRequestId());

		SecurityGroupUtils.marshallSecurityGroups(root, grps);
		return root.toString();
	}

	private DescribeSecurityGroupsRequestMessage unmarshall(
			final Map<String, String[]> in) {
		final DescribeSecurityGroupsRequestMessage.Builder req = DescribeSecurityGroupsRequestMessage.newBuilder();
		{
			final String s = "GroupName.";
			for (int i = 1;; ++i) {
				if (!in.containsKey(s + i)) {
					break;
				}
				req.addGroupName(QueryUtil.getString(in, s + i));
			}
		}
		{
			final String s = "GroupId.";
			for (int i = 1;; ++i) {
				if (!in.containsKey(s + i)) {
					break;
				}
				req.addGroupId(QueryUtil.getString(in, s + i));
			}
		}
		{
			final String s = "Filter.";
			for (int i = 1;; i++) {
				if (!in.containsKey(s + i + ".Name")) {
					break;
				}
				Filter.Builder f = Filter.newBuilder();
				f.setName(QueryUtil.getString(in, s + i + ".Name"));
				for (int m = 1;; m++) {
					if (!in.containsKey(s + i + ".Value." + m)) {
						break;
					}
					f.addValue(QueryUtil.getString(in, s + i + ".Value." + m));
				}
			}

		}
		return req.buildPartial();
	}

	@Override
	public ServiceResponse buildResponse(ServiceResponse resp,
			DescribeSecurityGroupsResponseMessage message) {
		resp.setPayload(marshall(resp, message));
        return resp;
	}

	@Override
	public DescribeSecurityGroupsRequestMessage handleRequest(
			ServiceRequest req, ServiceRequestContext context)
			throws ErrorResponse {
		return unmarshall(req
                .getParameterMap());
	}
}
