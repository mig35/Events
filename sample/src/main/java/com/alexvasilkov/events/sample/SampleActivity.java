package com.alexvasilkov.events.sample;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.alexvasilkov.events.Event;
import com.alexvasilkov.events.EventCallback;
import com.alexvasilkov.events.Events;
import com.alexvasilkov.events.sample.base.BaseActivity;

public final class SampleActivity extends BaseActivity {

    private static final String TAG = SampleActivity.class.getSimpleName();

    private String mEvent2Result;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mEvent2Result = null == savedInstanceState ? null : savedInstanceState.getString("event_2_result");

        Events.post(R.id.event_1);
        if (null == mEvent2Result) {
            // single event is good for activity recreation. we can be sure that only one event is executing in the moment event if we send a lot of single events
            // postTo is good to be sure that event result will be triggered only for this Activity instance (activity recreations are handled in sdk,
            // so this different instances because of recreation are the same for postTo method). This is true because of using EventsActivity and EventsFragment class for registering
            Events.create(R.id.event_2).single().data("hello").post();
        }

        Events.create(R.id.event_broadcast_3).data("broadcast for all!").post();

        final Fragment sampleFragment = getFragmentManager().findFragmentById(R.id.fr_sample);
        Events.create(R.id.event_broadcast_4).data("broadcast for fragment only!").postTo(sampleFragment);
    }

    @Events.Callback(R.id.event_1)
    private void onCallback1(final EventCallback callback) {
        Log.d(TAG, "Callback 1: " + callback.getStatus());
        if (callback.isFinished()) {
            Toast.makeText(this, "Event 1 was handled", Toast.LENGTH_SHORT).show();
        }
    }

    @Events.Callback(R.id.event_2)
    private void onCallback2(final EventCallback callback) {
        Log.d(TAG, "Callback 2: " + callback.getStatus());
        if (callback.isResult()) {
            mEvent2Result = callback.getResult();
            Toast.makeText(this, "Event 2 was handled: " + mEvent2Result, Toast.LENGTH_SHORT).show();
        }
    }

    @Events.Receiver(R.id.event_broadcast_3)
    private void onBroadcastEvent3(final Event event) {
        Log.d(TAG, "onBroadcastEvent3 in Activity");
        Toast.makeText(this, "Event Broadcast 3 in Activity was handled: " + event.getData(), Toast.LENGTH_SHORT).show();
    }

    @Events.Receiver(R.id.event_broadcast_4)
    private void onBroadcastEvent4(final Event event) {
        Log.d(TAG, "onBroadcastEvent4 in Activity");
        // this code should never be called due to postTo
        Toast.makeText(this, "Event Broadcast 4 in Activity was handled: " + event.getData(), Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("event_2_result", mEvent2Result);
    }
}