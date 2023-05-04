package com.android.captureinterfacexposed.socket;

import android.content.Context;

import com.android.captureinterfacexposed.utils.CollectDataUtil;
import com.android.captureinterfacexposed.utils.CurrentClickUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

// 客户端
public class ClientSocket {
    private final Context mContext;
    BufferedReader in = null;
    PrintWriter out = null;

    private Socket serverSocket;

    public ClientSocket(Context context, String HOSTNAME, int PORT){
        mContext = context;
        // 连接服务端
        try {
            serverSocket = new Socket(HOSTNAME,PORT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }




    public void send(String Msg, CountDownLatch countDownLatch) {

        try {
            // 向服务端发送消息
            out = new PrintWriter(serverSocket.getOutputStream(), true);
            out.println(Msg);
            // 从服务端接收消息并打印
            in = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
            String packageName = in.readLine();
            CurrentClickUtil.setClickPackageName(packageName);
            String serverMsg = in.readLine();
            CollectDataUtil.getInstance(mContext.getApplicationContext()).setSdkJson(serverMsg);
            countDownLatch.countDown();

        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                in.close();
                out.close();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
    }


    /**
     * 创建文件夹
     */
    public void newDirectory(String _path, String dirName) {
        File file = new File(_path + "/" + dirName);
        try {
            if (!file.exists()) {
                file.mkdirs();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
