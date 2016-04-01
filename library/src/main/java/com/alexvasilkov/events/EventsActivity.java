package com.alexvasilkov.events;

import android.app.Activity;
import android.os.Bundle;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Helper class for registering Activities.<br />
 * Generally simple Events.register method register instance with hard reference and doesn't understand that different activity objects can be in fact the same
 * activity but after configuration change.<br />
 * So this class helps to manage activity recreations. Activities that are registered in this class will be registered, unregistered and resumed in proprete way.<br />
 * So user can be sure about:<br />
 * 1. Event that was posted before activity recreation with postTo (or post) will be routed to the new recreated activity (and not old one).<br />
 * 2. No leaks will be occurred because of good register/unregister calls.<br />
 * 3. No event will be triggered after onSaveInstanceState method call (after this call it is not save to perform any operation).<br />
 * <br />
 * Please call all lifecycle methods of this class
 *
 * @author MiG35
 */
public final class EventsActivity {

    private static final String EXTRA_EVENTS_UID = "com.alexvasilkov.events.EventsActivity.EXTRA_EVENTS_UID";

    private static final Map<Activity, String> ACTIVITY_UIDS_LIST = new HashMap<Activity, String>();

    private EventsActivity() {
    }

    public static void onCreate(final Activity activity, final Bundle savedState) {
        checkActivity(activity);
        final String uid;
        if (null == savedState) {
            uid = generateUid();
        } else {
            uid = savedState.getString(EXTRA_EVENTS_UID);
            if (null == uid) {
                throw new IllegalStateException(
                        String.format("wrong lifecycle! have you called onSaveInstanceState method for %s?", activity.getClass().getName()));
            }
        }

        ACTIVITY_UIDS_LIST.put(activity, uid);
        EventsDispatcher.register(activity, false, uid, false);
    }

    public static void onResume(final Activity activity) {
        checkActivity(activity);
        EventsDispatcher.resume(activity);
    }

    public static void onSaveInstanceState(final Activity activity, final Bundle outState) {
        checkActivity(activity);
        if (null == outState) {
            throw new NullPointerException("saveState can't be null");
        }
        outState.putString(EXTRA_EVENTS_UID, getActivityUid(activity));
        EventsDispatcher.pause(activity, getActivityUid(activity));
    }

    public static void onDestroy(final Activity activity) {
        checkActivity(activity);
        if (activity.isFinishing()) {
            EventsDispatcher.unregister(activity);
        } else {
            EventsDispatcher.pause(activity, getActivityUid(activity));
        }
        ACTIVITY_UIDS_LIST.remove(activity);
    }

    private static String getActivityUid(final Activity activity) {
        final String uid = ACTIVITY_UIDS_LIST.get(activity);
        if (null == uid) {
            throw new IllegalStateException(
                    String.format("wrong activity passed! have you ever called onCreate on %s activity", activity.getClass().getName()));
        }
        return uid;
    }

    static void checkActivity(final Activity activity) {
        if (null == activity) {
            throw new NullPointerException("activity can't be null");
        }
    }

    static String generateUid() {
        return UUID.randomUUID().toString();
    }
}