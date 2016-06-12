/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.android.authenticator;

import android.app.Application;
import android.util.Log;

import com.google.android.gms.analytics.ExceptionReporter;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import org.xwiki.android.authenticator.utils.CrashHandler;
import org.xwiki.android.authenticator.utils.SharedPrefsUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * AppContext.
 */
public class AppContext extends Application {
    private static final String TAG = "AppContext";

    private static AppContext instance;
    //google analytics
    private Tracker mTracker;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Log.d(TAG, "on create");

        //init google analytics tracker
        mTracker = getDefaultTracker();
        CrashHandler crashHandler = CrashHandler.getInstance();
        crashHandler.init(this);
        /*
        Thread.UncaughtExceptionHandler myHandler = new ExceptionReporter(
                mTracker,
                Thread.getDefaultUncaughtExceptionHandler(),
                this);
        Thread.setDefaultUncaughtExceptionHandler(myHandler);
        */
    }

    public static AppContext getInstance() {
        if(instance == null){
            instance = new AppContext();
        }
        return instance;
    }

    /**
     * Gets the default {@link Tracker} for this {@link Application}.
     * @return tracker
     */
    synchronized public Tracker getDefaultTracker() {
        if (mTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            // To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
            mTracker = analytics.newTracker(R.xml.global_tracker);
        }
        return mTracker;
    }

    public static void addAuthorizedApp(int uid, String packageName) {
        Log.d(TAG, "packageName=" + packageName + ", uid=" + uid);
        List<String> packageList = SharedPrefsUtils.getArrayList(instance.getApplicationContext(), Constants.PACKAGE_LIST);
        if (packageList == null) {
            packageList = new ArrayList<>();
        }
        packageList.add(packageName);
        SharedPrefsUtils.putArrayList(instance.getApplicationContext(), Constants.PACKAGE_LIST, packageList);
    }

    public static boolean isAuthorizedApp(String packageName) {
        List<String> packageList = SharedPrefsUtils.getArrayList(instance.getApplicationContext(), Constants.PACKAGE_LIST);
        if (packageList != null && packageList.contains(packageName)) {
            return true;
        }
        return false;
    }
}
