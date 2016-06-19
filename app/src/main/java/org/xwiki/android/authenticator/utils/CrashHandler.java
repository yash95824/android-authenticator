package org.xwiki.android.authenticator.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.analytics.StandardExceptionParser;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * CrashHandler.
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler{
    private static final String TAG = "CrashHandler";

    private Context mContext;
    private Thread.UncaughtExceptionHandler mDefaultHandler;
    private Map<String, String> infos = new HashMap<String, String>();

    private static class SingletonHolder{
        public static final CrashHandler INSTANCE = new CrashHandler();
    }
    public static final CrashHandler getInstance(){
        return SingletonHolder.INSTANCE;
    }
    private CrashHandler(){};

    public void init(Context mContext){
        this.mContext = mContext;
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        if(mDefaultHandler != null && !handleException(ex)){
            mDefaultHandler.uncaughtException(thread, ex);
        }else{
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Log.e(TAG, "error : ", e);
            }
            //exit process
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }
    }

    private boolean handleException(Throwable ex){
        if(ex == null) return false;
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                Toast.makeText(mContext, "Sorry, an unexpected error occured! Please Try Again", Toast.LENGTH_SHORT).show();
                Looper.loop();
            }
        }).start();
        String deviceInfo = collectDeviceInfo();
        String exceptionInfo = collectException(ex);
        String standardInfo = new StandardExceptionParser(mContext, null).getDescription(Thread.currentThread().getName(), ex);
        //AnalyticsTrackers.trackScreenView(result.substring(0, (result.length() > 2048 ? 2040 : result.length())));
        Log.e(TAG, deviceInfo + exceptionInfo, ex);
        AnalyticsTrackers.trackEvent(standardInfo, deviceInfo, exceptionInfo);
        return true;
    }

    public String collectException(Throwable ex){
        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        Throwable cause = ex.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.close();
        String result = writer.toString();
        return result;
    }

    public String collectDeviceInfo() {
        try {
            PackageManager pm = mContext.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(mContext.getPackageName(), PackageManager.GET_ACTIVITIES);
            if (pi != null) {
                String versionName = pi.versionName == null ? "null" : pi.versionName;
                String versionCode = pi.versionCode + "";
                infos.put("versionName", versionName);
                infos.put("versionCode", versionCode);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "an error occured when collect package info", e);
        }
        Field[] fields = Build.class.getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                infos.put(field.getName(), field.get(null).toString());
                //Log.d(TAG, field.getName() + " : " + field.get(null));
            } catch (Exception e) {
                Log.e(TAG, "an error occured when collect crash info", e);
            }
        }
        //builder
        StringBuffer sb = new StringBuffer();
        for (Map.Entry<String, String> entry : infos.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            sb.append(key + "=" + value + "\n");
        }
        return sb.toString();
    }


}
