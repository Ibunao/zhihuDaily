package me.bunao.www.zhihudailytest.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.google.gson.GsonBuilder;

import java.util.List;

import me.bunao.www.zhihudailytest.bean.DailyNews;
import me.bunao.www.zhihudailytest.support.Constants;

//对数据库的一些操作
public final class DailyNewsDataSource {
    private SQLiteDatabase database;
    private DBHelper dbHelper;
    private String[] allColumns = {
            DBHelper.COLUMN_ID,
            DBHelper.COLUMN_DATE,
            DBHelper.COLUMN_CONTENT
    };

    public DailyNewsDataSource(Context context) {
        dbHelper = new DBHelper(context);
    }
    //获取数据库
    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }
    //插入单条信息，并从插入的数据中取出List<DailyNews>
    public List<DailyNews> insertDailyNewsList(String date, String content) {
        ContentValues values = new ContentValues();
        values.put(DBHelper.COLUMN_DATE, date);
        values.put(DBHelper.COLUMN_CONTENT, content);

        long insertId = database.insert(DBHelper.TABLE_NAME, null, values);
        Cursor cursor = database.query(DBHelper.TABLE_NAME,
                allColumns, DBHelper.COLUMN_ID + " = " + insertId, null, null, null, null);
        cursor.moveToFirst();//指向查询结果的第一个位置，一般通过判断cursor.moveToFirst()的值为true或false来确定查询结果是否为空。
        List<DailyNews> newsList = cursorToNewsList(cursor);
        cursor.close();
        return newsList;
    }
    //更新数据
    public void updateNewsList(String date, String content) {
        ContentValues values = new ContentValues();
//        values.put(DBHelper.COLUMN_DATE, date);
        values.put(DBHelper.COLUMN_CONTENT, content);
        database.update(DBHelper.TABLE_NAME, values, DBHelper.COLUMN_DATE + "=" + date, null);
    }
    //插入或更新数据
    public void insertOrUpdateNewsList(String date, String content) {
        if (newsOfTheDay(date) != null) {
            updateNewsList(date, content);
        } else {
            insertDailyNewsList(date, content);
        }
    }
    //查询数据
    public List<DailyNews> newsOfTheDay(String date) {
        Cursor cursor = database.query(DBHelper.TABLE_NAME,
                allColumns, DBHelper.COLUMN_DATE + " = " + date, null, null, null, null);

        cursor.moveToFirst();
        List<DailyNews> newsList = cursorToNewsList(cursor);
        cursor.close();
        return newsList;
    }

    //将获得的json转换成对象
    private List<DailyNews> cursorToNewsList(Cursor cursor) {
        if (cursor != null && cursor.getCount() > 0) {
            //将查询出的cursor取出COLUMN_CONTENT
            //指定该方法反序列化Json为指定的类型的一个对象。
            //cursor.getColumnIndex( DBHelper.COLUMN_CONTENT)=2
//            Log.i("ding",cursor.getString(2));
            return new GsonBuilder().create().fromJson(cursor.getString(cursor.getColumnIndex( DBHelper.COLUMN_CONTENT)), Constants.Types.newsListType);
        } else {
            return null;
        }
    }
}
/*
cursor.getString(2)的到的json
[
    {
        "dailyTitle": "一到这个年龄就变成了「熊孩子」，宝贝你是怎么了？",
        "date": "20160722",
        "questions": [
            {
                "title": "从进化心理学角度看，幼年（5~13岁）人类为何常常表现得很「熊」，以至于他们在很多成人眼中特别讨厌？",
                "url": "http://www.zhihu.com/question/47642989"
            }
        ],
        "thumbnailUrl": "http://pic4.zhimg.com/5d85a48661497236aa839633bad03e23.jpg"
    },
    {
        "dailyTitle": "13 年前的主持人郭德纲，在橱窗里待了 48 小时",
        "date": "20160722",
        "questions": [
            {
                "title": "郭德纲有哪些优点？",
                "url": "http://www.zhihu.com/question/48593127"
            }
        ],
        "thumbnailUrl": "http://pic4.zhimg.com/b7732de2ba84f6d2de16ed7090d9462f.jpg"
    },
    {
        "dailyTitle": "为什么艾滋病患的个人信息泄露是非常严重的事？",
        "date": "20160722",
        "questions": [
            {
                "title": "如何看待艾滋病人信息大被大规模泄露一事？",
                "url": "http://www.zhihu.com/question/48592095"
            }
        ],
        "thumbnailUrl": "http://pic3.zhimg.com/c1d738c4984a9172a0cfd43973433c9a.jpg"
    },
    {
        "dailyTitle": "微博上有人说两节干电池可以让网速快十倍，于是我亲手试了试",
        "date": "20160722",
        "questions": [
            {
                "title": "网线上绕电池可以提高网速？",
                "url": "http://www.zhihu.com/question/48498500"
            }
        ],
        "thumbnailUrl": "http://pic3.zhimg.com/068899be11f96463325334ec9fcc724e.jpg"
    },
    {
        "dailyTitle": "瞎扯 · 如何正确地吐槽",
        "date": "20160722",
        "questions": [
            {
                "title": "你都说过哪些不合时宜的大实话？",
                "url": "http://www.zhihu.com/question/48002856"
            },
            {
                "title": "地质学家被困在深山老林无论怎样都出不去会发生什么？",
                "url": "http://www.zhihu.com/question/48664969"
            },
            {
                "title": "智商下线是一种怎样的体验？",
                "url": "http://www.zhihu.com/question/46400790"
            }
        ],
        "thumbnailUrl": "http://pic3.zhimg.com/640abcdfa9aa0b2a81900c791777a8e2.jpg"
    }
]


 */
