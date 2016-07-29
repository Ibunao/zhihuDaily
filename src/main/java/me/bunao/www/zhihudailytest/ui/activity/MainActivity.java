package me.bunao.www.zhihudailytest.ui.activity;


import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.text.DateFormat;
import java.util.Calendar;

import me.bunao.www.zhihudailytest.R;
import me.bunao.www.zhihudailytest.support.Constants;
import me.bunao.www.zhihudailytest.ui.fragment.NewsListFragment;

public class MainActivity extends BaseActivity {
    //定义显示的页数
    private static final int PAGE_COUNT=7;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //使用父类中定义的成员变量，覆盖父类中的赋值
        //在super调用之前，这样就可以使用父类的setContext方法
        layoutResID=R.layout.activity_main;
        super.onCreate(savedInstanceState);
        TabLayout tabs = (TabLayout) findViewById(R.id.main_pager_tabs);
        ViewPager viewPager = (ViewPager) findViewById(R.id.main_pager);
        //定义viewpager的页数
        viewPager.setOffscreenPageLimit(PAGE_COUNT);

        //绑定适配器
        MainPagerAdapter adapter = new MainPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(adapter);
        //TabLayout和viewpager绑定，写在setAdapter方法之后
        tabs.setupWithViewPager(viewPager);
        FloatingActionButton floatingActionButton = (FloatingActionButton) findViewById(R.id.fab_pick_date);
        floatingActionButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        prepareIntent(PickDateActivity.class);
                    }
                }
//                v -> prepareIntent(PickDateActivity.class)
        );
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                return prepareIntent(PrefsActivity.class);
        }
        return super.onOptionsItemSelected(item);
    }
    private boolean prepareIntent(Class cla) {
        startActivity(new Intent(MainActivity.this, cla));
        return true;
    }


    //FragmentStatePagerAdapter实现自动三个自动生成和销毁，和PageAdapter相同
    private class MainPagerAdapter extends FragmentStatePagerAdapter {
        public MainPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            Bundle bundle = new Bundle();
            Fragment newFragment = new NewsListFragment();
            Calendar dateToGetUrl = Calendar.getInstance();
            //每一页加载相应的日期
            dateToGetUrl.add(Calendar.DAY_OF_YEAR, 0 - i);
            String date = Constants.Dates.simpleDateFormat.format(dateToGetUrl.getTime());
            bundle.putString(Constants.BundleKeys.DATE, date);
            bundle.putBoolean(Constants.BundleKeys.IS_FIRST_PAGE, i == 0);

            newFragment.setArguments(bundle);
            return newFragment;
        }

        @Override
        public int getCount() {
            return PAGE_COUNT;
        }

        //设置标题
        @Override
        public String getPageTitle(int position) {
            Calendar displayDate = Calendar.getInstance();
            displayDate.add(Calendar.DAY_OF_YEAR, -position);

            return (position == 0 ? getString(R.string.zhihu_daily_today) + " " : "")
                    + DateFormat.getDateInstance().format(displayDate.getTime());
        }
    }
}
