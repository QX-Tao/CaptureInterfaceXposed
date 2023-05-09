package com.android.captureinterfacexposed.service;

import static android.os.Environment.DIRECTORY_DOWNLOADS;
import static android.os.Environment.getExternalStoragePublicDirectory;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.media.projection.MediaProjectionManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.android.captureinterfacexposed.R;
import com.android.captureinterfacexposed.application.DefaultApplication;
import com.android.captureinterfacexposed.utils.ConfigUtil;
import com.android.captureinterfacexposed.utils.ShareUtil;
import com.android.captureinterfacexposed.utils.factory.ChannelFactory;
import com.android.captureinterfacexposed.db.PageDataHelper;
import com.android.captureinterfacexposed.socket.ClientSocket;
import com.android.captureinterfacexposed.utils.CollectDataUtil;
import com.android.captureinterfacexposed.utils.CurrentCollectUtil;
import com.highcapable.yukihookapi.YukiHookAPI;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FloatWindowService {
    private static final int SCREENSHOT_REQUEST_CODE = 100; // 截图请求码
    private static final String LSP_HOOK = "lsp_hook";
    private static final String APPLICATION_PACKAGE_NAME = "com.android.captureinterfacexposed";
    private static final String SCREEN_SHOT_TAG = "screenShot";
    private static final String DUMP_VIEW_TREE_TAG = "dumpViewTree";
    private PageDataHelper mDbHelper;
    private final Context context;
    private WindowManager windowManager;
    private View floatingView;
    private View floatListView;
    private WindowManager.LayoutParams floatingViewLayoutParams;
    private WindowManager.LayoutParams floatListViewLayoutParams;
    private boolean floatingViewOnTop = false;
    private boolean isStartFloatListView = false;
    private boolean isDisplayPageItemList = false;
    private int isClick = -1;
    private File filePath;
    private List<PageCollectItem> pageCollectItemList;
    private List<PageItem> pageItemList;
    private List<PageDataHelper.PageCollect> pageTmpCollectList;
    private static List<ApplicationInfo> appList;
    private ListView lv_select_location;

    public FloatWindowService(Context context) {
        this.context = context;
    }

    public void setActivity(Activity activity) {
        windowManager = activity.getWindowManager();
        // 创建文件夹
        filePath = getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS);
        newDirectory(filePath.toString(), context.getResources().getString(R.string.app_name));
        filePath = new File(filePath + File.separator + context.getResources().getString(R.string.app_name));
        // 数据库
        mDbHelper = new PageDataHelper(context);
    }

    public void startFloatingView() {
        // 1. 创建用于展示悬浮按钮的 view
        floatingView = LayoutInflater.from(context).inflate(R.layout.layout_floating_widget, null);
        ImageView iv_first = floatingView.findViewById(R.id.iv_first);
        ImageView iv_second = floatingView.findViewById(R.id.iv_second);

        // 2. 设置view的位置和大小
        floatingViewLayoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        floatingViewLayoutParams.gravity = Gravity.TOP | Gravity.START;
        floatingViewLayoutParams.x = 0;
        floatingViewLayoutParams.y = 100;

        // 3. 添加触摸监听器，实现按钮拖动
        floatingView.setOnTouchListener(new ItemViewTouchListener(floatingViewLayoutParams, windowManager,true));

        // 4. 添加点击监听器，实现点击事件
        iv_first.setOnClickListener(floatingViewClickListener);
        iv_second.setOnClickListener(floatingViewClickListener);
        iv_second.setOnLongClickListener(floatingViewLongClickListener);

        // 5. 实例化MediaProjectionManager,开启截屏意图
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent screenCaptureIntent = mediaProjectionManager.createScreenCaptureIntent();
        ((Activity) context).startActivityForResult(screenCaptureIntent, SCREENSHOT_REQUEST_CODE);

        // 6. 将 View 添加到 window 中
        windowManager.addView(floatingView, floatingViewLayoutParams);
    }

    public void stopFloatingView() {
        if (floatingView != null) {
            windowManager.removeView(floatingView);
            floatingView = null;
        }
    }

    public void startFloatListView() {
        // 1. 创建用于展示悬浮按钮的 view
        floatListView = LayoutInflater.from(context).inflate(R.layout.layout_floating_window, null);
        TextView bt_refresh = floatListView.findViewById(R.id.bt_refresh);
        TextView bt_switch_app = floatListView.findViewById(R.id.bt_switch_app);
        TextView tv_current_location = floatListView.findViewById(R.id.tv_current_location);
        ImageView bt_close = floatListView.findViewById(R.id.bt_close);
        lv_select_location = floatListView.findViewById(R.id.select_location_list_view);

        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;
        Log.d("screenWidth", String.valueOf(metrics.widthPixels));
        Log.d("screenHeight", String.valueOf(metrics.heightPixels));

        // 2. 设置view的位置和大小
        floatListViewLayoutParams = new WindowManager.LayoutParams(
                (int) ( 0.85 * screenWidth),
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        floatListViewLayoutParams.gravity = Gravity.START | Gravity.TOP;
        floatListViewLayoutParams.x = screenWidth / 2 - floatListView.getWidth() / 2;
        floatListViewLayoutParams.y = screenHeight / 2 - floatListView.getHeight() / 2;

        // 3. 添加触摸监听器，实现按钮拖动
        floatListView.setOnTouchListener(new ItemViewTouchListener(floatListViewLayoutParams, windowManager,false));

        // 4. 添加点击监听器，实现点击事件
        bt_refresh.setOnClickListener(floatListViewClickListener);
        bt_switch_app.setOnClickListener(floatListViewClickListener);
        bt_close.setOnClickListener(floatListViewClickListener);

        if(getWorkModeStatus() != 1) { bt_switch_app.setVisibility(View.INVISIBLE); }
        else { bt_switch_app.setVisibility(View.VISIBLE); }

        // 5. 将 View 添加到 window 中
        windowManager.addView(floatListView, floatListViewLayoutParams);
        isStartFloatListView = true;

        // 6. displayData
        long pageId = mDbHelper.getPageIdByPkgName(CurrentCollectUtil.getCollectPackageName());
        String tv_location = getAppNameByPkgName(CurrentCollectUtil.getCollectPackageName()) + " (" + CurrentCollectUtil.getCollectPackageName() + ")";
        pageTmpCollectList = mDbHelper.getPageCollectsByMid(pageId);
        pageCollectItemList = getPageCollectItemList(pageTmpCollectList);
        tv_current_location.setText(tv_location);
        PageCollectItemListAdapter pageCollectItemAdapter = new PageCollectItemListAdapter(pageCollectItemList);
        lv_select_location.setAdapter(pageCollectItemAdapter);
        lv_select_location.setOnItemClickListener(SelectLocationListViewItemClickListener);
    }

    public void stopFloatListView() {
        if (floatListView != null) {
            windowManager.removeView(floatListView);
            floatListView = null;
            isStartFloatListView = false;
        }
    }

    View.OnClickListener floatingViewClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.iv_first:
                    CurrentCollectUtil.setInterfaceNum(1);
                    CurrentCollectUtil.setRightButtonClickable(true);
                    isClick = 0;
                    break;
                case R.id.iv_second:
                    int tgNum = CurrentCollectUtil.getInterfaceNum() + 1;
                    CurrentCollectUtil.setInterfaceNum(tgNum);
                    isClick = 1;
                    break;
            }
            if(!CurrentCollectUtil.isLeftButtonClickable()){
                return;
            }
            if(CurrentCollectUtil.isRightButtonClickable()){
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
                                String clickFilePath = filePath + File.separator + CurrentCollectUtil.getCollectPackageName(); // 包名
                                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");
                                String dateStr = dateFormat.format(System.currentTimeMillis()); // 时间戳
                                CurrentCollectUtil.setCollectTime(dateStr);
                                newDirectory(clickFilePath, dateStr);
                                CurrentCollectUtil.setCollectFilePath(clickFilePath + File.separator  + dateStr);
                                collectDataUtil.saveCollectData();

                                long pageId = mDbHelper.getPageIdByPkgName(CurrentCollectUtil.getCollectPackageName()); // 根据包名查pageId
                                if(pageId == -1) { // 未记录
                                    String appName = getAppNameByPkgName(CurrentCollectUtil.getCollectPackageName());
                                    pageId = mDbHelper.addPage(CurrentCollectUtil.getCollectPackageName(),appName,1); // page表 + 1行,该应用记录一张页面,pageId为新插入的第几行
                                    mDbHelper.addCollect(pageId, CurrentCollectUtil.getCollectTime(), CurrentCollectUtil.getInterfaceNum()); // collect表 + 1行
                                } else {
                                    mDbHelper.incrementPageNumByPkgName(CurrentCollectUtil.getCollectPackageName()); // page记录 + 1
                                    mDbHelper.addCollect(pageId, CurrentCollectUtil.getCollectTime(), CurrentCollectUtil.getInterfaceNum()); // collect表 + 1行
                                }
                            } else { // 点击右键
                                collectDataUtil.saveCollectData();
                                long pageId = mDbHelper.getPageIdByPkgName(CurrentCollectUtil.getCollectPackageName()); // 根据包名查pageId
                                mDbHelper.incrementPageCollectNumByIdAndData(pageId, CurrentCollectUtil.getCollectTime()); // collect表记录 + 1
                            }
                        } else {
                            Log.e("CountDownLatch", "Timeout waiting for tasks to complete");
                            floatingView.post(() -> floatingView.setVisibility(View.VISIBLE));
                            if(isClick == 0) {
                                CurrentCollectUtil.setRightButtonClickable(false);
                            } else {
                                int tgNum = CurrentCollectUtil.getInterfaceNum() - 1;
                                CurrentCollectUtil.setInterfaceNum(tgNum);
                            }
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
    };

    View.OnClickListener floatListViewClickListener = v -> {
        switch (v.getId()){
            case R.id.bt_refresh:
                String tv_location = getAppNameByPkgName(CurrentCollectUtil.getCollectPackageName()) + " (" + CurrentCollectUtil.getCollectPackageName() + ")";
                TextView tv_current_location = floatListView.findViewById(R.id.tv_current_location);
                tv_current_location.setText(tv_location);
                long pageId = mDbHelper.getPageIdByPkgName(CurrentCollectUtil.getCollectPackageName());
                pageTmpCollectList = mDbHelper.getPageCollectsByMid(pageId);
                pageCollectItemList = getPageCollectItemList(pageTmpCollectList);
                lv_select_location = floatListView.findViewById(R.id.select_location_list_view);
                PageCollectItemListAdapter pageCollectItemAdapter = new PageCollectItemListAdapter(pageCollectItemList);
                lv_select_location.setAdapter(pageCollectItemAdapter);
                isDisplayPageItemList = false;
                updateFloatListView();
                break;
            case R.id.bt_switch_app:
                pageItemList = new ArrayList<>();
                for (ApplicationInfo app: appList) {
                    String packageName = app.packageName;
                    String appName = getAppNameByPkgName(packageName);
                    String pageNum = "0";
                    long tmpPageId = mDbHelper.getPageIdByPkgName(packageName);
                    if(tmpPageId != -1) pageNum = String.valueOf(mDbHelper.getPageNumById(tmpPageId));
                    Drawable appIcon = getIconByPkgName(packageName);
                    PageItem pageItem = new PageItem(appName,packageName,pageNum,appIcon);
                    pageItemList.add(pageItem);
                }
                PageItemListAdapter pageItemListAdapter = new PageItemListAdapter(pageItemList);
                lv_select_location.setAdapter(pageItemListAdapter);
                isDisplayPageItemList = true;
                updateFloatListView();
                break;
            case R.id.bt_close:
                stopFloatListView();
                break;
        }
    };

    View.OnLongClickListener floatingViewLongClickListener = v -> {
        if(CurrentCollectUtil.getCollectPackageName() != null) {
            if (!isStartFloatListView) {
                startFloatListView();
                return true;
            }
        }
        return false;
    };

    AdapterView.OnItemClickListener SelectLocationListViewItemClickListener = (parent, view, position, id) -> {
        if(isDisplayPageItemList){
            String packageName = pageItemList.get(position).packageName;
            DefaultApplication.killApp(CurrentCollectUtil.getCollectPackageName());
            DefaultApplication.enableHookByLSP(APPLICATION_PACKAGE_NAME, packageName);
            CurrentCollectUtil.setCollectPackageName(packageName);
            CurrentCollectUtil.setRightButtonClickable(false);
            isDisplayPageItemList = false;
            stopFloatListView();
        } else {
            String pageCollectData = pageCollectItemList.get(position).pageCollectData;
            String pageCollectNum = pageCollectItemList.get(position).pageCollectNum;
            CurrentCollectUtil.setCollectTime(pageCollectData);
            CurrentCollectUtil.setInterfaceNum(Integer.parseInt(pageCollectNum));
            CurrentCollectUtil.setRightButtonClickable(true);
            stopFloatListView();
        }
    };

    /**
     * new a directory
     *
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
     * 获取应用名称
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

    /**
     * get app icon by its packageName
     *
     * 获取应用图标
     */
    private Drawable getIconByPkgName(String packageName) {
        PackageManager packageManager = context.getPackageManager();
        try {
            ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
            return packageManager.getApplicationIcon(appInfo);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * touch listener
     *
     * 触摸监听器
     */
    private class ItemViewTouchListener implements View.OnTouchListener {
        private final WindowManager.LayoutParams layoutParams;
        private final WindowManager windowManager;
        private final boolean isFloatingView;
        private float lastX = 0.0f;
        private float lastY = 0.0f;
        ItemViewTouchListener(WindowManager.LayoutParams layoutParams, WindowManager windowManager, Boolean isFloatingView) {
            this.layoutParams = layoutParams;
            this.windowManager = windowManager;
            this.isFloatingView = isFloatingView;
        }
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()){
                case MotionEvent.ACTION_DOWN:
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (lastX == 0.0f || lastY == 0.0f) {
                        lastX = event.getRawX();
                        lastY = event.getRawY();
                    }
                    float nowX = event.getRawX();
                    float nowY = event.getRawY();
                    float movedX = nowX - lastX;
                    float movedY = nowY - lastY;
                    WindowManager.LayoutParams params = this.layoutParams;
                    params.x += (int) movedX;
                    params.y += (int) movedY;
                    windowManager.updateViewLayout(v,layoutParams);
                    lastX = nowX;
                    lastY = nowY;
                    break;
                case MotionEvent.ACTION_UP:
                    lastX = 0.0f;
                    lastY = 0.0f;
                    if(isFloatingView){
                        if(!floatingViewOnTop){
                            windowManager.removeView(floatingView);
                            windowManager.addView(floatingView,floatingViewLayoutParams);
                            floatingViewOnTop = true;
                        }
                    } else {
                        if(floatingViewOnTop){
                            windowManager.removeView(floatListView);
                            windowManager.addView(floatListView,floatListViewLayoutParams);
                            floatingViewOnTop = false;
                        }
                    }
                    break;
            }
            return true;
        }
    }

    /**
     * page collect item class
     *
     * pageCollectItem类
     */
    private static class PageCollectItem{
        private final String pageCollectData;
        private final String pageCollectNum;
        PageCollectItem(String pageCollectData, String pageCollectNum){
            this.pageCollectData = pageCollectData;
            this.pageCollectNum = pageCollectNum;
        }
        @NonNull
        @Override
        public String toString(){
            return "pageCollectData: " + pageCollectData + "pageCollectNum: " + pageCollectNum;
        }
    }

    /**
     * get pageCollectItemList
     *
     * 获取pageCollectItemList
     */
    private List<PageCollectItem> getPageCollectItemList(List<PageDataHelper.PageCollect> pageCollects){
        ArrayList<PageCollectItem> pageCollectItems = new ArrayList<>();
        for(int i = 0;i < pageCollects.size(); i++){
            String pageCollectData = pageCollects.get(i).pageCollectData;
            String pageCollectNum = pageCollects.get(i).pageCollectNum;
            PageCollectItem pageCollectItem = new PageCollectItem(pageCollectData, pageCollectNum);
            pageCollectItems.add(pageCollectItem);
        }
        return pageCollectItems;
    }

    /**
     * page collect item list adapter
     *
     * 列表适配器
     */
    private class PageCollectItemListAdapter extends BaseAdapter {
        private final List<PageCollectItem> pageCollectItemList;
        PageCollectItemListAdapter(List<PageCollectItem> pageCollectItemList){
            this.pageCollectItemList = pageCollectItemList;
        }
        @Override
        public int getCount() {
            return pageCollectItemList.size();
        }
        @Override
        public Object getItem(int position) {
            return pageCollectItemList.get(position).toString();
        }
        @Override
        public long getItemId(int position) {
            return position;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if(convertView == null){
                convertView = LayoutInflater.from(context).inflate(R.layout.float_list_item,parent,false);
                holder = new ViewHolder();
                holder.pageCollectIcon = convertView.findViewById(R.id.float_image_view);
                holder.pageCollectData = convertView.findViewById(R.id.main_text_view);
                holder.pageCollectNum = convertView.findViewById(R.id.num_text_view);
                holder.pageCollectDesc = convertView.findViewById(R.id.desc_text_view);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            PageCollectItem index = pageCollectItemList.get(position);
            if(Objects.equals(index.pageCollectNum, "1")) holder.pageCollectIcon.setImageResource(R.drawable.ic_collect_page_less);
            else if(Objects.equals(index.pageCollectNum, "2")) holder.pageCollectIcon.setImageResource(R.drawable.ic_collect_page_default);
            else holder.pageCollectIcon.setImageResource(R.drawable.ic_collect_page_more);
            holder.pageCollectData.setText( index.pageCollectData);
            holder.pageCollectNum.setText(index.pageCollectNum + "份");
            holder.pageCollectDesc.setText("收集时间");
            return convertView;
        }

        private class ViewHolder{
            public ImageView pageCollectIcon;
            public TextView pageCollectData;
            public TextView pageCollectNum;
            public TextView pageCollectDesc;
        }
    }

    /**
     * page item class
     *
     * pageItem类
     */
    private static class PageItem{
        private final String appName;
        private final String packageName;
        private final String pageNum;
        private final Drawable appIcon;
        PageItem(String appName, String packageName, String pageNum, Drawable appIcon){
            this.appName = appName;
            this.packageName = packageName;
            this.pageNum = pageNum;
            this.appIcon = appIcon;
        }
        @NonNull
        @Override
        public String toString(){
            return "appName: " + appName + "packageName: " + packageName + "pageNum: " + pageNum;
        }
    }

    /**
     * page item list adapter
     *
     * 列表适配器
     */
    private class PageItemListAdapter extends BaseAdapter {
        private final List<PageItem> pageItemList;
        PageItemListAdapter(List<PageItem> pageItemList){
            this.pageItemList = pageItemList;
        }
        @Override
        public int getCount() {
            return pageItemList.size();
        }
        @Override
        public Object getItem(int position) {
            return pageItemList.get(position).toString();
        }
        @Override
        public long getItemId(int position) {
            return position;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if(convertView == null){
                convertView = LayoutInflater.from(context).inflate(R.layout.float_list_item,parent,false);
                holder = new ViewHolder();
                holder.appIcon = convertView.findViewById(R.id.float_image_view);
                holder.appInfo = convertView.findViewById(R.id.main_text_view);
                holder.pageNum = convertView.findViewById(R.id.num_text_view);
                holder.pageDesc = convertView.findViewById(R.id.desc_text_view);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            PageItem index = pageItemList.get(position);
            holder.appIcon.setImageDrawable(index.appIcon);
            holder.appInfo.setText(index.appName + " (" + index.packageName + ")");
            holder.pageNum.setText(index.pageNum + "份");
            holder.pageDesc.setText("应用信息");
            return convertView;
        }

        private class ViewHolder{
            public ImageView appIcon;
            public TextView appInfo;
            public TextView pageNum;
            public TextView pageDesc;
        }
    }

    /**
     * update float list view
     *
     * 刷新浮窗
     */
    private void updateFloatListView(){
        TextView bt_switch_app = floatListView.findViewById(R.id.bt_switch_app);
        TextView bt_refresh = floatListView.findViewById(R.id.bt_refresh);
        if(getWorkModeStatus() != 1) {
            bt_switch_app.setVisibility(View.INVISIBLE);
        } else {
            if (isDisplayPageItemList){
                bt_switch_app.setVisibility(View.INVISIBLE);
                bt_refresh.setText("返回");
            } else {
                bt_switch_app.setVisibility(View.VISIBLE);
                bt_refresh.setText("刷新列表");
            }
        }
        windowManager.updateViewLayout(floatListView, floatListViewLayoutParams);
    }

    /**
     * check work mode: normal -> -1, lsp_off -> 0, lsp_on -> 1
     *
     * 返回运行模式：普通、LSP（未激活）、LSP（已激活）
     */
    private int getWorkModeStatus(){
        boolean isLspHook = ShareUtil.getBoolean(context, LSP_HOOK,false);
        if(isLspHook)
            if(YukiHookAPI.Status.INSTANCE.isXposedModuleActive())
                return 1;
            else
                return 0;
        else
            return -1;
    }

    public void processData(){
        final List<ApplicationInfo> appList_ = context.getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
        appList = new ArrayList<>();
        for (ApplicationInfo app : appList_){
            if(DefaultApplication.entryActivityClassName(app.packageName) != null && !app.packageName.equals(context.getApplicationInfo().packageName)) {
                appList.add(app);
            }
        }
    }
}

