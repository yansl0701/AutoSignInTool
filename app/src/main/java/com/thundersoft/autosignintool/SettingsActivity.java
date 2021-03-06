package com.thundersoft.autosignintool;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;
import androidx.preference.SwitchPreferenceCompat;

import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    public static final String TAG = SettingsActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            //actionBar.setDisplayHomeAsUpEnabled(true);
        }

        requestPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAccessibilityOn();
    }

    private void checkAccessibilityOn(){
        if (!isAccessibilitySettingsOn(this,
                AutoSigninService.class.getName())) {// 判断服务是否开启
            openAccessibilitySettings();
        } else {
            Utils.toast(this, "服务已开启");
        }
    }

    // 打开飞书
    private void startLarkApp(){
        Intent intent1 = new Intent("android.intent.action.MAIN");
        intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ComponentName componentName = new ComponentName("com.ss.android.lark", "com.ss.android.lark.main.app.MainActivity");
        intent1.setComponent(componentName);
        startActivity(intent1);
    }

    // 打开无障碍设置
    private void openAccessibilitySettings(){
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    // 判断自定义辅助功能服务是否开启
    private boolean isAccessibilitySettingsOn(Context context, String className) {
        if (context == null) {
            return false;
        }
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
            List<ActivityManager.RunningServiceInfo> runningServices =
                    activityManager.getRunningServices(100);// 获取正在运行的服务列表
            if (runningServices.size() < 0) {
                return false;
            }
            for (int i = 0; i < runningServices.size(); i++) {
                ComponentName service = runningServices.get(i).service;
                if (service.getClassName().equals(className)) {
                    return true;
                }
            }
            return false;
        } else {
            return false;
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        SwitchPreferenceCompat mSwitchPreference = null;
        Preference mStatePreference = null;
        Preference mDistancePreference = null;


        @Override
        public void onStart() {
            super.onStart();
            if (mSwitchPreference != null){
                mSwitchPreference.setChecked(((MyApplication)getActivity().getApplication()).isOpen());
            }
            if (mStatePreference != null){
                mStatePreference.setSummary("AutoSignInService is : " + ((SettingsActivity)getActivity()).isAccessibilitySettingsOn(getContext(), AutoSigninService.class.getName())
                        + "\nAutoService is : " + ((SettingsActivity)getActivity()).isAccessibilitySettingsOn(getContext(), AutoService.class.getName()));
            }
            if (mDistancePreference != null){
                Location location = Utils.getCurrentLocation(getContext());
                if (location != null)
                    mDistancePreference.setSummary("进入打卡范围：" + Utils.isEnterRange(location.getLatitude(), location.getLongitude()));
                else
                    mDistancePreference.setSummary("定位失败");
            }
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            // 状态
            mStatePreference = findPreference("state");

            // 定位
            Preference locationPreference = findPreference("location");
            locationPreference.setOnPreferenceClickListener(preference -> {
                String locationStr = Utils.getCurrentLocationStr(getContext());
                Log.e(TAG, "location:" + locationStr);
                preference.setSummary(locationStr);
                return false;
            });

            // 距离
            mDistancePreference = findPreference("distance");
            mDistancePreference.setOnPreferenceClickListener(preference -> {
                Location location = Utils.getCurrentLocation(getContext());
                if (location != null)
                    preference.setSummary("进入打卡范围：" + Utils.isEnterRange(location.getLatitude(), location.getLongitude()));
                else
                    preference.setSummary("定位失败");
                return false;
            });

            // 重置
            Preference resetPreference = findPreference("reset");
            resetPreference.setOnPreferenceClickListener(preference -> {
                reset();
                return false;
            });

            // 打开无障碍设置
            Preference startPre = findPreference("start");
            startPre.setOnPreferenceClickListener(preference -> {
                // 前往开启辅助服务界面
                ((SettingsActivity)getActivity()).openAccessibilitySettings();
                return false;
            });

            // 打开飞书
            Preference launchPre = findPreference("launch");
            launchPre.setOnPreferenceClickListener(preference -> {
                // 打开飞书应用
                ((SettingsActivity)getActivity()).startLarkApp();
                return false;
            });


            // 开关
            mSwitchPreference = findPreference("switch");
            mSwitchPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean switchState = (boolean) newValue;
                if (switchState){
                    ((MyApplication)getActivity().getApplication()).setOpen(true);
                    ((SettingsActivity)getActivity()).startLarkApp();
                }else{
                    ((MyApplication)getActivity().getApplication()).setOpen(false);
                }

                return true;
            });

            // 选择定时
            ListPreference listPreference = findPreference("choose_date");
            listPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                if (preference instanceof ListPreference) {
                    ListPreference listPreference1 = (ListPreference) preference;
                    CharSequence[] entries = listPreference1.getEntries();
                    int index = listPreference1.findIndexOfValue((String) newValue);
                    if(index == 0){
                        // 打开定时
                        Log.e(SettingsActivity.TAG, "打开定时");
                        getActivity().startService(new Intent(getActivity(), AutoService.class));
                    }else if (index == 1){
                        // 关闭定时
                        getActivity().stopService(new Intent(getActivity(), AutoService.class));
                    }
                }
                return true;
            });
        }

        private void reset() {
            ((MyApplication)getActivity().getApplication()).setEnterSignInRange(false);
            ((MyApplication)getActivity().getApplication()).setEnterSignInScreen(false);
            ((MyApplication)getActivity().getApplication()).setSigning(false);
        }
    }

    private void requestPermissions(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        }
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 0){
            Log.e(TAG, "0 result = " + grantResults);
        }else if(requestCode == 1) {
            Log.e(TAG, "1 result = " + grantResults);
        }
    }
}