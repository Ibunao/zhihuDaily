package me.bunao.www.zhihudailytest.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.annimon.stream.Stream;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import me.bunao.www.zhihudailytest.R;
import me.bunao.www.zhihudailytest.bean.DailyNews;
import me.bunao.www.zhihudailytest.bean.Question;
import me.bunao.www.zhihudailytest.support.Check;
import me.bunao.www.zhihudailytest.support.Constants;
import me.bunao.www.zhihudailytest.ui.ZhihuDailyApplication;

//RecyclerView.Adapter必须用holder，并且对使用holder进行了改变
//通过onCreateViewHolder方法和onBindViewHolder方法来完成
//加载数据并添加点击事件
public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.CardViewHolder> {
    private List<DailyNews> newsList;

    //开源的图片加载框架
    //在ZhihuDailyApplication设置了一些配置
    private ImageLoader imageLoader = ImageLoader.getInstance();
    private DisplayImageOptions options = new DisplayImageOptions.Builder()
            .showImageOnLoading(R.drawable.noimage)//设置图片在下载期间显示的图片
            .showImageOnFail(R.drawable.noimage)//设置图片Uri为空或是错误的时候显示的
            .showImageForEmptyUri(R.drawable.lks_for_blank_url)//设置图片加载/解码过程中错误时候显示的图片
            .cacheInMemory(true)//设置下载的图片是否缓存在内存中
            .cacheOnDisk(true)//设置下载的图片是否缓存在SD卡中
            .considerExifParams(true)//是否考虑JPEG图像EXIF参数（旋转，翻转）
            .build();//构建完成

    //设置图片加载完成添加到imageview的动画
    private ImageLoadingListener animateFirstListener = new AnimateFirstDisplayListener();

    public NewsAdapter(List<DailyNews> newsList) {
        this.newsList = newsList;

        //RecyclerView中给每个item固定的id，解决添加或删除时全部刷新闪烁
        //重写getItemId方法给他一个不同位置的唯一标识，并且hasStableIds返回true的时候应该返回相同的数据集；
        //接下来我们复写getItemId()方法，并且设置setHasStableIds(true); （ps:在构造函数中设置）
        setHasStableIds(true);
    }

    public void setNewsList(List<DailyNews> newsList) {
        this.newsList = newsList;
    }

    public void updateNewsList(List<DailyNews> newsList) {
        setNewsList(newsList);
        //刷新数据
        notifyDataSetChanged();
    }

    //创建viewholder
    @Override
    public CardViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final Context context = parent.getContext();

        View itemView = LayoutInflater
                .from(context)
                .inflate(R.layout.news_list_item, parent, false);

        return new CardViewHolder(itemView, new CardViewHolder.ClickResponseListener() {
            //点击item
            @Override
            public void onWholeClick(int position) {
                //在浏览器中打开
                browse(context, position);
            }
            //点击分享
            @Override
            public void onOverflowClick(View v, final int position) {
                //创建popupmenu
                PopupMenu popup = new PopupMenu(context, v);
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.contextual_news_list, popup.getMenu());
                popup.setOnMenuItemClickListener(
                        new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                switch (item.getItemId()) {
                            case R.id.action_share_url:
                                    share(context, position);
                                    break;
                            }
                            return true;
                            }
                        }
                );
                popup.show();
            }
        });
    }
    //类似于adapter中的getView，给控件绑定值
    @Override
    public void onBindViewHolder(CardViewHolder holder, int position) {
        DailyNews dailyNews = newsList.get(position);
        //下载、添加图片
        imageLoader.displayImage(dailyNews.getThumbnailUrl(), holder.newsImage, options, animateFirstListener);

        if (dailyNews.getQuestions().size() > 1) {
            //包含多个问题时，比如    今日好问题
            holder.questionTitle.setText(dailyNews.getDailyTitle());
            holder.dailyTitle.setText(Constants.Strings.MULTIPLE_DISCUSSION);
        } else {
            holder.questionTitle.setText(dailyNews.getQuestions().get(0).getTitle());
            holder.dailyTitle.setText(dailyNews.getDailyTitle());
        }
    }

    @Override
    public int getItemCount() {
        return newsList == null ? 0 : newsList.size();
    }

    @Override
    public long getItemId(int position) {
        return newsList.get(position).hashCode();
    }

    private void browse(Context context, int position) {
        DailyNews dailyNews = newsList.get(position);
//        //判断内容是否大于1
//        if (dailyNews.hasMultipleQuestions()) {
//            AlertDialog dialog = createDialog(context,
//                    dailyNews,
//                    makeGoToZhihuDialogClickListener(context, dailyNews));
//            dialog.show();
//
//        } else {
            goToZhihu(context, dailyNews.getQuestions().get(0).getUrl());
//        }
    }
    //分享
    private void share(Context context, int position) {
        DailyNews dailyNews = newsList.get(position);
//
//        if (dailyNews.hasMultipleQuestions()) {
//            AlertDialog dialog = createDialog(context,
//                    dailyNews,
//                    makeShareQuestionDialogClickListener(context, dailyNews));
//            dialog.show();
//        } else {
            shareQuestion(context,
                    dailyNews.getQuestions().get(0).getTitle(),
                    dailyNews.getQuestions().get(0).getUrl());
//        }
    }

    private AlertDialog createDialog(Context context, DailyNews dailyNews, DialogInterface.OnClickListener listener) {
        String[] questionTitles = getQuestionTitlesAsStringArray(dailyNews);

        return null;
//                new AlertDialog.Builder(context)
//                .setTitle(dailyNews.getDailyTitle())
//                .setItems(questionTitles, listener)
//                .create();
    }
