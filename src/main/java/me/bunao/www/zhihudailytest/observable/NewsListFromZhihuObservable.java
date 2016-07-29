package me.bunao.www.zhihudailytest.observable;

import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.annimon.stream.Optional;
import com.annimon.stream.Stream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

import me.bunao.www.zhihudailytest.bean.DailyNews;
import me.bunao.www.zhihudailytest.bean.Question;
import me.bunao.www.zhihudailytest.bean.Story;
import me.bunao.www.zhihudailytest.support.Constants;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;

import static me.bunao.www.zhihudailytest.observable.Helper.getHtml;
import static me.bunao.www.zhihudailytest.observable.Helper.toNonempty;


//获取一天的所有的item(DailyNews)
public class NewsListFromZhihuObservable {
    private static final String QUESTION_SELECTOR = "div.question";
    private static final String QUESTION_TITLES_SELECTOR = "h2.question-title";
    private static final String QUESTION_LINKS_SELECTOR = "div.view-more a";

    public static Observable<List<DailyNews>> ofDate(String date) {

        //解析json
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
        Observable<Story> stories = getHtml(Constants.Urls.ZHIHU_DAILY_BEFORE, date)

//                .flatMap(
//                        //Lambda表达式之引用方法，可以转换成下面的形式
////                        NewsListFromZhihuObservable::getStoriesJsonArrayObservable
//                )
                //接收上个onNext(Http.get(url, suffix))传过来的参数，进行转换
                .flatMap(new Func1<String, Observable<JSONArray>>() {
                            @Override
                            public Observable<JSONArray> call(String string) {
                                Log.i("dingIn","yongdaowole");
                                return  NewsListFromZhihuObservable.getStoriesJsonArrayObservable(string);
                            }
                        }
                )
                //接收上一个flatMap传出的值进行转换
                .flatMap(
                        new Func1<JSONArray, Observable<Story>>() {
                            @Override
                            public Observable<Story> call(JSONArray JsonArray) {
                                Log.i("dingIn","yeyongdaowole");
                                // 返回 Observable<Messages>，在订阅时请求消息列表，并在响应后发送请求到的消息列表
                                return  NewsListFromZhihuObservable.getStoriesObservable(JsonArray);
                            }
                        }
                );
        //最后一个flatmap简写形式
//                .flatMap(NewsListFromZhihuObservable::getStoriesObservable);

        //将json中获取的html解析成Document
        Observable<Document> documents = stories
                .flatMap(
                        new Func1<Story, Observable<Document>>() {
                            @Override
                            public Observable<Document> call(Story story) {
                                return  NewsListFromZhihuObservable.getDocumentObservable(story);
                            }
                        }
//                        NewsListFromZhihuObservable::getDocumentObservable
                );
        //Observable.zip(stories, documents, NewsListFromZhihuObservable::createPair);
        //将两个Observable合并成一个（1,2,3）（a,b,c）转换成（1a,2b,3c）,传入到第三个方法的参数形式
        //分别为（1，a）、（2，b）、（3，c）
        Observable<Optional<Pair<Story, Document>>> optionalStoryNDocuments
                = Observable.zip(stories, documents, NewsListFromZhihuObservable::createPair);
        //把无效的筛选掉
        Observable<Pair<Story, Document>> storyNDocuments = toNonempty(optionalStoryNDocuments);

        return toNonempty(storyNDocuments.map(NewsListFromZhihuObservable::convertToDailyNews))
                .doOnNext(news -> news.setDate(date))
                //将返回的所有的onNext()结果存到一个list中,并创建Observable<List<……>>
                .toList();
    }
    //获得json的stories
    private static Observable<JSONArray> getStoriesJsonArrayObservable(String html) {
        return Observable.create(
                new Observable.OnSubscribe<JSONArray>() {
                    @Override
                    public void call(Subscriber<? super JSONArray> subscriber) {
                        try {
                            subscriber.onNext(new JSONObject(html).getJSONArray("stories"));
                            subscriber.onCompleted();
                        } catch (JSONException e) {
                            subscriber.onError(e);
                        }
                    }
                }
//                subscriber -> {
//            try {
//                subscriber.onNext(new JSONObject(html).getJSONArray("stories"));
//                subscriber.onCompleted();
//            } catch (JSONException e) {
//                subscriber.onError(e);
//            }
//        }
        );
    }
    //解析出json中的story
    private static Observable<Story> getStoriesObservable(JSONArray newsArray) {
        return Observable.create(
                new Observable.OnSubscribe<Story>() {

                    @Override
                    public void call(Subscriber<? super Story> subscriber) {
                        try {
                            for (int i = 0; i < newsArray.length(); i++) {
                                JSONObject newsJson = newsArray.getJSONObject(i);
                                subscriber.onNext(getStoryFromJSON(newsJson));
                            }
                            subscriber.onCompleted();
                        } catch (JSONException e) {
                            subscriber.onError(e);
                        }
                    }
                }
//                subscriber -> {
//            try {
//                for (int i = 0; i < newsArray.length(); i++) {
//                    JSONObject newsJson = newsArray.getJSONObject(i);
//                    subscriber.onNext(getStoryFromJSON(newsJson));
//                }
//
//                subscriber.onCompleted();
//            } catch (JSONException e) {
//                subscriber.onError(e);
//            }
//        }
        );
    }

    private static Story getStoryFromJSON(JSONObject jsonStory) throws JSONException {
        Story story = new Story();

        story.setStoryId(jsonStory.getInt("id"));
        story.setDailyTitle(jsonStory.getString("title"));
        story.setThumbnailUrl(getThumbnailUrlForStory(jsonStory));

        return story;
    }

    private static String getThumbnailUrlForStory(JSONObject jsonStory) throws JSONException {
        if (jsonStory.has("images")) {
            return (String) jsonStory.getJSONArray("images").get(0);
        } else {
            return null;
        }
    }

    private static Observable<Document> getDocumentObservable(Story news) {
        return getHtml(Constants.Urls.ZHIHU_DAILY_OFFLINE_NEWS, news.getStoryId())
                .map(NewsListFromZhihuObservable::getStoryDocument);
    }

    private static Document getStoryDocument(String json) {
        try {
            JSONObject newsJson = new JSONObject(json);
            //如果json包含body，取出json的值用Jsoup对html进行解析
            return newsJson.has("body") ? Jsoup.parse(newsJson.getString("body")) : null;
        } catch (JSONException e) {
            return null;
        }
    }

    private static Optional<Pair<Story, Document>> createPair(Story story, Document document) {
        return Optional.ofNullable(document == null ? null : Pair.create(story, document));
    }

    private static Optional<DailyNews> convertToDailyNews(Pair<Story, Document> pair) {
        DailyNews result = null;

        Story story = pair.first;
        Document document = pair.second;
        String dailyTitle = story.getDailyTitle();

        List<Question> questions = getQuestions(document, dailyTitle);
        if (Stream.of(questions).allMatch(Question::isValidZhihuQuestion)) {
            result = new DailyNews();

            result.setDailyTitle(dailyTitle);
            result.setThumbnailUrl(story.getThumbnailUrl());
            result.setQuestions(questions);
        }
//        Log.i("dingDailyNews","dailyTitle:"+dailyTitle+"   ;"+"ThumbnailUrl:"+story.getThumbnailUrl());
//        for (int i=0;i<questions.size();i++){
//            Log.i("dingQuestion","questionTitle: "+questions.get(i).getTitle().toString()+"   ;"+"questionUrl:"+questions.get(i).getUrl());
//        }

        return Optional.ofNullable(result);
    }

    private static List<Question> getQuestions(Document document, String dailyTitle) {

        List<Question> result = new ArrayList<>();
        //获得所有的class为question的div
        Elements questionElements = getQuestionElements(document);
//        Log.i("dingquestionElements",questionElements.toString());
        //遍历Element是Elements中class为question的div的数组的一个子元素(是一块代码，而不是一个标签)
        for (Element questionElement : questionElements) {
            Question question = new Question();
//            Log.i("dingquestionElement",questionElement.toString());
            String questionTitle = getQuestionTitleFromQuestionElement(questionElement);
            String questionUrl = getQuestionUrlFromQuestionElement(questionElement);
            // Make sure that the question's title is not empty.
            questionTitle = TextUtils.isEmpty(questionTitle) ? dailyTitle : questionTitle;

            question.setTitle(questionTitle);
            question.setUrl(questionUrl);

            result.add(question);
        }

        return result;
    }

