package com.alexvasilkov.events.sample.base;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.alexvasilkov.events.EventsActivity;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EventsActivity.onCreate(this, savedInstanceState);
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();

        EventsActivity.onResume(this);
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);

        EventsActivity.onSaveInstanceState(this, outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        EventsActivity.onDestroy(this);
    }
}