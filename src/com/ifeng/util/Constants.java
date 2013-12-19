/**
 * Copyright (c) 2011 Baidu Inc.
 * 
 * @author 		Qingbiao Liu <liuqingbiao@baidu.com>
 * 
 * @date 2011-9-20
 */
package com.ifeng.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;

import com.ifeng.android.R;

/**
 * 记录系统相关常量及设置
 */
public final class Constants {
    /** 私有构造函数 */
    private Constants() {

    }
    
    /** 调试总开关，其他类 debug 开关采用 & 方式，方便总控制。 */
    public static final boolean DEBUG = false;
    /** 数字常量 10 */
    public static final int INTEGER_10 = 10;
    /** 内测版本的开关。 */
    public static final boolean ALPHA = false;
    /** Log TAG */
    private static final String TAG = Constants.class.getSimpleName();
    
    /** 开发者平台：APPID */
    public static final String APPID = "290472";
    /** 开发者平台：API Key */
    public static final String API_KEY = "fPfdaNnGi1sfLPyjSlFRioRr";
    /** 开发者平台：Secret Key */
    public static final String SECRET_KEY = "tOLtGzsx6CGguXmv6WjoQcSMjXZb2CWE";
    
    /** 系统设置 */
    public static final String SETTINGS_PREFERENCE = "settings_preference";
    /** 推送服务设置 */
    public static final String PUSHSERVICE_SETTINGS_PREFERENCE = "com.baidu.appsearch.push_settings";
    /** 最后一次检查更新的时间 */
    public static final String LAST_APPCHECK_TIME = "lasttimecheckeddate";
    /** 检查更新的时间 */
    public static final String APPCHECK_TIME = "timecheckeddate";
    /** userid */
    public static final String USER_ID = "user_id";
    /** user name */
    public static final String USER_NAME = "user_name";
    /** 设备的imei号 */
    public static final String IMEI = "imei";
    /** 一天24小时的毫秒数 */
    public static final long ONEDAY = DateUtils.DAY_IN_MILLIS;
    /** 设置云推送是否打开 */
    public static final String PUSH_SERVICE_SETTING = "push_on_off";
    /** push当前上线的bduss */
    public static final String PUSH_BDUSS = "push_bduss";
    /** pushguide当前版本 */
    public static final String PUSHGUIDE_VERSION = "pushguide_version";
    /** pushguide图片下载地址列表 */
    public static final String PUSHGUIDE_IMAGEURLS = "pushguide_imageurls";
    /** 显示图片是否打开 */
    public static final String SHOW_PICTURES_ENABLED = "show_pictures_enabled";
    /** 是否已经提示仅wifi下载 */
    public static final String WIFI_DOWNLOAD_INDICATED = "wifi_download_indicated";
    /** 是否已经进入过收藏列表 */
    public static final String VISITED_FAVORITES_LIST = "visit_favorite_list";
    /** 是否已经进入新功能提醒的主页提示 */
    public static final int VISITED_FAVORITES_LIST_MAIN = 1;
    /** 是否已经进入新功能提醒的应用管理页提示 */
    public static final int VISITED_FAVORITES_LIST_MYAPPS = 2;
    /** 是否已经进入过收藏列表新功能 */
    public static final int VISITED_FAVORITES_LIST_FAVORITES = 3;
    /** 是否已经判断过覆盖安装且记录了apk更新时间 */
    public static final String COVERE_INSTALL_KEY = "cover_install_key";
    /** 软件更新提示是否打开 */
    public static final String APP_UPDATABLE_NOTIFICATIONS = "apps_updatable_notifacations";
    /** 是否进入过应用管理中的更新下载页面key */
    public static final String IS_APP_UPDATE_DOWNLOAD_TAB_VISTED_KEY = "is_app_update_download_tab_visted";
    /** 是否自动安装下载完成的APK */
    public static final String AUTO_OPEN_INSTALL_APK = "auto_open_install_apk";
    /** 安装完成后是否删除APK的key */
    public static final String AUTO_DELETE_APK_AFTER_INSTALL = "auto_delete_apk_setting"; 
    /** 自动安装卸载的key */
    public static final String SILENT_INSTALL = "silent_install_setting"; 
    /** 自动安装卸载的key */
    public static final String SILENT_INSTALL_EVER_ENABLED = "silent_install_ever_enabled"; 
    /** 所有appSearch新建的线程的名字前缀 */
    public static final String APPSEARCH_THREAD_PRENAME = "appsearch_thread_";
    /** 最大的并行下载数，目前不支持用户设置 */
    public static final int MAX_CONCURRENT_DOWNLOAD_THREADS = 1;
    /** 记录页面的density，根据这个参数获取对应的页面，客户端请求页面时，应该加上此参数 */
    private static String mPSize = "-1";
    /** 记录是否已经创建快捷方式，以及是否已弹出云推送用户教育 */
    private static final String APP_SHOTCUT_CREATED = "real_app_shotcut_created";
    /** 记录是否已弹出跳至页首提示 */
    private static final String JUMP_TO_HEAD = "jump_to_head";
    /**
     * 记录是否已弹出云推送用户教育 不能修改，否则会引起已安装程序的用户覆盖安装的时候再次弹出用户教育
     * 2.0版本之前此处的字符串与名字不匹配，用于显示云推送教育 2.0开始字符串的内容修改，用于显示2.0改版后的教育，这样原来的逻辑可保持不变
     * */
    public static final String USER_EDUCATION_POPED = "user_education_poped"; 
    /** 记录客户端升级下载的安装包本地地址 */
    public static final String CLIENT_UPDATE_APK_PATH = "client_update_apk_path"; 
    /** 记录客户端升级下载的安装包对应的版本号 */
    public static final String CLIENT_UPDATE_APK_VCODE = "client_update_apk_vcode"; 
    /** 记录自动升级时忽略的版本名 */
    public static final String CLIENT_UPDATE_IGNORE_VCODE = "client_update_ignore_vcode";
    /** 记录自动升级时选择忽略的时间 */
    public static final String CLIENT_UPDATE_IGNORE_TIME = "client_update_ignore_time";
    /** 软件频率统计, 第一次启动统计service，随软件的打开而启动，后面随开机启动 */
    public static final String FREQ_STATISTIC_ENABLED = "freq_statistic_enabled";
    /** 软件频率统计开始的时间，每天总结的时候进行更新 */
    public static final String FREQ_STATISTIC_START_TIME = "freq_statistic_start_time";
    /** 软件频率统计数据库初始化 */
    public static final String FREQ_STATISTIC_DB_INITED = "freq_statistic_db_inited";
    /** 当前选择的排序方式 */
    public static final String CURRENT_SORTTYPE_SELECTION = "myapp_sorttype_current_selection";
    /** 当前图片查看的排序方式 */
    public static final String CURRENT_MEDIA_IMAGE_SORTTYPE_SELECTION = "media_image_sorttype_current_selection";
    /** 当前视频查看的排序方式 */
    public static final String CURRENT_MEDIA_VIDEO_SORTTYPE_SELECTION = "media_video_sorttype_current_selection";
    /** 当前音乐查看的排序方式 */
    public static final String CURRENT_MEDIA_AUDIO_SORTTYPE_SELECTION = "media_audio_sorttype_current_selection";
    /** 操作指南弹出 */
    public static final String OPERATION_GUIDE_POPED = "operation_guide_poped"; 
    /** 详情页引导弹出 */
    public static final String SHARE_AND_FAVORITE_GUIDE_POPED = "share_and_favorite_guide_poped";
    /** 是否显示过开启root的提醒 */
    public static final String REQUES_ROOTDIALOG_SHOWED = "Reques_RootDialog_Showed";
    /** 是否进行过高速下载 */
    public static final String IS_SPEED_DOWNLOAD_EXECED = "is_speed_download_execed";
    /** 高速下载apk名字正则表达式 */
    public static final String SPEED_DOWLOAD_APK_NAME_PATTERN = "^appsearch_[0-9]+_[0-9]+.apk$";
    /** 是否已安装应用的数据库表创建完毕 */
    public static final String IS_INSTALLED_APPS_DB_CREATED = "is_installed_apps_db_created";
    /** 是否应打开频度统计 */
    public static final String SHOULD_START_FREQSTATISTIC = "should_start_freqstatistic";
    /** 本机cpu的定义频率 */
    public static final String CPU_MAX_FREQ = "cpu_max_freq";
    /** CPU分界线 */
    public static final int CPU_FREQ = 900000; // 900M
    /** 静默下载是否完成的key */
    public static final String SILENT_UPDATE_END = "silent_update_end";
    /** 静默下载完成的数量 */
    public static final String SILENT_UPDATE_NUM = "silent_update_num";
    /** 静默下载完成的总大小 */
    public static final String SILENT_UPDATE_SIZE = "silent_update_size";
    /** 自动更新引导弹出的key */
    public static final String SILENT_UPDATE_GUIDE_POPED = "silent_update_guide_poped";
    /** 增量更新总共节约的流量大小的key */
    public static final String ALL_SMART_UPDATE_SAVED_SIZE = "all_smart_update_saved_size";
    /** 内测版本是否验证通过的key */
    public static final String IS_ALPHE_CHECK_OK = "is_alpha_check_ok";
    /** 应用是否安装过的key */
    public static final String IS_APPSEARCH_INSTALLED = "appsearch_is_installed";
    /** 静默下载是否提醒过 */
    public static boolean silentUpdateingIsNotified = false;
    
