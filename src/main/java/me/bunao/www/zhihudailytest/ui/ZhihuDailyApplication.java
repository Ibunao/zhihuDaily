package me.bunao.www.zhihudailytest.ui;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;

import me.bunao.www.zhihudailytest.db.DailyNewsDataSource;


//在AndroidManifest.xml中进行配置，当启动的时候进行加载初始化
//可以通过它方便的获取全局的context
public final class ZhihuDailyApplication extends Application {
    private static ZhihuDailyApplication applicationContext;
    private static DailyNewsDataSource dataSource;
    //初始化imageloader的一些配置
    public static void initImageLoader(Context context) {
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(context)
                .denyCacheImageMultipleSizesInMemory()//拒绝缓存多个图片
                .threadPriority(Thread.NORM_PRIORITY - 2)//当同一个Uri获取不同大小的图片，缓存到内存时，只缓存一个。默认会缓存多个不同的大小的相同图片
                .diskCacheFileNameGenerator(new Md5FileNameGenerator())//将保存的时候的URI名称用MD5 加密
                .tasksProcessingOrder(QueueProcessingType.LIFO)//设置图片下载和显示的工作队列排序
                .build();
        ImageLoader.getInstance().init(config);
    }

    public static ZhihuDailyApplication getInstance() {
        return applicationContext;
    }

    public static DailyNewsDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        applicationContext = this;

        //配置Imageloader参数
        initImageLoader(getApplicationContext());

        //打开数据库
        dataSource = new DailyNewsDataSource(getApplicationContext());
        dataSource.open();
    }

    //创建SharedPreferences实例，用来保存配置信息
    public static SharedPreferences getSharedPreferences() {
        //个应用有一个默认的偏好文件preferences.xml，使用getDefaultSharedPreferences获取
        return PreferenceManager.getDefaultSharedPreferences(applicationContext);
    }
}
