package com.ifeng.util.net.parser;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.text.TextUtils;

import com.ifeng.util.logging.Log;

/**
 * 自动解析json类，接口数据item类如需自动解析需要继承自该类。通过
 * {@link #addMappingRuleField(String, String)}及
 * {@link #addMappingRuleArrayField(String, String, Class)}
 * 方法，可进行对普通成员变量以及集合型成员变量进行解析，其中成员变量可以为{@link #AbstractIfengJSONItem}而进一步采取嵌套解析。
 * 在其中addMapping的过程，需要采取例如：node1/node2/targetKey的方式构成解析路径。 注意：
 * {@link #AbstractIfengJSONItem}可作为外部类和静态内部类，不能为非静态的内部类。
 * 
 * @author Calvin 2013-5-30
 * 
 */
public abstract class AbstractIFJSONItem extends AbstractIFItem {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3450668199391533573L;

	/** 值类型成员变量映射表 */
	private HashMap<String, String> mJsonFieldMap;
	/** 数组类型成员变量映射表 */
	private HashMap<String, ArrayParserItem> mJsonArrayMap;

	/**
	 * 构造
	 */
	public AbstractIFJSONItem() {
		mJsonFieldMap = new HashMap<String, String>();
		mJsonArrayMap = new HashMap<String, AbstractIFJSONItem.ArrayParserItem>();
	}

	/**
	 * 解析jsonstring
	 * 
	 * @param data
	 * @return
	 */
	public boolean parseData(String data) {
		if (TextUtils.isEmpty(data)) {
			if (DEBUG) {
				Log.d(TAG, "the data to parse is empty");
			}
			return false;
		}

		try {
			Object json = new JSONTokener(data).nextValue();
			if (json instanceof JSONObject) {
				return parseData((JSONObject) json);
			} else if (json instanceof JSONArray) {
				return parseData((JSONArray) json);
			}

			return false;
		} catch (JSONException e) {
			if (DEBUG) {
				Log.d(TAG, "the data to parse is in a wrong format");
				Log.e(TAG, e);
				Log.e(TAG, data);
			}
			return false;
		} catch (Exception e) {
			if (DEBUG) {
				Log.d(TAG, "caught unknow exception");
				Log.e(TAG, e);
			}
			return false;
		}
	}

	/**
	 * 解析JSONObject
	 * 
	 * @param rootDict
	 * @return
	 * @throws Exception
	 */
	private boolean parseData(JSONObject rootDict) throws Exception {
		if (rootDict == null) {
			return false;
		}

		try {
			/*
			 * 获取当前类所有public变量，其中包含从父类中继承下来的public变量
			 */
			Field[] fields = getClass().getFields();
			for (Field field : fields) {
				Class<?> classType = field.getType();
				if (classType == List.class || classType == ArrayList.class
						|| classType == LinkedList.class) {
					setListField(field, rootDict);
				} else {
					setField(field, rootDict);
				}
			}

		} catch (Exception e) {
			if (DEBUG) {
				Log.e(TAG, e);
			}
			throw new Exception("unknow exception caught");
		}

		return true;
	}

