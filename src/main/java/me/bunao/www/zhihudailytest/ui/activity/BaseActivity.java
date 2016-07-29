package me.bunao.www.zhihudailytest.ui.activity;

import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;

import me.bunao.www.zhihudailytest.R;


public class BaseActivity extends AppCompatActivity {
    private CoordinatorLayout mCoordinatorLayout;

    protected Toolbar mToolBar;
    protected int layoutResID = R.layout.activity_base;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(layoutResID);

        mToolBar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolBar);

        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator_layout);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case  android.R.id.home:
                Log.i("dingBack","back");
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    //显示Snackbar
    public void showSnackbar(int resId) {
        Snackbar.make(mCoordinatorLayout, resId, Snackbar.LENGTH_SHORT).show();
    }
}
