package com.ifeng.util.net.requestor;

import com.ifeng.util.model.IModelErrorCode;

/**
 * 接口数据错误列表，其中包含内部错误和服务器返回错误
 * 
 * @author xuwei
 * 
 */
public interface IRequestModelErrorCode extends IModelErrorCode {

	/** 没有请求的Url地址 2001 */
	public static final int ERROR_CODE_NO_URL = 2001;

	/** 网络访问错误 2002 */
	public static final int ERROR_CODE_NET_FAILED = 2002;

	/** 服务器返回错误 2003 */
	public static final int ERROR_CODE_SERVICE_ERROR = 2003;
}
