package com.android.captureinterfacexposed.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.android.captureinterfacexposed.R;
import com.android.captureinterfacexposed.utils.factory.ChannelFactory;
import com.android.captureinterfacexposed.utils.CollectDataUtil;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * 屏幕录制服务
 */
public class ScreenShotService extends Service {
    private static final int SERVICE_ID = 12345; // 任何整数 ID 都可以用于前台服务
    private static final String CHANNEL_ID = "my_channel_id"; // 设置一个唯一的通知通道 ID

    private static final String SCREEN_SHOT_TAG = "screenShot";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // 创建通知通道作为前台服务（针对 Android 8.0 及以上版本）
        // 创建了一个前台服务通知，它将在应用程序不在前台运行时使服务可见。
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "My Channel", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setSound(null, null);
        channel.enableVibration(false);
        notificationManager.createNotificationChannel(channel);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("界面信息采集")
                .setContentText("收集实时截屏...")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .build();

        startForeground(SERVICE_ID, notification);

        // do work

        Intent data = intent.getParcelableExtra("data");
        int resultCode = intent.getIntExtra("resultCode", -1);


        ExecutorService executor = Executors.newSingleThreadExecutor(); // 使用单独的线程池，以避免阻塞主UI线程;
        executor.execute(new ScreenShotTask(data, resultCode));

//        doScreenShot(resultCode, data);
        return START_NOT_STICKY;
    }

    class ScreenShotTask implements Runnable {

        private Intent data;

        private int resultCode;

        public ScreenShotTask(Intent data, int resultCode) {
            this.data = data;
            this.resultCode = resultCode;
        }

        @Override
        public void run() {
            // 阻塞获取直到消息为false
            while (true) {
                // 接收消息，开启截屏
                boolean start = ChannelFactory.getStartScreenShot().receive();
                if (!start) {
                    break;
                }
                Log.d(SCREEN_SHOT_TAG, "receive start screenshot");
                Bitmap screenShot = doScreenShot(resultCode, data);
                Log.d(SCREEN_SHOT_TAG, "send end screenshot");

                CollectDataUtil.getInstance(getApplicationContext()).setScreenPng(screenShot);

                ChannelFactory.getEndScreenShot().send(true);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * 截图
     *
     * @param resultCode
     * @param intent
     */
    private Bitmap doScreenShot(int resultCode, Intent intent) {
        // 将Intent数据转换为MediaProjection对象
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        MediaProjection mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, intent);

        if (mediaProjection == null) {
            return null;
        }

        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        // 获取屏幕参数
        DisplayMetrics metrics = new DisplayMetrics();

        windowManager.getDefaultDisplay().getRealMetrics(metrics);
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;
        int screenDensity = metrics.densityDpi;

        return shot(mediaProjection, screenWidth, screenHeight, screenDensity);
    }

    private Bitmap shot(MediaProjection mediaProjection, int screenWidth, int screenHeight, int screenDensity) {
        ImageReader imageReader = ImageReader.newInstance(screenWidth, screenHeight, (int) PixelFormat.RGBA_8888, 60);
        Surface surface = imageReader.getSurface();
        VirtualDisplay virtualDisplay = mediaProjection.createVirtualDisplay("ScreenCapture",
                screenWidth, screenHeight, screenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface, null, null);
        //取现在最新的图片
        SystemClock.sleep(2000);
        // 开始截图，并处理图像数据
        // ...
        Image image = imageReader.acquireLatestImage();
        // 停止截屏
        virtualDisplay.release();

        Bitmap bitmap = image2Bitmap(image);

        Log.d("TAG", "doScreenShot: " + bitmap);

        return bitmap;
    }

    /**
     * image2Bitmap
     *
     * @param image
     * @return
     */
    public static Bitmap image2Bitmap(Image image) {
        if (image == null) {
            System.out.println("image 为空");
            return null;
        }
        int width = image.getWidth();
        int height = image.getHeight();

        final Image.Plane[] planes = image.getPlanes();
        final ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;

        Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);
        bitmap = Bitmap.createBitmap(bitmap,0,0,width,height);

        image.close();
        return bitmap;
    }

}

