package com.ifeng.util.ui;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.ifeng.util.Utility;

/**
 * 自定义WebView，暂时只提供对html中px转换的处理
 * 
 * @author Calvin
 * 
 */
public class IFWebView extends WebView {

	/** js to java 处理类 */
	private static final String NEWS_JS_TO_JAVA_INTERFACE = "NewsProcess";
	/** js to java 调用方法_点击图片 */
	private static final String NEWS_JS_TO_JAVA_METHOD_IMG_CLICK = "onClickImg";
	/** js to java 调用方法_点击视频 */
	private static final String NEWS_JS_TO_JAVA_METHOD_VIDEO_CLICK = "onClickVideo";

	/** uri 图片缺省图 */
	private static final String URI_DEFAULT_IMAGE = "file:///android_asset/default_html_image.png";
	/** uri 视频缺省图 */
	private static final String URI_DEFAULT_VIDEO = "file:///android_asset/default_html_video.png";
	/** uri 视频播放图片 */
	private static final String URI_IMAGE_VIDEO = "file:///android_asset/image_html_video.png";

	/** 处理转换img标签的任务 */
	private List<HtmlMappingItem> mImgTagTasks;

	/** callback */
	private OnJSClick mOnJSClick;

	/**
	 * 构造
	 * 
	 * @param context
	 */
	public IFWebView(Context context) {
		super(context);
		init();
	}

	/**
	 * 构造方法
	 * 
	 * @param context
	 * @param attrs
	 */
	public IFWebView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	/**
	 * 初始化
	 */
	@SuppressLint("SetJavaScriptEnabled")
	private void init() {
		getSettings().setJavaScriptEnabled(true);
		addJavascriptInterface(new NewsProcess(), NEWS_JS_TO_JAVA_INTERFACE);
	}

	/**
	 * 点击事件的js to java处理
	 * 
	 * @author Calvin
	 * @see IFWebView#IMG_JS_TO_JAVA_INTERFACE
	 * 
	 */
	private class NewsProcess {

		/**
		 * 点击图片
		 * 
		 * @param img
		 * @see IFWebView#NEWS_JS_TO_JAVA_METHOD_IMG_CLICK
		 */
		@SuppressWarnings("unused")
		@JavascriptInterface
		public void onClickImg(final String img) {
			if (mOnJSClick != null) {
				mOnJSClick.onImageClick(img);
			}
		}

		/**
		 * 点击视频
		 * 
		 * @param video
		 * @see IFWebView#NEWS_JS_TO_JAVA_METHOD_VIDEO_CLICK
		 */
		@SuppressWarnings("unused")
		@JavascriptInterface
		public void onClickVideo(final String video) {
			if (mOnJSClick != null) {
				mOnJSClick.onVideoClick(video);
			}
		}
	}

	/**
	 * 加载新闻页面
	 * 
	 * @param data
	 * @param defaultUri
	 * @return
	 */
	public List<HtmlMappingItem> loadNewsHtml(String data, OnJSClick callback) {
		/*
		 * 参数校验，data和默认图uri均不能为空
		 */
		if (TextUtils.isEmpty(data)) {
			throw new IllegalArgumentException("data should not be empty");
		}

		mOnJSClick = callback;
		mImgTagTasks = new LinkedList<HtmlMappingItem>();

		data = changeDip2Px(data);
		data = processHtmlData(data);

		loadDataWithBaseURL(null, data, "text/html", "utf-8", null);

		return mImgTagTasks;
	}

	/**
	 * 加载新的图片
	 * 
	 * @param task
	 * @param newUri
	 */
	public void resetImage(HtmlMappingItem task) {
		this.loadUrl(task.getJSFunction());
	}

	/**
	 * 转换html中的px大小
	 * 
	 * @param data
	 * @return
	 */
	private String changeDip2Px(String data) {
		/*
		 * 正则表达，匹配以空格开头或：开头的，中间夹杂任意数字并以px结尾
		 */
		Pattern pattern = Pattern.compile("(\\s|:)([0-9]+)(px|PX|pX|Px)");
		Matcher m = pattern.matcher(data);
		StringBuffer sb = new StringBuffer(data);
		List<ConvertTask> tasks = new LinkedList<ConvertTask>();

		while (m.find()) {
			/*
			 * 记录下Group 2之中的字符串，为([0-9]+)中内容
			 */
			ConvertTask task = new ConvertTask();
			task.mFontSize = m.group(2);
			task.mStart = m.start(2);
			task.mEnd = m.end(2);
			tasks.add(task);
		}

		/*
		 * 偏移位置，由于替换的字符串可能比原有字符串长度不一，而需要进行后续的偏移计算
		 */
		int adjustLength = 0;
		for (ConvertTask convertTask : tasks) {
			int oldLength = convertTask.mFontSize.length();
			String convertFontSize = convertTask.getNewSize();
			int newLength = convertFontSize.length();

			sb.delete(convertTask.mStart + adjustLength, convertTask.mEnd
					+ adjustLength);
			sb.insert(convertTask.mStart + adjustLength, convertFontSize);
			adjustLength += (newLength - oldLength);
		}

		return sb.toString();
	}

