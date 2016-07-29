package me.bunao.www.zhihudailytest.ui.activity;

import android.content.Intent;
import android.os.Bundle;


import com.squareup.timessquare.CalendarPickerView;

import java.util.Calendar;
import java.util.Date;

import me.bunao.www.zhihudailytest.R;
import me.bunao.www.zhihudailytest.support.Constants;

//日历界面
public class PickDateActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        layoutResID = R.layout.activity_pick_date;

        super.onCreate(savedInstanceState);


        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Calendar nextDay = Calendar.getInstance();
        //设置Calendar.DAY_OF_YEAR天数加1
        nextDay.add(Calendar.DAY_OF_YEAR, 1);

        CalendarPickerView calendarPickerView = (CalendarPickerView) findViewById(R.id.calendar_view);
        //选择区间为2013.5.19-今天，默认选择为今天
        calendarPickerView.init(Constants.Dates.birthday, nextDay.getTime())
                .withSelectedDate(Calendar.getInstance().getTime());
        calendarPickerView.setOnDateSelectedListener(new CalendarPickerView.OnDateSelectedListener() {
            @Override
            public void onDateSelected(Date date) {
                //将日期设置为选择的时间
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date);
                calendar.add(Calendar.DAY_OF_YEAR, 1);

                Intent intent = new Intent(PickDateActivity.this, SingleDayNewsActivity.class);
                intent.putExtra(Constants.BundleKeys.DATE,
                        Constants.Dates.simpleDateFormat.format(calendar.getTime()));
                startActivity(intent);
            }

            @Override
            public void onDateUnselected(Date date) {

            }
        });
        //选择无效的时间，在规定区间之外的
        calendarPickerView.setOnInvalidDateSelectedListener(
                date -> {
            if (date.after(new Date())) {
                showSnackbar(R.string.not_coming);
            } else {
                showSnackbar(R.string.not_born);
            }
        });
    }
}
