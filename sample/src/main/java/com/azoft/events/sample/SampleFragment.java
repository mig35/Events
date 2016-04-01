package com.azoft.events.sample;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.azoft.events.Event;
import com.azoft.events.Events;
import com.azoft.events.sample.base.BaseFragment;

public final class SampleFragment extends BaseFragment {

    private static final String TAG = SampleFragment.class.getSimpleName();

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sample, container, false);
    }

    @Events.Receiver(R.id.event_broadcast_3)
    private void onBroadcastEvent3(final Event event) {
        Log.d(TAG, "onBroadcastEvent3 in Fragment");
        Toast.makeText(getActivity(), "Event Broadcast 3 in Fragment was handled: " + event.getData(), Toast.LENGTH_SHORT).show();
    }

    @Events.Receiver(R.id.event_broadcast_4)
    private void onBroadcastEvent4(final Event event) {
        Log.d(TAG, "onBroadcastEvent4 in Fragment");
        Toast.makeText(getActivity(), "Event Broadcast 4 in Fragment was handled: " + event.getData(), Toast.LENGTH_SHORT).show();
    }
}