package me.bunao.www.zhihudailytest.support;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import me.bunao.www.zhihudailytest.bean.DailyNews;


public final class Constants {
    private Constants() {

    }

    public static final class Urls {
        //ZHIHU_DAILY_BEFORE
        // 加上date查询当天json格式的所有Story，例：http://news.at.zhihu.com/api/4/news/before/20160724
        /*
        {

            "date": "20160723",
            "stories": [
                {
                    "images": [
                        "http://pic2.zhimg.com/2452ab21c2ac701f3c508da46563e091.jpg"
                    ],
                    "type": 0,
                    "id": 8594158,
                    "ga_prefix": "072308",
                    "title": "一图解释，为什么日本的贫富差距这么小"
                },
                {
                    "images": [
                        "http://pic2.zhimg.com/3eb4eb8c7366b9ac4c5e0ceff278d901.jpg"
                    ],
                    "type": 0,
                    "id": 8596471,
                    "ga_prefix": "072307",
                    "title": "关于「地方债」，需要知道的都在这儿了"
                },
                {
                    "images": [
                        "http://pic3.zhimg.com/59a0d5c0ccf309d6800a1d4f97a6e5c6.jpg"
                    ],
                    "type": 0,
                    "id": 8598503,
                    "ga_prefix": "072307",
                    "title": "好多科学家做梦都想要的「室温超导」，究竟实现了没？"
                },
                {
                    "images": [
                        "http://pic1.zhimg.com/427d934eeb07b69db87e542cc20e8684.jpg"
                    ],
                    "type": 0,
                    "id": 8584521,
                    "ga_prefix": "072307",
                    "title": "其实，哆啦 A 梦根本通不过大雄家旧房子的走廊"
                },
                {
                    "images": [
                        "http://pic1.zhimg.com/a260acf366f3837da6b9060690a8c5d4.jpg"
                    ],
                    "type": 0,
                    "id": 8598898,
                    "ga_prefix": "072307",
                    "title": "读读日报 24 小时热门 TOP 5 · 惨痛的理发经历"
                },
                {
                    "images": [
                        "http://pic3.zhimg.com/45d78d9cd9f995b4915e141fb84dea7a.jpg"
                    ],
                    "type": 0,
                    "id": 8597931,
                    "ga_prefix": "072306",
                    "title": "瞎扯 · 如何正确地吐槽"
                }
            ]

        }
         */
        public static final String ZHIHU_DAILY_BEFORE = "http://news.at.zhihu.com/api/4/news/before/";
        //返回单条的story
        public static final String ZHIHU_DAILY_OFFLINE_NEWS = "http://news-at.zhihu.com/api/4/news/";
        //和ZHIHU_DAILY_BEFORE一样
        public static final String ZHIHU_DAILY_PURIFY_BEFORE = "http://zhihu-daily-purify.azurewebsites.net/news/";
        public static final String SEARCH = "http://zhihu-daily-purify.azurewebsites.net/search/";
    }

    public static final class Dates {
        public static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
        public static final Date birthday=getBirthday(); // May 19th, 2013
        private static Date getBirthday(){
            try {
                return simpleDateFormat.parse("20130519");
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public static final class Types {
        public static final Type newsListType = new TypeToken<List<DailyNews>>() {
        }.getType();
    }

    public static final class Strings {
        public static final String ZHIHU_QUESTION_LINK_PREFIX = "http://www.zhihu.com/question/";
        public static final String SHARE_FROM_ZHIHU = " 分享自知乎网";
        public static final String MULTIPLE_DISCUSSION = "这里包含多个知乎讨论，请点击后选择";
    }

    public static final class Information {
        public static final String ZHIHU_PACKAGE_ID = "com.zhihu.android";
    }

    public static final class SharedPreferencesKeys {
        public static final String KEY_SHOULD_ENABLE_ACCELERATE_SERVER = "enable_accelerate_server?";
        public static final String KEY_SHOULD_USE_CLIENT = "using_client?";
        public static final String KEY_SHOULD_AUTO_REFRESH = "auto_refresh?";
        public static final String KEY_SHOULD_USE_ACCELERATE_SERVER = "using_accelerate_server?";
    }

    public static final class BundleKeys {
        public static final String DATE = "date";
        public static final String IS_FIRST_PAGE = "first_page?";
    }
}