    /** 是否显示泛优化入口上的"new"字图标 */
    public static final String IS_SHOW_NEW_ON_PLUGIN_MENU = "is_show_new_on_plugins_menu";
    
    /** 是否进入过手机优化屏 */
    public static final String IS_PHONE_OPTIMIZE_USED = "is_phone_optimize_used";
    
    /** 是否直接启动安卓优化大师 */
    public static final String IS_LAUNCH_YOUHUADASHI_DIRECTLY = "is_launch_youhuadashi_directly";
    
    /** 当前客户端支持的Native 接口级别 */
    public static final String NATIVE_API_LEVEL = "1";
    
    /**
     * 设置设备的imei
     * 
     * @param ctx
     *            Context
     * @param imei
     *            设备的imei
     */
    public static void setDeviceID(Context ctx, String imei) {
        SharedPreferences preference = ctx.getSharedPreferences(Constants.SETTINGS_PREFERENCE, 0);
        Editor edit = preference.edit();
        edit.putString(Constants.IMEI, imei);
        edit.commit();
    }

    /**
     * 获取设备的imei
     * 
     * @param ctx
     *            Context
     * @return imei 如果没有则为空
     */
    public static String getDeviceID(Context ctx) {
        SharedPreferences preference = ctx.getSharedPreferences(Constants.SETTINGS_PREFERENCE, 0);
        return preference.getString(Constants.IMEI, "");
    }

