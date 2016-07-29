package me.bunao.www.zhihudailytest.bean;

import java.util.List;

//知乎日报的一个item，正常的情况下一个DailyNews只有一个问题Question，但是像今日好问题就有多个Question
public final class DailyNews {
    private String date;
    private String dailyTitle;
    private String thumbnailUrl;
    private List<Question> questions;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getDailyTitle() {
        return dailyTitle;
    }

    public void setDailyTitle(String dailyTitle) {
        this.dailyTitle = dailyTitle;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }

    public boolean hasMultipleQuestions() {
        return this.getQuestions().size() > 1;
    }
}