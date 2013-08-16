package com.transcend.compute.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.generationjava.io.xml.XMLNode;
import com.msi.tough.query.AbstractQueuedAction;
import com.msi.tough.query.ErrorResponse;
import com.msi.tough.query.QueryUtil;
import com.msi.tough.query.ServiceRequest;
import com.msi.tough.query.ServiceRequestContext;
import com.msi.tough.query.ServiceResponse;
import com.transcend.compute.message.DescribeInstancesMessage.DescribeInstancesRequestMessage;
import com.transcend.compute.message.DescribeInstancesMessage.DescribeInstancesRequestMessage.Filter;
import com.transcend.compute.message.DescribeInstancesMessage.DescribeInstancesResponseMessage;
import com.transcend.compute.utils.InstanceUtils;
import com.yammer.metrics.core.Meter;

@Component
public class DescribeInstancesV2
		extends
		AbstractQueuedAction<DescribeInstancesRequestMessage,
		DescribeInstancesResponseMessage> {

	private static Map<String, Meter> meters = initMeter("Compute",
			"DescribeInstanceV2");

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.msi.tough.query.AbstractQueuedAction#buildResponse(com.msi.tough.
	 * query.ServiceResponse, com.google.protobuf.Message)
	 */
	@Override
	public ServiceResponse buildResponse(ServiceResponse resp,
			DescribeInstancesResponseMessage result) {
		marshall(resp, result);
		return resp;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.msi.tough.query.AbstractQueuedAction#handleRequest(com.msi.tough.
	 * query.ServiceRequest)
	 */
	@Override
	public DescribeInstancesRequestMessage handleRequest(ServiceRequest req,
			ServiceRequestContext context) throws ErrorResponse {
		final DescribeInstancesRequestMessage request = unmarshall(req
				.getParameterMap());
		return request;
	}

	@Override
	protected void mark(Object ret, Exception e) {
		markStandard(meters, e);
	}

	// @Override
	public void marshall(ServiceResponse response,
			DescribeInstancesResponseMessage result) throws ErrorResponse {

		final XMLNode root = new XMLNode("DescribeInstancesResponse");
		root.addAttr("xmlns", "http://ec2.amazonaws.com/doc/2012-12-01/");
		QueryUtil.addNode(root, "requestId", response.getRequestId());
		final XMLNode reservationSet = QueryUtil
				.addNode(root, "reservationSet");
		// InstanceUtils.marshallReservations(reservationSet,
		// result.getReservations());
		if (result.getReservationsCount() > 0) {
			InstanceUtils.marshallReservations2(reservationSet,
					result.getReservationsList());
		}
		response.setPayload(root.toString());
	}

	private DescribeInstancesRequestMessage unmarshall(Map<String, String[]> in) {
		final DescribeInstancesRequestMessage.Builder req =
		        DescribeInstancesRequestMessage.newBuilder();
		{
			for (int i = 1;; ++i) {
				if (!in.containsKey("InstanceId." + i)) {
					break;
				}
				req.addInstanceIds(QueryUtil.getString(in, "InstanceId." + i));
			}
		}
		{
			final String s = "Filter.";
			//final List<Filter> filters = new ArrayList<Filter>();
			for (int i = 1;; i++) {
				if (!in.containsKey(s + i + ".Name")) {
					break;
				}
				final List<String> ids = new ArrayList<String>();
				for (int m = 1;; m++) {
					if (!in.containsKey(s + i + ".Value." + m)) {
						break;
					}
					ids.add(QueryUtil.getString(in, s + i + ".Value." + m));
				}
				Filter.Builder filter = Filter.newBuilder();
				filter.setName(QueryUtil.getString(in, s + i + ".Name"));
				filter.addAllValue(ids);
				req.addFilter(filter);
			}
		}
		return req.buildPartial();
	}

}
