/*
 * TopStack (c) Copyright 2012-2013 Transcend Computing, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an AS IS BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.transcend.compute.client.util;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Resource;

import org.hibernate.Query;
import org.hibernate.Session;

import com.msi.tough.core.QueryBuilder;
import com.msi.tough.core.StringHelper;
import com.msi.tough.message.CoreMessage.ErrorResult;
import com.msi.tough.model.InstanceBean;
import com.msi.tough.workflow.WorkflowSubmitter;
import com.transcend.compute.message.DescribeInstancesMessage.DescribeInstancesRequestMessage;
import com.transcend.compute.message.DescribeInstancesMessage.DescribeInstancesRequestMessage.InstanceDescribeDepth;
import com.transcend.compute.message.DescribeInstancesMessage.DescribeInstancesResponseMessage;
import com.transcend.compute.message.InstanceMessage.Instance;
import com.transcend.compute.message.ReservationMessage.Reservation;

public class RunningInstanceUtil {

    /** How long to wait for describe. */
    private static final int MAX_SECS = 30;

    @Resource
    private WorkflowSubmitter toCompute = null;

    /**
     * Convert a non-standard instance ID into a cloud-normal instance ID. In
     * some circumstances (e.g. essex instance data) an EC2 style ID (i-XXXXXX)
     * is returned rather than a native ID. This converts to native by reading
     * from instance table, and verifies by describing instances, if necessary.
     *
     * @param session
     * @param instId
     * @param describe describe instances, if need be.
     * @return
     * @throws Exception
     */
    public String normalizeInstanceId(final Session session,
            String instanceId, boolean describe) {
        if (instanceId == null || !instanceId.startsWith("i-")) {
            return instanceId;
        }
        String ip = null;
        int colonPos = instanceId.indexOf(':');
        if (colonPos != -1) {
            ip = instanceId.substring(colonPos+1);
            instanceId = instanceId.substring(0, colonPos-1);
        }
        final Query q = new QueryBuilder("from InstanceBean").
                equals("ec2Id", instanceId).toQuery(session);
        InstanceBean ib = (InstanceBean) q.uniqueResult();
        if (ib != null && !StringHelper.isBlank(ib.getInstanceId())) {
            return ib.getInstanceId();
        }
        if (ip != null) {
            final Query byIp = new QueryBuilder("from InstanceBean")
                .equals("privateIp", ip)
                .equals("health", "Healthy")
                .toQuery(session);
            ib = (InstanceBean) byIp.uniqueResult();
        }
        if (ib != null && !StringHelper.isBlank(ib.getInstanceId())) {
            return ib.getInstanceId();
        }
        if (describe && ip != null) {
            List<Instance> instances = describeAllInstances(session);
            for (Instance inst : instances) {
                if (inst.getPrivateIp().equals(ip)) {
                    ib = new InstanceBean();
                    ib.setUserId(0);
                    ib.setInstanceId(inst.getInstanceId());
                    ib.setPrivateIp(ip);
                    ib.setEc2Id(instanceId);
                    session.save(ib);
                    return ib.getInstanceId();
                }
            }
        }
        return instanceId;
    }

    public List<Instance> describeAllInstances(final Session session) {
        DescribeInstancesRequestMessage.Builder request =
                DescribeInstancesRequestMessage.newBuilder();
        request.setTypeId(true);
        request.setCallerAccessKey("SYSTEM");
        request.setRequestId(UUID.randomUUID().toString());
        request.setInstanceDescribeDepth(InstanceDescribeDepth.BASIC_ONLY);
        DescribeInstancesResponseMessage response = null;
        ArrayList<Instance> instances = new ArrayList<Instance>();
        try {
            Object result = toCompute.submitAndWait(request.build(), MAX_SECS);
            if (result instanceof DescribeInstancesResponseMessage) {
                response = (DescribeInstancesResponseMessage) result;
                for (Reservation r : response.getReservationsList()) {
                    for (Instance inst : r.getInstanceList()) {
                        instances.add(inst);
                    }
                }
            } else if (result instanceof ErrorResult) {
                return instances;
            }
            return instances;
        } catch (Exception e) {
            return instances;
        }
    }
}