    private static Elements getQuestionElements(Document document) {
        //部分log在最底端，document.select("div.question")表示搜索标签为div，class为question的标签内的内容
//        Log.i("dingcss",document.select(QUESTION_SELECTOR).toString());
        return document.select(QUESTION_SELECTOR);
    }
    //获取问题标题
    private static String getQuestionTitleFromQuestionElement(Element questionElement) {
        Element questionTitleElement = questionElement.select(QUESTION_TITLES_SELECTOR).first();

        if (questionTitleElement == null) {
            return null;
        } else {
            return questionTitleElement.text();
        }
    }
    //获取问题的链接
    private static String getQuestionUrlFromQuestionElement(Element questionElement) {
        Element viewMoreElement = questionElement.select(QUESTION_LINKS_SELECTOR).first();

        if (viewMoreElement == null) {
            return null;
        } else {
            return viewMoreElement.attr("href");
        }
    }
}
/*
07-23 17:14:16.510 1996-2037/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8589107
07-23 17:14:17.362 1996-2037/io.github.izzyleung.zhihudailypurify I/dingcss: <div class="question">
                                                                              <h2 class="question-title"></h2>
                                                                              <div class="answer">
                                                                               <div class="content">
                                                                                <p style="text-align: center;">* * *</p>
                                                                                <p style="text-align: center;">只要想起一生中后悔的事</p>
                                                                                <p style="text-align: center;">梅花便落满了南山</p>
                                                                                <p style="text-align: center;">* * *</p>
                                                                               </div>
                                                                              </div>
                                                                             </div>
                                                                             <div class="question">
                                                                              <h2 class="question-title"></h2>
                                                                              <div class="answer">
                                                                               <div class="meta">
                                                                                <img class="avatar" src="http://pic2.zhimg.com/ee93a48be6d5d6fab2d1c20b52250e29_is.jpg" />
                                                                                <span class="author">李恒林，</span>
                                                                                <span class="bio">前已无通路，后不见归途</span>
                                                                               </div>
                                                                               <div class="content">
                                                                                <p>小学的时候班上有个脑瘫的女生，曾经跟着其他男生一起起哄嘲笑过她。</p>
                                                                                <p>后来班主任老师让我向她道歉，我也就照做了。她说没关系的时候我的心里才突然特别难受。</p>
                                                                                <p>到现在我还是会想起那种，自己莫名其妙地对待比自己不幸的人的丑陋心理，无比愧疚。</p>
                                                                                <p>毕竟我活到现在，也是个弱势的人，还在经受许多人的打压嘲弄，我想我现在才真的懂那个女生的痛苦，还有她笑着说没关系的时候的可敬。</p>
                                                                               </div>
                                                                              </div>
                                                                              <div class="answer">
                                                                               <div class="meta">
                                                                                <img class="avatar" src="http://pic1.zhimg.com/bcc6ff908ab7ab6436b8139c42db7718_is.jpg" />
                                                                                <span class="author">DaturaGeass，</span>
                                                                                <span class="bio">留学生/猫奴/游泳/动漫/popping/单身狗</span>
                                                                               </div>
                                                                               <div class="content">
                                                                                <p>很小的时候，跟老妈说了句“你长得真丑”。</p>
                                                                                <p>当时我妈没有生气，笑了笑去买菜了。</p>
                                                                                <p>老爸超级生气，逼我跪下来，恨不得拿菜刀砍我，那时候老爸生病，不能多说话，但是他一直骂我，反反复复只有一句话。</p>
                                                                                <p>“子不嫌母丑”</p>
                                                                                <p>当时他说的嘴角流血。</p>
                                                                                <p>长大一点了，想到当时家里的情况，我妈作为一个女人，肯定也希望自己打扮漂漂亮亮，希望有金银首饰。但是她没有，她要维持这个家，她要筹钱给我爸看病，要给我学费想办法，更要为家里一日三餐忙碌，家里好的都是给我，吃的穿的。</p>
                                                                                <p>一辈子后悔这句话，一辈子都后悔。</p>
                                                                                <p>到现在十几年都过去了，我爸忘了，我妈忘了。</p>
                                                                                <p>我一直记得，子不嫌母丑。</p>
                                                                                <p>很多时候，朋友跟我抱怨父母，我都会把这个事说给他们听，更多的是，再一次提醒自己。</p>
                                                                                <p>我不否认世上有不负责的父母，但是大部分，绝大部分父母是爱你的。</p>
                                                                                <p>还有。</p>
                                                                                <p>我妈是世界上最漂亮，最好看的女人。</p>
                                                                               </div>
                                                                              </div>
                                                                              <div class="answer">
                                                                               <div class="meta">
                                                                                <img class="avatar" src="http://pic2.zhimg.com/5f0c0b95bcd0ceb0f980ea0cfa1172dd_is.jpg" />
                                                                                <span class="author">哥本哈根，</span>
                                                                                <span class="bio">我就是我</span>
                                                                               </div>
                                                                               <div class="content">
                                                                                <p>家里有个哥哥，小脑萎缩，智力正常只是腿不会走路。我妈是小学老师，所以就把哥哥弄到班级里上课，念了整个小学。毕竟我妈是老师，班里还都是小孩，没人欺负我哥哥，我哥哥应该过得很舒服吧！但当时我很小（我俩差了 6 岁），我记事起就知道家里哥哥不会走路，去卫生间什么的全是自己在地下爬什么的，然后小便用瓶子接。那时候习惯了，也没觉得害怕什么的，所以也没啥不好意思的，后来我带同学来我家玩，看见我哥哥在地下爬着去卫生间的时候他吓坏了，还和班里同学说来着，然后同学就知道我有个在地下爬着的哥哥了。</p>
                                                                                <p>我当时感觉非常无地自容，感觉这个残疾哥哥给我丢了脸，所以以后我不带同学回家了，在家也是对哥哥冷言冷语，甚至动手打他，真的，他撵不上我，我打完他就跑，拽他头发提他�
 */


