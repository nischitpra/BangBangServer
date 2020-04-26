package com.nhuchhe.bangbang.network;

import com.nhuchhe.bangbang.enums.network.GameManagerAction;
import com.nhuchhe.bangbang.pojo.network.GameManagerPojo;
import com.nhuchhe.bangbang.pojo.network.Lobby;
import com.nhuchhe.bangbang.utilities.Logger;
import org.apache.commons.lang3.SerializationUtils;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.HashMap;

public class NetworkWire {

    private HashMap<String, Lobby> gameNameToPlayerIdMap = new HashMap();

    private ZContext context = new ZContext();
    private ZMQ.Socket gameManagerSocket;
    private ZMQ.Socket gameDataDownstream;
    private ZMQ.Socket gameDataUpstream;

    private Thread gameManagerThread;
    private Thread inGameBroadcastReceiverThread;

    public void init() throws InterruptedException {
        gameManagerSocket = context.createSocket(SocketType.REP);
        gameManagerSocket.bind("tcp://192.168.0.169:5554");

        gameDataUpstream = context.createSocket(SocketType.SUB);
        gameDataUpstream.bind("tcp://192.168.0.169:5555");

        gameDataDownstream = context.createSocket(SocketType.PUB);
        gameDataDownstream.bind("tcp://192.168.0.169:5556");

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
                while (!Thread.currentThread().isInterrupted()) {
                    GameManagerPojo managerPojo = SerializationUtils.deserialize(gameManagerSocket.recv());
                    String data = managerPojo.data;
                    Lobby lobby = gameNameToPlayerIdMap.get(data);
                    switch (managerPojo.action) {
                        case CREATE_LOBBY:
                            if (lobby == null) {
                                Logger.log("creating lobby");
                                lobby = new Lobby(data);
                                gameNameToPlayerIdMap.put(data, lobby);
                                gameManagerSocket.send(SerializationUtils.serialize(true));
                            } else {
                                Logger.log("lobby is not null");
                                gameManagerSocket.send(SerializationUtils.serialize(false));
                            }
                            break;
                        case GET_LOBBY:
                            Logger.log("get lobbies");
                            gameManagerSocket.send(SerializationUtils.serialize(gameNameToPlayerIdMap.keySet().toArray(new String[0])));
                            break;
                        case JOIN_LOBBY:
                            if (lobby == null) {
                                gameManagerSocket.send(SerializationUtils.serialize(-1));
                            } else {
                                gameManagerSocket.send(SerializationUtils.serialize(lobby.addPlayer()));
                                gameDataUpstream.subscribe(lobby.lobbyName);
                                gameDataUpstream.subscribe("game." + lobby.lobbyName);
                                Logger.log("upstream subscribed to " + lobby.lobbyName);
                                Logger.log("upstream subscribed to game." + lobby.lobbyName);
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
                                gameDataDownstream.sendMore(data); // send topic
                                managerPojo.action = GameManagerAction.CHANGE_SCREEN;
                                managerPojo.data = "GameScreen";
                                gameDataDownstream.send(SerializationUtils.serialize(managerPojo));
                            }
                            break;
                        case DISCONNECT:
                            int id = (int) managerPojo.extra;
                            lobby.playerIdSet.remove(id);
                            if (lobby.playerIdSet.size() == 0) {
                                gameNameToPlayerIdMap.remove(data);
                                Logger.log("removed lobby: " + data);

                            }
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
                while (!Thread.currentThread().isInterrupted()) {
                    gameDataDownstream.sendMore(gameDataUpstream.recv());// lobbyName
                    gameDataDownstream.send(gameDataUpstream.recv());// ingamepojo
                }
            }
        });
        inGameBroadcastReceiverThread.start();
    }
}
