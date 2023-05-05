package com.android.captureinterfacexposed.utils;

public class CurrentCollectUtil {
    private static String collectFilePath = null;
    private static String collectPackageName = null;
    private static String collectTime = null;
    private static int interfaceNum = 0;
    private static boolean rightButtonClickable = false;

    public static String getCollectFilePath() {
        return collectFilePath;
    }

    public static void setCollectFilePath(String collectFilePath) {
        CurrentCollectUtil.collectFilePath = collectFilePath;
    }

    public static String getCollectPackageName() {
        return collectPackageName;
    }

    public static void setCollectPackageName(String collectPackageName) {
        CurrentCollectUtil.collectPackageName = collectPackageName;
    }

    public static int getInterfaceNum() {
        return interfaceNum;
    }

    public static void setInterfaceNum(int interfaceNum) {
        CurrentCollectUtil.interfaceNum = interfaceNum;
    }

    public static String getCollectTime() {
        return collectTime;
    }

    public static void setCollectTime(String collectTime) {
        CurrentCollectUtil.collectTime = collectTime;
    }

    public static boolean isRightButtonClickable() {
        return rightButtonClickable;
    }

    public static void setRightButtonClickable(boolean rightButtonClickable) {
        CurrentCollectUtil.rightButtonClickable = rightButtonClickable;
    }
}
