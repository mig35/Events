package com.alexvasilkov.events.sample.app;

import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import com.alexvasilkov.events.Event;
import com.alexvasilkov.events.Events;
import com.alexvasilkov.events.sample.R;

@SuppressWarnings("MethodMayBeStatic")
final class EventsHolder {

    private static final String TAG = EventsHolder.class.getSimpleName();

    @Events.AsyncMethod(value = R.id.event_1, singleThreadExecutor = true)
    private void runTask1(final Event event) throws Exception {
        SystemClock.sleep(10000);
    }

    @Events.UiMethod(R.id.event_2)
    private void runTask2P3(final Event event) throws Exception {
        Log.d(TAG, "Postponing event 2");
        event.postpone();

        event.sendResult("first result");

        // Here we can do any work and wait some time for finishing event
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Finishing postponed event 2");
                event.sendResult("last result");
                event.finish();
            }
        }, 5000);
    }
}