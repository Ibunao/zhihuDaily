package me.bunao.www.zhihudailytest.ui.activity;

import android.os.Bundle;

import me.bunao.www.zhihudailytest.R;
import me.bunao.www.zhihudailytest.ui.fragment.PrefsFragment;

public class PrefsActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_frame, new PrefsFragment())
                .commit();
    }
}