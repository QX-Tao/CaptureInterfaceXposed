package com.android.captureinterfacexposed.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.android.captureinterfacexposed.application.DefaultApplication;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class CollectDataUtil {
    private static CollectDataUtil instance = null;
    private String sdkJson;
    private String accessibleJson;
    private Bitmap screenPng;
    private final Context context;

    private CollectDataUtil(Context context) {
        this.context = context.getApplicationContext();
    }

    public static CollectDataUtil getInstance(Context context){
        if(instance == null){
            instance = new CollectDataUtil(context);
        }
        return instance;
    }

    public String getSdkJson() {
        return sdkJson;
    }

    public void setSdkJson(String sdkJson) {
        this.sdkJson = sdkJson;
    }

    public String getAccessibleJson() {
        return accessibleJson;
    }

    public void setAccessibleJson(String accessibleJson) {
        this.accessibleJson = accessibleJson;
    }

    public Bitmap getScreenPng() {
        return screenPng;
    }

    public void setScreenPng(Bitmap screenPng) {
        this.screenPng = screenPng;
    }

    public void initCollectData(){
        sdkJson = null;
        accessibleJson = null;
        screenPng = null;
    }

    public boolean isCollectIntact(){
        return sdkJson != null && accessibleJson != null && screenPng != null;
    }

    public void saveCollectData() throws IOException {
        if(CurrentCollectUtil.getCollectFilePath() == null){
            Log.e("Data Collect","path null error");
            return;
        }
        String sdkFileName = "SDK" + "_" + "TreeView(" + CurrentCollectUtil.getInterfaceNum() +").json";
        String sdkFtrFilePath = CurrentCollectUtil.getCollectFilePath() + File.separator + sdkFileName;
        File sdkSaveFile = new File(sdkFtrFilePath);
        RandomAccessFile sdkRaf = new RandomAccessFile(sdkSaveFile, "rwd");
        sdkRaf.seek(sdkSaveFile.length());
        sdkRaf.write(sdkJson.getBytes());
        sdkRaf.close();

        String accessibleFileName = "无障碍" + "_" + "TreeView(" + CurrentCollectUtil.getInterfaceNum() + ").json";
        String accessibleFilePath = CurrentCollectUtil.getCollectFilePath() + File.separator + accessibleFileName;
        File accessibleSaveFile = new File(accessibleFilePath);
        RandomAccessFile accessRaf = new RandomAccessFile(accessibleSaveFile, "rwd");
        accessRaf.seek(accessibleSaveFile.length());
        accessRaf.write(accessibleJson.getBytes());
        accessRaf.close();

        if(isUseCmdGetScreen()) {
            String screenName = "Screen(" + CurrentCollectUtil.getInterfaceNum() + ").png";
            DefaultApplication.getScreen(CurrentCollectUtil.getCollectFilePath() + File.separator + screenName);
        } else {
            SaveImageUtil.saveAlbum(context, screenPng, Bitmap.CompressFormat.PNG, 100, true, "Screen");
        }
    }

    private boolean isUseCmdGetScreen(){
        return ShareUtil.getBoolean(context.getApplicationContext(), "use_cmd", false) &&
                ShareUtil.getBoolean(context.getApplicationContext(),"lsp_hook",false);
    }
}

