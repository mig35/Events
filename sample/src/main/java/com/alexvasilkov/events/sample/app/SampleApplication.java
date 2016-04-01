package com.alexvasilkov.events.sample.app;

import android.app.Application;

import com.alexvasilkov.events.Events;

public final class SampleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Events.register(new EventsHolder());
    }
}
