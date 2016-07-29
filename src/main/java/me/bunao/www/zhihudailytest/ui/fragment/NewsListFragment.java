package me.bunao.www.zhihudailytest.ui.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import me.bunao.www.zhihudailytest.R;
import me.bunao.www.zhihudailytest.adapter.NewsAdapter;
import me.bunao.www.zhihudailytest.bean.DailyNews;
import me.bunao.www.zhihudailytest.observable.NewsListFromAccelerateServerObservable;
import me.bunao.www.zhihudailytest.observable.NewsListFromDatabaseObservable;
import me.bunao.www.zhihudailytest.observable.NewsListFromZhihuObservable;
import me.bunao.www.zhihudailytest.support.Constants;
import me.bunao.www.zhihudailytest.task.SaveNewsListTask;
import me.bunao.www.zhihudailytest.ui.ZhihuDailyApplication;
import me.bunao.www.zhihudailytest.ui.activity.BaseActivity;
import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

//SwipeRefreshLayout可以包裹一些组件，比如recyclerview,listview,webview等，可以实现下拉刷新，上拉加载
//Rxjava Observer 设计模式中分别扮演的角色:Observer是观察者角色，Observable是被观察目标(subject)角色
//一个可在观察者要得到 observable 对象更改通知时可实现 Observer 接口的类。
public class NewsListFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener,Observer<List<DailyNews>>
{
    private List<DailyNews> newsList = new ArrayList<>();
    private String date;
    private NewsAdapter mAdapter;

    // Fragment is single in SingleDayNewsActivity
    private boolean isToday;
    private boolean isRefreshed = false;

    private SwipeRefreshLayout mSwipeRefreshLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //第一次创建fragment而不是翻转屏幕时导致的销毁重建
        if (savedInstanceState == null) {
            //获取activity传递过来的bundle
            Bundle bundle = getArguments();
            date = bundle.getString(Constants.BundleKeys.DATE);
            isToday = bundle.getBoolean(Constants.BundleKeys.IS_FIRST_PAGE);

            //setRetainInstance(true);此方法可以有效地提高系统的运行效率，对流畅性要求较高的应用可以适当采用此方法进行设置
            //当在onCreate()方法中调用了setRetainInstance(true)后，
            // Fragment恢复时会跳过onCreate()和onDestroy()方法，因此不能在onCreate()中放置一些初始化逻辑
            setRetainInstance(true);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_news_list, container, false);
        RecyclerView mRecyclerView = (RecyclerView) view.findViewById(R.id.news_list);

        //设置RecyclerView的item的数量时候不变，设置这个选项为true可以提高性能
        mRecyclerView.setHasFixedSize(!isToday);

        /*
         * 设置布局管理器
         * listview风格则设置为LinearLayoutManager
         * gridview风格则设置为GridLayoutManager
         * 瀑布流风格的设置为StaggeredGridLayoutManager
          */
        //getActivity()获得绑定的activity中的context
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(llm);

        mAdapter = new NewsAdapter(newsList);
        mRecyclerView.setAdapter(mAdapter);

        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        //刷新时转圈的颜色
        mSwipeRefreshLayout.setColorSchemeResources(R.color.color_primary);

        return view;
    }

    //fragment声明周期之要显示了
    @Override
    public void onResume() {
        super.onResume();
        //从数据库读取数据
        NewsListFromDatabaseObservable.ofDate(date)
                //在Subscriber前面执行的代码都是在I/O线程中运行。最后，observeOn之后的代码在主线程中运行.
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                //绑定当前的Observer，实现订阅模式，绑定后在Observable中调用的subscriber.onNext等于是
                //调用当前Observer接口中实现的方法onNext，
                .subscribe(this);
    }

    //判断fragment是否可见
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        refreshIf(shouldRefreshOnVisibilityChange(isVisibleToUser));
    }
    //如果可见、设置为自动刷新、isRefreshed为false则doRefresh
    private void refreshIf(boolean prerequisite) {
        if (prerequisite) {
            doRefresh();
        }
    }

    private void doRefresh() {
        getNewsListObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this);

        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.setRefreshing(true);
        }
    }

    private Observable<List<DailyNews>> getNewsListObservable() {
        if (shouldSubscribeToZhihu()) {
            //如果是today并且用户没有开启加速模式    ，从知乎加载数据
            return NewsListFromZhihuObservable.ofDate(date);
        } else {
            //没什么区别
            return NewsListFromAccelerateServerObservable.ofDate(date);
        }
    }
    //如果是today并且用户没有开启加速模式
    private boolean shouldSubscribeToZhihu() {
        return isToday || !shouldUseAccelerateServer();
    }
    //是否开启加速
    private boolean shouldUseAccelerateServer() {
        return ZhihuDailyApplication.getSharedPreferences()
                .getBoolean(Constants.SharedPreferencesKeys.KEY_SHOULD_USE_ACCELERATE_SERVER, false);
    }
    //看用户是否选择自动刷新的设置
    private boolean shouldAutoRefresh() {
        return ZhihuDailyApplication.getSharedPreferences()
                .getBoolean(Constants.SharedPreferencesKeys.KEY_SHOULD_AUTO_REFRESH, true);
    }

    private boolean shouldRefreshOnVisibilityChange(boolean isVisibleToUser) {
        return isVisibleToUser && shouldAutoRefresh() && !isRefreshed;
    }

    //SwipeRefreshLayout.OnRefreshListener监听实现方法，刷新时调用
    @Override
    public void onRefresh() {
        doRefresh();
    }
    //在被订阅者中调用观察者的方法
    @Override
    public void onNext(List<DailyNews> newsList) {
        this.newsList = newsList;
    }
    //通知观察者,经历了一个错误条件。
    @Override
    public void onError(Throwable e) {
        mSwipeRefreshLayout.setRefreshing(false);
        //判断fragment是否添加到Activity
        if (isAdded()) {
            e.printStackTrace();
            //显示Snackbar
            ((BaseActivity) getActivity()).showSnackbar(R.string.network_error);
        }
    }
    //完成数据查询后刷新adapter
    //通知观察者,发送完成了基于推的通知。
    @Override
    public void onCompleted() {
        isRefreshed = true;
        //停止显示进度条
        mSwipeRefreshLayout.setRefreshing(false);
        //方法中包含有刷新数据的方法
        mAdapter.updateNewsList(newsList);
        //存入数据库
        new SaveNewsListTask(newsList).execute();
    }
}