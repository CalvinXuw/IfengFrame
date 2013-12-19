/**
 * Copyright (c) 2011 Baidu Inc.
 * 
 * @author 		Qingbiao Liu <liuqingbiao@baidu.com>
 * 
 * @date 2011-9-7
 */
package com.ifeng.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipFile;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.widget.Toast;

import com.ifeng.android.R;
import com.ifeng.util.logging.Log;

/**
 * 公共方法的集合
 */

public final class AppUtils {
	/** log tag. */
	private static final String TAG = AppUtils.class.getSimpleName();

	/** if enabled, logcat will output the log. */
	private static final boolean DEBUG = true & Constants.DEBUG;
	/** 1024,用于计算app大小 */
	public static final int NUM_1024 = 1024;
	/** 16进制数组 */
	static final char[] HEXCHAR = { '0', '1', '2', '3', '4', '5', '6', '7',
			'8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
	/** 通知栏显示的可更新软件名字的数目 */
	public static final int NOTIFICATION_APP_NUM = 3;
	/**
	 * 请求root权限的线程名。
	 */
	public static final String REQUEST_ROOT_THREAD_NAME = Constants.APPSEARCH_THREAD_PRENAME
			+ "RequestRootThread";

	/**
	 * 构造函数
	 */
	private AppUtils() {

	}

	/**
	 * 设置最新版本号的字体颜色：如1.2(2.1),2.1会显示为红色。
	 * 
	 * @param content
	 *            传入的内容
	 * @return 改变字体颜色后的内容
	 */
	public static SpannableStringBuilder getSpannableText(String content) {
		// TODO 优化
		int left = content.indexOf("(") + 1;
		int right = content.indexOf(")");
		SpannableStringBuilder ss = new SpannableStringBuilder(content);

		if (left < 0 || right < 0) {
			return ss;
		}
		ss.setSpan(new ForegroundColorSpan(Color.RED), left, right,
				Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		return ss;
	}

	/**
	 * 获取所有的已安装的PackageInfo
	 * 
	 * @param context
	 *            Context
	 * @return 所有PackageInfo
	 */
	public static List<PackageInfo> getInstalledPackages(Context context) {
		long start = System.currentTimeMillis();
		List<PackageInfo> installed = context.getPackageManager()
				.getInstalledPackages(0);
		final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
		mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		List<ResolveInfo> installedactivity = context.getPackageManager()
				.queryIntentActivities(mainIntent, 0);
		HashMap<String, String> launcherapps = new HashMap<String, String>();
		for (ResolveInfo ri : installedactivity) {
			launcherapps.put(ri.activityInfo.packageName,
					ri.activityInfo.packageName);
		}

		ArrayList<PackageInfo> appList = new ArrayList<PackageInfo>();
		for (PackageInfo pi : installed) {
			boolean flag = true;

			if ((pi.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM) {
				if (launcherapps.containsKey(pi.packageName)) {
					flag = true;
				} else {
					// 系统内置应用，如果不包含在launcher中，则不加入列表
					continue;
				}
			}
			// else {
			// if (DEBUG) {
			// Log.d(TAG, "不是系统应用:" + pi.packageName);
			// }
			// // 如果不是系统内置应用，则加入列表
			// }
			// 不加入客户端本身
			if (pi.packageName.equalsIgnoreCase(context.getPackageName())) {
				flag = false;
			}
			if (flag) {
				appList.add(pi);
			}
		}
		if (DEBUG) {
			Log.d(TAG, "加载所有应用花费时间:" + (System.currentTimeMillis() - start));
		}
		return appList;
	}

	/**
	 * 获取packageName 关联的PacakgeInfo
	 * 
	 * @param context
	 *            Context
	 * @param packageName
	 *            应用包名
	 * @return PackageInfo
	 */
	public static PackageInfo getPacakgeInfo(Context context, String packageName) {
		PackageInfo pi;
		try {
			pi = context.getPackageManager().getPackageInfo(packageName,
					PackageManager.GET_SIGNATURES);
			return pi;
		} catch (NameNotFoundException e) {
			if (DEBUG) {
				Log.w(TAG, "error:" + e.getMessage());
			}
			return null;
		}
	}

	/**
	 * 获取packageName 关联的Pacakge最后更新时间
	 * 
	 * @param context
	 *            Context
	 * @param packageName
	 *            应用包名
	 * @return PackageInfo
	 */
	public static long getPacakgeLastUpdateTime(Context context,
			String packageName) {
		long lastUpdateTime = 0;
		try {
			PackageInfo info = AppUtils.getPacakgeInfo(context,
					context.getPackageName());
			if (info == null || info.applicationInfo == null) {
				return -1;
			}
			String dir = info.applicationInfo.publicSourceDir;
			lastUpdateTime = new File(dir).lastModified();
		} catch (Exception e) {
			if (DEBUG) {
				Log.w(TAG, e.getMessage());
			}
			return 0;
		}
		return lastUpdateTime;
	}

	/**
	 * 根据md5生成signmd5,取出第8到24位之间的内容，对前8位，后8位分别计算，并将和相加取对应的Int值(数字不能超过32位)
	 * 
	 * @param md5
	 *            要处理的md5值
	 * @return 生成的signmd5
	 */
	public static String creatSignInt(String md5) {
		if (md5 == null || md5.length() < 32) {// SUPPRESS CHECKSTYLE
			if (DEBUG) {
				Log.d(TAG, "md5值异常:" + md5);
			}
			return "-1";
		}
		String sign = md5.substring(8, 8 + 16);// SUPPRESS CHECKSTYLE
		long id1 = 0;
		long id2 = 0;
		String s = "";
		for (int i = 0; i < 8; i++) {// SUPPRESS CHECKSTYLE
			id2 *= 16;// SUPPRESS CHECKSTYLE
			s = sign.substring(i, i + 1);// SUPPRESS CHECKSTYLE
			id2 += Integer.parseInt(s, 16); // SUPPRESS CHECKSTYLE
		}

		for (int i = 8; i < sign.length(); i++) {// SUPPRESS CHECKSTYLE
			id1 *= 16;// SUPPRESS CHECKSTYLE
			s = sign.substring(i, i + 1);
			id1 += Integer.parseInt(s, 16); // SUPPRESS CHECKSTYLE
		}
		long id = (id1 + id2) & 0xFFFFFFFFL;// SUPPRESS CHECKSTYLE

		return String.valueOf(id);
	}

	/**
	 * 卸载应用
	 * 
	 * @param ctx
	 *            Cotext
	 * @param packageName
	 *            应用package Name
	 */
	public static void uninstallApk(Context ctx, String packageName) {
		Uri packageURI = Uri.parse("package:" + packageName);
		Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
		uninstallIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		ctx.startActivity(uninstallIntent);
	}

	/**
	 * 支持自动安装的安装方法，为方便修改将其从原方法中抽出，增加AppItem参数。 预计在所有修改完成后，替换原来的installApk方法。
	 * 
	 * @param ctx
	 *            context
	 * @param filepath
	 *            filepath
	 * @param item
	 *            AppItem
	 */
	public static void installApk(Context ctx, String filepath) {
		if (filepath == null) {
			return;
		}

		try {
			Uri uri = Uri.fromFile(new File(filepath));
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.setDataAndType(uri,
					"application/vnd.android.package-archive");
			ctx.startActivity(intent);
		} catch (Exception e) {
			if (DEBUG) {
				Log.e(TAG, e);
			}
		}
	}

	/**
	 * 检查一个APK文件是否是可用的APK。
	 * 
	 * @param path
	 *            apk file path
	 * @param context
	 *            context
	 * @return true文件有效，false文件无效
	 */
	public static boolean isAPKFileValid(String path, Context context) {
		PackageManager pm = context.getPackageManager();
		PackageInfo pi = pm.getPackageArchiveInfo(path, 0);

		return pi != null;
	}

	/**
	 * 应用是否运行在2.1系统里。
	 * 
	 * @return true 是。
	 */
	public static boolean isRunningInEclair() {
		final int eclairVersionCode = 7;
		return Build.VERSION.SDK_INT == eclairVersionCode;
	}

	/**
	 * 创建安装用的临时文件 仅用于2.1版本。
	 * 
	 * @param context
	 *            context
	 * @param apkFileOnSDCard
	 *            sd卡上的apk文件
	 * @return 临时文件
	 */
	public static File createTempPackageFile(Context context,
			File apkFileOnSDCard) {
		if (apkFileOnSDCard == null) {
			return null;
		}
		String fileName = apkFileOnSDCard.getName();
		File tmpPackageFile = context.getFileStreamPath(fileName);
		if (tmpPackageFile == null) {
			Log.w(TAG, "Failed to create temp file");
			return null;
		}
		if (tmpPackageFile.exists()) {
			tmpPackageFile.delete();
		}
		// Open file to make it world readable
		FileOutputStream fos;
		try {
			fos = context.openFileOutput(fileName, Context.MODE_WORLD_READABLE);
		} catch (FileNotFoundException e1) {
			Log.e(TAG, "Error opening file " + fileName);
			return null;
		}
		try {
			fos.close();
		} catch (IOException e) {
			Log.e(TAG, "Error opening file " + fileName);
			return null;
		}

		if (!Utility.copyFile(apkFileOnSDCard, tmpPackageFile)) {
			Log.w(TAG, "Failed to make copy of file: " + apkFileOnSDCard);
			return null;
		}

		return tmpPackageFile;
	}

	/**
	 * 删除临时安装文件，只在Android2.1上有此机制。
	 * 
	 * @param context
	 *            context
	 * @param pathOnSDCard
	 *            原文件在sd卡上的路径，用于获取文件名。
	 */
	public static void deleteTempPackageFile(Context context,
			String pathOnSDCard) {
		File file = new File(pathOnSDCard);
		File tmpPackageFile = context.getFileStreamPath(file.getName());
		if (tmpPackageFile == null) {
			return;
		}
		if (tmpPackageFile.exists()) {
			tmpPackageFile.delete();
		}
	}

	/**
	 * 清空临时安装文件，只在Android2.1上有此机制。
	 * 
	 * @param context
	 *            context
	 * 
	 */
	public static void clearTempPackageFile(Context context) {
		File filesDir = context.getFilesDir();
		if (filesDir == null) {
			return;
		}
		File[] files = filesDir.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String filename) {
				if (filename.endsWith(".apk")) {
					return true;
				}
				return false;
			}
		});
		if (files != null && files.length > 0) {
			for (File apkFile : files) {
				apkFile.delete();
			}
		}
	}

