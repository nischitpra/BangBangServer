package com.nhuchhe.bangbang.pojo.network;

import java.util.HashSet;

public class Lobby {
    public String lobbyName;
    public HashSet<Integer> playerIdSet = new HashSet<>();
    public Counter idGenerator = new Counter();

    public Lobby(String lobbyName) {
        this.lobbyName = lobbyName;
    }

    public int addPlayer() {
        int id = idGenerator.increment();
        playerIdSet.add(id);
        return id;
    }

    public int[] getPlayerIdsArray() {
        int[] ids = new int[playerIdSet.size()];
        int i = 0;
        for (Integer id : playerIdSet) {
            ids[i++] = id;
        }
        return ids;
    }
}
