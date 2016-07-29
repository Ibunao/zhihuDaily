package me.bunao.www.zhihudailytest.bean;

//import io.github.izzyleung.zhihudailypurify.support.Constants;

import me.bunao.www.zhihudailytest.support.Constants;

public class Question {
    private String title;
    private String url;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isValidZhihuQuestion() {
        //判断是否是知乎问题
        return url != null && url.startsWith(Constants.Strings.ZHIHU_QUESTION_LINK_PREFIX);
    }
}

