package com.ifeng.util.logging;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import android.os.Environment;
import android.text.TextUtils;

import com.ifeng.BaseApplicaion;

/**
 * 辅助 log 类，输出log到 logcat 以及输出到 log 文件。 输出到文件是封装的java.util.logging.Logger。
 */
public final class Log {

	/** debug开关，关闭后，log功能失效，不能输出任何东西。建议relase发布时，关闭。 */
	private static final boolean DEBUG = BaseApplicaion.DEBUG;
	/** 用于控制是输出log到文件，还是logcat。 */
	private static boolean sLogToFile;
	/** 是否将全部日志信息输出到文件 或者 从配置文件中读取输出条件信息 */
	public static final boolean LOG_TO_FILE_ALLMESSAGE = BaseApplicaion.LOG_TO_FILE_ALLMESSAGE;

	/** logcat tag. */
	private static final String TAG = Log.class.getSimpleName();

	/** 是否完成初始化 */
	private static boolean sIsInit;
	/** java.util.logging.Logger object. */
	private static Logger sFilelogger;

	/** private constructor. */
	private Log() {
	}

	/**
	 * 初始化
	 */
	private static void init() {
		sIsInit = true;
		sLogToFile = BaseApplicaion.sLogToFile;

		if (sLogToFile && DEBUG) {

			/** java.util.logging.Logger 用到 */
			final String LOGGER_NAME = Utils.getLogFileName();

			/** log文件夹名。 */
			final String LOG_FILE_DIRECTORY = Environment
					.getExternalStorageDirectory().getPath()
					+ "/ifeng/data/log/";
			/** log文件名。 不同项目需要修改此文件名。 */
			final String LOG_FILE_NAME = LOG_FILE_DIRECTORY + LOGGER_NAME;

			File logDirFile;
			FileHandler fhandler;

			// 单个log文件的大小单位： byte。
			final int limit = 1000000; // SUPPRESS CHECKSTYLE
			// 最多的log文件的个数，以 0123编号作为后缀。
			int number = 3; // SUPPRESS CHECKSTYLE

			try {
				logDirFile = new File(LOG_FILE_DIRECTORY);
				if (logDirFile != null && !logDirFile.exists()) {
					logDirFile.mkdirs();
				}

				fhandler = new FileHandler(LOG_FILE_NAME + ".log", true);
				fhandler.setFormatter(new SimpleFormatter());

				sFilelogger = Logger.getLogger(LOGGER_NAME);
				sFilelogger.setLevel(Level.ALL);
				sFilelogger.addHandler(fhandler);

			} catch (SecurityException e) {
				Log.e(TAG, "error:" + e.getMessage());
			} catch (IOException e) {
				Log.e(TAG, "error:" + e.getMessage());
			}
		}
	}

	/**
	 * log info.
	 * 
	 * @param tag
	 *            tag
	 * @param msg
	 *            msg
	 */
	public static void i(String tag, String msg) {
		if (!sIsInit) {
			init();
		}

		if (DEBUG) {
			if (shouldLogToFile(tag, LogConstants.LOGLEVEL_INFO)) {
				sFilelogger.log(Level.INFO, tag + ": " + msg);
			} else {
				android.util.Log.i(tag, msg);
			}
		}
	}

	/**
	 * log info.
	 * 
	 * @param tag
	 *            tag
	 * @param msg
	 *            msg
	 */
	public static void v(String tag, String msg) {
		if (DEBUG) {
			i(tag, msg);
		}
	}

	/**
	 * log debug info.
	 * 
	 * @param tag
	 *            tag
	 * @param msg
	 *            msg
	 */
	public static void d(String tag, String msg) {
		if (!sIsInit) {
			init();
		}

		if (DEBUG) {
			if (shouldLogToFile(tag, LogConstants.LOGLEVEL_DEBUG)) {
				sFilelogger.log(Level.INFO, tag + ": " + msg);
			} else {
				android.util.Log.d(tag, msg);
			}
		}
	}

	/**
	 * log debug info.
	 * 
	 * @param tag
	 *            tag
	 * @param e
	 *            异常
	 */
	public static void d(String tag, Throwable e) {
		String msg = Utils.getStackTraceString(e);
		d(tag, msg);
	}

