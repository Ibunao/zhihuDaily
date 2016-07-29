package me.bunao.www.zhihudailytest.observable;

import java.util.List;

import me.bunao.www.zhihudailytest.bean.DailyNews;
import me.bunao.www.zhihudailytest.ui.ZhihuDailyApplication;
import rx.Observable;
import rx.Subscriber;


//从数据库查询数据
public class NewsListFromDatabaseObservable {
    public static Observable<List<DailyNews>> ofDate(String date) {
        return Observable.create(new Observable.OnSubscribe<List<DailyNews>>() {
            @Override
            public void call(Subscriber<? super List<DailyNews>> subscriber) {
                //从数据库读取
                List<DailyNews> newsList = ZhihuDailyApplication.getDataSource().newsOfTheDay(date);

            if (newsList != null) {
                subscriber.onNext(newsList);
            }

            subscriber.onCompleted();
            }

        });

    }
}
