package com.azoft.events.sample.app;

import android.app.Application;

import com.azoft.events.Events;

public final class SampleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Events.register(new EventsHolder());
    }
}
