package com.msi.compute.devtests;

import java.io.UnsupportedEncodingException;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.zeromq.ZMQ;

import com.msi.compute.integration.AbstractBaseComputeTest;
import com.msi.tough.core.Appctx;

public class ZmqRoundTripTest extends AbstractBaseComputeTest implements Runnable {

    private static int role = 1;
    public enum MODE {SERVER, CLIENT};
    private MODE mode = role == 1? MODE.SERVER : MODE.CLIENT;

    private ZMQ.Context zmqContext = null;
    private ZMQ.Socket zmqRecvSocket = null;
    private ZMQ.Poller items = null;
    private Thread recvThread = null;

    private final static Logger logger = Appctx
            .getLogger(ZmqRoundTripTest.class.getName());
    private static String recvEndpoint1 = "tcp://*:5555";
    private static String recvEndpoint2 = "tcp://*:5556";

    @Autowired
    private String targetServer = "localhost";

    @Autowired
    private String testerHost = "localhost";

    private String sendEndpoint1 = null;
    private String sendEndpoint2 = null;

    private boolean done = false;

    @Before
    public void establishRole() {
        sendEndpoint1 = "tcp://"+targetServer+":5555";
        sendEndpoint2 = "tcp://"+testerHost+":5556";
        String roleProp = System.getProperty("zmq.role");
        if (roleProp != null) {
            try {
                role = Integer.parseInt(roleProp);
                mode = role == 1? MODE.SERVER : MODE.CLIENT;
                logger.info("Using supplied role: " + role);
            } catch (NumberFormatException e) {
                logger.info("No role supplied, assuming " + role);
            }
        }
    }

    /**
     * Test sending a message to self; verify basic connectivity.
     *
     * @throws Exception
     */
    @Test
    public void testSendToSelf() throws Exception {
        zmqContext = ZMQ.context(1);

        if (role == 1) {
            logger.debug("Binding to ZMQ socket for receive1:" +
                    recvEndpoint1);
            zmqRecvSocket = zmqContext.socket(ZMQ.PULL);
            zmqRecvSocket.bind(recvEndpoint1);
        } else {
            zmqRecvSocket = zmqContext.socket(ZMQ.PULL);
            zmqRecvSocket.bind(recvEndpoint2);
        }

        items = new ZMQ.Poller(1);
        items.register(zmqRecvSocket, ZMQ.Poller.POLLIN);
        recvThread = new Thread(this);
        recvThread.start();

        if (mode == MODE.SERVER) {
            while (!done) {
                logger.debug("Waiting for request.");
                recvThread.join();
            }
            System.out.println("Server done, exiting normally.");
        } else {
            ZMQ.Socket zmqSocket = zmqContext.socket(ZMQ.PUSH);
            logger.info("Connecting to remote ZMQ socket:" +
                sendEndpoint1);
            zmqSocket.connect(sendEndpoint1);
            boolean sent = zmqSocket.send("PING", ZMQ.SNDMORE);
            // Send my return address as a second part.
            logger.info("Sending as my callback ZMQ socket:" +
                sendEndpoint2);
            sent = zmqSocket.send(sendEndpoint2.getBytes(), 0);
            logger.info("Sent message: "+sent);
            zmqSocket.close();
            logger.debug("Waiting for response.");
            recvThread.join();
            System.out.println("Client done, exiting normally.");
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        while (!done && !Thread.currentThread().isInterrupted()) {
            byte[] messageBytes;
            items.poll();
            if (items.pollin(0)) {
                messageBytes = zmqRecvSocket.recv(0);
                logger.info("Got an item:"+messageBytes.length);
                try {
                    String message = new String(messageBytes, "utf-8");
                    logger.info("Got message:"+message);
                    if (mode == MODE.SERVER) {
                        if (items.pollin(0)) {
                            messageBytes = zmqRecvSocket.recv(0);
                        }
                        message = new String(messageBytes, "utf-8");
                        logger.info("Got 2nd part:"+message);
                        logger.info("Connecting back to requestor ZMQ at:" +
                            message);
                        ZMQ.Socket zmqSocket = zmqContext.socket(ZMQ.PUSH);
                        zmqSocket.connect(message);
                        boolean sent = zmqSocket.send("PONG", 0);
                        logger.info("Sent response: "+sent);
                        zmqSocket.close();
                        logger.info("Server done, exiting.");
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                done = true;
            }
        }
        logger.debug("Exiting receive thread.");
        zmqRecvSocket.close();
    }
}
