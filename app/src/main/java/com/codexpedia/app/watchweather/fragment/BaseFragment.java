package com.codexpedia.app.watchweather.fragment;

import android.support.v4.app.Fragment;

import com.codexpedia.app.watchweather.util.WeatherApplication;
import com.squareup.leakcanary.RefWatcher;

public class BaseFragment extends Fragment {
    @Override public void onDestroy() {
        super.onDestroy();
        RefWatcher refWatcher = WeatherApplication.getRefWatcher(getActivity());
        refWatcher.watch(this);
    }
}