	/**
	 * 写文件到手机内存，用于存储下载时的icon
	 * 
	 * @param context
	 *            context
	 * @param filename
	 *            文件名字
	 * @param is
	 *            InputStream
	 */
	public static void writeFile(Context context, String filename,
			InputStream is) {
		FileOutputStream os = null;
		if (is == null) {
			return;
		}
		try {
			os = context.openFileOutput(filename, Context.MODE_PRIVATE);
			byte[] buff = new byte[NUM_1024];
			while (true) {
				int readed = is.read(buff);
				if (readed == -1) {
					break;
				}
				byte[] temp = new byte[readed];
				System.arraycopy(buff, 0, temp, 0, readed);
				os.write(temp);
			}
			is.close();
			os.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * 删除手机内存文件，主要是icon
	 * 
	 * @param context
	 *            Context
	 * @param filename
	 *            要删除的文件名字
	 */
	public static void deleteFile(Context context, String filename) {
		context.deleteFile(filename);
	}

	/**
	 * 压缩数据为gzip
	 * 
	 * @param data
	 *            要压缩的数据
	 * @return 返回压缩过的数据
	 */
	public static byte[] gZip(byte[] data) {
		byte[] b = null;
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			GZIPOutputStream gzip = new GZIPOutputStream(bos);
			gzip.write(data);
			gzip.finish();
			gzip.close();
			b = bos.toByteArray();
			bos.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return b;
	}

	/***
	 * 解压GZip
	 * 
	 * @param data
	 *            要解压的数据
	 * @return 解压过的数据
	 */
	public static byte[] unGZip(byte[] data) {

		if (data == null) {
			if (DEBUG) {
				Log.d(TAG, "unGZip data:" + null);
			}
			return null;
		}
		byte[] b = null;
		try {
			ByteArrayInputStream bis = new ByteArrayInputStream(data);
			GZIPInputStream gzip = new GZIPInputStream(bis);
			byte[] buf = new byte[NUM_1024];
			int num = -1;
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			while ((num = gzip.read(buf, 0, buf.length)) != -1) {
				baos.write(buf, 0, num);
			}
			b = baos.toByteArray();
			baos.flush();
			baos.close();
			gzip.close();
			bis.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return b;
	}

	/**
	 * 获取指定内容的md5值，16进制显示
	 * 
	 * @param plainText
	 *            要提取md5的字符串
	 * @return md5值
	 */
	public static String getMD5(byte[] plainText) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(plainText);
			byte[] b = md.digest();

			return toHexString(b);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 将字节数组转换为16进制字符串
	 * 
	 * @param byt
	 *            要转换的字节
	 * @return 字符串
	 */
	public static String toHexString(byte[] byt) {
		StringBuilder sb = new StringBuilder(byt.length * 2);
		for (int i = 0; i < byt.length; i++) {
			sb.append(HEXCHAR[(byt[i] & 0xf0) >>> 4]);// SUPPRESS CHECKSTYLE :
														// magic number
			sb.append(HEXCHAR[byt[i] & 0x0f]);// SUPPRESS CHECKSTYLE : magic
												// number
		}
		return sb.toString();
	}

	/**
	 * 调用系统InstalledAppDetails界面显示已安装应用程序的详细信息。 对于Android 2.3（Api Level
	 * 9）以上，使用SDK提供的接口； 2.3以下，使用非公开的接口（查看InstalledAppDetails源码）。
	 * 
	 * @param context
	 *            Context
	 * 
	 * @param packageName
	 *            应用程序的包名
	 */
	public static void showInstalledAppDetails(Context context,
			String packageName) {
		/**
		 * 调用系统InstalledAppDetails界面所需的Extra名称(用于Android 2.1及之前版本)
		 */
		String appPkgname21 = "com.android.settings.ApplicationPkgName";
		/**
		 * 调用系统InstalledAppDetails界面所需的Extra名称(用于Android 2.2)
		 */
		String appPkgname22 = "pkg";
		/**
		 * InstalledAppDetails所在包名
		 */
		String appDetailsPackageName = "com.android.settings";
		/**
		 * InstalledAppDetails类名
		 */
		String appDetailsClassName = "com.android.settings.InstalledAppDetails";
		/** 应用详细设置 */
		String applicationDetailsSettings = "android.settings.APPLICATION_DETAILS_SETTINGS";
		Intent intent = new Intent();
		final int apiLevel = Build.VERSION.SDK_INT;
		if (apiLevel >= 9) { // SUPPRESS CHECKSTYLE 2.3（ApiLevel 9）以上，使用SDK提供的接口
			intent.setAction(applicationDetailsSettings);
			Uri uri = Uri.fromParts("package", packageName, null);
			intent.setData(uri);
		} else { // 2.3以下，使用非公开的接口（查看InstalledAppDetails源码）
			// 2.2和2.1中，InstalledAppDetails使用的APP_PKG_NAME不同。
			final String appPkgName = (apiLevel == 8 ? appPkgname22
					: appPkgname21); // SUPPRESS CHECKSTYLE
			intent.setAction(Intent.ACTION_VIEW);
			intent.setClassName(appDetailsPackageName, appDetailsClassName);
			intent.putExtra(appPkgName, packageName);
		}
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}

	/**
	 * 打开应用
	 * 
	 * @param context
	 *            Context
	 * @param packageName
	 *            packagename
	 */
	public static void openApp(Context context, String packageName) {

		Intent resolveIntent = new Intent(Intent.ACTION_MAIN, null);
		resolveIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		resolveIntent.setPackage(packageName);

		List<ResolveInfo> apps = context.getPackageManager()
				.queryIntentActivities(resolveIntent, 0);

		if (apps != null && apps.size() > 0 && apps.iterator().next() != null) {
			ResolveInfo ri = apps.iterator().next();
			String className = ri.activityInfo.name;

			Intent intent = new Intent(Intent.ACTION_MAIN);
			intent.addCategory(Intent.CATEGORY_LAUNCHER);

			ComponentName cn = new ComponentName(packageName, className);
			intent.setComponent(cn);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			try {
				context.startActivity(intent);
			} catch (ActivityNotFoundException e) {
				// 当不能打开的时候，给出提示
				Toast.makeText(
						context,
						context.getResources().getText(
								R.string.appmanage_can_not_open_toast),
						Toast.LENGTH_SHORT).show();
			}
		} else {
			// 当不能打开的时候，给出提示
			Toast.makeText(
					context,
					context.getResources().getText(
							R.string.appmanage_can_not_open_toast),
					Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * 判断一个 app 是否是系统内置应用
	 * 
	 * @param context
	 *            Context
	 * @param packageName
	 *            需要检测的app的 packageName
	 * @return ApplicationInfo.FLAG_SYSTEM = true 返回true。
	 */
	public static boolean isSystemApp(Context context, String packageName) {
		ApplicationInfo appInfo;
		try {
			appInfo = context.getPackageManager().getApplicationInfo(
					packageName, 0);

			if ((ApplicationInfo.FLAG_SYSTEM & appInfo.flags) != 0) {
				// FLAG_UPDATED_SYSTEM_APP = true 肯定 FLAG_SYSTEM = true
				return true;
			}
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		return false;
	}

	/**
	 * 通过解析APk文件包，获取AndroidManifest.xml，来判断是否是正常的APK文件。如果找到则认为是正常的，否则认为是错误的。
	 * 
	 * @param filename
	 *            文件名字
	 * @return true表示正常,false 表示不正常。
	 */
	public static boolean isAPK(String filename) {
		boolean relt = false;

		if (TextUtils.isEmpty(filename) || !(new File(filename).exists())) {
			if (DEBUG) {
				Log.e(TAG, "apk文件找不到");
			}
			return false;
		}

		try {
			// 使用ZipFile判断下载的包里是否包含Manifest文件
			ZipFile zipfile = new ZipFile(filename);
			if (zipfile.getEntry("AndroidManifest.xml") != null) {
				relt = true;
			}

			zipfile.close();
		} catch (IOException e) {
			if (DEBUG) {
				Log.e(TAG, "解析APK出错:" + e.getMessage());
			}
			relt = false;
		}

		return relt;
	}

	/**
	 * 应用是否已经安装
	 * 
	 * @param context
	 *            ApplicationContext
	 * @param packageName
	 *            包名
	 * @return 是否已经安装
	 */
	public static boolean isPackageInstalled(Context context, String packageName) {
		try {
			context.getPackageManager().getPackageInfo(packageName,
					PackageManager.GET_SIGNATURES);
		} catch (NameNotFoundException e) {
			return false;
		}

		return true;
	}
}
