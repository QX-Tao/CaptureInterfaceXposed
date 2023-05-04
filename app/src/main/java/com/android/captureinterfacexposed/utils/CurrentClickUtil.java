package com.android.captureinterfacexposed.utils;

public class CurrentClickUtil {
    private static String clickFilePath = null;
    private static String clickPackageName = null;
    private static String clickTime = null;
    private static int interfaceNum = 0;

    public static String getClickFilePath() {
        return clickFilePath;
    }

    public static void setClickFilePath(String clickFilePath) {
        CurrentClickUtil.clickFilePath = clickFilePath;
    }

    public static String getClickPackageName() {
        return clickPackageName;
    }

    public static void setClickPackageName(String clickPackageName) {
        CurrentClickUtil.clickPackageName = clickPackageName;
    }

    public static int getInterfaceNum() {
        return interfaceNum;
    }

    public static void setInterfaceNum(int interfaceNum) {
        CurrentClickUtil.interfaceNum = interfaceNum;
    }

    public static String getClickTime() {
        return clickTime;
    }

    public static void setClickTime(String clickTime) {
        CurrentClickUtil.clickTime = clickTime;
    }
}