//
//    private DialogInterface.OnClickListener makeGoToZhihuDialogClickListener(Context context, DailyNews dailyNews) {
//        //->Lambda表达式,也可以写成下面的那个方法
//        return (dialog, which) -> {
//            String questionTitle = dailyNews.getQuestions().get(which).getTitle(),
//                    questionUrl = dailyNews.getQuestions().get(which).getUrl();
//
//            shareQuestion(context, questionTitle, questionUrl);
//        };
//    }
//
//    private DialogInterface.OnClickListener makeShareQuestionDialogClickListener(Context context, DailyNews dailyNews) {
//        return new DialogInterface.OnClickListener(){
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                String questionTitle = dailyNews.getQuestions().get(which).getTitle(),
//                    questionUrl = dailyNews.getQuestions().get(which).getUrl();
//
//            shareQuestion(context, questionTitle, questionUrl);
//            }
//        };
//    }

    private void goToZhihu(Context context, String url) {
        //判断配置信息中是否让在客户端中打开
        if (!ZhihuDailyApplication.getSharedPreferences()
                .getBoolean(Constants.SharedPreferencesKeys.KEY_SHOULD_USE_CLIENT, false)) {
            openUsingBrowser(context, url);
        }
        //如果安装有知乎客户端则在客户端中打开
        else if (Check.isZhihuClientInstalled()) {
            openUsingZhihuClient(context, url);
        }
    else {
            openUsingBrowser(context, url);
        }
    }
    //在浏览器中打开
    private void openUsingBrowser(Context context, String url) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));

        if (Check.isIntentSafe(browserIntent)) {
            context.startActivity(browserIntent);
        } else {
            Toast.makeText(context, context.getString(R.string.no_browser), Toast.LENGTH_SHORT).show();
        }
    }

    //在知乎客户端中打开
    private void openUsingZhihuClient(Context context, String url) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        browserIntent.setPackage(Constants.Information.ZHIHU_PACKAGE_ID);
        context.startActivity(browserIntent);
    }

    //分享文字功能
    private void shareQuestion(Context context, String questionTitle, String questionUrl) {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        //noinspection deprecation
        share.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        share.putExtra(Intent.EXTRA_TEXT,
                questionTitle + " " + questionUrl + Constants.Strings.SHARE_FROM_ZHIHU);
        context.startActivity(Intent.createChooser(share, context.getString(R.string.share_to)));
    }
    //获取title数组
    private String[] getQuestionTitlesAsStringArray(DailyNews dailyNews) {
        //::Lambda表达式,表示调用方法，Stream也是java8的新特性
        //String[]::new创建一个String[]
        //将dailyNews.getQuestions()获得的List<Question>进行遍历获取getTitle标题，转换为String[]数组
        return Stream.of(dailyNews.getQuestions()).map(Question::getTitle).toArray(String[]::new);
    }

    //holder优化
    public static class CardViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public ImageView newsImage;
        public TextView questionTitle;
        public TextView dailyTitle;
        public ImageView overflow;

        //定义的接口实现函数的回调
        private ClickResponseListener mClickResponseListener;
        public CardViewHolder(View v, ClickResponseListener clickResponseListener) {
            super(v);

            this.mClickResponseListener = clickResponseListener;

            newsImage = (ImageView) v.findViewById(R.id.thumbnail_image);
            questionTitle = (TextView) v.findViewById(R.id.question_title);
            dailyTitle = (TextView) v.findViewById(R.id.daily_title);
            overflow = (ImageView) v.findViewById(R.id.card_share_overflow);

            v.setOnClickListener(this);
            overflow.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (v == overflow) {
                //getAdapterPosition()方法获取item位置
                //点击分享小图片时触发
                mClickResponseListener.onOverflowClick(v, getAdapterPosition());
            } else {
                //真个item接收到的点击触发
                mClickResponseListener.onWholeClick(getAdapterPosition());
            }
        }
        //实现回调函数的接口
        public interface ClickResponseListener {
            void onWholeClick(int position);

            void onOverflowClick(View v, int position);
        }
    }
    //对图片加载进行监控
    private static class AnimateFirstDisplayListener extends SimpleImageLoadingListener {
        //在Java集合工具类Collections中，提供了一个Collections.synchronizedList方法，
        // 可以传入一个List对象，返回出一个SynchronizedList。
        //SynchronizedList只是提供了一个对List对象的封装，对List的每个操作都添加了synchronized修饰，
        //基本上与Vector一致，只是用法不同而已。比如现在已经有个LinkedList，如果想要一个线程安全的List，
        // 只需执行Collections.synchronized(linkedList)即可

        //ArrayList内部是数组实现的，LinkedList内部是链表实现的。
        //因此，当遇到读取比较多，插入、删除比较少的时候，推荐使用ArrayList，毕竟数组读取速度飞快，
        //插入删除速度需要移动大量元素；而当遇到插入删除比较多的时候，推荐使用LinkedList。
        static final List<String> displayedImages =Collections.synchronizedList(new LinkedList<String>());
        //图片下载完成时
        @Override
        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
            if (loadedImage != null) {
                ImageView imageView = (ImageView) view;
                boolean firstDisplay = !displayedImages.contains(imageUri);
                if (firstDisplay) {
                    //ImageView加载图片添加动画
                    FadeInBitmapDisplayer.animate(imageView, 500);
                    displayedImages.add(imageUri);
                }
            }
        }
    }
}