    /**
     * 获取当前显示图片的设置状态
     * 
     * @param ctx
     *            Context
     * @return true 表示开启，false表示关闭
     */
    public static boolean isShowPicturesEnabled(Context ctx) {
        SharedPreferences preference = ctx.getSharedPreferences(Constants.SETTINGS_PREFERENCE, 0);
        boolean isshow = preference.getBoolean(Constants.SHOW_PICTURES_ENABLED, true);
        return isshow; // 默认为显示
    }

    /**
     * 设置是否显示图片
     * 
     * @param ctx
     *            Context
     * @param isEnabled
     *            true表示开启，false表示关闭
     */
    public static void setShowPicturesEnabled(Context ctx, boolean isEnabled) {
        SharedPreferences preference = ctx.getSharedPreferences(Constants.SETTINGS_PREFERENCE, 0);
        Editor edit = preference.edit();
        edit.putBoolean(Constants.SHOW_PICTURES_ENABLED, isEnabled);
        edit.commit();
    }
    
    /**
     * 设置当前的页面参数 <br/>
     * 方案： 客户端在各个页面加参数：psize=x 其中： x是的值分别是0、1、2、3
     * 
     * x值与图片的对应关系:
     * 
     * x值<br/>
     * 分辩率 图大小<br/>
     * 0 240X320 24 <br/>
     * 1 320X480 48 <br/>
     * 2 480X800 72 <br/>
     * 3 640X960 72
     * 
     * 说明： 在大图不存在的时侯，会自动传小一号的图。客户端要自动进行拉伸。
     * 
     * @param context
     *            Context
     */
    public static void setPSize(Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        if (density <= 0.75) { // SUPPRESS CHECKSTYLE : magic number
            mPSize = "0";
        } else if (density <= 1) {// SUPPRESS CHECKSTYLE : magic number
            mPSize = "1";
        } else if (density <= 1.5) {// SUPPRESS CHECKSTYLE : magic number
            mPSize = "2";
        } else {
            mPSize = "3";// SUPPRESS CHECKSTYLE : magic number
        }
    }

