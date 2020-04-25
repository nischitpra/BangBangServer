package com.nhuchhe.bangbang.utilities;

public class Logger {
    public static void log(String s) {
        StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[3];
        System.out.println(stackTraceElement.getClassName() + "." + stackTraceElement.getMethodName() + " : " + s);
    }
}
