package com.ifeng;

/**
 * 需要子类重写父类方法，用于提示的异常
 * 
 * @author Calvin
 * 
 */
public class NeedOverrideException extends RuntimeException {

	private static final long serialVersionUID = -5969876713354283460L;

	public NeedOverrideException(String string) {
		super(string);
	}

}
