package com.nhuchhe.bangbang.network;

import com.nhuchhe.bangbang.enums.network.GameManagerAction;
import com.nhuchhe.bangbang.pojo.network.GameManagerPojo;
import com.nhuchhe.bangbang.pojo.network.Lobby;
import org.apache.commons.lang3.SerializationUtils;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.HashMap;

public class NetworkWire {

    private HashMap<String, Lobby> gameNameToPlayerIdMap = new HashMap();

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
                    Lobby lobby = gameNameToPlayerIdMap.get(data);
                    switch (managerPojo.action) {
                        case CREATE_LOBBY:
                            if (lobby == null) {
                                lobby = new Lobby(data);
                                gameNameToPlayerIdMap.put(data, lobby);
                                gameManagerSocket.send(SerializationUtils.serialize(true));
                            } else {
                                gameManagerSocket.send(SerializationUtils.serialize(false));
                            }
                            break;
                        case GET_LOBBY:
                            gameManagerSocket.send(SerializationUtils.serialize(gameNameToPlayerIdMap.keySet().toArray(new String[0])));
                            break;
                        case JOIN_LOBBY:
                            if (lobby == null) {
                                gameManagerSocket.send(SerializationUtils.serialize(-1));
                            } else {
                                gameManagerSocket.send(SerializationUtils.serialize(lobby.addPlayer()));
                            }
                            break;
                        case GET_LOBBY_PLAYERS:
                            if (lobby == null) {
                                gameManagerSocket.send(SerializationUtils.serialize(new int[]{}));
                            } else {
                                gameManagerSocket.send(SerializationUtils.serialize(lobby.getPlayerIdsArray()));
                            }
                            break;
                        case START_GAME:
                            if (lobby == null) {
                                gameManagerSocket.send(SerializationUtils.serialize(false));
                            } else {
                                gameManagerSocket.send(SerializationUtils.serialize(true));
                                inGameSenderSocket.send(data, ZMQ.SNDMORE); // send topic
                                managerPojo.action = GameManagerAction.CHANGE_SCREEN;
                                managerPojo.data = "GameScreen";
                                inGameSenderSocket.send(SerializationUtils.serialize(managerPojo));
                            }
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
