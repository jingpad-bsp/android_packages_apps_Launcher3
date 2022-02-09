package com.android.launcher3;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.LauncherActivityInfo;
import android.graphics.drawable.Drawable;
import android.util.SparseArray;

import com.android.launcher3.util.ResourceBasedOverride;

public class IconProvider implements ResourceBasedOverride {

    public IconProvider() {
    }

    public static IconProvider newInstance(Context context) {
//        return Overrides.getObject(IconProvider.class, context, R.string.icon_provider_class);
        return new JingIconProvider(context);
    }

    public String getSystemStateForPackage(String systemState, String packageName) {
        return systemState;
    }

    /**
     * @param flattenDrawable true if the caller does not care about the specification of the
     *                        original icon as long as the flattened version looks the same.
     */
    public Drawable getIcon(LauncherActivityInfo info, int iconDpi, boolean flattenDrawable) {
        return info.getIcon(iconDpi);
    }

    private static class JingIconProvider extends IconProvider {
        private static SparseArray<ComponentName> pkgList = new SparseArray<>();

        static {
//            pkgList.put(R.drawable.jingic_ov_videoplayer, ComponentName.createRelative("com.android.gallery3d", "com.sprd.gallery3d.app.NewVideoActivity"));
            pkgList.put(R.drawable.jingic_ov_gallery, ComponentName.createRelative("com.android.gallery3d", ".v2.app.GalleryActivity2"));
//            pkgList.put(R.drawable.jingic_ov_browser, ComponentName.createRelative("org.chromium.chrome.stable", "com.google.android.apps.chrome.Main"));
            pkgList.put(R.drawable.jingic_ov_music, ComponentName.createRelative("com.android.music", ".MusicBrowserActivity"));
//            pkgList.put(R.drawable.jingic_ov_wang_yi_you_xiang, ComponentName.createRelative("com.netease.mobimail", ".activity.LaunchActivity"));
            pkgList.put(R.drawable.jingic_ov_zhang_yue_jing_xuan, ComponentName.createRelative("com.zhangyue.read.iReader", "com.chaozh.iReader.ui.activity.WelcomeActivity"));
            pkgList.put(R.drawable.jingic_ov_wei_xin, ComponentName.createRelative("com.tencent.mm", ".ui.LauncherUI"));
            pkgList.put(R.drawable.jingic_ov_jin_ri_tou_tiao, ComponentName.createRelative("com.ss.android.article.news", ".activity.MainActivity"));
//            pkgList.put(R.drawable.jingic_ov_yozo_office, ComponentName.createRelative("com.yozo.office", ".MainAppActivity"));
            pkgList.put(R.drawable.jingic_ov_teng_xun_shi_pin_hd, ComponentName.createRelative("com.tencent.qqlivepad", "com.tencent.qqlive.ona.activity.WelcomeActivity"));
//            pkgList.put(R.drawable.jingic_ov_ding_ding, ComponentName.createRelative("com.alibaba.android.rimet", ".biz.LaunchHomeActivity"));
//            pkgList.put(R.drawable.jingic_ov_tong_hua_shun, ComponentName.createRelative("com.hexin.plat.android", ".AndroidLogoActivity"));
//            pkgList.put(R.drawable.jingic_ov_xi_ma_la_ya, ComponentName.createRelative("com.ximalaya.ting.android", ".host.activity.MainActivity"));
//            pkgList.put(R.drawable.jingic_ov_bai_du_di_tu, ComponentName.createRelative("com.baidu.BaiduMap", "com.baidu.baidumaps.WelcomeScreen"));
//            pkgList.put(R.drawable.jingic_ov_bai_du_shu_ru_fa, ComponentName.createRelative("com.baidu.input", ".ImeAppMainActivity"));
//            pkgList.put(R.drawable.jingic_ov_zhi_fu_bao, ComponentName.createRelative("com.eg.android.AlipayGphone", ".AlipayLogin"));
//            pkgList.put(R.drawable.jingic_ov_dou_yin, ComponentName.createRelative("com.ss.android.ugc.aweme", ".splash.SplashActivity"));
//            pkgList.put(R.drawable.jingic_ov_hang_lv_zhong_heng, ComponentName.createRelative("com.umetrip.android.msky.app", ".module.startup.SplashActivity"));
            pkgList.put(R.drawable.jingic_ov_qq_mail, ComponentName.createRelative("com.tencent.androidqqmail", "com.tencent.qqmail.launcher.desktop.LauncherActivity"));
        }

        private Context context;

        public JingIconProvider(Context context) {
            this.context = context;
        }

        @Override
        public Drawable getIcon(LauncherActivityInfo info, int iconDpi, boolean flattenDrawable) {
            for (int i = 0; i < pkgList.size(); i++) {
                if (pkgList.valueAt(i).equals(info.getComponentName())) {
                    return context.getResources().getDrawableForDensity(pkgList.keyAt(i), iconDpi);
                }
            }
            return super.getIcon(info, iconDpi, flattenDrawable);
        }
    }

}
