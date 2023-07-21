package com.android.captureinterfacexposed.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.android.captureinterfacexposed.application.DefaultApplication;
import com.android.captureinterfacexposed.hook.HookEntry;
import com.android.captureinterfacexposed.hook.UIHierarchyHook;
import com.android.captureinterfacexposed.ui.activity.MainActivity;
import com.blankj.utilcode.util.FileUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class CollectDataUtil {
    private static CollectDataUtil instance = null;
    private String sdkJson;
    private String accessibleJson;
    private Bitmap screenPng;
    private final Context context;
    private static final ReentrantLock sdkLock = new ReentrantLock();
    private static final ReentrantLock accessibleLock = new ReentrantLock();

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
    public boolean isCollectIntactCmd(){
        return sdkJson != null && accessibleJson != null;
    }

    public Boolean saveCollectData() throws IOException {
        if(CurrentCollectUtil.getCollectFilePath() == null){
            Log.e("Data Collect","path null error");
            return false;
        }
        if(!isUseCmdGetScreen() && !isCollectIntact()) {
            Log.e("Data Collect","collect null error");
            return false;
        }
        if(isUseCmdGetScreen() && !isCollectIntactCmd()) {
            Log.e("Data Collect cmd","collect null error");
            return false;
        }
        String sdkFileName = "SDK" + "_" + "TreeView(" + CurrentCollectUtil.getInterfaceNum() +").json";
        String sdkFtrFilePath = CurrentCollectUtil.getCollectFilePath() + File.separator + sdkFileName;
        File sdkSaveFile = new File(sdkFtrFilePath);
        FileOutputStream sdkFileOutputStream = new FileOutputStream(sdkSaveFile, true);
        sdkLock.lock();
        try (sdkFileOutputStream; FileChannel sdkFileChannel = sdkFileOutputStream.getChannel()) {
            ByteBuffer byteBuffer = ByteBuffer.wrap(sdkJson.getBytes());
            sdkFileChannel.write(byteBuffer);
        } finally {
            sdkLock.unlock();
        }

        String accessibleFileName = "无障碍" + "_" + "TreeView(" + CurrentCollectUtil.getInterfaceNum() + ").json";
        String accessibleFilePath = CurrentCollectUtil.getCollectFilePath() + File.separator + accessibleFileName;
        File accessibleSaveFile = new File(accessibleFilePath);
        FileOutputStream accessFileOutputStream = new FileOutputStream(accessibleSaveFile, true);
        accessibleLock.lock();
        try (accessFileOutputStream; FileChannel accessibleChannel = accessFileOutputStream.getChannel()){
            ByteBuffer byteBuffer = ByteBuffer.wrap(accessibleJson.getBytes());
            accessibleChannel.write(byteBuffer);
        } finally {
            accessibleLock.unlock();
        }

        if(isUseCmdGetScreen()) {
            String screenName = "Screen(" + CurrentCollectUtil.getInterfaceNum() + ").png";
            DefaultApplication.getScreen(CurrentCollectUtil.getCollectFilePath() + File.separator + screenName);
        } else {
            SaveImageUtil.saveAlbum(context, screenPng, Bitmap.CompressFormat.PNG, 100, true, "Screen");
        }
        return true;
    }

    private boolean isUseCmdGetScreen(){
        return ShareUtil.getBoolean(context.getApplicationContext(), "use_cmd", false) &&
                ShareUtil.getBoolean(context.getApplicationContext(),"lsp_hook",false);
    }

    public void saveActivityNameToFile(){
        String filePath = CurrentCollectUtil.getCollectFilePath() + File.separator + "Activity_" + MainActivity.getCurrentActivityName();
        FileUtils.createOrExistsFile(filePath);
    }
    public boolean isSameActivity(){
        if (!ShareUtil.getBoolean(context.getApplicationContext(),"lsp_hook",false)) return true;
        String filePath = CurrentCollectUtil.getCollectFilePath();
        if (filePath == null) return false;
        String currentActivity = MainActivity.getCurrentActivityName();
        String localSaveActivity = null;
        List<String> filesName = getFileNames(filePath);
        for (int i = 0; i < filesName.size(); i++){
            if(filesName.get(i).contains("Activity_")){
                localSaveActivity = filesName.get(i).substring(9);
                break;
            }
        }
        assert currentActivity != null;
        return currentActivity.equals(localSaveActivity);
    }

    private List<String> getFileNames(String path) {
        File dir = new File(path);
        File[] files = dir.listFiles(File::isFile);
        if (files != null) {
            List<String> fileNames = new ArrayList<>();
            for (File file : files) {
                fileNames.add(file.getName());
            }
            return fileNames;
        } else {
            return Collections.emptyList();
        }
    }
}

