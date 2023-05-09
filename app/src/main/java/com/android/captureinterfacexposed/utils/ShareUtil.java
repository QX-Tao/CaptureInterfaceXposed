package com.android.captureinterfacexposed.utils;


import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

/**
 * @author: panyitao
 * @date: 2022/08/16 22:27
 * @description: SharedPreferences Tool.
 */
public class ShareUtil {

    public static void putString(Context mContext,String key,String value){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        sp.edit().putString(key, value).apply();
    }

    //键 默认值
    public static String getString(Context mContext,String key,String defValue){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        return sp.getString(key,defValue);
    }

    //键 值
    public static void putInt(Context mContext,String key,int value){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        sp.edit().putInt(key, value).apply();
    }

    //键 默认值
    public static int getInt(Context mContext,String key,int defValue){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        return sp.getInt(key,defValue);
    }

    //键 值
    public static void putBoolean(Context mContext, String key, boolean value) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        sp.edit().putBoolean(key, value).apply();
    }

    //键 默认值
    public static boolean getBoolean(Context mContext, String key, boolean defValue) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        return sp.getBoolean(key, defValue);
    }

    //删除 单个
    public static void delShare(Context mContext, String key){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        sp.edit().remove(key).apply();
    }

    //删除 全部
    public static void delAll(Context mContext){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        sp.edit().clear().apply();
    }
}
