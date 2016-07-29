package me.bunao.www.zhihudailytest.support;

import android.content.Intent;
import android.content.pm.PackageManager;

import me.bunao.www.zhihudailytest.ui.ZhihuDailyApplication;

public final class Check {
    private Check() {

    }

    public static boolean isZhihuClientInstalled() {
        try {
            //判断是否安装了知乎客户端
            return preparePackageManager().getPackageInfo(Constants.Information.ZHIHU_PACKAGE_ID, PackageManager.GET_ACTIVITIES) != null;
        } catch (PackageManager.NameNotFoundException ignored) {
            return false;
        }
    }
    //检查intent指向的是否存在
    public static boolean isIntentSafe(Intent intent) {
        return preparePackageManager().queryIntentActivities(intent, 0).size() > 0;
    }

    private static PackageManager preparePackageManager() {
        return ZhihuDailyApplication.getInstance().getPackageManager();
    }
}
