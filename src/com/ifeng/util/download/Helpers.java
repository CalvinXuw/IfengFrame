/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ifeng.util.download;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Config;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.ifeng.android.R;
import com.ifeng.util.Utility;
import com.ifeng.util.logging.Log;

/**
 * Some helper functions for the download manager
 */
public final class Helpers {

	/** Random object used to generate random ... */
	public static final Random RANDOM = new Random(SystemClock.uptimeMillis());

	/** Regex used to parse content-disposition headers */
	private static final Pattern CONTENT_DISPOSITION_PATTERN = Pattern.compile(
			"attachment;\\s*filename\\s*=\\s*(\"?)([^\"]*)\\1\\s*$",
			Pattern.CASE_INSENSITIVE);

	/** 根据mimetype来查看系统能否处理该类型，如果不能，停止下载。 */
	private static final boolean CHECK_MIME_TYPE = false;
	/** \/data/data/区域中剩余的最小空间大小 20M */
	private static final long MIN_SPACE_LEFT_IN_DATA_LOCATION = 20 * 1024 * 1024;
	/** \/data/data/区域中剩余的最小空间大小 为总大小的10% */
	private static final double MIN_SPACE_PERCENT_LEFT_IN_DATA_LOCATION = 0.1;

	/**
	 * hide.
	 */
	private Helpers() {
	}

	/**
	 * Parse the Content-Disposition HTTP Header. The format of the header is
	 * defined here: http://www.w3.org/Protocols/rfc2616/rfc2616-sec19.html This
	 * header provides a filename for content that is going to be downloaded to
	 * the file system. We only support the attachment type.
	 * 
	 * @param contentDisposition
	 *            Content-Disposition HTTP Header
	 * @return contentDisposition
	 */
	private static String parseContentDisposition(String contentDisposition) {
		try {
			Matcher m = CONTENT_DISPOSITION_PATTERN.matcher(contentDisposition);
			if (m.find()) {
				return m.group(2);
			}
		} catch (IllegalStateException ex) {
			// This function is defined as returning null when it can't parse
			// the header
			Log.w("Helpers", ex);
		}
		return null;
	}

	/**
	 * Exception thrown from methods called by generateSaveFile() for any fatal
	 * error.
	 */
	public static class GenerateSaveFileError extends Exception {
		/** status. */
		int mStatus;
		/** message. */
		String mMessage;

		/**
		 * constructor.
		 * 
		 * @param status
		 *            status
		 * @param message
		 *            message
		 */
		public GenerateSaveFileError(int status, String message) {
			mStatus = status;
			mMessage = message;
		}
	}

	/**
	 * Creates a filename (where the file should be saved) from info about a
	 * download.
	 * 
	 * @param context
	 *            context
	 * @param url
	 *            url
	 * @param hint
	 *            hint
	 * @param contentDisposition
	 *            content disposition
	 * @param contentLocation
	 *            content location
	 * @param mimeType
	 *            mime type
	 * @param destination
	 *            destination
	 * @param contentLength
	 *            content length
	 * @param isPublicApi
	 *            is public api
	 * @return the file name
	 * @throws GenerateSaveFileError
	 *             GenerateSaveFileError
	 */
	public static String generateSaveFile(Context context, String url,
			String hint, String contentDisposition, String contentLocation,
			String mimeType, int destination, long contentLength,
			boolean isPublicApi) throws GenerateSaveFileError {
		checkCanHandleDownload(context, mimeType, destination, isPublicApi);
		if (destination == Downloads.Impl.DESTINATION_FILE_URI) {
			return getPathForFileUri(context, hint, contentLength);
		} else {
			return chooseFullPath(context, url, hint, contentDisposition,
					contentLocation, mimeType, destination, contentLength);
		}
	}

	/**
	 * getPathForFileUri
	 * 
	 * @param context
	 *            context
	 * @param hint
	 *            hint
	 * @param contentLength
	 *            contentLength
	 * @return file path
	 * @throws GenerateSaveFileError
	 *             GenerateSaveFileError
	 */
	private static String getPathForFileUri(Context context, String hint,
			long contentLength) throws GenerateSaveFileError {
		// if path heads to sdcard，then check if sdcard is mounted
		if (hint.startsWith(Environment.getExternalStorageDirectory()
				.getAbsolutePath()) && !isExternalMediaMounted()) {
			throw new GenerateSaveFileError(
					Downloads.Impl.STATUS_DEVICE_NOT_FOUND_ERROR,
					"external media not mounted");
		}
		String path = Uri.parse(hint).getPath();
		if (new File(path).exists()) {
			Log.d(Constants.TAG, "File already exists: " + path);
			throw new GenerateSaveFileError(
					Downloads.Impl.STATUS_FILE_ALREADY_EXISTS_ERROR,
					"requested destination file already exists");
		}
		if (getAvailableBytes(getFilesystemRoot(context, path)) < contentLength) {
			throw new GenerateSaveFileError(
					Downloads.Impl.STATUS_INSUFFICIENT_SPACE_ERROR,
					"insufficient space on external storage");
		}
		// <add by wangdanyang 2012.11.20 BEGIN
		// 如果是下载在application内部目录中，则需要将文件变成全局可读写，否则无法被别的应用打开（比如APK文件无法被ApkInstaller安装)
		processDestFileDataLocate(context, path);
		// add by wangdanyang 2012.11.20 END>
		return path;
	}

