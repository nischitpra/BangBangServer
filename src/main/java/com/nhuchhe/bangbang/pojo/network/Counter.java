package com.nhuchhe.bangbang.pojo.network;

public class Counter {
    public int count;

    public int increment() {
        return ++count;
    }

    public int decrement() {
        return --count;
    }
}
