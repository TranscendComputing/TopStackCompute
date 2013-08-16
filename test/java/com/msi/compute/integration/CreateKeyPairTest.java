package com.msi.compute.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.UUID;

import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.bouncycastle.util.io.pem.PemWriter;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;

import com.amazonaws.services.ec2.model.CreateKeyPairRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairResult;
import com.amazonaws.services.ec2.model.DeleteKeyPairRequest;
import com.amazonaws.services.ec2.model.KeyPair;
import com.msi.tough.core.Appctx;

public class CreateKeyPairTest extends AbstractBaseComputeTest {

    private static Logger logger = Appctx.getLogger(CreateKeyPairTest.class
            .getName());

    private final String keyName = "createKP-"+
            UUID.randomUUID().toString().substring(0, 8);

    

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
        
        final DeleteKeyPairRequest request = new DeleteKeyPairRequest();
        request.setKeyName(keyName);
        getComputeClientV2().deleteKeyPair(request);
        
    }

    @Test
    public void testGoodCreate() {
        CreateKeyPairResult createResult = null;
        logger.info("Creating KeyPair");
        final CreateKeyPairRequest request = new CreateKeyPairRequest();
        request.setKeyName(keyName);
        createResult = getComputeClientV2().createKeyPair(request);
        
        KeyPair keyPair = createResult.getKeyPair();
        assertNotNull("Expect a fingerprint.", keyPair.getKeyFingerprint());
        assertEquals("Expect valid name.", keyName, keyPair.getKeyName());
        assertNotNull("Expect a key value.", keyPair.getKeyMaterial());
        logger.debug("Got resulting key: " + keyPair.getKeyMaterial());
    }

    @Test
    @Ignore
    public void testBadName() {
        CreateKeyPairResult createResult = null;
        logger.info("Creating KeyPair");
        final CreateKeyPairRequest request = new CreateKeyPairRequest();
        request.setKeyName(keyName+":foo"); // colon is illegal in keyname
        createResult = getComputeClientV2().createKeyPair(request);
//        keyNames.add(keyName);
        logger.debug("Got resulting key: " +
                createResult.getKeyPair().getKeyMaterial());
    }

    //added ignore because this fails on delete otherwise
    @Test
    @Ignore
    public void testReadingPem() throws Exception {
        String pem = "-----BEGIN RSA PRIVATE KEY-----\n"+
                "MIICWwIBAAKBgQC6N0cvwG2uWr1ueu0i5dnNaZq4z96r3doSqQ4loIrA4e/7mHbb\n"+
                "UYWCtwRTD0vHJGS7uTUpAKqP+d2daWS772oqm1V+as8sMIbJDNu6r8kFNstlp3ua\n"+
                "E17knkItzH79tomgYPM9WWZXHB/MCT5DKZ9JpxfWP83tlQpz+TBzFDV+DwIDAQAB\n"+
                "AoGAIZf9WKj/YHfwOrEkfKo4q60Eg5jEk/7W64ziB3m57mgUMjBkKNbBHj7EIfPd\n"+
                "eNSg85jK9VWwb37lMxOjX6AmZ7CHGMmLug8jypYGETOldCUQoad9Jn1xE8IHjz+Q\n"+
                "XA+h5pkE1r8G2zaGOfEzVg9Ej9U/J/45oMSbEwTAyDjVxMkCQQDfq2cKLs2xeo0G\n"+
                "S7ljFkurdXW/iQLk7xhf25QIqWO5p/lpzx8hT6JiFDRN8I9EamsL+EBDPDNyb94W\n"+
                "vPcQshNrAkEA1SH0MxRklWWJQ83MCkZsJXFgj/+gPbYUBSpxgonNsdWAA49vGEoa\n"+
                "s+h2P1BnVwV4NuSp5hoYlEiz/E8LH/GM7QJAX89DNxhvYev2BtFfGzPMvCh3hNFC\n"+
                "3SASF9WuJruwjTGH5Cwl0JYVH+A5u30lUZAoRJtVo7dg7k8/Ggxd5Nfy+wJAZt0i\n"+
                "TNzUxr3gh9b1WSgv3cpgfl8zaVVNSEj5y7TSj7epNw6s4Z4yUQ4qs9gMaBV9tZCa\n"+
                "5zFFF5wXMfi8N8iO6QJAUlHOZ7NDvxtEMNfyjLB//Omwrmz69RVYId0W4d7DY92A\n"+
                "nyL7wA6EP5Gcm0C/Rmx2KJ8bN1p3BdxP4Mm8+hgPag==\n"+
                "-----END RSA PRIVATE KEY-----\n";
        StringReader stringReader = new StringReader(pem);
        PemReader reader = new PemReader(stringReader);
        PemObject pemO = reader.readPemObject();
        reader.close();
        StringWriter stringWriter = new StringWriter();
        PemWriter writer = new PemWriter(stringWriter);
        writer.writeObject(pemO);
        writer.flush();
        writer.close();
        assertEquals("Expect PEM to be identical after read/write.",
                pem, stringWriter.toString());
        logger.debug("Result pem: "+stringWriter.toString());
    }
}