	/**
	 * the root of the filesystem containing the given path
	 * 
	 * @param context
	 *            context
	 * @param path
	 *            path.
	 * @return the root of the filesystem containing the given path
	 */
	public static File getFilesystemRoot(Context context, String path) {
		File cache = Environment.getDownloadCacheDirectory();
		if (path.startsWith(cache.getPath())) {
			return cache;
		}
		File external = Environment.getExternalStorageDirectory();
		if (path.startsWith(external.getPath())) {
			return external;
		}
		// 下载到内存
		File internal = context.getFilesDir();
		if (path.startsWith(internal.getAbsolutePath())) {
			return internal;
		}
		throw new IllegalArgumentException(
				"Cannot determine filesystem root for " + path);
	}

	/**
	 * chooseFullPath
	 * 
	 * @param context
	 *            context
	 * @param url
	 *            url
	 * @param hint
	 *            hint
	 * @param contentDisposition
	 *            contentDisposition
	 * @param contentLocation
	 *            contentLocation
	 * @param mimeType
	 *            mimeType
	 * @param destination
	 *            destination
	 * @param contentLength
	 *            contentLength
	 * @return full path
	 * @throws GenerateSaveFileError
	 *             GenerateSaveFileError
	 */
	private static String chooseFullPath(Context context, String url,
			String hint, String contentDisposition, String contentLocation,
			String mimeType, int destination, long contentLength)
			throws GenerateSaveFileError {
		File base = locateDestinationDirectory(context, mimeType, destination,
				contentLength);
		String filename = chooseFilename(url, hint, contentDisposition,
				contentLocation, destination);

		// Split filename between base and extension
		// Add an extension if filename does not have one
		String extension = null;
		// int dotIndex = filename.indexOf('.');
		int dotIndex = filename.lastIndexOf('.'); // 找到最后一个.
		if (dotIndex < 0) {
			extension = chooseExtensionFromMimeType(mimeType, true);
		} else {
			extension = chooseExtensionFromFilename(mimeType, destination,
					filename, dotIndex);
			filename = filename.substring(0, dotIndex);
		}

		boolean recoveryDir = Constants.RECOVERY_DIRECTORY
				.equalsIgnoreCase(filename + extension);

		filename = base.getPath() + File.separator + filename;

		if (Constants.LOGVV) {
			Log.v(Constants.TAG, "target file: " + filename + extension);
		}

		String destFile = chooseUniqueFilename(destination, filename,
				extension, recoveryDir);

		// <add by qumiao 2012.7.18 BEGIN
		// 如果是下载在application内部目录中，则需要将文件变成全局可读写，否则无法被别的应用打开（比如APK文件无法被ApkInstaller安装)
		processDestFileDataLocate(context, destFile);
		// add by qumiao 2012.7.18 END>

		return destFile;
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
	private static void processDestFileDataLocate(Context context,
			String destFile) {
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
	 * check Can Handle Download
	 * 
	 * @param context
	 *            context
	 * @param mimeType
	 *            mimeType
	 * @param destination
	 *            destination
	 * @param isPublicApi
	 *            isPublicApi
	 * @throws GenerateSaveFileError
	 *             GenerateSaveFileError
	 */
	private static void checkCanHandleDownload(Context context,
			String mimeType, int destination, boolean isPublicApi)
			throws GenerateSaveFileError {
		if (isPublicApi) {
			return;
		}

		if (!CHECK_MIME_TYPE) {
			return;
		}

		if (destination == Downloads.Impl.DESTINATION_EXTERNAL
				|| destination == Downloads.Impl.DESTINATION_CACHE_PARTITION_PURGEABLE) {
			if (mimeType == null) {
				throw new GenerateSaveFileError(
						Downloads.Impl.STATUS_NOT_ACCEPTABLE,
						"external download with no mime type not allowed");
			}
			if (!Constants.MIMETYPE_DRM_MESSAGE.equalsIgnoreCase(mimeType)) {
				// Check to see if we are allowed to download this file. Only
				// files
				// that can be handled by the platform can be downloaded.
				// special case DRM files, which we should always allow
				// downloading.
				Intent intent = new Intent(Intent.ACTION_VIEW);

				// We can provide data as either content: or file: URIs,
				// so allow both. (I think it would be nice if we just did
				// everything as content: URIs)
				// Actually, right now the download manager's UId restrictions
				// prevent use from using content: so it's got to be file: or
				// nothing

				PackageManager pm = context.getPackageManager();
				intent.setDataAndType(Uri.fromParts("file", "", null), mimeType);
				ResolveInfo ri = pm.resolveActivity(intent,
						PackageManager.MATCH_DEFAULT_ONLY);
				// Log.i(Constants.TAG, "*** FILENAME QUERY " + intent + ": " +
				// list);

				if (ri == null) {
					if (Constants.LOGV) {
						Log.v(Constants.TAG, "no handler found for type "
								+ mimeType);
					}
					throw new GenerateSaveFileError(
							Downloads.Impl.STATUS_NOT_ACCEPTABLE,
							"no handler found for this download type");
				}
			}
		}
	}

	/**
	 * locateDestinationDirectory.
	 * 
	 * @param context
	 *            context
	 * @param mimeType
	 *            mimeType
	 * @param destination
	 *            destination
	 * @param contentLength
	 *            contentLength
	 * @return the located file.
	 * @throws GenerateSaveFileError
	 *             contentLength
	 */
	private static File locateDestinationDirectory(Context context,
			String mimeType, int destination, long contentLength)
			throws GenerateSaveFileError {
		// DRM messages should be temporarily stored internally and then passed
		// to
		// the DRM content provider
		if (destination == Downloads.Impl.DESTINATION_CACHE_PARTITION
				|| destination == Downloads.Impl.DESTINATION_CACHE_PARTITION_PURGEABLE
				|| destination == Downloads.Impl.DESTINATION_CACHE_PARTITION_NOROAMING
				|| Constants.MIMETYPE_DRM_MESSAGE.equalsIgnoreCase(mimeType)) {
			return getCacheDestination(context, contentLength);
		}

		return getDestination(context, contentLength);
	}

	/**
	 * getDestination.
	 * 
	 * @param context
	 *            Context
	 * @param contentLength
	 *            contentLength
	 * @return File
	 * @throws GenerateSaveFileError
	 *             GenerateSaveFileError
	 */
	private static File getDestination(final Context context, long contentLength)
			throws GenerateSaveFileError {
		File base = null;
		Handler handler = new Handler(context.getMainLooper());
		boolean hassdcard = false;
		boolean hasEnoughSDSpace = false;
		boolean hasExtVolume = false;
		boolean downloadToMem = false;
		if (Constants.LOGV) {
			Log.d(Constants.TAG, "Download destination mode is: "
					+ Downloads.getDestinationMode().name());
			Log.d(Constants.TAG,
					"Download destination dir is: "
							+ Downloads.getDestinationDir());
		}

		switch (Downloads.getDestinationMode()) {
		case AUTO:

			/*
			 * Modify by dongfengyu 20121129 下载路径选择顺序：
			 * 1.使用通用API,获取外部存储路径(这个未必是常规理解上的可插拔的SD卡);
			 * 2.如果条件1不满足,那么查看当前设备是否还有其他加载的Volume; 3.如果条件2不满足,那么就写入data区域.
			 */
			hassdcard = isExternalMediaMounted();
			hasEnoughSDSpace = getAvailableBytes(Environment
					.getExternalStorageDirectory()) > contentLength;
			if (hassdcard && hasEnoughSDSpace) {
				base = new File(Environment.getExternalStorageDirectory()
						.getPath() + Constants.DEFAULT_DL_SUBDIR);

				if (Constants.LOGV) {
					Log.d(Constants.TAG, "Common ExternalStorage path ======="
							+ base);
				}
			} else {
				Object[] volumes = Utility.getVolumeList(context
						.getApplicationContext());

				// 所有Volume的数目
				int totalVolumeCount = 0;

				// 存在其他Volume:
				if (volumes != null) {
					hasExtVolume = true;
					totalVolumeCount = volumes.length;
					if (Constants.LOGV) {
						Log.d(Constants.TAG, "TotalvolumeCount="
								+ totalVolumeCount);
					}

					// 遍历设备上的所有Volume，直到获取满足条件的Volume.
					for (int i = 0; i < totalVolumeCount; i++) {

						String volumePath = Utility.getVolumePath(volumes[i]);
						if (Constants.LOGV) {
							Log.d(Constants.TAG, "invoke path[" + i
									+ "]===========" + volumePath);
						}

						String state = Utility.getVolumeState(
								context.getApplicationContext(), volumePath);

						if (state.equals(Environment.MEDIA_MOUNTED)
								&& getAvailableBytes(new File(volumePath)) > contentLength) {
							base = new File(volumePath
									+ Constants.DEFAULT_DL_SUBDIR);
							if (Constants.LOGV) {
								Log.d(Constants.TAG,
										"Other Volume path ======="
												+ volumePath
												+ Constants.DEFAULT_DL_SUBDIR);
							}

							break;
						}
					}
				}
			}

			// 最后尝试下载至user data区域.
			if (base == null) {
				if (hassdcard) {
					if (!hasEnoughSDSpace) {
						// 显示sd卡空间不够
						handler.post(new Runnable() {
							@Override
							public void run() {
								Toast.makeText(
										context,
										context.getResources()
												.getString(
														R.string.no_space_on_sd_card_will_download_to_mem),
										Toast.LENGTH_SHORT).show();
							}
						});
					}
				} else {
					if (!hasExtVolume) {
						// 显示没有sd卡
						handler.post(new Runnable() {

							@Override
							public void run() {
								Toast.makeText(
										context,
										context.getResources()
												.getString(
														R.string.no_sd_card_will_download_to_mem),
										Toast.LENGTH_SHORT).show();
							}
						});
					}
				}
				base = context.getFilesDir();
				downloadToMem = true;
			}

			break;

		case INTERNAL_ONLY:
			base = context.getFilesDir();
			break;

		case EXTERNAL_ONLY:
			if (!isExternalMediaMounted()) {
				throw new GenerateSaveFileError(
						Downloads.Impl.STATUS_DEVICE_NOT_FOUND_ERROR,
						"external media not mounted");
			}

			File root = Environment.getExternalStorageDirectory();
			base = new File(root.getPath() + Constants.DEFAULT_DL_SUBDIR);
			break;

		case CUSTOM:
			base = new File(Downloads.getDestinationDir());
			break;

		default:
			break;
		}

		if (base == null || (!base.isDirectory() && !base.mkdirs())) {
			// Can't create download directory, e.g. because a file called
			// "download"
			// already exists at the root level, or the SD card filesystem is
			// read-only.
			throw new GenerateSaveFileError(Downloads.Impl.STATUS_FILE_ERROR,
					"unable to create downloads directory "
							+ (base == null ? null : base.getPath()));
		}

		long available = getAvailableBytes(base);
		long totalBytes = getTotaltes(base);
		double availablepercent = ((double) available / totalBytes);
		if (Constants.LOGV) {
			Log.d(Constants.TAG, "download dir is: " + base.getAbsolutePath());
			Log.d(Constants.TAG, "available space is: " + available);
			Log.d(Constants.TAG, "totalBytes space is: " + totalBytes);
			Log.d(Constants.TAG, "available/totalBytes percent is: "
					+ ((double) available / totalBytes));
			Log.d(Constants.TAG,
					"availablepercent<0.1 is: "
							+ (availablepercent < MIN_SPACE_PERCENT_LEFT_IN_DATA_LOCATION));
			Log.d(Constants.TAG, " available < 20 * 1024 * 1024 is: "
					+ (available < MIN_SPACE_LEFT_IN_DATA_LOCATION));
		}

		if (downloadToMem) {
			// Insufficient space.
			if ((availablepercent < MIN_SPACE_PERCENT_LEFT_IN_DATA_LOCATION
					|| available < contentLength * 2 || available < MIN_SPACE_LEFT_IN_DATA_LOCATION)) {
				handler.post(new Runnable() {
					@Override
					public void run() {
						// FIXME 增加显示的Toast的时间
						Toast toast = Toast
								.makeText(
										context,
										context.getResources()
												.getString(
														R.string.no_space_on_phone_dialog_message),
										Toast.LENGTH_LONG);
						toast.show();
					}
				});
				Log.d(Constants.TAG,
						"download aborted - not enough free space on memory");
				throw new GenerateSaveFileError(
						Downloads.Impl.STATUS_FILE_ERROR,
						"insufficient space on media");
			}
		} else if (available < contentLength) {
			Log.d(Constants.TAG,
					"download aborted - not enough free space on external storage");
			throw new GenerateSaveFileError(Downloads.Impl.STATUS_FILE_ERROR,
					"insufficient space on media");
		}

		return base;
	}

	/**
	 * isExternalMediaMounted.
	 * 
	 * @return isExternalMediaMounted
	 */
	public static boolean isExternalMediaMounted() {
		if (!Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			// No SD card found.
			Log.d(Constants.TAG, "no external storage");
			return false;
		}
		return true;
	}

	/**
	 * get Cache Destination.
	 * 
	 * @param context
	 *            context
	 * @param contentLength
	 *            contentLength
	 * @return File
	 * @throws GenerateSaveFileError
	 *             GenerateSaveFileError
	 */
	private static File getCacheDestination(Context context, long contentLength)
			throws GenerateSaveFileError {
		File base;
		base = Environment.getDownloadCacheDirectory();
		long bytesAvailable = getAvailableBytes(base);
		while (bytesAvailable < contentLength) {
			// Insufficient space; try discarding purgeable files.
			if (!discardPurgeableFiles(context, contentLength - bytesAvailable)) {
				// No files to purge, give up.
				throw new GenerateSaveFileError(
						Downloads.Impl.STATUS_INSUFFICIENT_SPACE_ERROR,
						"not enough free space in internal download storage, unable to free any "
								+ "more");
			}
			bytesAvailable = getAvailableBytes(base);
		}
		return base;
	}

	/**
	 * the number of bytes available on the filesystem rooted at the given File
	 * 
	 * @param root
	 *            root file dir.
	 * @return the number of bytes available on the filesystem rooted at the
	 *         given File
	 */
	public static long getAvailableBytes(File root) {
		StatFs stat = new StatFs(root.getPath());
		// put a bit of margin (in case creating the file grows the system by a
		// few blocks)
		long availableBlocks = (long) stat.getAvailableBlocks() - 4; // SUPPRESS
																		// CHECKSTYLE
		return stat.getBlockSize() * availableBlocks;
	}

	/**
	 * the number of bytes on the filesystem rooted at the given File
	 * 
	 * @param root
	 *            root file dir.
	 * @return the number of bytes available on the filesystem rooted at the
	 *         given File
	 */
	public static long getTotaltes(File root) {
		StatFs stat = new StatFs(root.getPath());
		// put a bit of margin (in case creating the file grows the system by a
		// few blocks)
		long availableBlocks = (long) stat.getBlockCount() - 4; // SUPPRESS
																// CHECKSTYLE //
																// CHECKSTYLE
		return stat.getBlockSize() * availableBlocks;
	}

	/**
	 * choose File name from the info.
	 * 
	 * @param url
	 *            url
	 * @param hint
	 *            hint
	 * @param contentDisposition
	 *            contentDisposition
	 * @param contentLocation
	 *            contentLocation
	 * @param destination
	 *            destination
	 * @return file name.
	 */
	private static String chooseFilename(String url, String hint,
			String contentDisposition, String contentLocation, int destination) {
		String filename = null;

		// First, try to use the hint from the application, if there's one
		if (filename == null && hint != null && !hint.endsWith("/")) {
			if (Constants.LOGVV) {
				Log.v(Constants.TAG, "getting filename from hint");
			}
			int index = hint.lastIndexOf('/') + 1;
			if (index > 0) {
				filename = hint.substring(index);
			} else {
				filename = hint;
			}
		}

		// If we couldn't do anything with the hint, move toward the content
		// disposition
		if (filename == null && contentDisposition != null) {
			filename = parseContentDisposition(contentDisposition);
			if (filename != null) {
				if (Constants.LOGVV) {
					Log.v(Constants.TAG,
							"getting filename from content-disposition");
				}
				int index = filename.lastIndexOf('/') + 1;
				if (index > 0) {
					filename = filename.substring(index);
				}
			}
		}

		// If we still have nothing at this point, try the content location
		if (filename == null && contentLocation != null) {
			String decodedContentLocation = Uri.decode(contentLocation);
			if (decodedContentLocation != null
					&& !decodedContentLocation.endsWith("/")
					&& decodedContentLocation.indexOf('?') < 0) {
				if (Constants.LOGVV) {
					Log.v(Constants.TAG,
							"getting filename from content-location");
				}
				int index = decodedContentLocation.lastIndexOf('/') + 1;
				if (index > 0) {
					filename = decodedContentLocation.substring(index);
				} else {
					filename = decodedContentLocation;
				}
			}
		}

		// If all the other http-related approaches failed, use the plain uri
		if (filename == null) {
			String decodedUrl = Uri.decode(url);
			if (decodedUrl != null && !decodedUrl.endsWith("/")
					&& decodedUrl.indexOf('?') < 0) {
				int index = decodedUrl.lastIndexOf('/') + 1;
				if (index > 0) {
					if (Constants.LOGVV) {
						Log.v(Constants.TAG, "getting filename from uri");
					}
					filename = decodedUrl.substring(index);
				}
			}
		}

		// Finally, if couldn't get filename from URI, get a generic filename
		if (filename == null) {
			if (Constants.LOGVV) {
				Log.v(Constants.TAG, "using default filename");
			}
			filename = Constants.DEFAULT_DL_FILENAME;
		}
		// 使用中文名，不替换
		filename = filename.replaceAll(
				"[()（）.,：:\\-|^$#_，。：=、/+《》<>*?？‘“”''\"\"]", "_");
		return filename;
	}

	/**
	 * choose Extension From Mime Type
	 * 
	 * @param mimeType
	 *            mimeType
	 * @param useDefaults
	 *            useDefaults
	 * @return extension.
	 */
	private static String chooseExtensionFromMimeType(String mimeType,
			boolean useDefaults) {
		String extension = null;
		if (mimeType != null) {
			extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(
					mimeType);
			if (extension != null) {
				if (Constants.LOGVV) {
					Log.v(Constants.TAG, "adding extension from type");
				}
				extension = "." + extension;
			} else if (mimeType.equals(Constants.MIMETYPE_EPUB)) {
				extension = Constants.DEFAULT_DL_EPUB_EXTENSION;
			} else {
				if (Constants.LOGVV) {
					Log.v(Constants.TAG, "couldn't find extension for "
							+ mimeType);
				}
			}
		}
		if (extension == null) {
			if (mimeType != null && mimeType.toLowerCase().startsWith("text/")) {
				if (mimeType.equalsIgnoreCase("text/html")) {
					if (Constants.LOGVV) {
						Log.v(Constants.TAG, "adding default html extension");
					}
					extension = Constants.DEFAULT_DL_HTML_EXTENSION;
				} else if (useDefaults) {
					if (Constants.LOGVV) {
						Log.v(Constants.TAG, "adding default text extension");
					}
					extension = Constants.DEFAULT_DL_TEXT_EXTENSION;
				}
			} else if (useDefaults) {
				if (Constants.LOGVV) {
					Log.v(Constants.TAG, "adding default binary extension");
				}
				extension = Constants.DEFAULT_DL_BINARY_EXTENSION;
			}
		}
		return extension;
	}

	/**
	 * choose Extension From Filename
	 * 
	 * @param mimeType
	 *            mimeType
	 * @param destination
	 *            destination
	 * @param filename
	 *            filename
	 * @param dotIndex
	 *            dotIndex
	 * @return extension.
	 */
	private static String chooseExtensionFromFilename(String mimeType,
			int destination, String filename, int dotIndex) {
		String extension = null;
		if (mimeType != null) {
			// Compare the last segment of the extension against the mime type.
			// If there's a mismatch, discard the entire extension.
			int lastDotIndex = filename.lastIndexOf('.');
			String typeFromExt = MimeTypeMap.getSingleton()
					.getMimeTypeFromExtension(
							filename.substring(lastDotIndex + 1));
			if (typeFromExt == null || !typeFromExt.equalsIgnoreCase(mimeType)) {
				extension = chooseExtensionFromMimeType(mimeType, false);
				if (extension != null) {
					if (Constants.LOGVV) {
						Log.v(Constants.TAG, "substituting extension from type");
					}
				} else {
					if (Constants.LOGVV) {
						Log.v(Constants.TAG, "couldn't find extension for "
								+ mimeType);
					}
				}
			}
		}
		if (extension == null) {
			if (Constants.LOGVV) {
				Log.v(Constants.TAG, "keeping extension");
			}
			extension = filename.substring(dotIndex);
		}
		return extension;
	}

	/**
	 * chooseUniqueFilename.
	 * 
	 * @param destination
	 *            destination
	 * @param filename
	 *            filename
	 * @param extension
	 *            extension
	 * @param recoveryDir
	 *            recoveryDir
	 * @return chooseUniqueFilename
	 * @throws GenerateSaveFileError
	 *             GenerateSaveFileError
	 */
	private static String chooseUniqueFilename(int destination,
			String filename, String extension, boolean recoveryDir)
			throws GenerateSaveFileError {
		String fullFilename = filename + extension;
		if (!new File(fullFilename).exists()
				&& (!recoveryDir || (destination != Downloads.Impl.DESTINATION_CACHE_PARTITION
						&& destination != Downloads.Impl.DESTINATION_CACHE_PARTITION_PURGEABLE && destination != Downloads.Impl.DESTINATION_CACHE_PARTITION_NOROAMING))) {
			return fullFilename;
		}
		filename = filename + Constants.FILENAME_SEQUENCE_SEPARATOR;
		/*
		 * This number is used to generate partially randomized filenames to
		 * avoid collisions. It starts at 1. The next 9 iterations increment it
		 * by 1 at a time (up to 10). The next 9 iterations increment it by 1 to
		 * 10 (random) at a time. The next 9 iterations increment it by 1 to 100
		 * (random) at a time. ... Up to the point where it increases by
		 * 100000000 at a time. (the maximum value that can be reached is
		 * 1000000000) As soon as a number is reached that generates a filename
		 * that doesn't exist, that filename is used. If the filename coming in
		 * is [base].[ext], the generated filenames are [base]-[sequence].[ext].
		 */
		int sequence = 1;
		for (int magnitude = 1; magnitude < 1000000000; magnitude *= 10) { // SUPPRESS
																			// CHECKSTYLE
			for (int iteration = 0; iteration < 9; ++iteration) { // SUPPRESS
																	// CHECKSTYLE
				fullFilename = filename + sequence + extension;
				if (!new File(fullFilename).exists()) {
					return fullFilename;
				}
				if (Constants.LOGVV) {
					Log.v(Constants.TAG, "file with sequence number "
							+ sequence + " exists");
				}
				sequence += RANDOM.nextInt(magnitude) + 1;
			}
		}
		throw new GenerateSaveFileError(Downloads.Impl.STATUS_FILE_ERROR,
				"failed to generate an unused filename on internal download storage");
	}

	/**
	 * Deletes purgeable files from the cache partition. This also deletes the
	 * matching database entries. Files are deleted in LRU order until the total
	 * byte size is greater than targetBytes.
	 * 
	 * @param context
	 *            context
	 * @param targetBytes
	 *            targetBytes
	 * @return see the description
	 */
	public static boolean discardPurgeableFiles(Context context,
			long targetBytes) {
		Cursor cursor = context.getContentResolver().query(
				Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI,
				null,
				"( " + Downloads.Impl.COLUMN_STATUS + " = '"
						+ Downloads.Impl.STATUS_SUCCESS + "' AND "
						+ Downloads.Impl.COLUMN_DESTINATION + " = '"
						+ Downloads.Impl.DESTINATION_CACHE_PARTITION_PURGEABLE
						+ "' )", null, Downloads.Impl.COLUMN_LAST_MODIFICATION);
		if (cursor == null) {
			return false;
		}
		long totalFreed = 0;
		try {
			cursor.moveToFirst();
			while (!cursor.isAfterLast() && totalFreed < targetBytes) {
				File file = new File(cursor.getString(cursor
						.getColumnIndex(Downloads.Impl.DATA)));
				if (Constants.LOGVV) {
					Log.v(Constants.TAG, "purging " + file.getAbsolutePath()
							+ " for " + file.length() + " bytes");
				}
				totalFreed += file.length();
				boolean deleted = file.delete();
				if (!deleted) {
					Log.v(Constants.TAG, "delete file failed.");
				}
				long id = cursor.getLong(cursor
						.getColumnIndex(Downloads.Impl._ID));
				context.getContentResolver().delete(
						ContentUris.withAppendedId(
								Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, id),
						null, null);
				cursor.moveToNext();
			}
		} finally {
			cursor.close();
		}
		if (Constants.LOGV) {
			if (totalFreed > 0) {
				Log.v(Constants.TAG, "Purged files, freed " + totalFreed
						+ " for " + targetBytes + " requested");
			}
		}
		return totalFreed > 0;
	}

	/**
	 * Returns whether the network is available
	 * 
	 * @param system
	 *            SystemFacade
	 * @return see the description.
	 */
	public static boolean isNetworkAvailable(SystemFacade system) {
		return system.getActiveNetworkType() != null;
	}

	/**
	 * Checks whether the filename looks legitimate
	 * 
	 * @param filename
	 *            filename
	 * @return see the description
	 */
	public static boolean isFilenameValid(String filename) {
		filename = filename.replaceFirst("/+", "/"); // normalize leading
														// slashes
		// 增加对下载到手机内存文件名字的支持
		return filename.startsWith(Environment.getDownloadCacheDirectory()
				.toString())
				|| filename.startsWith(Environment
						.getExternalStorageDirectory().toString())
				|| filename.startsWith(Environment.getDataDirectory()
						.toString());
	}

	/**
	 * Checks whether this looks like a legitimate selection parameter
	 * 
	 * @param selection
	 *            selection
	 * @param allowedColumns
	 *            allowedColumns
	 */
	public static void validateSelection(String selection,
			Set<String> allowedColumns) {
		try {
			if (selection == null || TextUtils.isEmpty(selection)) {
				return;
			}
			Lexer lexer = new Lexer(selection, allowedColumns);
			parseExpression(lexer);
			if (lexer.currentToken() != Lexer.TOKEN_END) {
				throw new IllegalArgumentException("syntax error");
			}
		} catch (RuntimeException ex) {
			if (Constants.LOGV) {
				Log.d(Constants.TAG, "invalid selection [" + selection
						+ "] triggered " + ex);
			} else if (Config.LOGD) {
				Log.d(Constants.TAG, "invalid selection triggered " + ex);
			}
			throw ex;
		}

	}

	/**
	 * expression <- ( expression ) | statement [AND_OR ( expression ) |
	 * statement] * | statement [AND_OR expression]*
	 * 
	 * @param lexer
	 *            lexer
	 */
	private static void parseExpression(Lexer lexer) {
		for (;;) {
			// ( expression )
			if (lexer.currentToken() == Lexer.TOKEN_OPEN_PAREN) {
				lexer.advance();
				parseExpression(lexer);
				if (lexer.currentToken() != Lexer.TOKEN_CLOSE_PAREN) {
					throw new IllegalArgumentException(
							"syntax error, unmatched parenthese");
				}
				lexer.advance();
			} else {
				// statement
				parseStatement(lexer);
			}
			if (lexer.currentToken() != Lexer.TOKEN_AND_OR) {
				break;
			}
			lexer.advance();
		}
	}

	/**
	 * statement <- COLUMN COMPARE VALUE | COLUMN IS NULL
	 * 
	 * @param lexer
	 *            lexer
	 */
	private static void parseStatement(Lexer lexer) {
		// both possibilities start with COLUMN
		if (lexer.currentToken() != Lexer.TOKEN_COLUMN) {
			throw new IllegalArgumentException(
					"syntax error, expected column name");
		}
		lexer.advance();

		// statement <- COLUMN COMPARE VALUE
		if (lexer.currentToken() == Lexer.TOKEN_COMPARE) {
			lexer.advance();
			if (lexer.currentToken() != Lexer.TOKEN_VALUE) {
				throw new IllegalArgumentException(
						"syntax error, expected quoted string");
			}
			lexer.advance();
			return;
		}

		// statement <- COLUMN IS NULL
		if (lexer.currentToken() == Lexer.TOKEN_IS) {
			lexer.advance();
			if (lexer.currentToken() != Lexer.TOKEN_NULL) {
				throw new IllegalArgumentException(
						"syntax error, expected NULL");
			}
			lexer.advance();
			return;
		}

		// didn't get anything good after COLUMN
		throw new IllegalArgumentException("syntax error after column name");
	}

	/**
	 * A simple lexer that recognizes the words of our restricted subset of SQL
	 * where clauses
	 */
	private static class Lexer {
		/** start token. */
		public static final int TOKEN_START = 0;
		/** open paren token. */
		public static final int TOKEN_OPEN_PAREN = 1;
		/** close paren token. */
		public static final int TOKEN_CLOSE_PAREN = 2;
		/** and or token. */
		public static final int TOKEN_AND_OR = 3;
		/** column token. */
		public static final int TOKEN_COLUMN = 4;
		/** compare token. */
		public static final int TOKEN_COMPARE = 5;
		/** value token. */
		public static final int TOKEN_VALUE = 6;
		/** is token. */
		public static final int TOKEN_IS = 7;
		/** null token. */
		public static final int TOKEN_NULL = 8;
		/** end token. */
		public static final int TOKEN_END = 9;
		/** the selection string. */
		private final String mSelection;
		/** mAllowedColumns. */
		private final Set<String> mAllowedColumns;
		/** current offset. */
		private int mOffset = 0;
		/** current token. */
		private int mCurrentToken = TOKEN_START;
		/** mChars. */
		private final char[] mChars;

		/**
		 * constructor.
		 * 
		 * @param selection
		 *            selection
		 * @param allowedColumns
		 *            allowedColumns
		 */
		public Lexer(String selection, Set<String> allowedColumns) {
			mSelection = selection;
			mAllowedColumns = allowedColumns;
			mChars = new char[mSelection.length()];
			mSelection.getChars(0, mChars.length, mChars, 0);
			advance();
		}

		/**
		 * get current token.
		 * 
		 * @return currentToken
		 */
		public int currentToken() {
			return mCurrentToken;
		}

		/**
		 * advance.
		 */
		public void advance() {
			char[] chars = mChars;

			// consume whitespace
			while (mOffset < chars.length && chars[mOffset] == ' ') {
				++mOffset;
			}

			// end of input
			if (mOffset == chars.length) {
				mCurrentToken = TOKEN_END;
				return;
			}

			// "("
			if (chars[mOffset] == '(') {
				++mOffset;
				mCurrentToken = TOKEN_OPEN_PAREN;
				return;
			}

			// ")"
			if (chars[mOffset] == ')') {
				++mOffset;
				mCurrentToken = TOKEN_CLOSE_PAREN;
				return;
			}

			// "?"
			if (chars[mOffset] == '?') {
				++mOffset;
				mCurrentToken = TOKEN_VALUE;
				return;
			}

			// "=" and "=="
			if (chars[mOffset] == '=') {
				++mOffset;
				mCurrentToken = TOKEN_COMPARE;
				if (mOffset < chars.length && chars[mOffset] == '=') {
					++mOffset;
				}
				return;
			}

			// ">" and ">="
			if (chars[mOffset] == '>') {
				++mOffset;
				mCurrentToken = TOKEN_COMPARE;
				if (mOffset < chars.length && chars[mOffset] == '=') {
					++mOffset;
				}
				return;
			}

			// "<", "<=" and "<>"
			if (chars[mOffset] == '<') {
				++mOffset;
				mCurrentToken = TOKEN_COMPARE;
				if (mOffset < chars.length
						&& (chars[mOffset] == '=' || chars[mOffset] == '>')) {
					++mOffset;
				}
				return;
			}

			// "!="
			if (chars[mOffset] == '!') {
				++mOffset;
				mCurrentToken = TOKEN_COMPARE;
				if (mOffset < chars.length && chars[mOffset] == '=') {
					++mOffset;
					return;
				}
				throw new IllegalArgumentException(
						"Unexpected character after !");
			}

			// columns and keywords
			// first look for anything that looks like an identifier or a
			// keyword
			// and then recognize the individual words.
			// no attempt is made at discarding sequences of underscores with no
			// alphanumeric
			// characters, even though it's not clear that they'd be legal
			// column names.
			if (isIdentifierStart(chars[mOffset])) {
				int startOffset = mOffset;
				++mOffset;
				while (mOffset < chars.length
						&& isIdentifierChar(chars[mOffset])) {
					++mOffset;
				}
				String word = mSelection.substring(startOffset, mOffset);
				if (mOffset - startOffset <= 4) { // SUPPRESS CHECKSTYLE
					if (word.equals("IS")) {
						mCurrentToken = TOKEN_IS;
						return;
					}
					if (word.equals("OR") || word.equals("AND")) {
						mCurrentToken = TOKEN_AND_OR;
						return;
					}
					if (word.equals("NULL")) {
						mCurrentToken = TOKEN_NULL;
						return;
					}
				}
				if (mAllowedColumns.contains(word)) {
					mCurrentToken = TOKEN_COLUMN;
					return;
				}
				throw new IllegalArgumentException(
						"unrecognized column or keyword");
			}

			// quoted strings
			if (chars[mOffset] == '\'') {
				++mOffset;
				while (mOffset < chars.length) {
					if (chars[mOffset] == '\'') {
						if (mOffset + 1 < chars.length
								&& chars[mOffset + 1] == '\'') {
							++mOffset;
						} else {
							break;
						}
					}
					++mOffset;
				}
				if (mOffset == chars.length) {
					throw new IllegalArgumentException("unterminated string");
				}
				++mOffset;
				mCurrentToken = TOKEN_VALUE;
				return;
			}

			// anything we don't recognize
			throw new IllegalArgumentException("illegal character: "
					+ chars[mOffset]);
		}

		/**
		 * is Identifier Start
		 * 
		 * @param c
		 *            CHAR
		 * @return IS RETURN TRUE
		 */
		private static boolean isIdentifierStart(char c) {
			return c == '_' || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
		}

		/**
		 * is Identifier char
		 * 
		 * @param c
		 *            CHAR
		 * @return IS RETURN TRUE
		 */
		private static boolean isIdentifierChar(char c) {
			return c == '_' || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')
					|| (c >= '0' && c <= '9');
		}
	}

	/**
	 * Delete the given file from device and delete its row from the downloads
	 * database.
	 * 
	 * @param resolver
	 *            resolver
	 * @param id
	 *            id
	 * @param path
	 *            path
	 * @param mimeType
	 *            mimeType
	 */
	/* package */static void deleteFile(ContentResolver resolver, long id,
			String path, String mimeType) {
		try {
			File file = new File(path);
			boolean deleted = file.delete();
			if (!deleted) {
				Log.w(Constants.TAG, "deleteFile failed");
			}
		} catch (Exception e) {
			Log.w(Constants.TAG, "file: '" + path + "' couldn't be deleted", e);
		}
		resolver.delete(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI,
				Downloads.Impl._ID + " = ? ",
				new String[] { String.valueOf(id) });
	}
}
