package com.alexvasilkov.events.sample.base;

import android.app.Fragment;
import android.os.Bundle;

import com.alexvasilkov.events.EventsFragment;

public abstract class BaseFragment extends Fragment {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EventsFragment.onCreate(this, getActivity(), savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        EventsFragment.onResume(this, getActivity());
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);

        EventsFragment.onSaveInstanceState(this, outState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        EventsFragment.onDestroyView(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        EventsFragment.onDestroy(this, getActivity());
    }
}