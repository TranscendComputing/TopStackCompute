package com.transcend.compute.utils;

import java.util.List;

import com.amazonaws.services.ec2.model.KeyPair;
import com.generationjava.io.xml.XMLNode;
import com.msi.tough.query.QueryUtil;

public class KeyPairUtils {
    public static void marshallKeyPair(final XMLNode node, final KeyPair keyPair) {
        QueryUtil.addNode(node, "keyName", keyPair.getKeyName());
        QueryUtil.addNode(node, "keyFingerprint", keyPair.getKeyFingerprint());
        QueryUtil.addNode(node, "keyMaterial", keyPair.getKeyMaterial());

    }

    public static void marshallKeyPairs(final XMLNode node,
            final List<KeyPair> keyPairs) {
        for (KeyPair keyPair : keyPairs) {
            final XMLNode item = QueryUtil.addNode(node, "item");
            marshallKeyPair(item, keyPair);
        }
    }
}
