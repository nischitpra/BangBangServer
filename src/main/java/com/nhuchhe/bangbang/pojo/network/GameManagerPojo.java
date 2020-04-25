package com.nhuchhe.bangbang.pojo.network;

import com.nhuchhe.bangbang.enums.network.GameManagerAction;

import java.io.Serializable;

public class GameManagerPojo implements Serializable {
    private static final long serialVersionUID = 1L;

    public GameManagerAction action;
    public String data;
    public Object extra;

    @Override
    public String toString() {
        return String.format("action: %s, data: %s", action.name(), data);
    }

}
