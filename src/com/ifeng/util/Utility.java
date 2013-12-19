package com.ifeng.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Random;

import org.apache.http.conn.util.InetAddressUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.telephony.CellLocation;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;

import com.ifeng.BaseApplicaion;
import com.ifeng.util.logging.Log;
import com.ifeng.util.net.ConnectManager;

/**
 * 提供一些常用工具方法.
 */
public final class Utility {
	/** Log TAG. */
	private static final String TAG = "Utility";
	/** log 开关。 */
	private static final boolean DEBUG = BaseApplicaion.DEBUG & true;

	/** 私有构造函数. */
	private Utility() {

	}

	/**
	 * 为当前应用添加桌面快捷方式<br>
	 * 其中需要进行相关权限配置 "com.android.launcher.permission.INSTALL_SHORTCUT"
	 * 
	 * @param cx
	 * @param classs
	 *            快捷方式启动的Activity
	 * @param shortCutTitle
	 *            快捷方式名称
	 * @param shortCutResId
	 *            快捷方式图标
	 */
	public static void addShortcut(Context cx,
			Class<? extends Activity> classs, String shortCutTitle,
			int shortCutResId) {
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		intent.setClass(cx, classs);
		intent.removeExtra("data");

		// create new
		Intent shortcut = new Intent(
				"com.android.launcher.action.INSTALL_SHORTCUT");
		shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent);
		// 快捷方式名称
		shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, shortCutTitle);
		// 不允许重复创建（不一定有效）
		shortcut.putExtra("duplicate", false);
		// 快捷方式的图标
		ShortcutIconResource iconResource = Intent.ShortcutIconResource
				.fromContext(cx, shortCutResId);
		shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);

		cx.sendBroadcast(shortcut);
	}

	/**
	 * 删除当前应用的桌面快捷方式。<br>
	 * 其中需要进行相关权限配置 "com.android.launcher.permission.UNINSTALL_SHORTCUT"
	 * 
	 * @param cx
	 */
	public static void deleteShortcut(Context cx) {
		Intent shortcut = new Intent(
				"com.android.launcher.action.UNINSTALL_SHORTCUT");

		// 获取当前应用名称
		String title = null;
		try {
			final PackageManager pm = cx.getPackageManager();
			title = pm.getApplicationLabel(
					pm.getApplicationInfo(cx.getPackageName(),
							PackageManager.GET_META_DATA)).toString();
		} catch (Exception e) {
		}
		// 快捷方式名称
		shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, title);
		Intent shortcutIntent = cx.getPackageManager()
				.getLaunchIntentForPackage(cx.getPackageName());
		shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
		cx.sendBroadcast(shortcut);
	}

	/**
	 * 判断桌面是否已添加快捷方式<br>
	 * 其中需要进行相关权限配置 "com.android.launcher.permission.READ_SETTINGS"
	 * 
	 * @param cx
	 * @return
	 */
	public static boolean hasShortcut(Context cx) {
		boolean result = false;
		// 获取当前应用名称
		String title = null;
		try {
			final PackageManager pm = cx.getPackageManager();
			title = pm.getApplicationLabel(
					pm.getApplicationInfo(cx.getPackageName(),
							PackageManager.GET_META_DATA)).toString();
		} catch (Exception e) {
		}

		final String uriStr;
		if (android.os.Build.VERSION.SDK_INT < 8) {
			uriStr = "content://com.android.launcher.settings/favorites?notify=true";
		} else {
			uriStr = "content://com.android.launcher2.settings/favorites?notify=true";
		}
		final Uri CONTENT_URI = Uri.parse(uriStr);
		final Cursor c = cx.getContentResolver().query(CONTENT_URI, null,
				"title=?", new String[] { title }, null);
		if (c != null && c.getCount() > 0) {
			result = true;
		}
		return result;
	}

	/**
	 * 计算上次更新时间的提示文字
	 */
	public static String countLastRefreshHintText(long time) {
		long dTime = System.currentTimeMillis() - time;
		// 15分钟
		if (dTime < 15 * 60 * 1000) {
			return "刚刚";
		} else if (dTime < 60 * 60 * 1000) {
			// 一小时
			return "一小时内";
		} else if (dTime < 24 * 60 * 60 * 1000) {
			return (int) (dTime / (60 * 60 * 1000)) + "小时前";
		} else {
			return DateFormat.format("MM-dd kk:mm", System.currentTimeMillis())
					.toString();
		}
	}

	/**
	 * 计算View的width以及height
	 * 
	 * @param child
	 */
	public static void measureView(View child) {
		ViewGroup.LayoutParams p = child.getLayoutParams();
		if (p == null) {
			p = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT);
		}
		int childWidthSpec = ViewGroup.getChildMeasureSpec(0, 0 + 0, p.width);
		int lpHeight = p.height;
		int childHeightSpec;
		if (lpHeight > 0) {
			childHeightSpec = MeasureSpec.makeMeasureSpec(lpHeight,
					MeasureSpec.EXACTLY);
		} else {
			childHeightSpec = MeasureSpec.makeMeasureSpec(0,
					MeasureSpec.UNSPECIFIED);
		}
		child.measure(childWidthSpec, childHeightSpec);
	}

	/**
	 * 获取当前小时分钟，24小时制
	 * 
	 * @return
	 */
	public static String getTime24Hours() {
		SimpleDateFormat formatter = new SimpleDateFormat("HH:mm", Locale.CHINA);
		Date curDate = new Date(System.currentTimeMillis());// 获取当前时间
		return formatter.format(curDate);
	}

	/**
	 * 获取电池电量,0~1
	 * 
	 * @param context
	 * @return
	 */
	public static float getBattery(Context context) {
		Intent batteryInfoIntent = context.getApplicationContext()
				.registerReceiver(null,
						new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		int status = batteryInfoIntent.getIntExtra("status", 0);
		int health = batteryInfoIntent.getIntExtra("health", 1);
		boolean present = batteryInfoIntent.getBooleanExtra("present", false);
		int level = batteryInfoIntent.getIntExtra("level", 0);
		int scale = batteryInfoIntent.getIntExtra("scale", 0);
		int plugged = batteryInfoIntent.getIntExtra("plugged", 0);
		int voltage = batteryInfoIntent.getIntExtra("voltage", 0);
		int temperature = batteryInfoIntent.getIntExtra("temperature", 0); // 温度的单位是10℃
		String technology = batteryInfoIntent.getStringExtra("technology");
		return ((float) level) / scale;
	}

	/**
	 * 获取手机名称
	 * 
	 * @return
	 */
	public static String getMobileName() {
		return android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL;
	}

	/**
	 * 获取application层级的metadata
	 * 
	 * @param context
	 * @param key
	 * @return
	 */
	public static String getApplicationMetaData(Context context, String key) {
		try {
			Object metaObj = context.getPackageManager().getApplicationInfo(
					context.getPackageName(), PackageManager.GET_META_DATA).metaData
					.get(key);
			if (metaObj instanceof String) {
				return metaObj.toString();
			} else if (metaObj instanceof Integer) {
				return ((Integer) metaObj).intValue() + "";
			} else if (metaObj instanceof Boolean) {
				return ((Boolean) metaObj).booleanValue() + "";
			}
		} catch (NameNotFoundException e) {
			if (DEBUG) {
				Log.w(TAG, e);
			}
		}
		return null;
	}

	/**
	 * 获取sdk版本号
	 * 
	 * @return
	 */
	public static String getSdkVersion() {
		return android.os.Build.VERSION.RELEASE;
	}

	/**
	 * 获取mac地址
	 * 
	 * @param context
	 * @return
	 */
	public static String getMacAddress(Context context) {
		String macAddress = null;

		try {
			WifiManager manager = (WifiManager) context
					.getSystemService(Context.WIFI_SERVICE);
			WifiInfo info = manager.getConnectionInfo();
			macAddress = info.getMacAddress();
			if (macAddress != null) {
				return macAddress;
			}
		} catch (Exception e) {
			if (DEBUG) {
				Log.w(TAG, e);
			}
		}
		try {
			Process pp = Runtime.getRuntime().exec(
					"cat /sys/class/net/wlan0/address ");
			InputStreamReader ir = new InputStreamReader(pp.getInputStream());
			LineNumberReader input = new LineNumberReader(ir);

			String str = "";
			for (; null != str;) {
				str = input.readLine();
				if (str != null) {
					macAddress = str.trim();// 去空格
					break;
				}
			}
		} catch (Exception ex) {
			if (DEBUG) {
				Log.w(TAG, ex);
			}
		}

		if (TextUtils.isEmpty(macAddress)) {
			macAddress = "00:00:00:00:00:00";
		}
		return macAddress;
	}

	/**
	 * 获取通知栏高度
	 * 
	 * @param context
	 * @return
	 */
	public static int getStatusBarHeight(Context context) {
		int x = 0, statusBarHeight = 0;
		try {
			Class<?> c = Class.forName("com.android.internal.R$dimen");
			Object obj = c.newInstance();
			Field field = c.getField("status_bar_height");
			x = Integer.parseInt(field.get(obj).toString());
			statusBarHeight = context.getResources().getDimensionPixelSize(x);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		return statusBarHeight;
	}

	/**
	 * 获取标题栏高度
	 * 
	 * @param context
	 * @return
	 */
	public static int getTitleBarHeight(Activity context) {
		int contentTop = context.getWindow()
				.findViewById(Window.ID_ANDROID_CONTENT).getTop();
		return contentTop - getStatusBarHeight(context);
	}

	/**
	 * 获取应用版本名称
	 * 
	 * @param context
	 * @return
	 */
	public static String getAppVersionName(Context context) {
		PackageManager packageManager = context.getPackageManager();
		PackageInfo packInfo = null;
		try {
			packInfo = packageManager.getPackageInfo(context.getPackageName(),
					0);
		} catch (NameNotFoundException e) {
			if (DEBUG) {
				Log.w(TAG, e);
			}
			return "";
		}

		return packInfo.versionName;
	}

	/**
	 * 获取应用版本号
	 * 
	 * @param context
	 * @return
	 */
	public static int getAppVersionCode(Context context) {
		PackageManager packageManager = context.getPackageManager();
		PackageInfo packInfo = null;
		try {
			packInfo = packageManager.getPackageInfo(context.getPackageName(),
					0);
		} catch (NameNotFoundException e) {
			if (DEBUG) {
				Log.w(TAG, e);
			}
			return 1;
		}

		return packInfo.versionCode;
	}

	/**
	 * 获取指定文件大小
	 * 
	 * @param f
	 * @return
	 */
	public static int getFileSize(File f) {
		int size = 0;
		try {
			File flist[] = f.listFiles();
			for (int i = 0; i < flist.length; i++) {
				if (flist[i].isDirectory()) {
					size = size + getFileSize(flist[i]);
				} else {
					size = (int) (size + flist[i].length());
				}
			}
		} catch (Exception e) {
			if (DEBUG) {
				Log.w(TAG, e);
			}
		}
		return size;
	}

	/**
	 * 转换文件大小格式，byte数-->123.41MB
	 * 
	 * @param size
	 * @return
	 */
	public static String convertFileSize(long size) {
		try {
			DecimalFormat df = new DecimalFormat("###.##");
			float f;
			if (size < 1024 * 1024) {
				f = (float) ((float) size / (float) 1024);
				return (df.format(Float.valueOf(f).doubleValue()) + "KB");
			} else {
				f = (float) ((float) size / (float) (1024 * 1024));
				return (df.format(Float.valueOf(f).doubleValue()) + "MB");
			}

		} catch (Exception e) {
			if (DEBUG) {
				Log.w(TAG, e);
			}
			return "";
		}
	}

	/**
	 * 获取屏幕宽度，px
	 * 
	 * @param context
	 * @return
	 */
	public static float getScreenWidth(Context context) {
		DisplayMetrics dm = context.getResources().getDisplayMetrics();
		return dm.widthPixels;
	}

	/**
	 * 获取屏幕高度，px
	 * 
	 * @param context
	 * @return
	 */
	public static float getScreenHeight(Context context) {
		DisplayMetrics dm = context.getResources().getDisplayMetrics();
		return dm.heightPixels;
	}

	/**
	 * 获取屏幕像素密度
	 * 
	 * @param context
	 * @return
	 */
	public static float getDensity(Context context) {
		DisplayMetrics dm = context.getResources().getDisplayMetrics();
		return dm.density;
	}

	/**
	 * 获取scaledDensity
	 * 
	 * @param context
	 * @return
	 */
	public static float getScaledDensity(Context context) {
		DisplayMetrics dm = context.getResources().getDisplayMetrics();
		return dm.scaledDensity;
	}

	/**
	 * Hides the input method.
	 * 
	 * @param context
	 *            context
	 * @param view
	 *            The currently focused view
	 */
	public static void hideInputMethod(Context context, View view) {
		if (context == null || view == null) {
			return;
		}

		InputMethodManager imm = (InputMethodManager) context
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null) {
			imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
		}
	}

	/**
	 * Show the input method.
	 * 
	 * @param context
	 *            context
	 * @param view
	 *            The currently focused view, which would like to receive soft
	 *            keyboard input
	 */
	public static void showInputMethod(Context context, View view) {
		if (context == null || view == null) {
			return;
		}

		InputMethodManager imm = (InputMethodManager) context
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null) {
			imm.showSoftInput(view, 0);
		}
	}

	/**
	 * 获取指定resource id的Uri.
	 * 
	 * @param packageContext
	 *            指定的包。
	 * @param res
	 *            资源ID
	 * @return 资源的Uri
	 */
	public static Uri getResourceUri(Context packageContext, int res) {
		try {
			Resources resources = packageContext.getResources();
			return getResourceUri(resources, packageContext.getPackageName(),
					res);
		} catch (Resources.NotFoundException e) {
			Log.e(TAG,
					"Resource not found: " + res + " in "
							+ packageContext.getPackageName());
			return null;
		}
	}

	/**
	 * 根据ApplicationInfo 获取Resource Uri.
	 * 
	 * @param context
	 *            context
	 * @param appInfo
	 *            ApplicationInfo
	 * @param res
	 *            资源ID
	 * @return 资源的Uri
	 */
	public static Uri getResourceUri(Context context, ApplicationInfo appInfo,
			int res) {
		try {
			Resources resources = context.getPackageManager()
					.getResourcesForApplication(appInfo);
			return getResourceUri(resources, appInfo.packageName, res);
		} catch (PackageManager.NameNotFoundException e) {
			Log.e(TAG, "Resources not found for " + appInfo.packageName);
			return null;
		} catch (Resources.NotFoundException e) {
			Log.e(TAG, "Resource not found: " + res + " in "
					+ appInfo.packageName);
			return null;
		}
	}

	/**
	 * 构建Resource Uri。
	 * 
	 * @param resources
	 *            应用关联的资源。
	 * @param appPkg
	 *            应用包名。
	 * @param res
	 *            资源包名
	 * @return 资源的Uri
	 * @throws Resources.NotFoundException
	 *             资源没找到的异常。
	 */
	private static Uri getResourceUri(Resources resources, String appPkg,
			int res) throws Resources.NotFoundException {
		String resPkg = resources.getResourcePackageName(res);
		String type = resources.getResourceTypeName(res);
		String name = resources.getResourceEntryName(res);
		return makeResourceUri(appPkg, resPkg, type, name);
	}

	/**
	 * 构建Resource Uri.
	 * 
	 * @param appPkg
	 *            应用包名。
	 * @param resPkg
	 *            资源包名
	 * @param type
	 *            资源类型
	 * @param name
	 *            资源名
	 * @return 资源的Uri
	 */
	private static Uri makeResourceUri(String appPkg, String resPkg,
			String type, String name) {
		// TODO 了解type种类。
		Uri.Builder uriBuilder = new Uri.Builder();
		uriBuilder.scheme(ContentResolver.SCHEME_ANDROID_RESOURCE);
		uriBuilder.encodedAuthority(appPkg);
		uriBuilder.appendEncodedPath(type);
		if (!appPkg.equals(resPkg)) {
			uriBuilder.appendEncodedPath(resPkg + ":" + name);
		} else {
			uriBuilder.appendEncodedPath(name);
		}
		return uriBuilder.build();
	}

	/**
	 * 检查当前是否有可用网络
	 * 
	 * @param context
	 *            Context
	 * @return true 表示有可用网络，false 表示无可用网络
	 */
	public static boolean isNetWorkEnabled(Context context) {

		ConnectivityManager manager = (ConnectivityManager) context
				.getApplicationContext().getSystemService(
						Context.CONNECTIVITY_SERVICE);

		if (manager == null) {
			return false;
		}

		for (NetworkInfo info : manager.getAllNetworkInfo()) {
			if (info != null && info.isConnected()) {
				return true;
			}
		}

		return false;
	}

	/**
	 * 判断当前网络类型是否是wifi
	 * 
	 * @param context
	 *            Context
	 * @return true 是wifi网络，false 非wifi网络
	 */
	public static boolean isWifiNetWork(Context context) {
		String networktype = "NotAvaliable";
		ConnectivityManager manager = (ConnectivityManager) context
				.getApplicationContext().getSystemService(
						Context.CONNECTIVITY_SERVICE);
		if (manager != null) {
			NetworkInfo networkinfo = manager.getActiveNetworkInfo();
			if (networkinfo != null && networkinfo.isAvailable()) {
				networktype = networkinfo.getTypeName().toLowerCase();
				if (networktype.equalsIgnoreCase("wifi")) {
					return true;
				}
			} else {
				// 无网环境
				return true;
			}
		} else {
			// 无网环境
			return true;
		}
		return false;
	}

	/**
	 * 获取当前的网络类型。
	 * 
	 * @param context
	 *            Context
	 * @return network-type wifi或具体apn
	 */
	public static String getCurrentNetWorkType(Context context) {
		ConnectManager cm = new ConnectManager(context);
		return cm.getNetType();
	}

	/**
	 * 取得网络类型，wifi 2G 3G
	 * 
	 * @param context
	 *            context
	 * @return WF 2G 3G 4G，或空 如果没网
	 */
	public static String getWifiOr2gOr3G(Context context) {
		String networkType = "";
		if (context != null) {
			ConnectivityManager cm = (ConnectivityManager) context
					.getApplicationContext().getSystemService(
							Context.CONNECTIVITY_SERVICE);
			NetworkInfo activeNetInfo = cm.getActiveNetworkInfo();
			if (activeNetInfo != null
					&& activeNetInfo.isConnectedOrConnecting()) { // 有网
				networkType = activeNetInfo.getTypeName().toLowerCase();
				if (networkType.equals("wifi")) {
					networkType = "WF";
				} else { // 移动网络
					// //如果使用移动网络，则取得apn
					// apn = activeNetInfo.getExtraInfo();
					// 将移动网络具体类型归一化到2G 3G 4G
					networkType = "2G"; // 默认是2G
					int subType = activeNetInfo.getSubtype();
					switch (subType) {
					case TelephonyManager.NETWORK_TYPE_1xRTT:
						networkType = "3G";
						break;
					case TelephonyManager.NETWORK_TYPE_CDMA: // IS95
						break;
					case TelephonyManager.NETWORK_TYPE_EDGE: // 2.75
						break;
					case TelephonyManager.NETWORK_TYPE_EVDO_0:
						networkType = "3G";
						break;
					case TelephonyManager.NETWORK_TYPE_EVDO_A:
						networkType = "3G";
						break;
					case TelephonyManager.NETWORK_TYPE_GPRS: // 2.5
						break;
					case TelephonyManager.NETWORK_TYPE_HSDPA: // 3.5
						networkType = "3G";
						break;
					case TelephonyManager.NETWORK_TYPE_HSPA: // 3.5
						networkType = "3G";
						break;
					case TelephonyManager.NETWORK_TYPE_HSUPA:
						networkType = "3G";
						break;
					case TelephonyManager.NETWORK_TYPE_UMTS:
						networkType = "3G";
						break;
					case TelephonyManager.NETWORK_TYPE_EHRPD:
						networkType = "3G";
						break; // ~ 1-2 Mbps
					case TelephonyManager.NETWORK_TYPE_EVDO_B:
						networkType = "3G";
						break; // ~ 5 Mbps
					case TelephonyManager.NETWORK_TYPE_HSPAP:
						networkType = "3G";
						break; // ~ 10-20 Mbps
					case TelephonyManager.NETWORK_TYPE_IDEN:
						break; // ~25 kbps
					case TelephonyManager.NETWORK_TYPE_LTE:
						networkType = "4G";
						break; // ~ 10+ Mbps
					default:
						break;
					}
				} // end 移动网络if
			} // end 有网的if
		}
		return networkType;
	}

	/**
	 * 获取设备的imei号
	 * 
	 * @param context
	 *            Context
	 * @return imei号
	 */
	public static String getDeviceId(Context context) {
		String deviceId = Constants.getDeviceID(context);
		if (deviceId != null && deviceId.length() > 0) {
			return deviceId;
		}

		try {
			TelephonyManager tm = (TelephonyManager) context
					.getSystemService(Context.TELEPHONY_SERVICE);
			if (tm != null) {
				deviceId = tm.getDeviceId();
				// 模拟器会返回 000000000000000
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (TextUtils.isEmpty(deviceId)) {
			// 如果imei好为空，使用随机数产生一个 15 位 deviceid，
			final int imeiLength = 15; // gsm imei 号长度 15
			final int ten = 10; // 产生10以内的随机数
			Random random = new Random();
			StringBuffer sb = new StringBuffer(imeiLength);
			for (int i = 0; i < imeiLength; i++) {
				int r = random.nextInt(ten);
				sb.append(r);
			}
			deviceId = sb.toString();
		} else {
			// 如果获取到 imei，对系统imei号进行反转
			StringBuffer sb = new StringBuffer(deviceId);
			deviceId = sb.reverse().toString();
		}
		Constants.setDeviceID(context, deviceId);
		return deviceId;
	}

	/**
	 * 是否安装了sdcard。
	 * 
	 * @return true表示有，false表示没有
	 */
	public static boolean haveSDCard() {
		if (android.os.Environment.getExternalStorageState().equals(
				android.os.Environment.MEDIA_MOUNTED)) {
			return true;
		}
		return false;
	}

	/**
	 * Get total size of the SD card.
	 * 
	 * @return 获取sd卡的大小
	 */
	public static long getSDCardTotalSize() {
		long total = 0;
		if (haveSDCard()) {
			File path = Environment.getExternalStorageDirectory();
			StatFs statfs = new StatFs(path.getPath());
			long blocSize = statfs.getBlockSize();
			long totalBlocks = statfs.getBlockCount();
			total = totalBlocks * blocSize;
		} else {
			total = -1;
		}
		return total;
	}

	/**
	 * Get available size of the SD card.
	 * 
	 * @return available size
	 */
	public static long getAvailableSize() {
		long available = 0;
		if (haveSDCard()) {
			File path = Environment.getExternalStorageDirectory();
			StatFs statfs = new StatFs(path.getPath());
			long blocSize = statfs.getBlockSize();
			long availaBlock = statfs.getAvailableBlocks();

			available = availaBlock * blocSize;
		} else {
			available = -1;
		}
		return available;
	}

	/**
	 * 是否已经是root用户
	 * 
	 * @param context
	 *            Context
	 * @return true表示是，false表示不是
	 */
	public static boolean isRooted(Context context) {
		// Object obj = invokeSystemPropertiesGetBooleanMethod();
		// if (obj != null &&
		// obj.toString().trim().toLowerCase().equals("false")) {
		// if (DEBUG) {
		// Log.d(TAG, "该设备有root权限。");
		// }
		// return true;
		// }
		// else {
		// File sufilebin = new File("/system/bin/su");
		// File sufilexbin = new File("/system/xbin/su");
		// if (sufilebin.exists() || sufilexbin.exists()) {
		// if (DEBUG) {
		// Log.d(TAG, "该设备有root权限。");
		// }
		// return true;
		// }
		// }
		File sufilebin = new File("/data/data/root");
		try {
			sufilebin.createNewFile();
			if (sufilebin.exists()) {
				sufilebin.delete();
			}
			if (DEBUG) {
				Log.d(TAG, "该设备有root权限。");
			}
			return true;
		} catch (IOException e) {
			if (AppUtils.getPacakgeInfo(context, "com.noshufou.android.su") != null
					|| AppUtils.getPacakgeInfo(context, "com.miui.uac") != null
					|| AppUtils.getPacakgeInfo(context, "eu.chainfire.supersu") != null
					|| AppUtils
							.getPacakgeInfo(context, "com.lbe.security.miui") != null) {
				if (DEBUG) {
					Log.d(TAG, "该设备有root权限。");
				}
				return true;
			}
			if (DEBUG) {
				Log.d(TAG, "该设备没有root权限。");
			}
			return false;
		}

	}

	/**
	 * 反射调用android.os.SystemProperties.getBoolean("ro.secure",false);
	 * ro.secure为1表示没有root权限，为0表示有root权限。
	 * 
	 * @return "true" 表示没有root,"false"表示有root
	 */
	@SuppressWarnings("unchecked")
	public static Object invokeSystemPropertiesGetBooleanMethod() {
		Method method = null;
		Object result = null;
		try {
			Class klass = Utility.class.getClassLoader().loadClass(
					"android.os.SystemProperties");
			Object instance = klass.newInstance();
			Class[] cls = new Class[2];
			cls[0] = String.class;
			cls[1] = boolean.class;
			method = klass.getMethod("getBoolean", cls);
			result = method.invoke(instance, "ro.secure", false);
		} catch (Exception e) {
			if (DEBUG) {
				Log.e(TAG, "error:" + e.getMessage());
			}
		}
		return result;
	}

	/**
	 * 获取汉字对应的拼音，如果是英文字母则直接返回原内容。
	 * 
	 * @param input
	 *            输入的内容
	 * @return 返回对应的拼音
	 */
	public static String getPinYin(String input) {
		StringBuilder sb = new StringBuilder();
		ArrayList<HanziToPinyin.Token> tokens = HanziToPinyin.getInstance()
				.get(input);

		if (tokens != null && tokens.size() > 0) {
			for (HanziToPinyin.Token token : tokens) {
				if (HanziToPinyin.Token.PINYIN == token.type) {
					sb.append(token.target);
				} else {
					sb.append(token.source);
				}
			}
		}

		return sb.toString().toLowerCase();
	}

	/**
	 * 去除字符串前后的空格，包括全角与半角
	 * 
	 * @param str
	 *            要去掉的空格的内容
	 * @return 去掉空格后的内容
	 */
	public static String trimAllSpace(String str) {
		if (str == null) {
			return str;
		}
		return str.replaceAll("^[\\s　]*|[\\s　]*$", "");
	}

	/**
	 * copy file. 注意，若目标文件已存在，此函数不会删除目标文件。
	 * 
	 * @param srcFile
	 *            srcFile
	 * @param destFile
	 *            destFile
	 * @return is copy success
	 */
	public static boolean copyFile(File srcFile, File destFile) {
		boolean result = false;
		try {
			InputStream in = new FileInputStream(srcFile);
			try {
				result = copyToFile(in, destFile);
			} finally {
				in.close();
			}
		} catch (IOException e) {
			result = false;
		}
		return result;
	}

	/**
	 * Copy data from a source stream to destFile. 注意，若目标文件已存在，此函数不会删除目标文件。
	 * 
	 * @param inputStream
	 *            data input
	 * @param destFile
	 *            destFile
	 * @return Return true if succeed, return false if failed.
	 */
	public static boolean copyToFile(InputStream inputStream, File destFile) {
		try {
			FileOutputStream out = new FileOutputStream(destFile);
			try {
				byte[] buffer = new byte[4096]; // SUPPRESS CHECKSTYLE
				int bytesRead;
				while ((bytesRead = inputStream.read(buffer)) >= 0) {
					out.write(buffer, 0, bytesRead);
				}
			} finally {
				out.flush();
				try {
					out.getFD().sync();
				} catch (IOException e) {
					e.printStackTrace();
				}
				out.close();
			}
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * 设置是否自动旋转屏幕。 若Activity原本设置了屏幕方向，则不改变其设置。
	 * 
	 * @param activity
	 *            Activity
	 * @param autoRotate
	 *            是否自动旋转
	 */
	public static void setActivityAutoRotateScreen(Activity activity,
			boolean autoRotate) {
		int currentOrientation = activity.getRequestedOrientation();
		if (autoRotate) {
			if (currentOrientation == ActivityInfo.SCREEN_ORIENTATION_NOSENSOR) {
				activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
			}
		} else {
			if (currentOrientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
				activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
			}
		}
	}

	/** 定义buffer的大小 */
	private static final int BUFFERSIZE = 1024;

	/**
	 * 接收网络数据流
	 * 
	 * @param is
	 *            读取网络数据的流
	 * @param isGzip
	 *            是否是gzip压缩数据
	 * @return 字符串类型数据
	 */
	public static String recieveData(InputStream is, boolean isGzip) {
		String s = null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		if (is == null) {
			if (DEBUG) {
				Log.d(TAG, "recieveData inputstream is null.");
			}
			return null;
		}
		try {
			// os = new BufferedOutputStream();
			byte[] buff = new byte[BUFFERSIZE];
			int readed = -1;
			while ((readed = is.read(buff)) != -1) {
				baos.write(buff, 0, readed);
			}
			byte[] result = null;
			if (isGzip) {
				result = AppUtils.unGZip(baos.toByteArray());
			} else {
				result = baos.toByteArray();
			}
			if (result == null) {
				return null;
			}
			s = new String(result, "utf-8");
			is.close();
			baos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (DEBUG) {
			Log.s(TAG, "服务器下发数据:" + s);
		}
		return s;
	}

	/** html 中 特殊字符集 */
	private static final HashMap<String, String> HTML_SPECIALCHARS_TABLE = new HashMap<String, String>();
	/**
	 * 静态初始化
	 */
	static {
		HTML_SPECIALCHARS_TABLE.put("&lt;", "<");
		HTML_SPECIALCHARS_TABLE.put("&gt;", ">");
		HTML_SPECIALCHARS_TABLE.put("&amp;", "&");
		HTML_SPECIALCHARS_TABLE.put("&quot;", "\"");
		HTML_SPECIALCHARS_TABLE.put("&#039;", "'");
	}

	/**
	 * 通过xml传递url时，如果其中包含特殊字符，需要替换。
	 * 
	 * @param htmlEncodedString
	 *            html encoded 字符串
	 * @return html decoded 字符串
	 */
	public static String htmlSpecialcharsDecode(String htmlEncodedString) {
		if (TextUtils.isEmpty(htmlEncodedString)) {
			return htmlEncodedString;
		}
		Collection<String> en = HTML_SPECIALCHARS_TABLE.keySet();
		Iterator<String> it = en.iterator();
		while (it.hasNext()) {
			String key = it.next();
			String val = HTML_SPECIALCHARS_TABLE.get(key);
			htmlEncodedString = htmlEncodedString.replaceAll(key, val);
		}
		return htmlEncodedString;
	}

	/**
	 * 判断当前设备是否有物理menu按键，api14以下都默认有，14以上根据系统属性来设置。
	 * 
	 * @param ctx
	 *            Context
	 * @return api14以下都默认有(3.x系列由于只有平板，所以默认没有)，14以上根据系统属性来设置:true=有，false=没有
	 */
	@SuppressLint("NewApi")
	public static boolean hasParmanentKey(Context ctx) {
		if (Build.VERSION.SDK_INT < 11) {// SUPPRESS CHECKSTYLE
			return true;
		} else if (Build.VERSION.SDK_INT >= 11 && Build.VERSION.SDK_INT < 14) {// SUPPRESS
																				// CHECKSTYLE
			return false;
		} else {
			return ViewConfiguration.get(ctx).hasPermanentMenuKey();
		}
	}

	/**
	 * 得到当前网络的dns服务地址
	 * 
	 * @param ctx
	 *            Context
	 * @return dns
	 */
	public static String getDNS(Context ctx) {
		if (isWifiNetWork(ctx)) {
			WifiManager wifi = (WifiManager) ctx
					.getSystemService(Context.WIFI_SERVICE);
			DhcpInfo info = wifi.getDhcpInfo();
			return intToInetAddress(info.dns1).getHostAddress();
		} else {
			return "";
		}
	}

	/**
	 * Convert a IPv4 address from an integer to an InetAddress.
	 * 
	 * @param hostAddress
	 *            an int corresponding to the IPv4 address in network byte order
	 * @return {@link InetAddress}
	 */
	public static InetAddress intToInetAddress(int hostAddress) {
		byte[] addressBytes = { (byte) (0xff & hostAddress), // SUPPRESS
																// CHECKSTYLE
				(byte) (0xff & (hostAddress >> 8)), // SUPPRESS CHECKSTYLE
				(byte) (0xff & (hostAddress >> 16)), // SUPPRESS CHECKSTYLE
				(byte) (0xff & (hostAddress >> 24)) }; // SUPPRESS CHECKSTYLE

		try {
			return InetAddress.getByAddress(addressBytes);
		} catch (UnknownHostException e) {
			throw new AssertionError();
		}
	}

	/**
	 * 获取CPU最大频率（单位KHZ） "/system/bin/cat" 命令行
	 * "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq" 存储最大频率的文件的路径
	 * 
	 * @return cpu的最大频率
	 **/
	public static int getMaxCpuFreq() {
		try {
			String result = "";
			ProcessBuilder cmd;

			String[] args = { "/system/bin/cat",
					"/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq" };
			cmd = new ProcessBuilder(args);
			Process process = cmd.start();
			InputStream in = process.getInputStream();
			byte[] re = new byte[24];// SUPPRESS CHECKSTYLE
			while (in.read(re) != -1) {
				result = result + new String(re);
			}
			in.close();

			result = result.trim();

			int infoFreq = getCpuInfo();

			if (TextUtils.isEmpty(result)) {
				return infoFreq;
			}
			if (DEBUG) {
				Log.d(TAG, "cpu result:" + result);
			}
			int maxFreq = Integer.parseInt(result);
			if (maxFreq >= infoFreq) {
				return maxFreq;
			}
			return infoFreq;
		} catch (Exception ex) {
			return 0;
		}
	}

	/**
	 * 获取CPU信息（单位KHZ） "/proc/cpuinfo"第二行存储的cpu额定频率
	 * 
	 * @return cpuinfo文件中存储的cpu频率
	 **/
	private static int getCpuInfo() {
		try {
			String str1 = "/proc/cpuinfo";
			String str2 = "";
			String cpuInfo = "";
			String[] arrayOfString;
			FileReader fr = new FileReader(str1);
			BufferedReader localBufferedReader = new BufferedReader(fr, 1024);// SUPPRESS
																				// CHECKSTYLE

			while ((str2 = localBufferedReader.readLine()) != null) {
				if (str2.indexOf("BogoMIPS") != -1) {
					arrayOfString = str2.split("\\s+");
					cpuInfo = arrayOfString[2];
					break;
				}
			}

			localBufferedReader.close();

			cpuInfo = cpuInfo.trim();
			if (DEBUG) {
				Log.d(TAG, "cpuInfo:" + cpuInfo);
			}
			return (int) (Float.parseFloat(cpuInfo) * 1000);// SUPPRESS
															// CHECKSTYLE
		} catch (Exception e) {
			return 0;
		}
	}

	/**
	 * 转换字符串MD5值
	 * 
	 * @param s
	 * @return
	 */
	public final static String getMD5(String s) {
		char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
				'A', 'B', 'C', 'D', 'E', 'F' };
		try {
			byte[] btInput = s.getBytes();
			// 获得MD5摘要算法的 MessageDigest 对象
			MessageDigest mdInst = MessageDigest.getInstance("MD5");
			// 使用指定的字节更新摘要
			mdInst.update(btInput);
			// 获得密文
			byte[] md = mdInst.digest();
			// 把密文转换成十六进制的字符串形式
			int j = md.length;
			char str[] = new char[j * 2];
			int k = 0;
			for (int i = 0; i < j; i++) {
				byte byte0 = md[i];
				str[k++] = hexDigits[byte0 >>> 4 & 0xf];
				str[k++] = hexDigits[byte0 & 0xf];
			}
			return new String(str);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 获取单个文件的MD5值！
	 * 
	 * @param file
	 *            要计算md5的文件
	 * @return 获取文件的md5值
	 */
	public static String getFileMD5(File file) {
		long starttime = System.currentTimeMillis();
		if (!file.isFile()) {
			return null;
		}
		FileInputStream in;
		try {
			in = new FileInputStream(file);
			FileChannel ch = in.getChannel();
			MappedByteBuffer byteBuffer;
			byteBuffer = ch
					.map(FileChannel.MapMode.READ_ONLY, 0, file.length());
			MessageDigest digest = MessageDigest.getInstance("MD5");
			digest.update(byteBuffer);
			// 获得密文
			byte[] md = digest.digest();
			// 把密文转换成十六进制的字符串形式
			int j = md.length;
			char str[] = new char[j * 2];
			int k = 0;
			char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8',
					'9', 'A', 'B', 'C', 'D', 'E', 'F' };
			for (int i = 0; i < j; i++) {
				byte byte0 = md[i];
				str[k++] = hexDigits[byte0 >>> 4 & 0xf];
				str[k++] = hexDigits[byte0 & 0xf];
			}
			if (DEBUG) {
				Log.d(TAG, "get md5 use time:"
						+ (System.currentTimeMillis() - starttime));
			}
			return new String(str);
		} catch (Exception e) {
			if (DEBUG) {
				Log.e(TAG, "error:" + e.getMessage());
			}
			return null;
		}

		// BigInteger bigInt = new BigInteger(1, digest.digest());
		// return bigInt.toString(16);
	}

	/**
	 * 获取签名信息
	 * 
	 * @param context
	 *            上下文
	 * @param pkgName
	 *            包名
	 * @return 签名String
	 */
	public static String getSign(Context context, String pkgName) {
		// 默认签名为空串
		String sign = "";

		try {
			PackageInfo info = context.getPackageManager().getPackageInfo(
					pkgName, PackageManager.GET_SIGNATURES);
			if ((info.signatures != null) && (info.signatures.length > 0)) {
				sign = info.signatures[0].toCharsString();
			}
		} catch (NameNotFoundException e) {
			if (DEBUG) {
				Log.e(TAG, "Get Sign Fail : " + pkgName);
			}
		}

		return sign;
	}

	/**
	 * 获取手机IP信息
	 * 
	 * @return info ip地址
	 */
	public static String getIpInfo() {
		String ipInfo = null;

		try {
			Enumeration<NetworkInterface> faces = NetworkInterface
					.getNetworkInterfaces();

			LOOP: while (faces.hasMoreElements()) {
				Enumeration<InetAddress> addresses = faces.nextElement()
						.getInetAddresses();

				while (addresses.hasMoreElements()) {
					InetAddress inetAddress = addresses.nextElement();

					if (!inetAddress.isLoopbackAddress()
							&& InetAddressUtils
									.isIPv4Address(ipInfo = inetAddress
											.getHostAddress())) {
						ipInfo = inetAddress.getHostAddress().toString();

						break LOOP;
					}
				}
			}

		} catch (Exception e) {
			if (DEBUG) {
				Log.e(TAG, "getIpInfo fail!" + e.toString());
			}
		}

		if (TextUtils.isEmpty(ipInfo)) {
			ipInfo = "";
		}

		return ipInfo;
	}

	/**
	 * 获取基站信息， gsm网络是cell id，cdma是base station id
	 * 
	 * @param ctx
	 *            Context
	 * @return info
	 */
	public static String getCellInfo(Context ctx) {
		String cellInfo = null;

		try {
			TelephonyManager teleMgr = (TelephonyManager) ctx
					.getSystemService(Context.TELEPHONY_SERVICE);
			CellLocation cellLocation = teleMgr.getCellLocation();

			if (cellLocation instanceof GsmCellLocation) {
				GsmCellLocation gsmCellLocation = (GsmCellLocation) cellLocation;
				cellInfo = Integer.toString(gsmCellLocation.getCid());

			} else if (cellLocation instanceof CdmaCellLocation) {
				CdmaCellLocation cdmaCellLocation = (CdmaCellLocation) cellLocation;
				cellInfo = Integer
						.toString(cdmaCellLocation.getBaseStationId());
			}

		} catch (Exception e) {
			if (DEBUG) {
				Log.e(TAG, "getCellInfo fail!" + e.toString());
			}
		}

		if (TextUtils.isEmpty(cellInfo)) {
			cellInfo = "";
		}

		return cellInfo;
	}

	/**
	 * 获取Wifi信息，mac地址
	 * 
	 * @param ctx
	 *            Context
	 * @return info
	 */
	public static String getWifiInfo(Context ctx) {
		String wifiInfo = null;

		try {
			WifiManager wifiMgr = (WifiManager) ctx
					.getSystemService(Context.WIFI_SERVICE);
			wifiInfo = wifiMgr.getConnectionInfo().getMacAddress();

		} catch (Exception e) {
			if (DEBUG) {
				Log.e(TAG, "getWifiInfo fail!" + e.toString());
			}
		}

		if (TextUtils.isEmpty(wifiInfo)) {
			wifiInfo = "";
		}

		return wifiInfo;
	}

	/**
	 * 获取IMSI信息
	 * 
	 * @param ctx
	 *            Context
	 * @return info
	 */
	public static String getImsiInfo(Context ctx) {
		String imsiInfo = null;

		try {
			TelephonyManager teleMgr = (TelephonyManager) ctx
					.getSystemService(Context.TELEPHONY_SERVICE);
			imsiInfo = teleMgr.getSubscriberId();

		} catch (Exception e) {
			if (DEBUG) {
				Log.e(TAG, "getImsiInfo fail!" + e.toString());
			}
		}

		if (TextUtils.isEmpty(imsiInfo)) {
			imsiInfo = "";
		}

		return imsiInfo;
	}

	/**
	 * 如果是下载在application内部目录中，则需要将文件变成全局可读写，否则无法被别的应用打开（比如APK文件无法被ApkInstaller安装
	 * )
	 * 
	 * @param context
	 *            ApplicationContext
	 * @param destFile
	 *            目标文件
	 */
	public static void processAPKInDataLocation(Context context, String destFile) {
		if (destFile.startsWith(context.getFilesDir().getAbsolutePath())) {
			String destFileName = new File(destFile).getName();
			try {
				OutputStream out = context.openFileOutput(destFileName,
						Context.MODE_WORLD_READABLE
								| Context.MODE_WORLD_WRITEABLE);
				out.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 判断是否是Gzip文件，gzip文件前4个字节是：1F8B0800
	 * 
	 * @param srcfile
	 *            指定文件
	 * @return true 是Gzip文件，false不是Gzip文件
	 */
	public static boolean isGzipFile(String srcfile) {
		File file = new File(srcfile);
		if (!file.exists()) {
			return false;
		}
		// 取出前4个字节进行判断
		byte[] filetype = new byte[4]; // SUPPRESS CHECKSTYLE
		FileInputStream fis;
		try {
			fis = new FileInputStream(file);
			fis.read(filetype);
			if ("1F8B0800".equalsIgnoreCase(bytesToHexString(filetype))) {
				return true;
			}
			return false;
		} catch (Exception e) {
			Log.e(TAG, "error:" + e.getMessage());
			return false;
		}
	}

	/**
	 * byte数组转换成16进制字符串
	 * 
	 * @param src
	 *            数据源
	 * @return byte转为16进制
	 */
	public static String bytesToHexString(byte[] src) {
		StringBuilder stringBuilder = new StringBuilder();
		if (src == null || src.length <= 0) {
			return null;
		}
		for (int i = 0; i < src.length; i++) {
			int v = src[i] & 0xFF; // SUPPRESS CHECKSTYLE
			String hv = Integer.toHexString(v);
			if (hv.length() < 2) {
				stringBuilder.append(0);
			}
			stringBuilder.append(hv);
		}
		return stringBuilder.toString();
	}

	/** 一小时的毫秒数 */
	private static long oneHour = 60 * 60 * 1000; // SUPPRESS CHECKSTYLE

	/**
	 * 获取设备上某个volume对应的存储路径
	 * 
	 * @param volume
	 *            存储介质
	 * @return 存储路径
	 */
	public static String getVolumePath(Object volume) {
		String result = "";
		Object o = Utility.invokeHideMethodForObject(volume, "getPath", null,
				null);
		if (o != null) {
			result = (String) o;
		}

		return result;
	}

	/**
	 * 获取设备上所有volume
	 * 
	 * @param context
	 *            ApplicationContext
	 * @return Volume数组
	 */
	public static Object[] getVolumeList(Context context) {
		StorageManager manager = (StorageManager) context
				.getSystemService(Context.STORAGE_SERVICE);
		Object[] result = null;
		Object o = Utility.invokeHideMethodForObject(manager, "getVolumeList",
				null, null);
		if (o != null) {
			result = (Object[]) o;
		}

		return result;
	}

	/**
	 * 获取设备上某个volume的状态
	 * 
	 * @param context
	 *            ApplicationContext
	 * @param volumePath
	 *            存储介质
	 * @return Volume状态
	 */
	public static String getVolumeState(Context context, String volumePath) {
		StorageManager manager = (StorageManager) context
				.getSystemService(Context.STORAGE_SERVICE);
		String result = "";
		Object o = Utility.invokeHideMethodForObject(manager, "getVolumeState",
				new Class[] { String.class }, new Object[] { volumePath });
		if (o != null) {
			result = (String) o;
		}

		return result;
	}

	/**
	 * 调用一个对象的隐藏方法。
	 * 
	 * @param obj
	 *            调用方法的对象.
	 * @param methodName
	 *            方法名。
	 * @param types
	 *            方法的参数类型。
	 * @param args
	 *            方法的参数。
	 * @return 隐藏方法调用的返回值。
	 */
	public static Object invokeHideMethodForObject(Object obj,
			String methodName, Class<?>[] types, Object[] args) {
		Object o = null;
		try {
			Method method = obj.getClass().getMethod(methodName, types);
			o = method.invoke(obj, args);
			if (DEBUG) {
				Log.d(TAG, "Method \"" + methodName + "\" invoked success!");
			}
		} catch (Exception e) {
			if (DEBUG) {
				Log.d(TAG,
						"Method \"" + methodName + "\" invoked failed: "
								+ e.getMessage());
			}
		}
		return o;
	}

	/**
	 * 判断多媒体数据库中数据是否为空.如果未Null,UNKNOWN 也认为为空
	 * 
	 * @param str
	 *            判断str是否为空
	 * @return true 为空，false 不为空
	 */
	public static boolean isEmpty(String str) {
		return TextUtils.isEmpty(str) || "NULL".equals(str.toUpperCase())
				|| "<UNKNOWN>".equals(str.toUpperCase());
	}

	/**
	 * 把时长转为 时间格式
	 * 
	 * @param seconds
	 *            .
	 * @return 将时长转换为时间
	 */
	public static String convertSecondsToDuration(long seconds) {
		long days = seconds
				/ (DateUtils.DAY_IN_MILLIS / DateUtils.SECOND_IN_MILLIS);
		seconds -= days
				* (DateUtils.DAY_IN_MILLIS / DateUtils.SECOND_IN_MILLIS);
		long hours = seconds
				/ (DateUtils.HOUR_IN_MILLIS / DateUtils.SECOND_IN_MILLIS);
		seconds -= hours
				* (DateUtils.HOUR_IN_MILLIS / DateUtils.SECOND_IN_MILLIS);
		long minutes = seconds
				/ (DateUtils.MINUTE_IN_MILLIS / DateUtils.SECOND_IN_MILLIS);
		seconds -= minutes
				* (DateUtils.MINUTE_IN_MILLIS / DateUtils.SECOND_IN_MILLIS);

		StringBuffer sb = new StringBuffer();
		// 小于一小时不显示小时数
		if (hours > 0) {
			if (hours < Constants.INTEGER_10) {
				sb.append("0");
			}
			sb.append(hours);
			sb.append(":");
		}

		if (minutes < Constants.INTEGER_10) {
			sb.append("0");
		}
		sb.append(minutes);
		sb.append(":");
		if (seconds < Constants.INTEGER_10) {
			sb.append("0");
		}
		sb.append(seconds);

		if (days > 0) {
			return "" + days + "d " + sb.toString();
		} else {
			return "" + sb.toString();
		}
	}

/**
	 * 检查一个应用是否可以移动 
	 * 由于此方法采用了反射和一些系统未公开属性，存在不安全性
	 * 以下是转用的ApplicationInfo的未公开属性。
	 * int FLAG_FORWARD_LOCK = 1'<<'29;
	 * 以下是PackageInfo的未公开属性
	 *  int INSTALL_LOCATION_PREFER_EXTERNAL = 2;
	 *  int INSTALL_LOCATION_AUTO = 0;
	 *  int INSTALL_LOCATION_UNSPECIFIED = -1;
	 * 以下是PackageHelper的未公开属性
	 *  PackageHelper.APP_INSTALL_EXTERNAL=2
	 *  还从系统设置数据库读取了默认应用安装位置
	 *  "default_install_location";
	 * @param context
	 *            Context
	 * @param info
	 *            ApplicationInfo
	 * @return true:可移动，false:不能移动
	 */
	public static boolean checkAppCanMove(Context context, ApplicationInfo info) {
		boolean canBe = false;
		try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				return canBe;
			}
			if ((info.flags & ApplicationInfo.FLAG_EXTERNAL_STORAGE) != 0) {
				canBe = true;
			} else {
				Field field = info.getClass().getDeclaredField(
						"installLocation");
				field.setAccessible(true);
				int infoInstallLocation = field.getInt(info);
				// FLAG_FORWARD_LOCK
				if ((info.flags & (1 << 29)) == 0// SUPPRESS CHECKSTYLE
						&& (info.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
					// INSTALL_LOCATION_PREFER_EXTERNAL or INSTALL_LOCATION_AUTO
					if (infoInstallLocation == 2 || infoInstallLocation == 0) {
						canBe = true;
						// INSTALL_LOCATION_UNSPECIFIED
					} else if (infoInstallLocation == -1) {
						int defInstallLocation = android.provider.Settings.System
								.getInt(context.getContentResolver(),
										"default_install_location", 0);
						// APP_INSTALL_EXTERNAL
						if (defInstallLocation == 2) {
							// For apps with no preference and the default value
							// set
							// to install on sdcard.
							canBe = true;
						}
					}
				}
			}
		} catch (Exception e) {
			if (DEBUG) {
				Log.e(TAG, "error:", e);
			}
		}
		return canBe;
	}

	/**
	 * 从输入流中获得字符串.
	 * 
	 * @param inputStream
	 *            {@link InputStream}
	 * @return 字符串
	 */
	public static String getStringFromInput(InputStream inputStream) {

		byte[] buf = getByteFromInputStream(inputStream);
		if (buf != null) {
			return new String(buf);
		}

		return null;
	}

	/**
	 * 从输入流中读出byte数组
	 * 
	 * @param inputStream
	 *            输入流
	 * @return byte[]
	 */
	public static byte[] getByteFromInputStream(InputStream inputStream) {
		if (inputStream == null) {
			return null;
		}

		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		byte[] buffer = new byte[1024]; // SUPPRESS CHECKSTYLE
		do {
			int len = 0;
			try {
				len = inputStream.read(buffer, 0, buffer.length);
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (len != -1) {
				bos.write(buffer, 0, len);
			} else {
				break;
			}
		} while (true);

		buffer = bos.toByteArray();
		try {
			bos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return buffer;
	}

	/**
	 * 读取文本文件内容
	 * 
	 * @param filePath
	 *            文件地址
	 * @return 文件内容
	 */
	public static String readStringFile(String filePath) {
		if (filePath != null) {
			return readStringFile(new File(filePath));
		}
		return null;
	}

	/**
	 * 读取文本文件内容
	 * 
	 * @param file
	 *            文件
	 * @return 文件内容
	 */
	public static String readStringFile(File file) {

		String content = null;
		if (file != null && file.exists()) {
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(file));
				StringBuffer strBuffer = new StringBuffer();
				String line = null;
				while ((line = reader.readLine()) != null) {
					strBuffer.append(line).append("\n");
				}
				content = strBuffer.toString();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

		return content;
	}

	/**
	 * 将文本保存到文件
	 * 
	 * @param filePath
	 *            文件地址
	 * @param content
	 *            内容
	 */
	public static void writeStringToFile(String filePath, String content) {
		if (filePath != null && content != null) {
			writeStringToFile(new File(filePath), content);
		}
	}

	/**
	 * 将文本保存到文件
	 * 
	 * @param file
	 *            文件
	 * @param content
	 *            内容
	 */
	public static void writeStringToFile(File file, String content) {
		PrintStream writer = null;
		try {
			writer = new PrintStream(file);
			writer.println(content);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (writer != null) {
				writer.close();
			}
		}
	}
}