    /**
     * 获取当前的density对应的图片大小
     * 
     * @return 参看setPsize();
     */
    public static String getPSize() {
        return mPSize;
    }
    
    /**
     * 快捷方式是否已创建
     * 
     * @param ctx
     *            Context
     * @return Boolean
     */
    public static Boolean isShotcutCreated(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getBoolean(Constants.APP_SHOTCUT_CREATED, false);
    }
    
    /**
     * 记录快捷方式是否已创建
     * 
     * @param ctx
     *            Context
     * @param isCreated
     *            boolean
     */
    public static void setShotcutCreated(Context ctx, boolean isCreated) {
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(ctx);
        Editor edit = preference.edit();
        edit.putBoolean(Constants.APP_SHOTCUT_CREATED, isCreated);
        edit.commit();
    }
    
    /**
     * 是否已弹出跳至页首提示
     * 
     * @param ctx
     *            Context
     * @return Boolean
     */
    public static Boolean isJumpToHeadPoped(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getBoolean(Constants.JUMP_TO_HEAD, false);
    }
    
    /**
     * 记录跳至页首提示是否已弹出
     * 
     * @param ctx
     *            Context
     * @param isPoped
     *            boolean
     */
    public static void setJumpToHeadPoped(Context ctx, boolean isPoped) {
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(ctx);
        Editor edit = preference.edit();
        edit.putBoolean(Constants.JUMP_TO_HEAD, isPoped);
        edit.commit();
    }
    
    /**
     * 是否已弹出云推送用户教育
     * 
     * @param ctx
     *            Context
     * @return Boolean
     */
    public static Boolean isUserEducationPoped(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getBoolean(Constants.USER_EDUCATION_POPED, false);
    }
    
    /**
     * 记录云推送用户教育是否已弹出
     * 
     * @param ctx
     *            Context
     * @param isPoped
     *            boolean
     */
    public static void setUserEducationPoped(Context ctx, boolean isPoped) {
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(ctx);
        Editor edit = preference.edit();
        edit.putBoolean(Constants.USER_EDUCATION_POPED, isPoped);
        edit.commit();
    }
    
    /**
     * 是否已弹出操作指南
     * 
     * @param ctx
     *            Context
     * @return Boolean
     */
    public static Boolean isOperationGuidePoped(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getBoolean(Constants.OPERATION_GUIDE_POPED, false);
    }
    
    /**
     * 记录操作指南是否已弹出
     * 
     * @param ctx
     *            Context
     * @param isPoped
     *            boolean
     */
    public static void setOperationGuidePoped(Context ctx, boolean isPoped) {
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(ctx);
        Editor edit = preference.edit();
        edit.putBoolean(Constants.OPERATION_GUIDE_POPED, isPoped);
        edit.commit();
    }

    /**
     * 是否已弹出详情页引导
     * 
     * @param ctx
     *            Context
     * @return Boolean
     */
    public static Boolean isShareAndFavoriteGuidePoped(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getBoolean(Constants.SHARE_AND_FAVORITE_GUIDE_POPED, false);
    }

    /**
     * 记录详情页引导是否已弹出
     * 
     * @param ctx
     *            Context
     * @param isPoped
     *            boolean
     */
    public static void setShareAndFavoriteGuidePoped(Context ctx, boolean isPoped) {
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(ctx);
        Editor edit = preference.edit();
        edit.putBoolean(Constants.SHARE_AND_FAVORITE_GUIDE_POPED, isPoped);
        edit.commit();
    }
    