	/**
	 * 向html tag中添加id
	 * 
	 * @param data
	 * @return
	 */
	private String processHtmlData(String data) {
		try {
			Document doc = Jsoup.parse(data);
			Elements es = doc.getElementsByTag("img");
			for (Element e : es) {
				String imgUrl = e.attr("src");
				String md5 = Utility.getMD5(imgUrl);

				HtmlMappingItem mappingItem = null;
				e.attr("name", md5);

				String videoUrl = e.attr("video");
				// 如果为视频链接的话，则响应播放事件
				if (TextUtils.isEmpty(videoUrl)) {
					mappingItem = new ImageHtmlMappingItem();

					String str = "window." + NEWS_JS_TO_JAVA_INTERFACE + "."
							+ NEWS_JS_TO_JAVA_METHOD_IMG_CLICK + "('" + imgUrl
							+ "')";
					e.attr("onclick", str);
					e.attr("src", URI_DEFAULT_IMAGE);
				} else {
					mappingItem = new VideoHtmlMappingItem();

					String str = "window." + NEWS_JS_TO_JAVA_INTERFACE + "."
							+ NEWS_JS_TO_JAVA_METHOD_VIDEO_CLICK + "('"
							+ videoUrl + "')";
					e.attr("onclick", str);
					e.attr("src", URI_IMAGE_VIDEO);
					e.attr("style",
							"background-size:cover;background-repeat:no-repeat;background-image:url("
									+ URI_DEFAULT_VIDEO + ")");
				}

				mappingItem.mId = md5;
				mappingItem.mOrlUrl = imgUrl;
				mImgTagTasks.add(mappingItem);
			}
			return doc.html();
		} catch (Exception e) {
			return data;
		}
	}

	/**
	 * 转换任务
	 * 
	 * @author Calvin
	 * 
	 */
	private class ConvertTask {
		/** 原大小px */
		public String mFontSize;
		/** 起始字符位置 */
		public int mStart;
		/** 终止字符位置 */
		public int mEnd;

		/**
		 * 换算px
		 * 
		 * @return
		 */
		public String getNewSize() {
			try {
				int oldSize = Integer.parseInt(mFontSize);
				int newSize = (int) (oldSize
						* Utility.getScaledDensity(getContext()) + 0.5f);
				return newSize + "";
			} catch (Exception e) {
			}
			return mFontSize;
		}
	}

	/**
	 * 转换html img任务对象
	 * 
	 * @author Calvin
	 * 
	 */
	public abstract class HtmlMappingItem {
		/** 标签id */
		protected String mId;
		/** 原地址 */
		protected String mOrlUrl;
		/** 目标Uri */
		protected String mTargetUri;

		/**
		 * 获取原始url
		 * 
		 * @return
		 */
		public String getOrlUrl() {
			return mOrlUrl;
		}

		/**
		 * 设置下载后的图片文件uri
		 */
		public void setTargetUri(String localImageUri) {
			mTargetUri = localImageUri;
		}

		/**
		 * 获取js函数
		 * 
		 * @return
		 */
		protected abstract String getJSFunction();
	}

	/**
	 * 图片加载任务
	 * 
	 * @author Calvin
	 * 
	 */
	public class ImageHtmlMappingItem extends HtmlMappingItem {

		@Override
		protected String getJSFunction() {
			return "javascript:(function(){"
					+ "var objs = document.getElementsByName(\"" + mId
					+ "\"); " + "for(var i=0;i<objs.length;i++){"
					+ "objs[i].setAttribute(\"src\",\"" + mTargetUri + "\");"
					+ "}" + "})()";
		}
	}

	/**
	 * 视频缩略图加载任务
	 * 
	 * @author Calvin
	 * 
	 */
	public class VideoHtmlMappingItem extends HtmlMappingItem {

		@Override
		protected String getJSFunction() {
			return "javascript:(function(){"
					+ "var objs = document.getElementsByName(\""
					+ mId
					+ "\"); "
					+ "for(var i=0;i<objs.length;i++){"
					+ "objs[i].setAttribute(\"style\",\""
					+ "background-size:cover;background-repeat:no-repeat;background-image:url("
					+ mTargetUri + ")" + "\");" + "}" + "})()";
		}

	}

	/**
	 * JS点击事件的回调
	 * 
	 * @author Calvin
	 * 
	 */
	public interface OnJSClick {
		/**
		 * 图片被点击
		 * 
		 * @param url
		 */
		public void onImageClick(String url);

		/**
		 * 视频被点击
		 * 
		 * @param url
		 */
		public void onVideoClick(String url);
	}
}
