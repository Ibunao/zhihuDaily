package me.bunao.www.zhihudailytest.observable;


import java.util.List;

import me.bunao.www.zhihudailytest.bean.DailyNews;
import me.bunao.www.zhihudailytest.support.Constants;
import rx.Observable;

import static me.bunao.www.zhihudailytest.observable.Helper.getHtml;
import static me.bunao.www.zhihudailytest.observable.Helper.toNewsListObservable;

public class NewsListFromAccelerateServerObservable {
    public static Observable<List<DailyNews>> ofDate(String date) {
        return toNewsListObservable(getHtml(Constants.Urls.ZHIHU_DAILY_PURIFY_BEFORE, date));
    }
}
/*

{

    "date": "20160726",
    "stories": [
        {
            "images": [
                "http://pic1.zhimg.com/2608bc24d18cd14bc6b47ba26af925c8.jpg"
            ],
            "type": 0,
            "id": 8611391,
            "ga_prefix": "072622",
            "title": "大事 · 在动物园被猛兽袭击"
        },
        {
            "images": [
                "http://pic2.zhimg.com/e357476c1ccdec57a1f6e122a4bbe64d.jpg"
            ],
            "type": 0,
            "id": 8607417,
            "ga_prefix": "072621",
            "title": "三部影片让他名声大噪，这部是其中之一"
        },
        {
            "title": "没有多少电气知识，也可以搞懂家居配电设计",
            "ga_prefix": "072620",
            "images": [
                "http://pic1.zhimg.com/8918990d954512ec244e5a5d063b2bc0.jpg"
            ],
            "multipic": true,
            "type": 0,
            "id": 8611266
        },
        {
            "title": "吃一口，再来一口，一套市中心房子没了",
            "ga_prefix": "072619",
            "images": [
                "http://pic2.zhimg.com/8e6e5fd635dff972b5dc89c17c41ff0d.jpg"
            ],
            "multipic": true,
            "type": 0,
            "id": 8553273
        },
        {
            "images": [
                "http://pic3.zhimg.com/719ba0b97b2cf6778b650cfd884da00a.jpg"
            ],
            "type": 0,
            "id": 8611865,
            "ga_prefix": "072618",
            "title": "三分钟搞明白：如何自己对付口臭？"
        },
        {
            "images": [
                "http://pic1.zhimg.com/6ecee5a8ac7419c453ff7a430bbfa750.jpg"
            ],
            "type": 0,
            "id": 8607340,
            "ga_prefix": "072617",
            "title": "知乎好问题 · 中国有哪些逆天的文物？"
        },
        {
            "images": [
                "http://pic4.zhimg.com/919c83480ff4430b094b0818c1c714eb.jpg"
            ],
            "type": 0,
            "id": 8610592,
            "ga_prefix": "072616",
            "title": "「摧毁地球的最可怕瘟疫之一」"
        },
        {
            "images": [
                "http://pic1.zhimg.com/1b955de34547ee91b8ddc292949037dc.jpg"
            ],
            "type": 0,
            "id": 8608621,
            "ga_prefix": "072615",
            "title": "一直以为大脑在指挥身体，但看完这个小实验，我也不确定了"
        },
        {
            "images": [
                "http://pic1.zhimg.com/6c5694b80b778f15becade276d7c161c.jpg"
            ],
            "type": 0,
            "id": 8611072,
            "ga_prefix": "072614",
            "title": "陪伴我们整个上网史的雅虎，被一家通信公司收购了"
        },
        {
            "images": [
                "http://pic2.zhimg.com/545298cb4aec1048d459e0d6c0fb7535.jpg"
            ],
            "type": 0,
            "id": 8610653,
            "ga_prefix": "072613",
            "title": "「21 世纪是生命科学的世纪」，所以读完博士做什么？"
        },
        {
            "images": [
                "http://pic2.zhimg.com/8bbbc381c71e50aab745ffdf1012df95.jpg"
            ],
            "type": 0,
            "id": 8609730,
            "ga_prefix": "072612",
            "title": "大误 · 这个故事 JianZhi 厉害"
        },
        {
            "images": [
                "http://pic1.zhimg.com/27ed208f8ebea94996890ee5c27e7ff8.jpg"
            ],
            "type": 0,
            "id": 8608529,
            "ga_prefix": "072611",
            "title": "说到「我」就用大写 I，怎么 he she you 都是小写？"
        },
        {
            "title": "科幻设定里那些极其复杂的建筑和机械，是怎么设计出来的（很多图）",
            "ga_prefix": "072610",
            "images": [
                "http://pic2.zhimg.com/b2e3a4d46ebd798ba60e5fb487794691.jpg"
            ],
            "multipic": true,
            "type": 0,
            "id": 8608321
        },
        {
            "images": [
                "http://pic4.zhimg.com/6de873dc3d415bfb856711c9935e857b.jpg"
            ],
            "type": 0,
            "id": 8607975,
            "ga_prefix": "072609",
            "title": "和父母一起住如何避免矛盾？试着把房间好好规划一下"
        },
        {
            "images": [
                "http://pic1.zhimg.com/8d08b1a179a5b660abf36f6d5924830c.jpg"
            ],
            "type": 0,
            "id": 8608704,
            "ga_prefix": "072608",
            "title": "为了认出那张脸，你知道大脑有多努力吗？"
        },
        {
            "images": [
                "http://pic3.zhimg.com/76897c6a25a2bdc469e6a16283af0d3e.jpg"
            ],
            "type": 0,
            "id": 8609664,
            "ga_prefix": "072607",
            "title": "「就花这一次」，所以更需要看这份婚纱摄影避坑指南"
        },
        {
            "images": [
                "http://pic2.zhimg.com/8b61f5236f55b26fb13d7efe8daa7ded.jpg"
            ],
            "type": 0,
            "id": 8608049,
            "ga_prefix": "072607",
            "title": "南京大学发现了一个重要天体的候选体，叫「夸克星」"
        },
        {
            "images": [
                "http://pic2.zhimg.com/0ee8d3497ec5e31c9f0e15609fc1b8ad.jpg"
            ],
            "type": 0,
            "id": 8606492,
            "ga_prefix": "072607",
            "title": "一个惊人的数字：去年爱尔兰的 GDP 增速是 26%"
        },
        {
            "images": [
                "http://pic4.zhimg.com/a72fa128978974e517f3361b11878be3.jpg"
            ],
            "type": 0,
            "id": 8609515,
            "ga_prefix": "072607",
            "title": "读读日报 24 小时热门 TOP 5 · 不要忘了老虎是老虎"
        },
        {
            "images": [
                "http://pic1.zhimg.com/677a8ada3e6b748b9cc9b664921efe80.jpg"
            ],
            "type": 0,
            "id": 8607322,
            "ga_prefix": "072606",
            "title": "瞎扯 · 如何正确地吐槽"
        }
    ]
}
 */