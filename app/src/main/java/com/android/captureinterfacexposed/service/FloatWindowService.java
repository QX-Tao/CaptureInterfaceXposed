package com.android.captureinterfacexposed.service;

import static android.os.Environment.DIRECTORY_DOWNLOADS;
import static android.os.Environment.getExternalStoragePublicDirectory;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.media.projection.MediaProjectionManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;

import com.android.captureinterfacexposed.R;
import com.android.captureinterfacexposed.utils.factory.ChannelFactory;
import com.android.captureinterfacexposed.db.PageDataHelper;
import com.android.captureinterfacexposed.socket.ClientSocket;
import com.android.captureinterfacexposed.utils.CollectDataUtil;
import com.android.captureinterfacexposed.utils.CurrentClickUtil;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 悬浮按钮的视图
 */
public class FloatWindowService implements View.OnTouchListener, View.OnClickListener, View.OnLongClickListener {

    // 截图请求码
    private static final int SCREENSHOT_REQUEST_CODE = 100;
    private static final String SCREEN_SHOT_TAG = "screenShot";
    private static final String DUMP_VIEW_TREE_TAG = "dumpViewTree";


    private PageDataHelper mDbHelper;
    private final Context context;
    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams layoutParams;

    // 按钮的坐标消息
    private int initialX;
    private int initialY;
    private float initialTouchX;
    private float initialTouchY;

    // 是否移动
    private boolean isMove;

    private boolean isCreate = false;

    private int isClick = -1;

    private MediaProjectionManager mediaProjectionManager;

    private ImageView iv_first;
    private ImageView iv_second;
    private File filePath;


    public FloatWindowService(Context context) {
        this.context = context;
    }

    public void setActivity(Activity activity) {
        Window window = activity.getWindow();
        windowManager = activity.getWindowManager();
    }

    public void startFloatingView() {
        // 1. 创建用于展示悬浮按钮的 view
        floatingView = LayoutInflater.from(context).inflate(R.layout.layout_floating_widget, null);
        iv_first = floatingView.findViewById(R.id.iv_first);
        iv_second = floatingView.findViewById(R.id.iv_second);

        // 2. 设置view的位置和大小
        layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
        layoutParams.x = 0;
        layoutParams.y = 100;

        // 3. 添加触摸监听器，实现按钮拖动
        floatingView.setOnTouchListener(this);

        // 4. 添加点击监听器，实现点击事件
        floatingView.setOnClickListener(this);
        iv_first.setOnClickListener(this);
        iv_second.setOnClickListener(this);
        iv_second.setOnLongClickListener(this);

        // 5. 实例化MediaProjectionManager,开启截屏意图
        mediaProjectionManager = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent screenCaptureIntent = mediaProjectionManager.createScreenCaptureIntent();
        ((Activity) context).startActivityForResult(screenCaptureIntent, SCREENSHOT_REQUEST_CODE);

        // 6. 将 View 添加到 window 中
        windowManager.addView(floatingView, layoutParams);

        // 创建文件夹
        filePath = getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS);
        newDirectory(filePath.toString(), context.getResources().getString(R.string.app_name));
        filePath = new File(filePath + File.separator + context.getResources().getString(R.string.app_name));

