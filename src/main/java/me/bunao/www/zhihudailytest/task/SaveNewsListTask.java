package me.bunao.www.zhihudailytest.task;

import android.os.AsyncTask;

import com.google.gson.GsonBuilder;

import java.util.List;

import me.bunao.www.zhihudailytest.bean.DailyNews;
import me.bunao.www.zhihudailytest.db.DailyNewsDataSource;
import me.bunao.www.zhihudailytest.ui.ZhihuDailyApplication;

public class SaveNewsListTask extends AsyncTask<Void, Void, Void> {
    private List<DailyNews> newsList;

    public SaveNewsListTask(List<DailyNews> newsList) {
        this.newsList = newsList;
    }

    @Override
    protected Void doInBackground(Void... params) {
        if (newsList != null && newsList.size() > 0) {
            saveNewsList(newsList);
        }

        return null;
    }

    private void saveNewsList(List<DailyNews> newsList) {

        DailyNewsDataSource dataSource = ZhihuDailyApplication.getDataSource();
        String date = newsList.get(0).getDate();
        //查询出date天的数据
        List<DailyNews> originalData = dataSource.newsOfTheDay(date);
        //插入数据库
        if (originalData == null || !originalData.equals(newsList)) {
            //把newsList转换为json
            dataSource.insertOrUpdateNewsList(date, new GsonBuilder().create().toJson(newsList));
        }
    }
}