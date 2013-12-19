package com.ifeng;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Properties;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.BMapManager;
import com.baidu.mapapi.MKGeneralListener;
import com.baidu.mapapi.map.MKEvent;
import com.ifeng.android.BuildConfig;
import com.ifeng.util.CrashHandler;
import com.ifeng.util.download.DownloadServiceCallback;
import com.ifeng.util.logging.Configuration;
import com.ifeng.util.logging.Log;

/**
 * 基类application，实现了地图sdk以及下载管理服务，以及全局的配置信息
 * 
 * @author Calvin
 * 
 */
public class BaseApplicaion extends Application implements
		DownloadServiceCallback {

	/** 配置文件位置，需要在src目录下创建配置文件 */
	private static final String PROPERTY_FILENAME = "/android.ifeng.cfg";
	/** 是否输出日志到文件中 */
	private static final String KEY_PROPERTY_LOG_TO_FILE = "logToFile";
	/** 百度地图api key */
	private static final String KEY_PROPERTY_BAIDUMAP_KEY = "baiduApiKey";
	/** 包名 */
	private static final String KEY_PROPERTY_PACKAGENAME = "packagename";

	static {
		InputStream is = Configuration.class
				.getResourceAsStream(PROPERTY_FILENAME);
		Properties prop = new Properties();
		try {
			prop.load(is);
			sPackageName = prop.getProperty(KEY_PROPERTY_PACKAGENAME,
					"com.ifeng.android");
			sMapApiKey = prop.getProperty(KEY_PROPERTY_BAIDUMAP_KEY,
					"ED1B37071DB67221265CE4A38DC6828539579298");
			sLogToFile = Boolean.parseBoolean(prop.getProperty(
					KEY_PROPERTY_LOG_TO_FILE, "true"));
		} catch (IOException e) {
			Log.e(Configuration.class.getName(),
					"check the ifeng frame config file again !");
			Log.e(Configuration.class.getName(), e.getMessage());
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					Log.e(Configuration.class.getName(), e.getMessage());
				}
			}
		}
	}

	/** tag */
	public final String TAG = getClass().getSimpleName();
	/** 是否开启调试模式 */
	public static final boolean DEBUG = BuildConfig.DEBUG;
	/** 是否将全部日志信息输出到文件 或者 从配置文件中读取输出条件信息 */
	public static final boolean LOG_TO_FILE_ALLMESSAGE = true;

	/** 是否将log日志输出到日志文件中 */
	public static boolean sLogToFile;
	/** 包名 */
	public static String sPackageName;

	@Override
	public void onCreate() {
		super.onCreate();

		// 初始化定位服务
		initLocationService();
		// 初始化地图引擎
		initBMapEngineManager();
		// 初始化异常崩溃类
		CrashHandler crashHandler = CrashHandler.getInstance();
		crashHandler.init(getApplicationContext());
	}

	@Override
	public void onTerminate() {
		clearReference();
		super.onTerminate();
	}

	@Override
	public void onDownloadServiceCreate() {

	}

	/**
	 * 退出前需要清除持有的引用
	 */
	public void clearReference() {
		destoryLocationService();
		releaseEngine();
	}

	/***
	 * 退出应用程序
	 * 
	 * @param context
	 */
	public void AppExit() {
		try {
			ActivityManager activityMgr = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
			activityMgr.restartPackage(getPackageName());
			System.exit(0);
		} catch (Exception e) {
		}
	}

	/**
	 * 百度地图api定位服务
	 * 
	 * @author Calvin 61 ： GPS定位结果 62 ： 扫描整合定位依据失败。此时定位结果无效。 63
	 *         ：网络异常，没有成功向服务器发起请求。此时定位结果无效。 65 ： 定位缓存的结果。 66
	 *         ：离线定位结果。通过requestOfflineLocaiton调用时对应的返回结果 67
	 *         ：离线定位失败。通过requestOfflineLocaiton调用时对应的返回结果 68
	 *         ：网络连接失败时，查找本地离线定位时对应的返回结果 161： 表示网络定位结果 162~167： 服务端定位失败。 api文档
	 *         http://developer.baidu.com/map/android_refer/index.html
	 */

	/** 百度地图api key */
	private static String sMapApiKey;
	/** 个别情况下mapengine会找不到so库而初始化错误 */
	private boolean mIsCannotInitMapEngine;
	/** 观察者列表 */
	private LinkedList<LocationServiceListener> mLocationServiceListeners;

	/** 百度地图api定位核心类 */
	private LocationClient mLocationClient;
	/** 定位信息监听 */
	private LocationListener mLocationListener;
	/** 百度地图api地图引擎管理类 */
	private BMapManager mBMapManager;

	/**
	 * 初始化定位服务
	 */
	private void initLocationService() {
		// 配置百度地图api的参数
		LocationClientOption locationClientOption = new LocationClientOption();

		// 需要获取地址信息
		locationClientOption.setAddrType("all");

		// 需要使用gps
		locationClientOption.setOpenGps(true);

		/*
		 * 返回国测局经纬度坐标系 coor=gcj02 返回百度墨卡托坐标系 coor=bd09 返回百度经纬度坐标系 coor=bd09ll
		 */
		locationClientOption.setCoorType("bd09ll");

		/*
		 * 设置扫描间隔的时间，毫秒级单位 不设置或设置时间小于一秒时，
		 */
		// locationClientOption.setScanSpan(3000);

		/*
		 * 设置定位方式的优先级。目前定位SDK的定位方式有两类：一是使用GPS进行定位。优点是定位准确，精度在几十米，缺点是第一次定位速度较慢，甚至需要2
		 * 、3分钟。二是使用网络定位。优点是定位速度快，服务端只需8ms，考虑到网速的话，一般客户端3秒左右即可定位，缺点是没有gps准确
		 * ，精度在几十到几百米。为了方便用户，我们提供了有两个整型的项：LocationClientOption.GpsFirst 以及
		 * LocationClientOption.NetWorkFirst：
		 * 
		 * GpsFirst：当gps可用，而且获取了定位结果时，不再发起网络请求，直接返回给用户坐标。这个选项适合希望得到准确坐标位置的用户。
		 * 如果gps不可用，再发起网络请求，进行定位。
		 * NetWorkFirst：即时有gps，而且可用，也仍旧会发起网络请求。这个选项适合对精确坐标不是特别敏感，但是希望得到位置描述的用户。
		 */
		locationClientOption.setPriority(LocationClientOption.NetWorkFirst);

		// 设置最多可返回的POI个数，默认值为3。由于POI查询比较耗费流量，设置最多返回的POI个数，以便节省流量。
		// locationClientOption.setPoiNumber(3);

		// 设置查询范围，默认值为500，即以当前定位位置为中心的半径大小。
		// locationClientOption.setPoiDistance(500);

		// 设置是否返回POI的电话和地址等详细信息。默认值为false，即不返回POI的电话和地址信息。
		// locationClientOption.setPoiExtraInfo(true);

		// 设置是否启用缓存定位
		locationClientOption.disableCache(true);

		locationClientOption.setServiceName("com.baidu.location.service_v2.9");

		mLocationClient = new LocationClient(this, locationClientOption);
		mLocationListener = new LocationListener();
		mLocationClient.registerLocationListener(mLocationListener);
		mLocationServiceListeners = new LinkedList<LocationServiceListener>();
	}

	/**
	 * 初始化mapengine
	 */
	private void initBMapEngineManager() {
		try {

			mBMapManager = new BMapManager(this);
			if (!mBMapManager.init(sMapApiKey, new MapGeneralListener())) {
				Log.e(TAG, "BMapManager  初始化错误!");
			}

		} catch (ExceptionInInitializerError e) {
			Log.e(TAG, "caught unknow excepiton");
			Log.e(TAG, e);

			mIsCannotInitMapEngine = true;
			mBMapManager = null;
		}
	}

	/**
	 * 
	 * @author Calvin 位置服务监听接口
	 * 
	 */
	private class LocationListener implements BDLocationListener {

		@Override
		public void onReceivePoi(BDLocation location) {
			Log.d(TAG, "onReceivePoi");

			synchronized (mLocationServiceListeners) {
				for (LocationServiceListener listener : mLocationServiceListeners) {
					listener.onReceivePoi(getSuccessLocation(location));
				}
			}
		}

		@Override
		public void onReceiveLocation(BDLocation location) {
			Log.d(TAG, "onReceiveLocation");

			synchronized (mLocationServiceListeners) {
				for (LocationServiceListener listener : mLocationServiceListeners) {
					listener.onReceiveLocation(getSuccessLocation(location));
				}
			}
		}

		/**
		 * 仅传递成功的返回值
		 * 
		 * @param location
		 * @return
		 */
		private BDLocation getSuccessLocation(BDLocation location) {
			if (location != null) {
				switch (location.getLocType()) {
				case 61:
				case 65:
				case 66:
				case 68:
				case 161:
					return location;
				case 63:
					// 网络异常

					break;
				default:
					// 定位失败
					break;
				}
			}
			return null;
		}
	};

	/**
	 * 请求定位
	 * 
	 * @return
	 */
	public void requestLocation() {
		if (mIsCannotInitMapEngine) {
			mLocationListener.onReceiveLocation(null);
			return;
		}

		Runnable runnable = new Runnable() {

			@Override
			public void run() {
				mLocationClient.requestLocation();
			}
		};
		Handler handler = new Handler();

		if (!mLocationClient.isStarted()) {
			mLocationClient.start();
			handler.postDelayed(runnable, 3000);
		} else {
			handler.post(runnable);
		}
	}

	/**
	 * 请求周边信息
	 * 
	 * @return
	 */
	public void requestPoi() {
		if (mIsCannotInitMapEngine) {
			mLocationListener.onReceivePoi(null);
			return;
		}

		Runnable runnable = new Runnable() {

			@Override
			public void run() {
				mLocationClient.requestPoi();
			}
		};
		Handler handler = new Handler();

		if (!mLocationClient.isStarted()) {
			mLocationClient.start();
			handler.postDelayed(runnable, 3000);
		} else {
			handler.post(runnable);
		}
	}

	/**
	 * 添加到监听观察者
	 * 
	 * @param {@link LocationServiceListener} listener
	 */
	public void addLocationServiceListener(LocationServiceListener listener) {
		synchronized (mLocationServiceListeners) {
			mLocationServiceListeners.add(listener);
		}
	}

	/**
	 * 注销到监听观察者
	 * 
	 * @param {@link LocationServiceListener} listener
	 */
	public void removeLocationServiceListener(LocationServiceListener listener) {
		synchronized (mLocationServiceListeners) {
			mLocationServiceListeners.remove(listener);
		}
	}

	/**
	 * 关闭定位服务
	 */
	public void stopLocationService() {
		if (mLocationClient.isStarted()) {
			mLocationClient.stop();
		}
	}

	/**
	 * 彻底关闭定位服务
	 */
	public void destoryLocationService() {
		stopLocationService();
		mLocationClient.unRegisterLocationListener(mLocationListener);
	}

	/**
	 * 检查引擎可用性
	 */
	public void initEngine() {
		if (mBMapManager == null) {
			initBMapEngineManager();
		}
	}

	/**
	 * 获取地图引擎
	 * 
	 * @return BMapManager
	 */
	public BMapManager getBMapManager() {
		initBMapEngineManager();
		return mBMapManager;
	}

	/**
	 * 释放地图引擎
	 */
	public void releaseEngine() {
		if (mBMapManager != null) {
			mBMapManager.destroy();
			mBMapManager = null;
		}
	}

	/**
	 * 常用事件监听，用来处理通常的网络错误，授权验证错误等
	 * 
	 * @author Calvin
	 * 
	 */
	private class MapGeneralListener implements MKGeneralListener {

		@Override
		public void onGetNetworkState(int iError) {
			if (iError == MKEvent.ERROR_NETWORK_CONNECT) {
				Log.e(TAG, "network error");

				Toast.makeText(getApplicationContext(), "您的网络出错啦！",
						Toast.LENGTH_LONG).show();
			} else if (iError == MKEvent.ERROR_NETWORK_DATA) {
				Log.e(TAG, "输入正确的检索条件！");

				Toast.makeText(getApplicationContext(), "输入正确的检索条件！",
						Toast.LENGTH_LONG).show();
			}
		}

		@Override
		public void onGetPermissionState(int iError) {
			if (iError == MKEvent.ERROR_PERMISSION_DENIED) {
				try {
					throw new NoPermission();
				} catch (NoPermission e) {
					Log.e(TAG, e);
				}
			}
		}

		/**
		 * 授权检测
		 * 
		 * @author Calvin
		 * 
		 */
		private class NoPermission extends Throwable {

			/**
			 * 缺少授权key
			 */
			private static final long serialVersionUID = 1L;

		}
	}

	/**
	 * 注册位置服务监听
	 * 
	 * @author Calvin
	 * 
	 */
	public interface LocationServiceListener {

		/**
		 * 接收周边信息
		 * 
		 * @param {@link BDLocation} location
		 */
		public void onReceivePoi(BDLocation location);

		/**
		 * 接收定位信息
		 * 
		 * @param {@link BDLocation} location
		 */
		public void onReceiveLocation(BDLocation location);

	}

}
