package bangbang.network;

import bangbang.enums.GameManagerAction;
import bangbang.pojo.Counter;
import bangbang.pojo.GameManagerPojo;
import org.apache.commons.lang3.SerializationUtils;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.HashMap;

public class NetworkWire {

    private HashMap<String, Counter> gameNameToPlayerIdMap = new HashMap<>();

    private ZContext context = new ZContext();
    private ZMQ.Socket inGameSenderSocket;
    private ZMQ.Socket inGameReceiverSocket;
    private ZMQ.Socket gameManagerSocket;

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
                    if (managerPojo.action == GameManagerAction.JOIN_GAME) {
                        String gameName = managerPojo.data;
                        try {
                            gameManagerSocket.send(gameNameToPlayerIdMap.get(gameName).increment() + "");
                            inGameReceiverSocket.subscribe(gameName);
                        } catch (Exception e) {
                            Counter counter = new Counter();
                            gameNameToPlayerIdMap.put(gameName, counter);
                            gameManagerSocket.send(counter.count + "");
                        }
                    } else {
                        gameManagerSocket.send("pong");
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
