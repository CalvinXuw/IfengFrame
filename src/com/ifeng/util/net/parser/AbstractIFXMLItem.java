package com.ifeng.util.net.parser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.text.TextUtils;

import com.ifeng.util.logging.Log;

/**
 * 自动解析xml类，接口数据item类如需自动解析需要继承自该类。通过
 * {@link #addMappingRuleField(String, String)}及
 * {@link #addMappingRuleArrayField(String, String, Class)}
 * 方法，可进行对普通成员变量以及集合型成员变量进行解析，其中成员变量可以为{@link #AbstractIfengXMLItem}而进一步采取嵌套解析。
 * 在其中addMapping的过程，对于值节点需要采取例如：node1/node2/targetKey/# 或
 * node1/node2/targetKey/：count 的方式构成解析路径。 注意： {@link #AbstractIfengXMLItem}
 * 可作为外部类和静态内部类，不能为非静态的内部类。
 * 
 * @author Calvin 2013-5-30
 * 
 */
public class AbstractIFXMLItem extends AbstractIFItem {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8583103880807742760L;

	/** 值类型成员变量映射表 */
	private HashMap<String, String> mXmlFieldMap;
	/** 数组类型成员变量映射表 */
	private HashMap<String, ArrayParserItem> mXmlArrayMap;

	/**
	 * 构造
	 */
	public AbstractIFXMLItem() {
		mXmlFieldMap = new HashMap<String, String>();
		mXmlArrayMap = new HashMap<String, AbstractIFXMLItem.ArrayParserItem>();
	}

	/**
	 * 解析xmlstring
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
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(new ByteArrayInputStream(data
					.getBytes()));
			Element rootElement = document.getDocumentElement();
			return parseData(rootElement);

		} catch (ParserConfigurationException e) {
			if (DEBUG) {
				Log.d(TAG, e);
			}
			return false;
		} catch (SAXException e) {
			if (DEBUG) {
				Log.d(TAG, e);
			}
			return false;
		} catch (IOException e) {
			if (DEBUG) {
				Log.d(TAG, e);
			}
			return false;
		} catch (Exception e) {
			if (DEBUG) {
				Log.d(TAG, "caught unknow exception");
			}
			return false;
		}
	}

	/**
	 * 解析Element
	 * 
	 * @param element
	 * @return
	 * @throws Exception
	 */
	private boolean parseData(Element element) throws Exception {
		if (element == null) {
			return false;
		}

		try {
			/*
			 * 获取当前类所有public变量，其中包含从父类中继承下来的public变量
			 */
			Field[] fields = getClass().getFields();
			for (Field field : fields) {
				boolean isArrayField = false;

				if (field.getType() == List.class) {
					isArrayField = true;
				} else if (field.getType() == ArrayList.class) {
					isArrayField = true;
				} else if (field.getType() == LinkedList.class) {
					isArrayField = true;
				}

				if (isArrayField) {
					setListField(field, element);
				} else {
					setField(field, element);
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
	private void setField(Field field, Element root) {
		String path = mXmlFieldMap.get(field.getName());
		if (TextUtils.isEmpty(path)) {
			return;
		}

		String[] route = path.split("/");

		String key = route[0];
		Element node = root;

		try {
			for (int i = 1; i < route.length; i++) {
				node = (Element) node.getElementsByTagName(key).item(0);
				key = route[i];
			}

			if (field.getType() == int.class) {
				String value = getNodeValue(key, node);
				if (value == null) {
					return;
				}
				field.setInt(this, Integer.parseInt(value));
			} else if (field.getType() == long.class) {
				String value = getNodeValue(key, node);
				if (value == null) {
					return;
				}
				field.setLong(this, Long.parseLong(value));
			} else if (field.getType() == float.class) {
				String value = getNodeValue(key, node);
				if (value == null) {
					return;
				}
				field.setFloat(this, Float.parseFloat(value));
			} else if (field.getType() == double.class) {
				String value = getNodeValue(key, node);
				if (value == null) {
					return;
				}
				field.setDouble(this, Double.parseDouble(value));
			} else if (field.getType() == boolean.class) {
				field.setBoolean(this,
						Boolean.parseBoolean(getNodeValue(key, node)));
			} else if (field.getType() == String.class) {
				String parseString = getNodeValue(key, node);
				if (TextUtils.isEmpty(parseString)
						|| "null".equalsIgnoreCase(parseString)) {
					parseString = null;
				}
				field.set(this, parseString);
			} else {
				Object object = field.getType().getConstructor().newInstance();
				if (object instanceof AbstractIFXMLItem) {
					AbstractIFXMLItem subItem = (AbstractIFXMLItem) object;
					if (subItem.parseData(node)) {
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
	private void setListField(Field field, Element root) {
		ArrayParserItem parserItem = mXmlArrayMap.get(field.getName());
		if (parserItem == null) {
			return;
		}

		String path = parserItem.mPath;
		Class<? extends AbstractIFXMLItem> classs = parserItem.mClass;

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
			Element node = root;
			for (int i = 1; i < route.length; i++) {
				node = (Element) node.getElementsByTagName(key).item(0);
				key = route[i];
			}

			Element array = null;
			if (key.equals("#")) {
				array = (Element) node;
			} else {
				array = (Element) node.getElementsByTagName(key).item(0);
			}

			if (array != null) {
				NodeList nodes = array.getChildNodes();
				for (int i = 0; i < nodes.getLength(); i++) {
					if (!(nodes.item(i) instanceof Element)) {
						continue;
					}
					Element element = (Element) nodes.item(i);
					AbstractIFXMLItem subItem = classs.getConstructor()
							.newInstance();
					if (subItem.parseData(element)) {
						list.add(subItem);
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
	 * 添加成员变量映射，其中path路径起始于xml根节点位置，属性的读取需要将path位移至该节点，即：解析根节点下的attr参数
	 * count的path为
	 * :count，该节点value的path为#。若某节点位置为xml下的books节点，如需读取该节点的count参数，则其path为books
	 * /:count。
	 * 
	 * @param field
	 * @param path
	 */
	protected void addMappingRuleField(String field, String path) {
		if (mXmlFieldMap == null) {
			mXmlFieldMap = new HashMap<String, String>();
		}
		mXmlFieldMap.put(field, path);
	}

	/**
	 * 添加数组成员变量映射，其中path路径起始于xml根节点位置，数组选取示范，xml根节点下的books节点包含若干book子节点，
	 * 则其path为books
	 * 
	 * @param field
	 * @param path
	 * @param classs
	 * @param pair
	 */
	protected void addMappingRuleArrayField(String field, String path,
			Class<? extends AbstractIFXMLItem> classs) {
		if (mXmlArrayMap == null) {
			mXmlArrayMap = new HashMap<String, ArrayParserItem>();
		}
		mXmlArrayMap.put(field, new ArrayParserItem(path, classs));
	}

	private String getNodeValue(String key, Element node) {
		String valueString = null;
		/*
		 * 若该属性位于attr位置
		 */
		if (key.contains(":")) {
			valueString = node.getAttribute(key.replace(":", ""));
		} else if (key.contains("")) {
			valueString = node.getFirstChild().getNodeValue();
		}

		if (TextUtils.isEmpty(valueString)) {
			return null;
		}

		return valueString;
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
		private static final long serialVersionUID = -5265021341700916882L;
		/** 节点路径 */
		public String mPath;
		/** Class */
		public Class<? extends AbstractIFXMLItem> mClass;

		public ArrayParserItem(String path,
				Class<? extends AbstractIFXMLItem> classs) {
			mPath = path;
			mClass = classs;
		}
	}
}
