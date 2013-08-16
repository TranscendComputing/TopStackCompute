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
import com.transcend.compute.message.DescribeVolumesMessage.DescribeVolumesRequestMessage;
import com.transcend.compute.message.DescribeVolumesMessage.DescribeVolumesRequestMessage.Filter;
import com.transcend.compute.message.DescribeVolumesMessage.DescribeVolumesResponseMessage;
import com.transcend.compute.message.VolumeMessage.Volume;
import com.transcend.compute.utils.VolumeUtils;
import com.yammer.metrics.core.Meter;

public class DescribeVolumes extends AbstractQueuedAction<DescribeVolumesRequestMessage, DescribeVolumesResponseMessage> {

	private static Map<String, Meter> meters = initMeter("Compute",
			"DescribeVolumes");

	@Override
	protected void mark(Object ret, Exception e) {
		markStandard(meters, e);
	}

	@Override
	public void process(ServiceRequest req, ServiceResponse resp)
			throws ErrorResponse {
		super.process(req, resp);
	}

	public String marshall(ServiceResponse resp,
            DescribeVolumesResponseMessage result) {
		final List<Volume> volumes = result.getVolumesList();
		final XMLNode root = new XMLNode("DescribeVolumesResponse");
		root.addAttr("xmlns", "http://ec2.amazonaws.com/doc/2012-10-01/");
		QueryUtil.addNode(root, "requestId", result.getRequestId());
		final XMLNode volumeSet = QueryUtil.addNode(root, "volumeSet");
		VolumeUtils.marshallVolumes(volumeSet, volumes);
		return root.toString();
	}

	private DescribeVolumesRequestMessage unmarshall(final Map<String, String[]> in) {
		final DescribeVolumesRequestMessage.Builder req = DescribeVolumesRequestMessage.newBuilder();
		{
			for (int i = 1;; ++i) {
				if (!in.containsKey("VolumeId." + i)) {
					break;
				}
				req.addVolumeId(QueryUtil.getString(in, "VolumeId." + i));
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
				req.addFilter(f.buildPartial());
			}
		}
		return req.buildPartial();
	}

	@Override
	public ServiceResponse buildResponse(ServiceResponse resp,
			DescribeVolumesResponseMessage message) {
		resp.setPayload(marshall(resp, message));
        return resp;
	}

	@Override
	public DescribeVolumesRequestMessage handleRequest(ServiceRequest req,
			ServiceRequestContext context) throws ErrorResponse {
		DescribeVolumesRequestMessage reqObj = unmarshall(req.getParameterMap());
		return reqObj;
	}

}
