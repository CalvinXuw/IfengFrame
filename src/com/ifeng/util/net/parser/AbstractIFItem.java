package com.ifeng.util.net.parser;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import com.ifeng.BaseApplicaion;

/**
 * 自动解析抽象类，接口数据item如需自动解析，需要继承自对应解析格式的子类:{@link AbstractIFJSONItem}提供 JSON解析、
 * {@link AbstractIFXMLItem}提供XML解析。具体的解析路径编写参照具体类目描述。 注意：
 * {@link #AbstractIfengItem}可作为外部类和静态内部类，不能为非静态的内部类。
 * 
 * @author Calvin 2013-5-31
 * 
 */
public abstract class AbstractIFItem implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 687035639105706641L;

	/** log tag. */
	protected final String TAG = getClass().getSimpleName();

	/** if enabled, logcat will output the log. */
	protected final boolean DEBUG = true & BaseApplicaion.DEBUG;

	/**
	 * 根据class获取实例,Class要求继承自{@link AbstractIFItem}
	 * 
	 * @param classs
	 * @return
	 */
	public static AbstractIFItem getInstance(
			Class<? extends AbstractIFItem> classs) {
		try {
			Constructor<? extends AbstractIFItem> constructor = null;
			// 若作为某类的内部类出现，则需要声明为static型，否则在构造时需要传入outer类的实例，但无法保证outer类实例均含有空构造方法，所以不做保护代码
			// if (classs.getName().contains("$")) {
			// /*
			// * inner class
			// */
			// Class<?> outerClass = Class.forName(classs.getName().substring(
			// 0, classs.getName().indexOf("$")));
			// constructor = classs.getConstructor(outerClass);
			// return constructor.newInstance(outerClass.newInstance());
			// }
			constructor = classs.getConstructor();
			return constructor.newInstance();
		} catch (IllegalArgumentException e) {
		} catch (InstantiationException e) {
		} catch (IllegalAccessException e) {
		} catch (InvocationTargetException e) {
		} catch (NoSuchMethodException e) {
		}
		return null;
	}

	/**
	 * 解析数据，子类需要负责具体实现
	 * 
	 * @param data
	 * @return
	 */
	public abstract boolean parseData(String data);
}