	/**
	 * 解析JSONArray
	 * 
	 * @param rootArray
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private boolean parseData(JSONArray rootArray) throws Exception {
		if (rootArray == null) {
			return false;
		}

		try {
			/*
			 * 获取当前类所有public变量，其中包含从父类中继承下来的public变量
			 */
			Field[] fields = getClass().getFields();
			for (Field field : fields) {
				Class<?> classType = field.getType();

				if (classType == List.class || classType == ArrayList.class
						|| classType == LinkedList.class) {
					ArrayParserItem parserItem = mJsonArrayMap.get(field
							.getName());
					if (parserItem == null) {
						continue;
					}

					Class<?> classs = parserItem.mClass;
					/*
					 * 猜测Collection类型，具体实例化
					 */
					List list = null;
					if (field.getType() == List.class) {
						list = new LinkedList();
					} else if (field.getType() == ArrayList.class) {
						list = new ArrayList();
					} else if (field.getType() == LinkedList.class) {
						list = new LinkedList();
					} else {
						if (DEBUG) {
							Log.d(TAG,
									"unknow type of collection , can not handle it");
						}
						continue;
					}
					field.set(this, list);

					for (int i = 0; i < rootArray.length(); i++) {
						Object subObject = rootArray.opt(i);

						if (subObject == null) {
							continue;
						}

						// 若为基本类型或者String类型
						if (subObject instanceof Integer
								|| subObject instanceof Long
								|| subObject instanceof Float
								|| subObject instanceof Double
								|| subObject instanceof Boolean
								|| subObject instanceof String) {
							list.add(subObject);
						} else {
							JSONObject subJsonObject = (JSONObject) subObject;
							/*
							 * 其余类型默认为JSONObject对象
							 */
							AbstractIFJSONItem subItem = (AbstractIFJSONItem) classs
									.getConstructor().newInstance();
							if (subItem.parseData(subJsonObject)) {
								list.add(subItem);
							}
						}
					}
				}
			}

		} catch (Exception e) {
			if (DEBUG) {
				Log.e(TAG, e);
			}
			throw new Exception("unknow exception caught");
		}

		return true;
	}

	/**
	 * 对于非集合变量及{@link AbstractIFJSONItem}的赋值
	 * 
	 * @param field
	 * @param root
	 */
	private void setField(Field field, JSONObject root) {
		String path = mJsonFieldMap.get(field.getName());
		if (TextUtils.isEmpty(path)) {
			return;
		}

		String[] route = path.split("/");

		String key = route[0];
		JSONObject node = root;
		for (int i = 1; i < route.length; i++) {
			node = node.optJSONObject(key);
			key = route[i];
		}

		try {

			if (field.getType() == int.class) {
				field.setInt(this, node.optInt(key));
			} else if (field.getType() == long.class) {
				field.setLong(this, node.optLong(key));
			} else if (field.getType() == float.class) {
				field.setFloat(this, (float) node.optDouble(key));
			} else if (field.getType() == double.class) {
				field.setDouble(this, (float) node.optDouble(key));
			} else if (field.getType() == boolean.class) {
				field.setBoolean(this, node.optBoolean(key));
			} else if (field.getType() == String.class) {
				String parseString = node.optString(key);
				if (TextUtils.isEmpty(parseString)
						|| "null".equalsIgnoreCase(parseString)) {
					parseString = null;
				}
				field.set(this, parseString);
			} else {
				Object object = field.getType().getConstructor().newInstance();
				if (object instanceof AbstractIFJSONItem) {
					AbstractIFJSONItem subItem = (AbstractIFJSONItem) object;
					if (subItem.parseData(node.optJSONObject(key))) {
						field.set(this, subItem);
					}
				}
			}

		} catch (IllegalArgumentException e) {
			if (DEBUG) {
				Log.d(TAG, e);
			}
		} catch (IllegalAccessException e) {
			if (DEBUG) {
				Log.d(TAG, e);
			}
		} catch (Exception e) {
			if (DEBUG) {
				Log.d(TAG, e);
			}
		}
	}

	/**
	 * 对于集合变量的赋值
	 * 
	 * @param field
	 * @param root
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void setListField(Field field, JSONObject root) {
		ArrayParserItem parserItem = mJsonArrayMap.get(field.getName());
		if (parserItem == null) {
			return;
		}

		String path = parserItem.mPath;
		Class<?> classs = parserItem.mClass;

		/*
		 * 猜测Collection类型，具体实例化
		 */
		List list = null;
		if (field.getType() == List.class) {
			list = new LinkedList();
		} else if (field.getType() == ArrayList.class) {
			list = new ArrayList();
		} else if (field.getType() == LinkedList.class) {
			list = new LinkedList();
		} else {
			if (DEBUG) {
				Log.d(TAG, "unknow type of collection , can not handle it");
			}
			return;
		}

		try {
			field.set(this, list);

			String[] route = path.split("/");

			String key = route[0];
			JSONObject node = root;
			for (int i = 1; i < route.length; i++) {
				node = node.optJSONObject(key);
				key = route[i];
			}

			JSONArray array = node.optJSONArray(key);
			if (array != null) {
				for (int i = 0; i < array.length(); i++) {
					Object subObject = array.opt(i);

					if (subObject == null) {
						continue;
					}

					// 若为基本类型或者String类型
					if (subObject instanceof Integer
							|| subObject instanceof Long
							|| subObject instanceof Float
							|| subObject instanceof Double
							|| subObject instanceof Boolean
							|| subObject instanceof String) {
						list.add(subObject);
					} else {
						JSONObject subJsonObject = (JSONObject) subObject;
						/*
						 * 其余类型默认为JSONObject对象
						 */
						AbstractIFJSONItem subItem = (AbstractIFJSONItem) classs
								.getConstructor().newInstance();
						if (subItem.parseData(subJsonObject)) {
							list.add(subItem);
						}
					}

				}
			}
		} catch (IllegalArgumentException e) {
			if (DEBUG) {
				Log.e(TAG, e);
			}
		} catch (IllegalAccessException e) {
			if (DEBUG) {
				Log.e(TAG, e);
			}
		} catch (Exception e) {
			if (DEBUG) {
				Log.e(TAG, e);
			}
		}
	}

	/**
	 * 添加成员变量映射，示例格式:{a:b}，取到b的值则为 path = a ；{a:{b:c}} ,取到c的值则为 path = a/b
	 * 
	 * @param field
	 * @param path
	 */
	protected void addMappingRuleField(String field, String path) {
		mJsonFieldMap.put(field, path);
	}

	/**
	 * 添加数组成员变量映射，需要json的格式满足{a:[{obj},{obj},{obj}]}且obj为
	 * {@link AbstractIFJSONItem}，既可采取 path = a 的方式对其中obj进行嵌套解析
	 * 
	 * @param field
	 * @param path
	 * @param classs
	 */
	protected void addMappingRuleArrayField(String field, String path,
			Class<?> classs) {
		mJsonArrayMap.put(field, new ArrayParserItem(path, classs));
	}

	/**
	 * 用于集合解析映射的容器
	 * 
	 * @author Calvin
	 * 
	 */
	private class ArrayParserItem implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 6519699831842963540L;
		/** json路径 */
		public String mPath;
		/** Class */
		public Class<?> mClass;

		public ArrayParserItem(String path, Class<?> classs) {
			mPath = path;
			mClass = classs;
		}
	}
}
