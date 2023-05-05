package com.android.captureinterfacexposed.utils;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ConfigUtil {

    private static final String TAG = "CONFIG TAG";
    private static final String FILE_NAME = "Config";

    private static ConfigUtil sInstance;
    private final Context mContext;
    private final Map<String, String> mCache;

    private ConfigUtil(Context context) {
        mContext = context.getApplicationContext();
        mCache = new HashMap<>();
        loadCache();
    }

    public static synchronized ConfigUtil getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ConfigUtil(context);
        }
        return sInstance;
    }

    public void putString(String key, String value) {
        mCache.put(key, value);
        saveCache();
    }

    public String getString(String key, String defaultValue) {
        if (mCache.containsKey(key)) {
            if(Objects.equals(mCache.get(key), "null")){
                return null;
            } else {
                return mCache.get(key);
            }
        } else {
            return defaultValue;
        }
    }

    private void loadCache() {
        File file = new File(mContext.getFilesDir(), FILE_NAME);
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("=");
                    if (parts.length == 2) {
                        mCache.put(parts[0], parts[1]);
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to load cache", e);
            }
        }
    }

    private void saveCache() {
        File file = new File(mContext.getFilesDir(), FILE_NAME);
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)))) {
            for (Map.Entry<String, String> entry : mCache.entrySet()) {
                writer.write(entry.getKey() + "=" + entry.getValue() + "\n");
            }
            writer.flush();
        } catch (IOException e) {
            Log.e(TAG, "Failed to save cache", e);
        }
    }

}
