package com.ifeng.util.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * 默认为获取到焦点的textview，用于跑马灯
 * 
 * @author Calvin
 * 
 */
public class FocusTextView extends TextView {

	public FocusTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public boolean isFocused() {
		return true;
	}

}
