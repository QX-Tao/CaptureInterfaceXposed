package com.android.captureinterfacexposed.socket;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.util.Log;

import com.android.captureinterfacexposed.utils.CollectDataUtil;
import com.android.captureinterfacexposed.utils.CurrentCollectUtil;

import java.io.BufferedReader;
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

    private final Socket serverSocket;

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
            Log.d("socket get pkgName", String.valueOf(packageName));
            CurrentCollectUtil.setCollectPackageName(packageName);
            String serverMsg = in.readLine();
            Log.d("socket get sdkJson", String.valueOf(serverMsg));
            CollectDataUtil.getInstance(mContext.getApplicationContext()).setSdkJson(serverMsg);
            countDownLatch.countDown();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
                out.close();
            } catch (IOException e) {
                Log.e(TAG,"socket error");
            }

        }
    }

}