    /**
     * 设置是否已显示过root请求对话框
     * 
     * @param ctx
     *            Context
     * @param isShowed
     *            true显示过，false没有显示过
     */
    public static void setRequestedRootDialog(Context ctx, boolean isShowed) {
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(ctx);
        Editor edit = preference.edit();
        edit.putBoolean(Constants.REQUES_ROOTDIALOG_SHOWED, isShowed);
        edit.commit();
    }

    /**
     * 判断是否显示过root请求对话框
     * 
     * @param ctx
     *            Context
     * @return true显示过，false没有显示过
     */
    public static boolean isRequestedRootDialogShowed(Context ctx) {
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(ctx);
        return preference.getBoolean(REQUES_ROOTDIALOG_SHOWED, false);
    }

    /**
     * 设置是否已显示过root请求对话框
     * 
     * @param ctx
     *            Context
     * @param isShowed
     *            true显示过，false没有显示过
     */
    public static void setLocalInstalledAppsDBCreated(Context ctx, boolean isShowed) {
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(ctx);
        Editor edit = preference.edit();
        edit.putBoolean(Constants.IS_INSTALLED_APPS_DB_CREATED, isShowed);
        edit.commit();
    }
    
    /**
     * 判断是否显示过root请求对话框
     * 
     * @param ctx
     *            Context
     * @return true显示过，false没有显示过
     */
    public static boolean isLocalInstalledAppsDBCreated(Context ctx) {
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(ctx);
        return preference.getBoolean(IS_INSTALLED_APPS_DB_CREATED, false);
    }
//    /**
    // * 开启一个线程，从服务器获取统计相关的设置。
//     */
//    private static class GetSettingsWorker extends Thread {
//        /** connect url. */
//        private CharSequence mUrl;
//        /** Context */
//        private Context mContext = null;
//
//        /**
//         * constructor .
//         * 
//         * @param url
//         *            the connect url.
//         * @param context
//         *            Context
//         */
//        public GetSettingsWorker(CharSequence url, Context context) {
//            setName(Constants.APPSEARCH_THREAD_PRENAME + "GetSettingsWorker");
//            mUrl = url;
//            mContext = context;
//        }
//
//        @Override
//        public void run() {
    // // 设置该线程为优先级最低
//            Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);
//
//            ProxyHttpClient httpClient = new ProxyHttpClient(mContext);
//            try {
//                String url = mUrl.toString();
//
//                HttpGet method = new HttpGet(url);
//                HttpResponse response = httpClient.execute(method);
//
//                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
//                    String result = EntityUtils.toString(response.getEntity());
//                    if (DEBUG) {
    // Log.d(TAG, "从server获取的设置信息：" + result);
//                    }
    // if (!isInterrupted()) { // 联网线程Interrupt后不会停止，所以我们在联网结束后判断。
//                        parseSettings(mContext, result);
//                    }
//                } else {
//                    Log.d(TAG, "request failed  " + response.getStatusLine());
//                }
//
//            } catch (Exception e) {
//                Log.w(TAG, e);
//            } finally {
//                httpClient.close();
//            }
//        }
//    }
//
//    /**
    // * 解析server返回的设置，目前只有用户行为统计及下载统计设置。<br>
//     * {"v0.9":{"enableuestatistic":true,"enabledownloadstatistic":true},
//     * "v2.0":{"enableuestatistic" :true,"enabledownloadstatistic":true,
//     * "enablesettingsstatistic"
//     * :true,"enablefreqencystatistic":true,"location_refresh_time"
//     * :10,"location_life_time":30}}
//     * 
//     * @param context
//     *            Context
//     * @param returnString
    // * 返回字符串
//     */
//    private static void parseSettings(Context context, String returnString) {
//        // try {
//        // JSONObject jsonObject = new JSONObject(returnString);
    // // // 获取设置信息。
//        // JSONObject settings = (JSONObject) jsonObject.get("v"
//        // + BaiduIdentityManager.getInstance(
//        // context).getVersionName());
//        // HashMap<String, Integer> settingmap = new HashMap<String, Integer>();
//        // settingmap.put("enableuestatistic",
//        // settings.optBoolean("enableuestatistic") ? 1 : 0);// SUPPRESS
//        // CHECKSTYLE
//        // settingmap.put("enabledownloadstatistic",
//        // settings.optBoolean("enabledownloadstatistic") ? 1 : 0);// SUPPRESS
//        // CHECKSTYLE
//        // settingmap.put("enablesettingsstatistic",
//        // settings.optBoolean("enablesettingsstatistic") ? 1 : 0);// SUPPRESS
//        // CHECKSTYLE
//        // settingmap.put("enablefreqencystatistic",
//        // settings.optBoolean("enablefreqencystatistic") ? 1 : 0);// SUPPRESS
//        // CHECKSTYLE
//        // int temp = settings.optInt("location_refresh_time");
//        // if (temp > 0) {
//        // settingmap.put("location_refresh_time", temp);
//        // }
//        // temp = settings.optInt("location_life_time");
//        // if (temp > 0) {
//        // settingmap.put("location_life_time", temp);
//        // }
//        // StatisticConfigurations.setSettingsFromServer(context, settingmap);
//        //
//        // } catch (JSONException e) {
//        // Log.w(TAG, e);
//        // }
//    }