	/**
	 * log debug info.
	 * 
	 * @param tag
	 *            tag
	 * @param message
	 *            异常信息
	 * @param e
	 *            异常
	 */
	public static void d(String tag, String message, Throwable e) {
		String msg = message + '\n' + Utils.getStackTraceString(e);
		d(tag, msg);
	}

	/**
	 * log warn.
	 * 
	 * @param tag
	 *            tag
	 * @param msg
	 *            msg
	 */
	public static void w(String tag, String msg) {
		if (DEBUG) {
			if (shouldLogToFile(tag, LogConstants.LOGLEVEL_WARNING)) {
				sFilelogger.log(Level.WARNING, tag + ": " + msg);
			} else {
				android.util.Log.w(tag, msg);
			}
		}
	}

	/**
	 * log warn.
	 * 
	 * @param tag
	 *            Log Tag
	 * @param e
	 *            Throwable
	 */
	public static void w(String tag, Throwable e) {
		String msg = Utils.getStackTraceString(e);
		w(tag, msg);
	}

	/**
	 * log warn.
	 * 
	 * @param tag
	 *            Log Tag
	 * @param message
	 *            异常信息
	 * @param e
	 *            Throwable
	 */
	public static void w(String tag, String message, Throwable e) {
		String msg = message + '\n' + Utils.getStackTraceString(e);
		w(tag, msg);
	}

	/**
	 * log error.
	 * 
	 * @param tag
	 *            tag
	 * @param msg
	 *            msg
	 */
	public static void e(String tag, String msg) {
		if (!sIsInit) {
			init();
		}

		// error级别log一律进行输出
		if (shouldLogToFile(tag, LogConstants.LOGLEVEL_ERROR)) {
			sFilelogger.log(Level.SEVERE, tag + ": " + msg);
		} else {
			android.util.Log.e(tag, msg);
		}
	}

	/**
	 * log error.
	 * 
	 * @param tag
	 *            tag
	 * @param e
	 *            Throwable
	 */
	public static void e(String tag, Throwable e) {
		String msg = Utils.getStackTraceString(e);

		e(tag, msg);
	}

	/**
	 * log error.
	 * 
	 * @param tag
	 *            tag
	 * @param message
	 *            异常信息
	 * @param e
	 *            Throwable
	 */
	public static void e(String tag, String message, Throwable e) {
		String msg = message + '\n' + Utils.getStackTraceString(e);

		e(tag, msg);
	}

	/**
	 * secure log level,主要是针对保密的log内容，一般情况下不应该输出到文件中，只对内部人员debug时可以开启.
	 * 
	 * @param tag
	 *            tag
	 * @param msg
	 *            msg
	 */
	public static void s(String tag, String msg) {
		if (!sIsInit) {
			init();
		}

		if (DEBUG) {
			if (shouldLogToFile(tag, LogConstants.LOGLEVEL_INFO)) {
				sFilelogger.log(Level.INFO, tag + ": " + msg);
			} else {
				android.util.Log.e(tag, msg);
			}
		}
	}

	/**
	 * 判断是否需要写入Log文件。根据配置文件判断是否应该记录该log。
	 * 
	 * @param tag
	 *            log的TAG名字，一般是类名
	 * @param specifiedLevel
	 *            指定的log等级
	 * @return true应该写入Log文件,false 不应该写入Log文件
	 */
	private static boolean shouldLogToFile(String tag, String specifiedLevel) {
		if (sLogToFile && sFilelogger != null) {
			if (LOG_TO_FILE_ALLMESSAGE) {
				// 输出全部等级为error的log信息
				return TextUtils.equals(specifiedLevel,
						LogConstants.LOGLEVEL_ERROR);
			} else if (Configuration.LOG_CONFIGURATIONS.containsKey(tag)) {
				String loglevel = Configuration.LOG_CONFIGURATIONS.get(tag);
				// 找不到对应的log等级，返回false
				if (TextUtils.isEmpty(loglevel)) {
					return false;
				} else {
					// 符合loglevel，返回true
					// 不符合，返回false
					Integer level = LogConstants.LOG_LEVELS.get(loglevel);
					if (level >= LogConstants.LOG_LEVELS.get(specifiedLevel)) {
						return true;
					}
				}
			}
			return false;
		} else {
			return false;
		}
	}

}