/*
另一份log
07-23 18:47:29.555 16051-16093/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:小事 · 年少无知犯的错   ;ThumbnailUrl:http://pic2.zhimg.com/77367e99ccb9d148367028c5c82e8675.jpg
07-23 18:47:29.555 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 小事 · 年少无知犯的错   ;questionUrl:null
07-23 18:47:29.563 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 小事 · 年少无知犯的错   ;questionUrl:http://www.zhihu.com/question/48556523
07-23 18:47:29.563 16051-16093/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8599045
07-23 18:47:29.871 16051-16093/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:有人说这是一部喜剧，但笑过之后你会看到残酷的生活   ;ThumbnailUrl:http://pic1.zhimg.com/a3448a8ff53b69e558fe6838af1d6618.jpg
07-23 18:47:29.871 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 生活的真相往往比电影还要残酷，但我们终究得走下去   ;questionUrl:http://zhuanlan.zhihu.com/p/20438592
07-23 18:47:29.903 16051-16093/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8600604
07-23 18:47:30.135 16051-16093/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:当年这么穿，要把胸压得平平的   ;ThumbnailUrl:http://pic4.zhimg.com/ade37ab71fd4fd2098b8fa80ee6e24cb.jpg
07-23 18:47:30.135 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 从 18 世纪的英国人穿什么说起   ;questionUrl:http://zhuanlan.zhihu.com/p/20215440
07-23 18:47:30.135 16051-16093/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8600217
07-23 18:47:33.383 16051-16093/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:如何分清精灵球里是哪只神奇宝贝？   ;ThumbnailUrl:http://pic2.zhimg.com/d65dcd38d80a2396b80d518d875e2a65.jpg
07-23 18:47:33.383 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 如何分清精灵球里是哪只神奇宝贝？   ;questionUrl:http://www.zhihu.com/question/22595515
07-23 18:47:33.383 16051-16093/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8598225
07-23 18:47:33.603 16051-16093/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:皮肤不白的女生，也可以穿得很美啊   ;ThumbnailUrl:http://pic4.zhimg.com/b7ce11ce70322bc1d8beaa85480815c3.jpg
07-23 18:47:33.603 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 皮肤黑的妹子怎么穿衣？   ;questionUrl:http://www.zhihu.com/question/31051280
07-23 18:47:33.611 16051-16093/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8597310
07-23 18:47:33.815 16051-16093/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:知乎好问题 · 汽车 4S 店有哪些黑幕？   ;ThumbnailUrl:http://pic1.zhimg.com/e1c8c01e9afd9ac3695673b6f9510a50.jpg
07-23 18:47:33.815 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 知乎好问题 · 汽车 4S 店有哪些黑幕？   ;questionUrl:null
07-23 18:47:33.823 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 汽车 4S 店有哪些黑幕？   ;questionUrl:null
07-23 18:47:33.827 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 如何提高逻辑思维能力和表达能力？   ;questionUrl:null
07-23 18:47:33.827 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 你有什么相见恨晚的日语学习方法？   ;questionUrl:null
07-23 18:47:33.827 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 有哪些像《极简欧洲史》这样好读又深入的历史类书籍？   ;questionUrl:null
07-23 18:47:33.835 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 有哪些让你很惊艳的小诗？   ;questionUrl:null
07-23 18:47:33.835 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 知乎好问题 · 汽车 4S 店有哪些黑幕？   ;questionUrl:null
07-23 18:47:33.835 16051-16093/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8600521
07-23 18:47:34.131 16051-16093/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:手游里的「十连抽」和赌博的区别在哪儿？   ;ThumbnailUrl:http://pic4.zhimg.com/67812320ec2b92d2bd47dd0a73b0d08f.jpg
07-23 18:47:34.131 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 手游抽卡算不算赌博？   ;questionUrl:http://zhuanlan.zhihu.com/p/21659261
07-23 18:47:34.143 16051-16093/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8580472
07-23 18:47:34.495 16051-16093/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:这些菜，在夏天是神一般的存在   ;ThumbnailUrl:http://pic4.zhimg.com/38d400be1ec49562b40a9b6220f5f603.jpg
07-23 18:47:34.499 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 这些菜，在夏天是神一般的存在   ;questionUrl:http://zhuanlan.zhihu.com/p/21566149
07-23 18:47:34.503 16051-16093/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8598964
07-23 18:47:34.815 16051-16093/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:我是遗体整容师，你们更熟悉的称呼应该是，「入殓师」   ;ThumbnailUrl:http://pic4.zhimg.com/5a0b55412e0da41002703f5fcba5fe2f.jpg
07-23 18:47:34.815 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 入殓师是如何工作的？   ;questionUrl:http://www.zhihu.com/question/19732623
07-23 18:47:34.815 16051-16093/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8598433
07-23 18:47:34.987 16051-16093/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:人只有四五千毫升的血，为什么手术大出血能出一万毫升？   ;ThumbnailUrl:http://pic4.zhimg.com/259178cd14999f7938111f5873f2155b.jpg
07-23 18:47:34.987 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 为什么做手术会有一万毫升的大出血？   ;questionUrl:http://www.zhihu.com/question/45771218
07-23 18:47:34.987 16051-16093/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8597796
07-23 18:47:35.191 16051-16093/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:大误 · 追我的人那么多，但我却   ;ThumbnailUrl:http://pic2.zhimg.com/ea3caf7bd84080c8e70cba1a68010a55.jpg
07-23 18:47:35.191 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 在交通工具上靠着陌生人的肩膀睡着了，或是被陌生人靠着肩膀睡着了是怎样一种体验？   ;questionUrl:http://www.zhihu.com/question/48442985
07-23 18:47:35.199 16051-16093/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8597275
07-23 18:47:35.383 16051-16093/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:地上头发扫不完，厨房墙上全是油，其实这些问题都可以简单搞定   ;ThumbnailUrl:http://pic4.zhimg.com/33b2263b621623d18d2982d0e83b9ad7.jpg
07-23 18:47:35.383 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 改变清洁体验，体现生活品质   ;questionUrl:http://zhuanlan.zhihu.com/p/21648957
07-23 18:47:35.383 16051-16093/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8597150
07-23 18:47:35.663 16051-16093/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:欧洲足坛迎来「人民币玩家」，马竞与埃瓦尔上演股东德比   ;ThumbnailUrl:http://pic2.zhimg.com/4cca32a0167ce319d750f5ee9608c475.jpg
07-23 18:47:35.663 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 登陆欧洲足坛：中国玩家意欲何为？   ;questionUrl:http://zhuanlan.zhihu.com/p/21682344
07-23 18:47:35.667 16051-16093/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8586001
07-23 18:47:36.143 16051-16093/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:去看了看真实的精神病院，没有铁丝电网，病人也没有被绑在床上   ;ThumbnailUrl:http://pic4.zhimg.com/332ec32bbc918255880a4a2085852233.jpg
07-23 18:47:36.143 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 被误解的精神科   ;questionUrl:http://zhuanlan.zhihu.com/p/21644222
07-23 18:47:36.151 16051-16093/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8594158
07-23 18:47:36.627 16051-16093/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:一图解释，为什么日本的贫富差距这么小   ;ThumbnailUrl:http://pic2.zhimg.com/2452ab21c2ac701f3c508da46563e091.jpg
07-23 18:47:36.627 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 日本的贫富差距为何这么小？基尼系数为何这么低？   ;questionUrl:http://www.zhihu.com/question/20335505
07-23 18:47:36.627 16051-16093/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8596471
07-23 18:47:37.315 16051-16093/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:新闻上总有各种「地方债」的消息，它是怎么运转的？   ;ThumbnailUrl:http://pic2.zhimg.com/3eb4eb8c7366b9ac4c5e0ceff278d901.jpg
07-23 18:47:37.315 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 「地方债」是怎么运转的？   ;questionUrl:http://www.zhihu.com/question/21591532
07-23 18:47:37.319 16051-16093/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8598503
07-23 18:47:37.583 16051-16093/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:好多科学家做梦都想要的「室温超导」，究竟实现了没？   ;ThumbnailUrl:http://pic3.zhimg.com/59a0d5c0ccf309d6800a1d4f97a6e5c6.jpg
07-23 18:47:37.583 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 好多科学家做梦都想要的「室温超导」，究竟实现了没？   ;questionUrl:null
07-23 18:47:37.583 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 室温超导有可能实现吗？   ;questionUrl:http://www.zhihu.com/question/22636832
07-23 18:47:37.587 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 好多科学家做梦都想要的「室温超导」，究竟实现了没？   ;questionUrl:null
07-23 18:47:37.587 16051-16093/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8584521
07-23 18:47:40.839 16051-16093/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:其实，哆啦 A 梦根本通不过大雄家旧房子的走廊   ;ThumbnailUrl:http://pic1.zhimg.com/427d934eeb07b69db87e542cc20e8684.jpg
07-23 18:47:40.839 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 有没有人能画出来大雄的家的户型图？   ;questionUrl:http://www.zhihu.com/question/48516891
07-23 18:47:40.847 16051-16093/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8598898
07-23 18:47:41.307 16051-16093/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:读读日报 24 小时热门 TOP 5 · 惨痛的理发经历   ;ThumbnailUrl:http://pic1.zhimg.com/a260acf366f3837da6b9060690a8c5d4.jpg
07-23 18:47:41.307 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 读读日报 24 小时热门 TOP 5 · 惨痛的理发经历   ;questionUrl:null
07-23 18:47:41.319 16051-16093/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8597931
07-23 18:47:41.519 16051-16093/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:瞎扯 · 如何正确地吐槽   ;ThumbnailUrl:http://pic3.zhimg.com/45d78d9cd9f995b4915e141fb84dea7a.jpg
07-23 18:47:41.519 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 人可以有多过分？   ;questionUrl:http://www.zhihu.com/question/48220638
07-23 18:47:41.519 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 如果你是诸葛亮，在摆空城计的时候你会唱什么歌？   ;questionUrl:http://www.zhihu.com/question/48318331
07-23 18:47:41.523 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 挑食是一种什么体验？   ;questionUrl:http://www.zhihu.com/question/31723595
07-23 18:47:41.527 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 一个人旅行应该怎样自拍？   ;questionUrl:http://www.zhihu.com/question/22425541
07-23 18:47:28.447 16051-16093/io.github.izzyleung.zhihudailypurify I/dingurl: http://news.at.zhihu.com/api/4/news/before/20160724
07-23 18:47:28.667 16051-16093/io.github.izzyleung.zhihudailypurify I/dingYong: yongdaowole
07-23 18:47:28.667 16051-16093/io.github.izzyleung.zhihudailypurify I/dingYong: yeyongdaowole
07-23 18:47:28.667 16051-16093/io.github.izzyleung.zhihudailypurify I/dingurl: http://news.at.zhihu.com/api/4/news/before/20160724
07-23 18:47:28.839 16051-16093/io.github.izzyleung.zhihudailypurify I/dingYong: yongdaowole
07-23 18:47:28.843 16051-16093/io.github.izzyleung.zhihudailypurify I/dingYong: yeyongdaowole
07-23 18:47:28.843 16051-16093/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8589107
07-23 18:47:29.555 16051-16093/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:小事 · 年少无知犯的错   ;ThumbnailUrl:http://pic2.zhimg.com/77367e99ccb9d148367028c5c82e8675.jpg
07-23 18:47:29.555 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 小事 · 年少无知犯的错   ;questionUrl:null
07-23 18:47:29.563 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 小事 · 年少无知犯的错   ;questionUrl:http://www.zhihu.com/question/48556523
07-23 18:47:29.563 16051-16093/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8599045
07-23 18:47:29.871 16051-16093/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:有人说这是一部喜剧，但笑过之后你会看到残酷的生活   ;ThumbnailUrl:http://pic1.zhimg.com/a3448a8ff53b69e558fe6838af1d6618.jpg
07-23 18:47:29.871 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 生活的真相往往比电影还要残酷，但我们终究得走下去   ;questionUrl:http://zhuanlan.zhihu.com/p/20438592
07-23 18:47:29.903 16051-16093/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8600604
07-23 18:47:30.135 16051-16093/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:当年这么穿，要把胸压得平平的   ;ThumbnailUrl:http://pic4.zhimg.com/ade37ab71fd4fd2098b8fa80ee6e24cb.jpg
07-23 18:47:30.135 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 从 18 世纪的英国人穿什么说起   ;questionUrl:http://zhuanlan.zhihu.com/p/20215440
07-23 18:47:30.135 16051-16093/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8600217
07-23 18:47:33.383 16051-16093/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:如何分清精灵球里是哪只神奇宝贝？   ;ThumbnailUrl:http://pic2.zhimg.com/d65dcd38d80a2396b80d518d875e2a65.jpg
07-23 18:47:33.383 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 如何分清精灵球里是哪只神奇宝贝？   ;questionUrl:http://www.zhihu.com/question/22595515
07-23 18:47:33.383 16051-16093/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8598225
07-23 18:47:33.603 16051-16093/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:皮肤不白的女生，也可以穿得很美啊   ;ThumbnailUrl:http://pic4.zhimg.com/b7ce11ce70322bc1d8beaa85480815c3.jpg
07-23 18:47:33.603 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 皮肤黑的妹子怎么穿衣？   ;questionUrl:http://www.zhihu.com/question/31051280
07-23 18:47:33.611 16051-16093/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8597310
07-23 18:47:33.815 16051-16093/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:知乎好问题 · 汽车 4S 店有哪些黑幕？   ;ThumbnailUrl:http://pic1.zhimg.com/e1c8c01e9afd9ac3695673b6f9510a50.jpg
07-23 18:47:33.815 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 知乎好问题 · 汽车 4S 店有哪些黑幕？   ;questionUrl:null
07-23 18:47:33.823 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 汽车 4S 店有哪些黑幕？   ;questionUrl:null
07-23 18:47:33.827 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 如何提高逻辑思维能力和表达能力？   ;questionUrl:null
07-23 18:47:33.827 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 你有什么相见恨晚的日语学习方法？   ;questionUrl:null
07-23 18:47:33.827 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 有哪些像《极简欧洲史》这样好读又深入的历史类书籍？   ;questionUrl:null
07-23 18:47:33.835 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 有哪些让你很惊艳的小诗？   ;questionUrl:null
07-23 18:47:33.835 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 知乎好问题 · 汽车 4S 店有哪些黑幕？   ;questionUrl:null
07-23 18:47:33.835 16051-16093/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8600521
07-23 18:47:34.131 16051-16093/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:手游里的「十连抽」和赌博的区别在哪儿？   ;ThumbnailUrl:http://pic4.zhimg.com/67812320ec2b92d2bd47dd0a73b0d08f.jpg
07-23 18:47:34.131 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 手游抽卡算不算赌博？   ;questionUrl:http://zhuanlan.zhihu.com/p/21659261
07-23 18:47:34.143 16051-16093/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8580472
07-23 18:47:34.495 16051-16093/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:这些菜，在夏天是神一般的存在   ;ThumbnailUrl:http://pic4.zhimg.com/38d400be1ec49562b40a9b6220f5f603.jpg
07-23 18:47:34.499 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 这些菜，在夏天是神一般的存在   ;questionUrl:http://zhuanlan.zhihu.com/p/21566149
07-23 18:47:34.503 16051-16093/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8598964
07-23 18:47:34.815 16051-16093/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:我是遗体整容师，你们更熟悉的称呼应该是，「入殓师」   ;ThumbnailUrl:http://pic4.zhimg.com/5a0b55412e0da41002703f5fcba5fe2f.jpg
07-23 18:47:34.815 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 入殓师是如何工作的？   ;questionUrl:http://www.zhihu.com/question/19732623
07-23 18:47:34.815 16051-16093/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8598433
07-23 18:47:34.987 16051-16093/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:人只有四五千毫升的血，为什么手术大出血能出一万毫升？   ;ThumbnailUrl:http://pic4.zhimg.com/259178cd14999f7938111f5873f2155b.jpg
07-23 18:47:34.987 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 为什么做手术会有一万毫升的大出血？   ;questionUrl:http://www.zhihu.com/question/45771218
07-23 18:47:34.987 16051-16093/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8597796
07-23 18:47:35.191 16051-16093/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:大误 · 追我的人那么多，但我却   ;ThumbnailUrl:http://pic2.zhimg.com/ea3caf7bd84080c8e70cba1a68010a55.jpg
07-23 18:47:35.191 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 在交通工具上靠着陌生人的肩膀睡着了，或是被陌生人靠着肩膀睡着了是怎样一种体验？   ;questionUrl:http://www.zhihu.com/question/48442985
07-23 18:47:35.199 16051-16093/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8597275
07-23 18:47:35.383 16051-16093/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:地上头发扫不完，厨房墙上全是油，其实这些问题都可以简单搞定   ;ThumbnailUrl:http://pic4.zhimg.com/33b2263b621623d18d2982d0e83b9ad7.jpg
07-23 18:47:35.383 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 改变清洁体验，体现生活品质   ;questionUrl:http://zhuanlan.zhihu.com/p/21648957
07-23 18:47:35.383 16051-16093/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8597150
07-23 18:47:35.663 16051-16093/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:欧洲足坛迎来「人民币玩家」，马竞与埃瓦尔上演股东德比   ;ThumbnailUrl:http://pic2.zhimg.com/4cca32a0167ce319d750f5ee9608c475.jpg
07-23 18:47:35.663 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 登陆欧洲足坛：中国玩家意欲何为？   ;questionUrl:http://zhuanlan.zhihu.com/p/21682344
07-23 18:47:35.667 16051-16093/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8586001
07-23 18:47:36.143 16051-16093/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:去看了看真实的精神病院，没有铁丝电网，病人也没有被绑在床上   ;ThumbnailUrl:http://pic4.zhimg.com/332ec32bbc918255880a4a2085852233.jpg
07-23 18:47:36.143 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 被误解的精神科   ;questionUrl:http://zhuanlan.zhihu.com/p/21644222
07-23 18:47:36.151 16051-16093/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8594158
07-23 18:47:36.627 16051-16093/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:一图解释，为什么日本的贫富差距这么小   ;ThumbnailUrl:http://pic2.zhimg.com/2452ab21c2ac701f3c508da46563e091.jpg
07-23 18:47:36.627 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 日本的贫富差距为何这么小？基尼系数为何这么低？   ;questionUrl:http://www.zhihu.com/question/20335505
07-23 18:47:36.627 16051-16093/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8596471
07-23 18:47:37.315 16051-16093/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:新闻上总有各种「地方债」的消息，它是怎么运转的？   ;ThumbnailUrl:http://pic2.zhimg.com/3eb4eb8c7366b9ac4c5e0ceff278d901.jpg
07-23 18:47:37.315 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 「地方债」是怎么运转的？   ;questionUrl:http://www.zhihu.com/question/21591532
07-23 18:47:37.319 16051-16093/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8598503
07-23 18:47:37.583 16051-16093/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:好多科学家做梦都想要的「室温超导」，究竟实现了没？   ;ThumbnailUrl:http://pic3.zhimg.com/59a0d5c0ccf309d6800a1d4f97a6e5c6.jpg
07-23 18:47:37.583 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 好多科学家做梦都想要的「室温超导」，究竟实现了没？   ;questionUrl:null
07-23 18:47:37.583 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 室温超导有可能实现吗？   ;questionUrl:http://www.zhihu.com/question/22636832
07-23 18:47:37.587 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 好多科学家做梦都想要的「室温超导」，究竟实现了没？   ;questionUrl:null
07-23 18:47:37.587 16051-16093/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8584521
07-23 18:47:40.839 16051-16093/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:其实，哆啦 A 梦根本通不过大雄家旧房子的走廊   ;ThumbnailUrl:http://pic1.zhimg.com/427d934eeb07b69db87e542cc20e8684.jpg
07-23 18:47:40.839 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 有没有人能画出来大雄的家的户型图？   ;questionUrl:http://www.zhihu.com/question/48516891
07-23 18:47:40.847 16051-16093/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8598898
07-23 18:47:41.307 16051-16093/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:读读日报 24 小时热门 TOP 5 · 惨痛的理发经历   ;ThumbnailUrl:http://pic1.zhimg.com/a260acf366f3837da6b9060690a8c5d4.jpg
07-23 18:47:41.307 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 读读日报 24 小时热门 TOP 5 · 惨痛的理发经历   ;questionUrl:null
07-23 18:47:41.319 16051-16093/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8597931
07-23 18:47:41.519 16051-16093/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:瞎扯 · 如何正确地吐槽   ;ThumbnailUrl:http://pic3.zhimg.com/45d78d9cd9f995b4915e141fb84dea7a.jpg
07-23 18:47:41.519 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 人可以有多过分？   ;questionUrl:http://www.zhihu.com/question/48220638
07-23 18:47:41.519 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 如果你是诸葛亮，在摆空城计的时候你会唱什么歌？   ;questionUrl:http://www.zhihu.com/question/48318331
07-23 18:47:41.523 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 挑食是一种什么体验？   ;questionUrl:http://www.zhihu.com/question/31723595
07-23 18:47:41.527 16051-16093/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 一个人旅行应该怎样自拍？   ;questionUrl:http://www.zhihu.com/question/22425541
07-23 22:22:48.961 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news.at.zhihu.com/api/4/news/before/20160724
07-23 22:22:49.201 16051-3902/io.github.izzyleung.zhihudailypurify I/dingYong: yongdaowole
07-23 22:22:49.205 16051-3902/io.github.izzyleung.zhihudailypurify I/dingYong: yeyongdaowole
07-23 22:22:49.217 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news.at.zhihu.com/api/4/news/before/20160724
07-23 22:22:49.385 16051-3902/io.github.izzyleung.zhihudailypurify I/dingYong: yongdaowole
07-23 22:22:49.385 16051-3902/io.github.izzyleung.zhihudailypurify I/dingYong: yeyongdaowole
07-23 22:22:49.389 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8589107
07-23 22:22:49.665 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:小事 · 年少无知犯的错   ;ThumbnailUrl:http://pic2.zhimg.com/77367e99ccb9d148367028c5c82e8675.jpg
07-23 22:22:49.665 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 小事 · 年少无知犯的错   ;questionUrl:null
07-23 22:22:49.677 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 小事 · 年少无知犯的错   ;questionUrl:http://www.zhihu.com/question/48556523
07-23 22:22:49.677 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8599045
07-23 22:22:49.913 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:有人说这是一部喜剧，但笑过之后你会看到残酷的生活   ;ThumbnailUrl:http://pic1.zhimg.com/a3448a8ff53b69e558fe6838af1d6618.jpg
07-23 22:22:49.913 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 生活的真相往往比电影还要残酷，但我们终究得走下去   ;questionUrl:http://zhuanlan.zhihu.com/p/20438592
07-23 22:22:49.917 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8600604
07-23 22:22:50.165 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:当年这么穿，要把胸压得平平的   ;ThumbnailUrl:http://pic4.zhimg.com/ade37ab71fd4fd2098b8fa80ee6e24cb.jpg
07-23 22:22:50.165 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 从 18 世纪的英国人穿什么说起   ;questionUrl:http://zhuanlan.zhihu.com/p/20215440
07-23 22:22:50.169 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8600217
07-23 22:22:53.457 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:如何分清精灵球里是哪只神奇宝贝？   ;ThumbnailUrl:http://pic2.zhimg.com/d65dcd38d80a2396b80d518d875e2a65.jpg
07-23 22:22:53.457 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 如何分清精灵球里是哪只神奇宝贝？   ;questionUrl:http://www.zhihu.com/question/22595515
07-23 22:22:53.461 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8598225
07-23 22:22:53.753 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:皮肤不白的女生，也可以穿得很美啊   ;ThumbnailUrl:http://pic4.zhimg.com/b7ce11ce70322bc1d8beaa85480815c3.jpg
07-23 22:22:53.753 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 皮肤黑的妹子怎么穿衣？   ;questionUrl:http://www.zhihu.com/question/31051280
07-23 22:22:53.757 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8597310
07-23 22:22:53.961 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:知乎好问题 · 汽车 4S 店有哪些黑幕？   ;ThumbnailUrl:http://pic1.zhimg.com/e1c8c01e9afd9ac3695673b6f9510a50.jpg
07-23 22:22:53.961 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 知乎好问题 · 汽车 4S 店有哪些黑幕？   ;questionUrl:null
07-23 22:22:53.961 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 汽车 4S 店有哪些黑幕？   ;questionUrl:null
07-23 22:22:53.965 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 如何提高逻辑思维能力和表达能力？   ;questionUrl:null
07-23 22:22:53.973 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 你有什么相见恨晚的日语学习方法？   ;questionUrl:null
07-23 22:22:53.977 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 有哪些像《极简欧洲史》这样好读又深入的历史类书籍？   ;questionUrl:null
07-23 22:22:53.985 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 有哪些让你很惊艳的小诗？   ;questionUrl:null
07-23 22:22:53.985 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 知乎好问题 · 汽车 4S 店有哪些黑幕？   ;questionUrl:null
07-23 22:22:53.985 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8600521
07-23 22:22:57.425 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:手游里的「十连抽」和赌博的区别在哪儿？   ;ThumbnailUrl:http://pic4.zhimg.com/67812320ec2b92d2bd47dd0a73b0d08f.jpg
07-23 22:22:57.445 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 手游抽卡算不算赌博？   ;questionUrl:http://zhuanlan.zhihu.com/p/21659261
07-23 22:22:57.445 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8580472
07-23 22:22:57.837 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:这些菜，在夏天是神一般的存在   ;ThumbnailUrl:http://pic4.zhimg.com/38d400be1ec49562b40a9b6220f5f603.jpg
07-23 22:22:57.837 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 这些菜，在夏天是神一般的存在   ;questionUrl:http://zhuanlan.zhihu.com/p/21566149
07-23 22:22:57.837 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8598964
07-23 22:22:58.049 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:我是遗体整容师，你们更熟悉的称呼应该是，「入殓师」   ;ThumbnailUrl:http://pic4.zhimg.com/5a0b55412e0da41002703f5fcba5fe2f.jpg
07-23 22:22:58.049 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 入殓师是如何工作的？   ;questionUrl:http://www.zhihu.com/question/19732623
07-23 22:22:58.049 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8598433
07-23 22:22:58.257 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:人只有四五千毫升的血，为什么手术大出血能出一万毫升？   ;ThumbnailUrl:http://pic4.zhimg.com/259178cd14999f7938111f5873f2155b.jpg
07-23 22:22:58.257 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 为什么做手术会有一万毫升的大出血？   ;questionUrl:http://www.zhihu.com/question/45771218
07-23 22:22:58.261 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8597796
07-23 22:22:58.577 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:大误 · 追我的人那么多，但我却   ;ThumbnailUrl:http://pic2.zhimg.com/ea3caf7bd84080c8e70cba1a68010a55.jpg
07-23 22:22:58.577 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 在交通工具上靠着陌生人的肩膀睡着了，或是被陌生人靠着肩膀睡着了是怎样一种体验？   ;questionUrl:http://www.zhihu.com/question/48442985
07-23 22:22:58.577 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8597275
07-23 22:22:58.837 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:地上头发扫不完，厨房墙上全是油，其实这些问题都可以简单搞定   ;ThumbnailUrl:http://pic4.zhimg.com/33b2263b621623d18d2982d0e83b9ad7.jpg
07-23 22:22:58.857 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 改变清洁体验，体现生活品质   ;questionUrl:http://zhuanlan.zhihu.com/p/21648957
07-23 22:22:58.857 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8597150
07-23 22:22:59.213 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:欧洲足坛迎来「人民币玩家」，马竞与埃瓦尔上演股东德比   ;ThumbnailUrl:http://pic2.zhimg.com/4cca32a0167ce319d750f5ee9608c475.jpg
07-23 22:22:59.217 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 登陆欧洲足坛：中国玩家意欲何为？   ;questionUrl:http://zhuanlan.zhihu.com/p/21682344
07-23 22:22:59.233 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8586001
07-23 22:22:59.405 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:去看了看真实的精神病院，没有铁丝电网，病人也没有被绑在床上   ;ThumbnailUrl:http://pic4.zhimg.com/332ec32bbc918255880a4a2085852233.jpg
07-23 22:22:59.409 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 被误解的精神科   ;questionUrl:http://zhuanlan.zhihu.com/p/21644222
07-23 22:22:59.413 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8594158
07-23 22:22:59.601 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:一图解释，为什么日本的贫富差距这么小   ;ThumbnailUrl:http://pic2.zhimg.com/2452ab21c2ac701f3c508da46563e091.jpg
07-23 22:22:59.601 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 日本的贫富差距为何这么小？基尼系数为何这么低？   ;questionUrl:http://www.zhihu.com/question/20335505
07-23 22:22:59.625 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8596471
07-23 22:22:59.869 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:新闻上总有各种「地方债」的消息，它是怎么运转的？   ;ThumbnailUrl:http://pic2.zhimg.com/3eb4eb8c7366b9ac4c5e0ceff278d901.jpg
07-23 22:22:59.869 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 「地方债」是怎么运转的？   ;questionUrl:http://www.zhihu.com/question/21591532
07-23 22:22:59.873 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8598503
07-23 22:23:00.177 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:好多科学家做梦都想要的「室温超导」，究竟实现了没？   ;ThumbnailUrl:http://pic3.zhimg.com/59a0d5c0ccf309d6800a1d4f97a6e5c6.jpg
07-23 22:23:00.177 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 好多科学家做梦都想要的「室温超导」，究竟实现了没？   ;questionUrl:null
07-23 22:23:00.193 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 室温超导有可能实现吗？   ;questionUrl:http://www.zhihu.com/question/22636832
07-23 22:23:00.193 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 好多科学家做梦都想要的「室温超导」，究竟实现了没？   ;questionUrl:null
07-23 22:23:00.197 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8584521
07-23 22:23:00.481 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:其实，哆啦 A 梦根本通不过大雄家旧房子的走廊   ;ThumbnailUrl:http://pic1.zhimg.com/427d934eeb07b69db87e542cc20e8684.jpg
07-23 22:23:00.481 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 有没有人能画出来大雄的家的户型图？   ;questionUrl:http://www.zhihu.com/question/48516891
07-23 22:23:00.481 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8598898
07-23 22:23:00.653 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:读读日报 24 小时热门 TOP 5 · 惨痛的理发经历   ;ThumbnailUrl:http://pic1.zhimg.com/a260acf366f3837da6b9060690a8c5d4.jpg
07-23 22:23:00.677 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 读读日报 24 小时热门 TOP 5 · 惨痛的理发经历   ;questionUrl:null
07-23 22:23:00.681 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8597931
07-23 22:23:00.917 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:瞎扯 · 如何正确地吐槽   ;ThumbnailUrl:http://pic3.zhimg.com/45d78d9cd9f995b4915e141fb84dea7a.jpg
07-23 22:23:00.921 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 人可以有多过分？   ;questionUrl:http://www.zhihu.com/question/48220638
07-23 22:23:00.925 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 如果你是诸葛亮，在摆空城计的时候你会唱什么歌？   ;questionUrl:http://www.zhihu.com/question/48318331
07-23 22:23:00.929 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 挑食是一种什么体验？   ;questionUrl:http://www.zhihu.com/question/31723595
07-23 22:23:00.929 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 一个人旅行应该怎样自拍？   ;questionUrl:http://www.zhihu.com/question/22425541
07-23 22:23:07.217 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news.at.zhihu.com/api/4/news/before/20160723
07-23 22:23:07.381 16051-3902/io.github.izzyleung.zhihudailypurify I/dingYong: yongdaowole
07-23 22:23:07.381 16051-3902/io.github.izzyleung.zhihudailypurify I/dingYong: yeyongdaowole
07-23 22:23:07.381 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news.at.zhihu.com/api/4/news/before/20160723
07-23 22:23:07.549 16051-3902/io.github.izzyleung.zhihudailypurify I/dingYong: yongdaowole
07-23 22:23:07.549 16051-3902/io.github.izzyleung.zhihudailypurify I/dingYong: yeyongdaowole
07-23 22:23:07.549 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8590433
07-23 22:23:07.977 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:小事 · 如果这都不算爱   ;ThumbnailUrl:http://pic1.zhimg.com/8a7ec3bcc58a6945b203b3f69fcef4e8.jpg
07-23 22:23:07.997 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 小事 · 如果这都不算爱   ;questionUrl:null
07-23 22:23:07.997 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 小事 · 如果这都不算爱   ;questionUrl:http://www.zhihu.com/question/48442985
07-23 22:23:07.997 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8596691
07-23 22:23:08.349 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:你好，我来推荐几部像《无间道》一样好看的片子   ;ThumbnailUrl:http://pic4.zhimg.com/a222ac91b350e1410b5fbb616a834a67.jpg
07-23 22:23:08.349 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 你好，我来推荐几部像《无间道》一样好看的片子   ;questionUrl:http://www.zhihu.com/question/25517201
07-23 22:23:08.349 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8597810
07-23 22:23:08.569 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:如果你跟我一样害怕自由落体，又想在空中翱翔，不妨试试这个   ;ThumbnailUrl:http://pic3.zhimg.com/f3829664de5897dd2a2ddfec19445b0a.jpg
07-23 22:23:08.597 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 滑翔伞 — 如鸟儿般自由飞翔   ;questionUrl:http://zhuanlan.zhihu.com/p/20888444
07-23 22:23:08.597 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8597233
07-23 22:23:12.033 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:完成这么伟大的发明，按理说该踏上人生巅峰吧，可惜……   ;ThumbnailUrl:http://pic3.zhimg.com/3344aa9b8bb81773b8d7772197a51ca6.jpg
07-23 22:23:12.033 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 雨刷，注定离你而去   ;questionUrl:http://zhuanlan.zhihu.com/p/21662536
07-23 22:23:12.033 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8596587
07-23 22:23:12.273 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:想看看地球里面长什么样，我钻个洞就行吗？   ;ThumbnailUrl:http://pic2.zhimg.com/612d49fdc0683abb4022492f998ffdf1.jpg
07-23 22:23:12.293 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 人类对地球内部的探索能达到什么程度？钻深井研究地球内部有没有意义？   ;questionUrl:http://www.zhihu.com/question/23003901
07-23 22:23:12.293 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8596755
07-23 22:23:12.681 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:知乎好问题 · 如何保持精力充沛，避免困倦、疲乏？   ;ThumbnailUrl:http://pic2.zhimg.com/e9dd9d90599c8fe4775a28f9df745951.jpg
07-23 22:23:12.681 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 知乎好问题 · 如何保持精力充沛，避免困倦、疲乏？   ;questionUrl:null
07-23 22:23:12.681 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 如何保持精力充沛，有效避免困倦、疲乏？   ;questionUrl:null
07-23 22:23:12.689 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 男朋友生日送什么礼物好？为什么？   ;questionUrl:null
07-23 22:23:12.689 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 如何写一个美观漂亮的 Word 文档？   ;questionUrl:null
07-23 22:23:12.689 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 如何改善说话时没底气这种情况？   ;questionUrl:null
07-23 22:23:12.689 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 如何专业地挑选洗面奶？   ;questionUrl:null
07-23 22:23:12.689 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 知乎好问题 · 如何保持精力充沛，避免困倦、疲乏？   ;questionUrl:null
07-23 22:23:12.689 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8596854
07-23 22:23:12.909 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:滴滴和 Uber 也要合并了？时机尚未成熟，同志仍需努力   ;ThumbnailUrl:http://pic2.zhimg.com/644d925cc5f36cb59686f4eaebcb58d9.jpg
07-23 22:23:12.909 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 为什么滴滴和 Uber 暂时还不会合并   ;questionUrl:http://zhuanlan.zhihu.com/p/21675491
07-23 22:23:12.917 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8592418
07-23 22:23:13.196 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:为什么人总是倾向于相信自己愿意相信的？   ;ThumbnailUrl:http://pic3.zhimg.com/021b690012d55631e1ac6b8efe095aaa.jpg
07-23 22:23:13.200 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 为什么人总是倾向于相信自己愿意相信的？   ;questionUrl:http://www.zhihu.com/question/21126315
07-23 22:23:13.204 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8592590
07-23 22:23:13.468 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:那些「二三流人物」的一流故事   ;ThumbnailUrl:http://pic1.zhimg.com/a934e71c8f73cef24edc70284264ea8c.jpg
07-23 22:23:13.468 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 有哪些「二三流人物」的一流故事？   ;questionUrl:http://www.zhihu.com/question/35063131
07-23 22:23:13.472 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8596396
07-23 22:23:13.852 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:关于英文邮件，你会不会也被这个误区影响了很多年？   ;ThumbnailUrl:http://pic1.zhimg.com/d1ac517261cf429e4258d4cda7985f80.jpg
07-23 22:23:13.872 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 哪些神句拯救了你的英文邮件？   ;questionUrl:http://www.zhihu.com/question/34147404
07-23 22:23:13.872 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8596452
07-23 22:23:14.116 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:大误 · 这是一个非常经典的笑话   ;ThumbnailUrl:http://pic4.zhimg.com/d5707c5e54a5636e31591da3b6a23a4f.jpg
07-23 22:23:14.124 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 福尔摩斯和华生看星空的笑话，体现出文科生和理科生的思路有哪些区别？   ;questionUrl:null
07-23 22:23:14.132 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 大误 · 这是一个非常经典的笑话   ;questionUrl:http://www.zhihu.com/question/28303674
07-23 22:23:14.136 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8596217
07-23 22:23:14.332 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:婚姻状态和事业有关系吗？   ;ThumbnailUrl:http://pic3.zhimg.com/bec039593f8b69df527c29f962c15912.jpg
07-23 22:23:14.336 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 婚姻状态和事业有关系吗？   ;questionUrl:http://www.zhihu.com/question/21799733
07-23 22:23:14.340 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8594730
07-23 22:23:14.540 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:「有人问我，什么是海，我说不清，于是拿出了杉本博司的海景」   ;ThumbnailUrl:http://pic1.zhimg.com/b35d65ce3374fd7ab43494b2d1ca8d08.jpg
07-23 22:23:14.540 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 从日本摄影师杉本博司的系列作品中，如何理解摄影与哲学之间的关系？   ;questionUrl:http://www.zhihu.com/question/34839882
07-23 22:23:14.548 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8589106
07-23 22:23:14.804 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:没真正当过小孩的人，是没办法成为大人的   ;ThumbnailUrl:http://pic1.zhimg.com/aa013375822d3a71516f12998cb72b94.jpg
07-23 22:23:14.804 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 没真正当过小孩的人，是没办法成为大人的｜你了解自己的「家庭规则」吗？   ;questionUrl:http://zhuanlan.zhihu.com/p/21658058
07-23 22:23:14.808 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8594414
07-23 22:23:15.012 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:用这属于盛夏的植物，做出一碗滑溜溜、清亮亮的甜点   ;ThumbnailUrl:http://pic1.zhimg.com/5fd54255d9ca5f8a262082d7141f8c6c.jpg
07-23 22:23:15.016 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 【节气手帖】大暑：木莲豆腐   ;questionUrl:http://zhuanlan.zhihu.com/p/21423645
07-23 22:23:15.020 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8593178
07-23 22:23:15.228 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:10 亿美元，联合利华打算买一家用互联网思维卖剃须刀的公司   ;ThumbnailUrl:http://pic2.zhimg.com/ccb58ea9fa6533f1c0eaa68a0b70ef89.jpg
07-23 22:23:15.244 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 联合利华为什么要收购男性护理品定购服务商 Dollar Shave Club?   ;questionUrl:http://www.zhihu.com/question/48702865
07-23 22:23:15.244 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8594451
07-23 22:23:15.468 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:国内即将上市的 HPV 疫苗安全吗？   ;ThumbnailUrl:http://pic2.zhimg.com/ebdd8ab410ffaf9f411e6069f3fb1671.jpg
07-23 22:23:15.468 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 国内即将上市的 HPV 疫苗安全吗？   ;questionUrl:http://zhuanlan.zhihu.com/p/21645566
07-23 22:23:15.484 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8592991
07-23 22:23:15.684 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:在美国，袭警者被击毙，法律上可以被定性为自杀   ;ThumbnailUrl:http://pic3.zhimg.com/af81feebba38ab7a38a07e6e7df5298e.jpg
07-23 22:23:15.684 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 美国警察是如何处置袭警事件的？   ;questionUrl:http://www.zhihu.com/question/30435413
07-23 22:23:15.708 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8594992
07-23 22:23:16.036 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:读读日报 24 小时热门 TOP 5 · 关于《千与千寻》的疑问有了标准答案   ;ThumbnailUrl:http://pic1.zhimg.com/e800d8bbe8ce62a52f6cd9833e2d0604.jpg
07-23 22:23:16.040 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 读读日报 24 小时热门 TOP 5 · 关于《千与千寻》的疑问有了标准答案   ;questionUrl:null
07-23 22:23:16.044 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8592973
07-23 22:23:16.596 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:瞎扯 · 如何正确地吐槽   ;ThumbnailUrl:http://pic2.zhimg.com/3cae725b7833d3d12e83f60e4f2263e1.jpg
07-23 22:23:16.596 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 淘宝网有哪些鲜为人知的使用技巧和功能？   ;questionUrl:http://www.zhihu.com/question/21377595
07-23 22:23:16.596 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 如何在公开场合讲黄段子，又能把尺度拿捏的很好？   ;questionUrl:http://www.zhihu.com/question/21062494
07-23 22:23:16.596 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 你第一次亲眼见到明星的情景和感觉是怎样？   ;questionUrl:http://www.zhihu.com/question/20279591
07-23 22:23:37.672 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news.at.zhihu.com/api/4/news/before/20160723
07-23 22:23:37.904 16051-3902/io.github.izzyleung.zhihudailypurify I/dingYong: yongdaowole
07-23 22:23:37.904 16051-3902/io.github.izzyleung.zhihudailypurify I/dingYong: yeyongdaowole
07-23 22:23:37.908 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news.at.zhihu.com/api/4/news/before/20160723
07-23 22:23:38.092 16051-3902/io.github.izzyleung.zhihudailypurify I/dingYong: yongdaowole
07-23 22:23:38.092 16051-3902/io.github.izzyleung.zhihudailypurify I/dingYong: yeyongdaowole
07-23 22:23:38.096 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8590433
07-23 22:23:38.340 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:小事 · 如果这都不算爱   ;ThumbnailUrl:http://pic1.zhimg.com/8a7ec3bcc58a6945b203b3f69fcef4e8.jpg
07-23 22:23:38.344 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 小事 · 如果这都不算爱   ;questionUrl:null
07-23 22:23:38.348 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 小事 · 如果这都不算爱   ;questionUrl:http://www.zhihu.com/question/48442985
07-23 22:23:38.352 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8596691
07-23 22:23:38.556 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:你好，我来推荐几部像《无间道》一样好看的片子   ;ThumbnailUrl:http://pic4.zhimg.com/a222ac91b350e1410b5fbb616a834a67.jpg
07-23 22:23:38.556 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 你好，我来推荐几部像《无间道》一样好看的片子   ;questionUrl:http://www.zhihu.com/question/25517201
07-23 22:23:38.560 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8597810
07-23 22:23:38.824 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:如果你跟我一样害怕自由落体，又想在空中翱翔，不妨试试这个   ;ThumbnailUrl:http://pic3.zhimg.com/f3829664de5897dd2a2ddfec19445b0a.jpg
07-23 22:23:38.828 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 滑翔伞 — 如鸟儿般自由飞翔   ;questionUrl:http://zhuanlan.zhihu.com/p/20888444
07-23 22:23:38.840 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8597233
07-23 22:23:39.008 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:完成这么伟大的发明，按理说该踏上人生巅峰吧，可惜……   ;ThumbnailUrl:http://pic3.zhimg.com/3344aa9b8bb81773b8d7772197a51ca6.jpg
07-23 22:23:39.032 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 雨刷，注定离你而去   ;questionUrl:http://zhuanlan.zhihu.com/p/21662536
07-23 22:23:39.044 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8596587
07-23 22:23:39.456 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:想看看地球里面长什么样，我钻个洞就行吗？   ;ThumbnailUrl:http://pic2.zhimg.com/612d49fdc0683abb4022492f998ffdf1.jpg
07-23 22:23:39.456 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 人类对地球内部的探索能达到什么程度？钻深井研究地球内部有没有意义？   ;questionUrl:http://www.zhihu.com/question/23003901
07-23 22:23:39.468 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8596755
07-23 22:23:40.020 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:知乎好问题 · 如何保持精力充沛，避免困倦、疲乏？   ;ThumbnailUrl:http://pic2.zhimg.com/e9dd9d90599c8fe4775a28f9df745951.jpg
07-23 22:23:40.040 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 知乎好问题 · 如何保持精力充沛，避免困倦、疲乏？   ;questionUrl:null
07-23 22:23:40.040 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 如何保持精力充沛，有效避免困倦、疲乏？   ;questionUrl:null
07-23 22:23:40.044 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 男朋友生日送什么礼物好？为什么？   ;questionUrl:null
07-23 22:23:40.048 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 如何写一个美观漂亮的 Word 文档？   ;questionUrl:null
07-23 22:23:40.052 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 如何改善说话时没底气这种情况？   ;questionUrl:null
07-23 22:23:40.052 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 如何专业地挑选洗面奶？   ;questionUrl:null
07-23 22:23:40.052 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 知乎好问题 · 如何保持精力充沛，避免困倦、疲乏？   ;questionUrl:null
07-23 22:23:40.056 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8596854
07-23 22:23:40.304 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:滴滴和 Uber 也要合并了？时机尚未成熟，同志仍需努力   ;ThumbnailUrl:http://pic2.zhimg.com/644d925cc5f36cb59686f4eaebcb58d9.jpg
07-23 22:23:40.304 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 为什么滴滴和 Uber 暂时还不会合并   ;questionUrl:http://zhuanlan.zhihu.com/p/21675491
07-23 22:23:40.304 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8592418
07-23 22:23:40.588 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:为什么人总是倾向于相信自己愿意相信的？   ;ThumbnailUrl:http://pic3.zhimg.com/021b690012d55631e1ac6b8efe095aaa.jpg
07-23 22:23:40.588 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 为什么人总是倾向于相信自己愿意相信的？   ;questionUrl:http://www.zhihu.com/question/21126315
07-23 22:23:40.588 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8592590
07-23 22:23:43.800 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:那些「二三流人物」的一流故事   ;ThumbnailUrl:http://pic1.zhimg.com/a934e71c8f73cef24edc70284264ea8c.jpg
07-23 22:23:43.800 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 有哪些「二三流人物」的一流故事？   ;questionUrl:http://www.zhihu.com/question/35063131
07-23 22:23:43.800 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8596396
07-23 22:23:44.124 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:关于英文邮件，你会不会也被这个误区影响了很多年？   ;ThumbnailUrl:http://pic1.zhimg.com/d1ac517261cf429e4258d4cda7985f80.jpg
07-23 22:23:44.124 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 哪些神句拯救了你的英文邮件？   ;questionUrl:http://www.zhihu.com/question/34147404
07-23 22:23:44.124 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8596452
07-23 22:23:44.532 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:大误 · 这是一个非常经典的笑话   ;ThumbnailUrl:http://pic4.zhimg.com/d5707c5e54a5636e31591da3b6a23a4f.jpg
07-23 22:23:44.544 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 福尔摩斯和华生看星空的笑话，体现出文科生和理科生的思路有哪些区别？   ;questionUrl:null
07-23 22:23:44.548 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 大误 · 这是一个非常经典的笑话   ;questionUrl:http://www.zhihu.com/question/28303674
07-23 22:23:44.560 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8596217
07-23 22:23:44.740 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:婚姻状态和事业有关系吗？   ;ThumbnailUrl:http://pic3.zhimg.com/bec039593f8b69df527c29f962c15912.jpg
07-23 22:23:44.740 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 婚姻状态和事业有关系吗？   ;questionUrl:http://www.zhihu.com/question/21799733
07-23 22:23:44.744 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8594730
07-23 22:23:45.964 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:「有人问我，什么是海，我说不清，于是拿出了杉本博司的海景」   ;ThumbnailUrl:http://pic1.zhimg.com/b35d65ce3374fd7ab43494b2d1ca8d08.jpg
07-23 22:23:45.964 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 从日本摄影师杉本博司的系列作品中，如何理解摄影与哲学之间的关系？   ;questionUrl:http://www.zhihu.com/question/34839882
07-23 22:23:45.964 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8589106
07-23 22:23:49.172 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:没真正当过小孩的人，是没办法成为大人的   ;ThumbnailUrl:http://pic1.zhimg.com/aa013375822d3a71516f12998cb72b94.jpg
07-23 22:23:49.176 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 没真正当过小孩的人，是没办法成为大人的｜你了解自己的「家庭规则」吗？   ;questionUrl:http://zhuanlan.zhihu.com/p/21658058
07-23 22:23:49.180 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8594414
07-23 22:23:52.568 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:用这属于盛夏的植物，做出一碗滑溜溜、清亮亮的甜点   ;ThumbnailUrl:http://pic1.zhimg.com/5fd54255d9ca5f8a262082d7141f8c6c.jpg
07-23 22:23:52.568 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 【节气手帖】大暑：木莲豆腐   ;questionUrl:http://zhuanlan.zhihu.com/p/21423645
07-23 22:23:52.572 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8593178
07-23 22:23:53.260 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:10 亿美元，联合利华打算买一家用互联网思维卖剃须刀的公司   ;ThumbnailUrl:http://pic2.zhimg.com/ccb58ea9fa6533f1c0eaa68a0b70ef89.jpg
07-23 22:23:53.260 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 联合利华为什么要收购男性护理品定购服务商 Dollar Shave Club?   ;questionUrl:http://www.zhihu.com/question/48702865
07-23 22:23:53.272 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8594451
07-23 22:23:53.476 16051-3902/io.github.izzyleung.zhihudailypurify I/dingDailyNews: dailyTitle:国内即将上市的 HPV 疫苗安全吗？   ;ThumbnailUrl:http://pic2.zhimg.com/ebdd8ab410ffaf9f411e6069f3fb1671.jpg
07-23 22:23:53.476 16051-3902/io.github.izzyleung.zhihudailypurify I/dingQuestions: questionTitle: 国内即将上市的 HPV 疫苗安全吗？   ;questionUrl:http://zhuanlan.zhihu.com/p/21645566
07-23 22:23:53.488 16051-3902/io.github.izzyleung.zhihudailypurify I/dingurl: http://news-at.zhihu.com/api/4/news/8592991

 */