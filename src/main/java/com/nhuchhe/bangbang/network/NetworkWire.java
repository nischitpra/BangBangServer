package com.nhuchhe.bangbang.network;

import com.nhuchhe.bangbang.pojo.network.Counter;
import com.nhuchhe.bangbang.pojo.network.GameManagerPojo;
import org.apache.commons.lang3.SerializationUtils;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.HashMap;

public class NetworkWire {

    private HashMap<String, Counter> gameNameToPlayerIdMap = new HashMap<String, Counter>() {{
        put("hello_bello", new Counter());
        put("test_lobby", new Counter());
    }};

    private ZContext context = new ZContext();
    private ZMQ.Socket gameManagerSocket;
    private ZMQ.Socket inGameSenderSocket;
    private ZMQ.Socket inGameReceiverSocket;

    private Thread gameManagerThread;
    private Thread inGameBroadcastSenderThread;
    private Thread inGameBroadcastReceiverThread;

    public void init() throws InterruptedException {
        gameManagerSocket = context.createSocket(SocketType.REP);
        gameManagerSocket.bind("tcp://localhost:5554");

        inGameReceiverSocket = context.createSocket(SocketType.SUB);
        inGameReceiverSocket.connect("tcp://localhost:5555");

        inGameSenderSocket = context.createSocket(SocketType.PUB);
        inGameSenderSocket.bind("tcp://localhost:5556");

        createServer();
    }

    public void createServer() throws InterruptedException {
        runGameManagerThread();
        runInGameBroadcastThread();
    }

    private void runGameManagerThread() {
        gameManagerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    GameManagerPojo managerPojo = SerializationUtils.deserialize(gameManagerSocket.recv());
                    String data = managerPojo.data;
                    Counter counter = gameNameToPlayerIdMap.get(data);
                    switch (managerPojo.action) {
                        case GET_LOBBY:
                            gameManagerSocket.send(SerializationUtils.serialize(gameNameToPlayerIdMap.keySet().toArray(new String[0])));
                            break;
                        case CREATE_LOBBY:
                            if (counter == null) {
                                counter = new Counter();
                                gameNameToPlayerIdMap.put(data, counter);
                                gameManagerSocket.send(counter.count + "");
                            } else {
                                gameManagerSocket.send(-1 + "");
                            }
                            break;
                        case JOIN_GAME:
                            if (counter == null) {
                                gameManagerSocket.send(-1 + "");
                            } else {
                                gameManagerSocket.send(counter.increment() + "");
                            }
                            break;
                        case START_GAME:
                            break;
                    }
                }
            }
        });
        gameManagerThread.start();
    }

    private void runInGameBroadcastThread() {
        inGameBroadcastReceiverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    String value = inGameReceiverSocket.recvStr();
                    System.out.println("val: " + value);
                }
            }
        });
        inGameBroadcastReceiverThread.start();

        inGameBroadcastSenderThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    inGameSenderSocket.send("GAME_NAME-HELLO_FROM_SERVER!");
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        inGameBroadcastSenderThread.start();
    }
}
