package com.android.captureinterfacexposed.ui.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.IBinder;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import com.android.captureinterfacexposed.application.DefaultApplication;
import com.android.captureinterfacexposed.R;
import com.android.captureinterfacexposed.databinding.ActivitySelectAppBinding;
import com.android.captureinterfacexposed.ui.activity.base.BaseActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SelectAppActivity extends BaseActivity<ActivitySelectAppBinding> {
    private ListView appListView;
    private SearchView searchView;
    private HashMap<Integer, Integer> posMap = new HashMap<>();
    private ProgressDialog loadingDialog;
    private static List<ApplicationInfo> appList;
    private static ArrayList<AppItem> appItemList;
    private AppListAdapter adapter;

    @Override
    public void onCreate() {

        // TitleThemes
        binding.includeTitleBarSecond.tvTitle.setText(getText(R.string.select_app));
        binding.includeTitleBarSecond.ivBackButton.setOnClickListener(v -> onBackPressed());
        binding.includeTitleBarSecond.ivMoreButton.setOnClickListener(v -> showPopupMenu(binding.includeTitleBarSecond.ivMoreButton));

        appListView = binding.appListView;
        searchView = binding.searchView;

        if(appList == null || appItemList == null){
            loadingDialog = ProgressDialog.show(SelectAppActivity.this, "数据加载中", "请稍后...", true, false);
            new LoadDataTask().execute();
        } else {
            adapter = new AppListAdapter(appItemList);
            appListView.setAdapter(adapter);
        }

        searchView.setSubmitButtonEnabled(false);
        searchView.onActionViewExpanded();

        // 添加搜索框的监听器
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (TextUtils.isEmpty(newText)) {
                    // 没有输入的时候就显示所有
                    setAdapter();
                } else {
                    List<AppItem> list = new ArrayList<>();
                    for (ApplicationInfo app: appList) {
                        String appName = app.loadLabel(getPackageManager()).toString();
                        String packageName = app.packageName;
                        String versionName = getVersionName(packageName);
                        String size = getSize(app);
                        Drawable appIcon = app.loadIcon(getPackageManager());
                        String item = appName + " (" + packageName + ")";
                        if (item.contains(newText)) {
                            int index = appList.indexOf(app);
                            AppItem appItem = new AppItem(item,appName,versionName,size,appIcon,index);
                            list.add(appItem);
                        }
                    }
                    ArrayList<AppItem> new_appItemList = new ArrayList<>(list);
                    adapter = new AppListAdapter(new_appItemList);
                    appListView.setAdapter(adapter);
                }
                return true;
            }
        });

        appListView.setOnItemClickListener((parent, view, position, id) -> {
            String appName = appList.get(posMap.get(position)).loadLabel(SelectAppActivity.this.getPackageManager()).toString();
            String packageName = appList.get(posMap.get(position)).packageName;

            // 将选择的应用名称和包名返回给 MainActivity
            Intent intent = new Intent();
            intent.putExtra("app_name", appName);
            intent.putExtra("package_name", packageName);
            setResult(RESULT_OK, intent);
            finish();
        });
    }


    private class LoadDataTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            processData(); // 拉取数据
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            setAdapter(); // 刷新列表
            loadingDialog.dismiss(); // 关闭进度条
        }
    }

    public void processData() {
        // 获取本机已安装应用信息
        final List<ApplicationInfo> appList_ = getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
        appList = new ArrayList<>();
        for (ApplicationInfo app : appList_){
            // 筛选有活动入口的应用程序
            if(DefaultApplication.entryActivityClassName(app.packageName) != null && !app.packageName.equals(getApplicationInfo().packageName)) {
                appList.add(app);
            }
        }
    }

    private void setAdapter(){
        appItemList = new ArrayList<>();
        for (ApplicationInfo app: appList) {
            String appName = app.loadLabel(getPackageManager()).toString();
            String packageName = app.packageName;
            String versionName = getVersionName(packageName);
            String size = getSize(app);
            Drawable appIcon = app.loadIcon(getPackageManager());
            int index = appList.indexOf(app);
            AppItem appItem = new AppItem(appName + " (" + packageName + ")",appName,versionName,size,appIcon,index);
            appItemList.add(appItem);
        }
        adapter = new AppListAdapter(appItemList);
        appListView.setAdapter(adapter);
    }

    private class AppListAdapter extends BaseAdapter {

        private final ArrayList<AppItem> appItemList;

        public AppListAdapter(ArrayList<AppItem> appItemList) {
            this.appItemList = appItemList;
        }

        @Override
        public int getCount() {
            return appItemList.size();
        }

        @Override
        public String getItem(int position) {
            return appItemList.get(position).toString();
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.app_list_item, parent, false);
            }
            ImageView appIconImageView = convertView.findViewById(R.id.app_icon_image_view);
            TextView appNamesTextView = convertView.findViewById(R.id.app_name_text_view);
            TextView appVersionTextView = convertView.findViewById(R.id.app_version_text_view);
            TextView appSizeTextView = convertView.findViewById(R.id.app_size_text_view);
            int index = appItemList.get(position).index;
            Drawable appIcon = appItemList.get(position).appIcon;
            String appNames = appItemList.get(position).appNames;
            String versionName = appItemList.get(position).appVersion;
            String size = appItemList.get(position).appSize;

            appIconImageView.setImageDrawable(appIcon);
            appNamesTextView.setText(appNames);
            appVersionTextView.setText("版本: " + versionName);
            appSizeTextView.setText(size);
            appNamesTextView.setTextColor(getResources().getColor(R.color.firstTextColor));
            appVersionTextView.setTextColor(getResources().getColor(R.color.secondTextColor));
            appSizeTextView.setTextColor(getResources().getColor(R.color.secondTextColor));

            posMap.put(position, index);
            return convertView;
        }
    }
    private String getVersionName(String packageName) {
        PackageManager packageManager = getPackageManager();
        String versionName = "";
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
            versionName = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionName;
    }

    private String getSize(ApplicationInfo app) {
        String apkPath = app.sourceDir;
        long size = new File(apkPath).length();
        return Formatter.formatFileSize(this, size);
    }

    private static class AppItem {
        private final String appNames;
        private final String appName;
        private final String appVersion;
        private final String appSize;
        private final Drawable appIcon;
        private final int index;

        AppItem(String appNames, String appName, String appVersion, String appSize, Drawable appIcon, int index){
            this.appNames = appNames;
            this.appName = appName;
            this.appVersion = appVersion;
            this.appSize = appSize;
            this.appIcon = appIcon;
            this.index = index;
        }

        @NonNull
        @Override
        public String toString() {
            return "appNames: " + appNames +
                    "\nappName: " + appName +
                    "\nappVersion: " + appVersion +
                    "\nappSize" + appSize;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View view = getCurrentFocus();
            if (isHideInput(view, ev)) {
                HideSoftInput(view.getWindowToken());
                view.clearFocus();
            }
        }
        return super.dispatchTouchEvent(ev);
    }
    // 判定是否需要隐藏
    private boolean isHideInput(View v, MotionEvent ev) {
        if (v instanceof EditText) {
            int[] l = {0, 0};
            v.getLocationInWindow(l);
            int left = l[0], top = l[1], bottom = top + v.getHeight(), right = left + v.getWidth();
            return !(ev.getX() > left) || !(ev.getX() < right) || !(ev.getY() > top) || !(ev.getY() < bottom);
        }
        return false;
    }
    // 隐藏软键盘
    private void HideSoftInput(IBinder token) {
        if (token != null) {
            InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(token, InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }
    private void showPopupMenu(View anchorView) {
        PopupMenu popupMenu = new PopupMenu(this, anchorView);
        popupMenu.getMenuInflater().inflate(R.menu.menu_popup, popupMenu.getMenu());
        popupMenu.getMenu().findItem(R.id.restart_sys).setVisible(false);
        popupMenu.getMenu().findItem(R.id.restart_app).setVisible(false);
        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.refresh_list) {
                searchView.setQuery("",false);
                appListView.setAdapter(null);
                appList.clear();
                appItemList.clear();
                loadingDialog = ProgressDialog.show(SelectAppActivity.this, "数据加载中", "请稍后...", true, false);
                new LoadDataTask().execute();
            }
            return true;
        });
        popupMenu.show();
    }
}