    /**
     * 获取当前设置中仅wifi下载是否生效
     * 
     * @param ctx
     *            Context
     * @return 仅wifi下载是否生效：true生效，false无效
     */
    public static boolean isWifiDownloadEnabled(Context ctx) {
        SharedPreferences preference = ctx.getSharedPreferences(
                Constants.SETTINGS_PREFERENCE, 0);
        return preference.getBoolean(ctx.getString(R.string.wifi_download_setting_key), false); // 默认为无效
    }
    
    /**
     * 设置仅wifi下载是否生效
     * 
     * @param ctx
     *            Context
     * 
     * @param enabled
     *            true表示生效，false表示未生效
     */
    public static void setWifiDownloadEnabled(Context ctx, boolean enabled) {
        SharedPreferences preference = ctx.getSharedPreferences(Constants.SETTINGS_PREFERENCE, 0);

        Editor edit = preference.edit();
        edit.putBoolean(ctx.getString(R.string.wifi_download_setting_key), enabled);
        edit.commit();
    }

    /**
     * 是否已经提示了仅wifi下载
     * 
     * @param ctx
     *            Context
     * @return true已经提示，false未提示
     */
    public static boolean isWifiDownloadIndicated(Context ctx) {
        SharedPreferences preference = ctx.getSharedPreferences(Constants.SETTINGS_PREFERENCE, 0);
        return preference.getBoolean(WIFI_DOWNLOAD_INDICATED, false); // 默认未提示
    }

    /**
     * 设置是否已经提示了仅wifi下载
     * 
     * @param ctx
     *            Context
     * 
     * @param isIndicated
     *            true已经提示，false未提示
     */
    public static void setWifiDownloadIndicated(Context ctx, boolean isIndicated) {
        SharedPreferences preference = ctx.getSharedPreferences(Constants.SETTINGS_PREFERENCE, 0);

        Editor edit = preference.edit();
        edit.putBoolean(WIFI_DOWNLOAD_INDICATED, isIndicated);
        edit.commit();
    }

    /**
     * 设置是否访问过我的收藏
     * 
     * @param ctx
     *            Context
     * @param visitedLevel
     *            是否访问过
     */
    public static void setFavoriteIsVisited(Context ctx, int visitedLevel) {
        if (visitedLevel < getIsVisitedFavorites(ctx)) {
            return;
        }
        SharedPreferences preference = ctx.getSharedPreferences(Constants.SETTINGS_PREFERENCE, 0);

        Editor edit = preference.edit();
        edit.putInt(VISITED_FAVORITES_LIST, visitedLevel);
        edit.commit();
    }

    /**
     * 获得是否访问过我的收藏
     * 
     * @param ctx
     *            Context
     * @return 1:主页提示点过；2：应用管理标签提示点过；3：进入过新功能收藏；0:新提示从没点过
     */
    public static int getIsVisitedFavorites(Context ctx) {
        SharedPreferences preference = ctx.getSharedPreferences(Constants.SETTINGS_PREFERENCE, 0);
        return preference.getInt(VISITED_FAVORITES_LIST, 0);
    }

