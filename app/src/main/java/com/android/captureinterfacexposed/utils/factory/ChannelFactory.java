package com.android.captureinterfacexposed.utils.factory;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * 单列模式，通知截图的管道
 */
public class ChannelFactory {

    // 开启截屏的channel
    private static ChannelFactory startScreenShot;

    // 结束截屏的channel
    private static ChannelFactory endScreenShot;

    private static ChannelFactory startDumpViewTree;
    private static ChannelFactory endDumpViewTree;

    private LinkedBlockingQueue<Boolean> messages;

    private ChannelFactory() {
        messages = new LinkedBlockingQueue<>();
    }

    public static synchronized ChannelFactory getStartScreenShot() {
        if (startScreenShot == null) {
            startScreenShot = new ChannelFactory();
        }
        return startScreenShot;
    }

    public static synchronized ChannelFactory getEndScreenShot() {
        if (endScreenShot == null) {
            endScreenShot = new ChannelFactory();
        }
        return endScreenShot;
    }

    public static synchronized ChannelFactory getStartDumpViewTree() {
        if (startDumpViewTree == null) {
            startDumpViewTree = new ChannelFactory();
        }
        return startDumpViewTree;
    }

    public static synchronized ChannelFactory getEndDumpViewTree() {
        if (endDumpViewTree == null) {
            endDumpViewTree = new ChannelFactory();
        }
        return endDumpViewTree;
    }

    /**
     * 发送消息
     *
     * @param message
     */
    public void send(boolean message) {
        try {
            messages.put(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 接收消息
     *
     * @return
     */
    public boolean receive() {
        try {
            return messages.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

}
