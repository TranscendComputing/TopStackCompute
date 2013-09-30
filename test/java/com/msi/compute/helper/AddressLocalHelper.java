package com.msi.compute.helper;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.msi.tough.query.ActionTestHelper;
import com.transcend.compute.message.AllocateAddressMessage.AllocateAddressRequestMessage;
import com.transcend.compute.message.AllocateAddressMessage.AllocateAddressResponseMessage;
import com.transcend.compute.message.ReleaseAddressMessage.ReleaseAddressRequestMessage;
import com.transcend.compute.worker.AllocateAddressWorker;
import com.transcend.compute.worker.ReleaseAddressWorker;

/**
 * Address helper for non-web tests (using actions in-VM).
 *
 * @author jgardner
 *
 */
@Component
public class AddressLocalHelper {

    private static List<String> allocations = new ArrayList<String>();

    private static ActionTestHelper actionHelper = null;

    private static AllocateAddressWorker allocateAddressWorker = null;

    private static ReleaseAddressWorker releaseAddressWorker = null;

    /**
     * Construct a minimal valid address request.
     *
     * @param addressName
     * @return
     */
    public static AllocateAddressRequestMessage allocateAddressRequest() {
        final AllocateAddressRequestMessage.Builder builder =
                AllocateAddressRequestMessage.newBuilder();
        builder.setTypeId(true);
        builder.setCallerAccessKey(actionHelper.getAccessKey());
        builder.setRequestId("test");
        return builder.build();
    }

    public static ReleaseAddressRequestMessage releaseAddressRequest(String ip) {
        final ReleaseAddressRequestMessage.Builder builder =
                ReleaseAddressRequestMessage.newBuilder();
        builder.setTypeId(true);
        builder.setCallerAccessKey(actionHelper.getAccessKey());
        builder.setRequestId("test");
        builder.setPublicIp(ip);
        return builder.build();
    }

    /**
     * Put a metric address (don't care about the details, just need one.
     *
     * @param addressName
     */
    public static String allocateAddress() throws Exception {
        AllocateAddressRequestMessage req = allocateAddressRequest();
        AllocateAddressResponseMessage result = null;
        result = allocateAddressWorker.doWork(req);

        allocations.add(result.getPublicIp());
        return result.getPublicIp();
    }

    /**
     * Release an address with the given name.
     *
     * @param ip
     * @param client
     */
    public static void releaseAddress(String ip) throws Exception {
        final ReleaseAddressRequestMessage request = releaseAddressRequest(ip);
        releaseAddressWorker.doWork(request);
    }

    /**
     * Delete an address with the given name.
     *
     * @param addressName
     * @param client
     */
    public static void releaseAllAllocatedAddresses() throws Exception {

        for (String ip : allocations) {
            final ReleaseAddressRequestMessage request = releaseAddressRequest(ip);
            releaseAddressWorker.doWork(request);
        }
        allocations.clear();
    }

    @Autowired(required=true)
    public void setActionTestHelper(ActionTestHelper actionTestHelper) {
        AddressLocalHelper.actionHelper = actionTestHelper;
    }

    @Autowired(required=true)
    public void setAllocateAddressWorker(AllocateAddressWorker allocateAddressWorker) {
        AddressLocalHelper.allocateAddressWorker = allocateAddressWorker;
    }

    @Autowired(required=true)
    public void setReleaseAddressWorker(ReleaseAddressWorker releaseAddressWorker) {
        AddressLocalHelper.releaseAddressWorker = releaseAddressWorker;
    }

}
