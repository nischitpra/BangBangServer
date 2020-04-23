package com.nhuchhe.bangbang;

import com.nhuchhe.bangbang.network.NetworkWire;

public class Starter {

    public static void main(String[] args) throws InterruptedException {
        NetworkWire networkWire = new NetworkWire();
        networkWire.init();
    }
}