        // 数据库
        mDbHelper = new PageDataHelper(context);

    }

    public void stopFloatingView() {
        if (floatingView != null) {
            windowManager.removeView(floatingView);
            floatingView = null;
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //获取手指相对于悬浮窗口的坐标和悬浮窗口的初始位置
                initialX = layoutParams.x;
                initialY = layoutParams.y;
                initialTouchX = event.getRawX();
                initialTouchY = event.getRawY();
                isMove = false;
                break;
            case MotionEvent.ACTION_MOVE:
                // 根据手指的位置更新悬浮窗口的位置
                layoutParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                layoutParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                // 更新悬浮窗口的位置
                windowManager.updateViewLayout(floatingView, layoutParams);
                isMove = true;
                break;
        }
        return isMove;
    }

    @Override
    public void onClick(View view) {

        switch (view.getId()){
            case R.id.iv_first:
                CurrentClickUtil.setInterfaceNum(1);
                isCreate = true;
                isClick = 0;
                break;
            case R.id.iv_second:
                int tgNum = CurrentClickUtil.getInterfaceNum() + 1;
                CurrentClickUtil.setInterfaceNum(tgNum);
                isClick = 1;
                break;
        }

        if(isCreate){
            CollectDataUtil collectDataUtil = CollectDataUtil.getInstance(context.getApplicationContext());
            collectDataUtil.initCollectData();
            CountDownLatch countDownLatch = new CountDownLatch(3);
            // 隐藏按钮
            floatingView.setVisibility(View.INVISIBLE);
            Executor executor = Executors.newFixedThreadPool(4); // 使用单独的线程池，以避免阻塞主UI线程
            executor.execute(() -> {
                boolean end = ChannelFactory.getEndScreenShot().receive();
                if (end) {
                    Log.d(SCREEN_SHOT_TAG, "receive end screenshot");
                    countDownLatch.countDown();
                }
            });
            executor.execute(() -> {
                boolean end = ChannelFactory.getEndDumpViewTree().receive();
                if (end) {
                    Log.d(DUMP_VIEW_TREE_TAG, "receive end dumpViewTree");
                    countDownLatch.countDown();
                }
            });
            executor.execute(() -> {
                try {
                    if(countDownLatch.await(10,TimeUnit.SECONDS)){
                        floatingView.post(() -> floatingView.setVisibility(View.VISIBLE));
                        if(isClick == 0){ // 点击左键
                            String clickFilePath = filePath + File.separator + CurrentClickUtil.getClickPackageName(); // 包名
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");
                            String dateStr = dateFormat.format(System.currentTimeMillis()); // 时间戳
                            CurrentClickUtil.setClickTime(dateStr);
                            newDirectory(clickFilePath, dateStr);
                            CurrentClickUtil.setClickFilePath(clickFilePath + File.separator  + dateStr);
                            collectDataUtil.saveCollectData();

                            long pageId = mDbHelper.getPageIdByPkgName(CurrentClickUtil.getClickPackageName()); // 根据包名查pageId
                            if(pageId == -1) { // 未记录
                                String appName = getAppNameByPkgName(CurrentClickUtil.getClickPackageName());
                                pageId = mDbHelper.addPage(CurrentClickUtil.getClickPackageName(),appName,1); // page表 + 1行,该应用记录一张页面,pageId为新插入的第几行
                                mDbHelper.addCollect(pageId, CurrentClickUtil.getClickTime(), CurrentClickUtil.getInterfaceNum()); // collect表 + 1行
                            } else {
                                mDbHelper.incrementPageNumByPkgName(CurrentClickUtil.getClickPackageName()); // page记录 + 1
                                mDbHelper.addCollect(pageId, CurrentClickUtil.getClickTime(), CurrentClickUtil.getInterfaceNum()); // collect表 + 1行
                            }
                        } else { // 点击右键
                            collectDataUtil.saveCollectData();
                            long pageId = mDbHelper.getPageIdByPkgName(CurrentClickUtil.getClickPackageName()); // 根据包名查pageId
                            mDbHelper.incrementPageCollectNumByIdAndData(pageId, CurrentClickUtil.getClickTime()); // collect表记录 + 1
                        }
                    } else {
                        Log.e("CountDownLatch", "Timeout waiting for tasks to complete");
                        floatingView.post(() -> floatingView.setVisibility(View.VISIBLE));
                    }
                    mDbHelper.close();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    Log.e("Collect data", "error");
                    throw new RuntimeException(e);
                }
            });


            Log.d(SCREEN_SHOT_TAG, "send start screenshot");
            // 通知开启截屏
            ChannelFactory.getStartScreenShot().send(true);
            // 通知无障碍收集控件树
            Log.d(DUMP_VIEW_TREE_TAG, "send start dumpViewTree");
            ChannelFactory.getStartDumpViewTree().send(true);


            Thread thread1 = new Thread(() -> {
                try {
                    ClientSocket c = new ClientSocket(context.getApplicationContext(),"127.0.0.1", 9000);
                    c.send("开始收集",countDownLatch);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            thread1.start();
        }
    }

    @Override
    public boolean onLongClick(View v) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("请选择一个选项");
        AlertDialog dialog = builder.create();
        dialog.show();
        return true;
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

    /**
     * get app name by its packageName
     *
     * 获取应用图标
     */
    private String getAppNameByPkgName(String packageName) {
        PackageManager packageManager = context.getPackageManager();
        try {
            ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
            return packageManager.getApplicationLabel(appInfo).toString();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }


}

