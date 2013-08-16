package com.msi.compute.helper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.amazonaws.services.ec2.model.KeyPair;
import com.msi.tough.query.ActionRequest;
import com.msi.tough.query.ActionTestHelper;
import com.transcend.compute.message.CreateKeyPairMessage.CreateKeyPairRequestMessage;
import com.transcend.compute.message.CreateKeyPairMessage.CreateKeyPairResponseMessage;
import com.transcend.compute.message.DeleteKeyPairMessage.DeleteKeyPairRequestMessage;
import com.transcend.compute.worker.CreateKeyPairWorker;
import com.transcend.compute.worker.DeleteKeyPairWorker;

/**
 * KeyPair helper for non-web tests (using actions in-VM).
 *
 * @author jgardner
 *
 */
@Component
public class KeyPairLocalHelper {

    private static List<String> keyPairs = new ArrayList<String>();

    private static ActionTestHelper actionHelper = null;

    private static DeleteKeyPairWorker deleteKeyPairWorker = null;

    private static CreateKeyPairWorker createKeyPairWorker = null;


    /**
     * Construct a minimal valid keyPair request.
     *
     * @param keyPairName
     * @return
     */
    public static CreateKeyPairRequestMessage createKeyPairRequest(String keyName) {
    	final CreateKeyPairRequestMessage.Builder builder =
        		CreateKeyPairRequestMessage.newBuilder();
        builder.setTypeId(true);
        builder.setCallerAccessKey(actionHelper.getAccessKey());
        builder.setRequestId("test");
        builder.setKeyName(keyName);

        return builder.build();
    }

    public static DeleteKeyPairRequestMessage deleteKeyPairRequest(String keyName) {
        final DeleteKeyPairRequestMessage.Builder builder =
                DeleteKeyPairRequestMessage.newBuilder();
        builder.setTypeId(true);
        builder.setCallerAccessKey(actionHelper.getAccessKey());
        builder.setRequestId("test");
        builder.setKeyName(keyName);
        return builder.build();
    }

    /**
     * Put a metric keyPair (don't care about the details, just need one.
     *
     * @param keyPairName
     */
    public static CreateKeyPairResponseMessage createKeyPair(String keyPairName) throws Exception {

        CreateKeyPairRequestMessage request = createKeyPairRequest(keyPairName);
        return createKeyPairWorker.doWork(request);
    }

    public static CreateKeyPairResponseMessage createKeyPair(CreateKeyPairRequestMessage createKeyPairRequestMessage ) throws Exception {

        return createKeyPairWorker.doWork(createKeyPairRequestMessage);
    }

    /**
     * Delete an keyPair with the given name.
     *
     * @param keyPairName
     * @param client
     */
    public static void deleteKeyPair(String keyPairName) throws Exception {
        final DeleteKeyPairRequestMessage request = deleteKeyPairRequest(keyPairName);
        deleteKeyPairWorker.doWork(request);
    }

    /**
     * Delete an keyPair with the given name.
     *
     * @param keyPairName
     * @param client
     */
    public static void deleteAllCreatedKeyPairs() throws Exception {
        for (String keyPairName : keyPairs) {
        	final DeleteKeyPairRequestMessage request = deleteKeyPairRequest(keyPairName);
            deleteKeyPairWorker.doWork(request);
        }
        keyPairs.clear();
    }

    /**
     * Delete an keyPair with the given name.
     *
     * @param keyPairName
     * @param client
     */
    public static void deleteAllCreatedKeyPairs(HashSet<String> keyPairs) throws Exception {
        for (String keyPairName : keyPairs) {
        	final DeleteKeyPairRequestMessage request = deleteKeyPairRequest(keyPairName);
            deleteKeyPairWorker.doWork(request);
        }
        keyPairs.clear();
    }

    /**
     * Get an keyPair from the collection.
     *
     * @param keyPairs
     * @param name
     * @return matching keyPair
     */
    public static KeyPair getKeyPairByName(List<KeyPair> keyPairs,
            String name) {
        for (KeyPair keyPair : keyPairs) {
            if (keyPair.getKeyName().equals(name)) {
                return keyPair;
            }
        }
        return null;
    }

    @Autowired(required=true)
    public void setActionTestHelper(ActionTestHelper actionTestHelper) {
        KeyPairLocalHelper.actionHelper = actionTestHelper;
    }


    public static class DescribeKeyPairsRequest
        extends ActionRequest {

        public DescribeKeyPairsRequest() {
            super();
        }

        public void reset() {
            super.reset();
        }

    }

    @Autowired(required=true)
    public void setDeleteKeyPairWorker(DeleteKeyPairWorker deleteKeyPairWorker)
    {
    	KeyPairLocalHelper.deleteKeyPairWorker = deleteKeyPairWorker;
    }

    @Autowired(required=true)
    public void setCreateKeyPairWorker(CreateKeyPairWorker createKeyPairWorker)
    {
    	KeyPairLocalHelper.createKeyPairWorker = createKeyPairWorker;
    }

}