    /**
     * 获取是否应该开启频度统计
     * 
     * @param ctx
     *            Context
     * @return -1表示未设置，1 表示开启，0表示关闭
     */
    public static int isShouldStartFreqStatistic(Context ctx) {
        SharedPreferences preference = ctx.getSharedPreferences(
                Constants.PUSHSERVICE_SETTINGS_PREFERENCE, Context.MODE_WORLD_READABLE);
        return preference.getInt(Constants.SHOULD_START_FREQSTATISTIC, -1);
    }

    /**
     * 设置是否应该开启频度统计
     * 
     * @param ctx
     *            Context
     * @param shouldStart
     *            -1表示未设置，1 表示开启，0表示关闭
     */
    public static void setShouldStartFreqStatistic(Context ctx, int shouldStart) {
        SharedPreferences preference = ctx.getSharedPreferences(
                Constants.PUSHSERVICE_SETTINGS_PREFERENCE, Context.MODE_WORLD_READABLE);
        Editor edit = preference.edit();
        edit.putInt(Constants.SHOULD_START_FREQSTATISTIC, shouldStart);
        edit.commit();
    }

    /**
     * 获取保存的cpu频率信息
     * 
     * @param ctx
     *            Context
     * @return cpu的频率，为-1表示没有存储对应信息
     */
    public static int getCPUFreq(Context ctx) {
        SharedPreferences preference = ctx.getSharedPreferences(Constants.SETTINGS_PREFERENCE,
                Context.MODE_WORLD_READABLE);
        return preference.getInt(Constants.CPU_MAX_FREQ, -1);
    }

    /**
     * 存储cpu信息
     * 
     * @param ctx
     *            Context
     * @param cpuFreq
     *            0表示未获取到，其他表示有效
     */
    public static void setCPUFreq(Context ctx, int cpuFreq) {
        SharedPreferences preference = ctx.getSharedPreferences(Constants.SETTINGS_PREFERENCE,
                Context.MODE_WORLD_READABLE);
        Editor edit = preference.edit();
        edit.putInt(Constants.CPU_MAX_FREQ, cpuFreq);
        edit.commit();
    }

    /**
     * 获得已经存储的总大小数据
     * 
     * @param ctx
     *            Context
     * @return 总数据大小
     */
    public static long getAllSmartUpdateSavedSize(Context ctx) {
        long size = 0;
        SharedPreferences preference = ctx.getSharedPreferences(Constants.SETTINGS_PREFERENCE,
                Context.MODE_WORLD_READABLE);
        size = preference.getLong(ALL_SMART_UPDATE_SAVED_SIZE, 0);
        return size;
    }

    /**
     * 记录每次增量更新的节省的数据大小
     * 
     * @param ctx
     *            Context
     * @param size
     *            节省的大小
     */
    public static void addSmartUpdateSize(Context ctx, long size) {
        SharedPreferences preference = ctx.getSharedPreferences(Constants.SETTINGS_PREFERENCE,
                Context.MODE_WORLD_READABLE);
        size = getAllSmartUpdateSavedSize(ctx) + size;
        Editor edit = preference.edit();
        edit.putLong(ALL_SMART_UPDATE_SAVED_SIZE, size);
        edit.commit();
    }

    /**
     * 判断是否显示过下载更新界面的loading进程
     * 
     * @param ctx
     *            Context
     * @return true:显示过，fasle:没有显示过
     */
    public static boolean isShowedUpdateFrameLoading(Context ctx) {
        SharedPreferences preference = ctx.getSharedPreferences(Constants.SETTINGS_PREFERENCE,
                Context.MODE_WORLD_READABLE);
        return preference.getBoolean("ShowedUpdateFrameLoading", false);
    }

    /**
     * 设置是否显示过更新下载界面的loading界面
     * 
     * @param ctx
     *            Context
     * @param showed
     *            true:显示了，false:没有显示
     */
    public static void setShowedUpdateFrameLoading(Context ctx, boolean showed) {
        SharedPreferences preference = ctx.getSharedPreferences(Constants.SETTINGS_PREFERENCE,
                Context.MODE_WORLD_READABLE);
        Editor edit = preference.edit();
        edit.putBoolean("ShowedUpdateFrameLoading", showed);
        edit.commit();
    }
}
