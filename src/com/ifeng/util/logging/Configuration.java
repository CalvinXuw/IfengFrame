/**
 * Copyright (c) 2013 ifeng Inc.
 * 
 * @author 		Xu Wei <xuwei@ifeng.com>
 * 
 * @date 2013-4-18
 */
package com.ifeng.util.logging;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;

import android.util.Log;

/**
 * 负责加载所有log配置。
 */
public final class Configuration {
	/**
	 * 私有构造函数
	 */
	private Configuration() {

	}

	/** 配置文件的名字 */
	public static final String CONFIGURATION_FILENAME = "ifenglog.cfg";
	/** 可打印Log的配置 */
	public static final HashMap<String, String> LOG_CONFIGURATIONS = new HashMap<String, String>();

	static {
		InputStream is = Configuration.class
				.getResourceAsStream(CONFIGURATION_FILENAME);
		Properties prop = new Properties();
		try {
			prop.load(is);
			Enumeration<Object> keys = prop.keys();
			while (keys.hasMoreElements()) {
				String key = (String) keys.nextElement();
				// 如果不是包，则直接加入
				LOG_CONFIGURATIONS.put(key, prop.getProperty(key));
				// TODO 如果是包，则需要加入所有包中的类
			}
		} catch (IOException e) {
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

}
