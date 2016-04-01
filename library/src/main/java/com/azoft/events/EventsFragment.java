package com.azoft.events;

import android.app.Activity;
import android.os.Bundle;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for registering <b>NOT</b> retain Fragments.<br />
 * Generally simple Events.register method register instance with hard reference and doesn't understand that different fragment objects can be in fact the same
 * fragment but after configuration change.<br />
 * So this class helps to manage fragment recreations. Fragments that are registered in this class will be registered, unregistered and resumed in proprete way.<br />
 * So user can be sure about:<br />
 * 1. Event that was posted before fragment recreation (or detach) with postTo (or post) will be routed to the new recreated (or attached) fragment (and not old one).<br />
 * 2. No leaks will be occurred because of good register/unregister calls.<br />
 * 3. No event will be triggered after onSaveInstanceState method call (after this call it is not save to perform any operation).<br />
 * 4. No event will be triggered after fragment detach (its view destroy).
 * <br />
 * Do not use this class for <b>retain</b> fragments!<br/>
 * Please call all lifecycle methods of this class
 * <p/>
 */
public final class EventsFragment {

    private static final String EXTRA_EVENTS_UID = "com.azoft.events.EventsViewFragment.EXTRA_EVENTS_UID";

    private static final Map<Object, String> FRAGMENT_UIDS_LIST = new HashMap<Object, String>();

    private EventsFragment() {
    }

    public static void onCreate(final Object fragment, final Activity activity, final Bundle savedState) {
        checkFragment(fragment);
        EventsActivity.checkActivity(activity);
        final String uid;
        if (null == savedState) {
            uid = EventsActivity.generateUid();
        } else {
            uid = savedState.getString(EXTRA_EVENTS_UID);
            if (null == uid) {
                throw new IllegalStateException(
                        String.format("wrong lifecycle! have you called onSaveInstanceState method for %s?", fragment.getClass().getName()));
            }
        }

        FRAGMENT_UIDS_LIST.put(fragment, uid);
        EventsDispatcher.register(fragment, false, uid, false);
    }

    public static void onResume(final Object fragment, final Activity activity) {
        checkFragment(fragment);
        EventsActivity.checkActivity(activity);
        EventsDispatcher.resume(fragment);
    }

    public static void onSaveInstanceState(final Object fragment, final Bundle outState) {
        checkFragment(fragment);
        if (null == outState) {
            throw new NullPointerException("saveState can't be null");
        }
        outState.putString(EXTRA_EVENTS_UID, getFragmentUid(fragment));
        EventsDispatcher.pause(fragment, getFragmentUid(fragment));
    }

    public static void onDestroyView(final Object fragment) {
        checkFragment(fragment);
        EventsDispatcher.pause(fragment, getFragmentUid(fragment));
    }

    public static void onDestroy(final Object fragment, final Activity activity) {
        checkFragment(fragment);
        EventsActivity.checkActivity(activity);

        if (activity.isFinishing()) {
            EventsDispatcher.unregister(fragment);
        } else {
            EventsDispatcher.pause(fragment, getFragmentUid(fragment));
        }
        FRAGMENT_UIDS_LIST.remove(fragment);
    }

    private static void checkFragment(final Object fragment) {
        if (null == fragment) {
            throw new NullPointerException("fragment can't be null");
        }
    }

    private static String getFragmentUid(final Object fragment) {
        final String uid = FRAGMENT_UIDS_LIST.get(fragment);
        if (null == uid) {
            throw new IllegalStateException(
                    String.format("wrong activity passed! have you ever called onCreate on %s fragment", fragment.getClass().getName()));
        }
        return uid;
    }
}