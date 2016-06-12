package org.xwiki.android.authenticator.utils;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.StandardExceptionParser;
import com.google.android.gms.analytics.Tracker;

import org.xwiki.android.authenticator.AppContext;

/**
 * AnalyticsTrackers.
 */
public class AnalyticsTrackers {

    public static void trackScreenView(String screenName) {
        Tracker t = AppContext.getInstance().getDefaultTracker();
        t.setScreenName(screenName);
        t.send(new HitBuilders.ScreenViewBuilder().build());
    }

    public static void trackException(Exception e) {
        if (e != null) {
            Tracker t = AppContext.getInstance().getDefaultTracker();
            t.send(new HitBuilders.ExceptionBuilder()
                    .setDescription(
                            new StandardExceptionParser(AppContext.getInstance(), null)
                                    .getDescription(Thread.currentThread().getName(), e))
                    .setFatal(false)
                    .build()
            );
        }
    }

    public static void trackEvent(String category, String action, String label) {
        Tracker t = AppContext.getInstance().getDefaultTracker();
        t.send(new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction(action)
                .setLabel(label)
                .setNonInteraction(true)
                .build()
        );
    }

    public static void trackException(String description){
        Tracker t = AppContext.getInstance().getDefaultTracker();
        t.send(new HitBuilders.ExceptionBuilder()
                    .setFatal(false)
                    .setDescription(description)
                    .build()
        );
    }

}
