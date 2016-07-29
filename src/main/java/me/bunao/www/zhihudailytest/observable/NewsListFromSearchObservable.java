package me.bunao.www.zhihudailytest.observable;

import java.util.List;

import me.bunao.www.zhihudailytest.bean.DailyNews;
import me.bunao.www.zhihudailytest.support.Constants;
import rx.Observable;

import static me.bunao.www.zhihudailytest.observable.Helper.getHtml;
import static me.bunao.www.zhihudailytest.observable.Helper.toNewsListObservable;

public class NewsListFromSearchObservable {
    public static Observable<List<DailyNews>> withKeyword(String keyword) {
        return toNewsListObservable(getHtml(Constants.Urls.SEARCH, "q", keyword));
    }
}
