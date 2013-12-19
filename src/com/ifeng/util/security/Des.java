package com.ifeng.util.security;

import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;

public class Des {

	private final static byte[] KEY_64 = "741f5093".getBytes();

	private final static byte[] IV_64 = "cb4a8c85".getBytes();

	public static String encrypt(String uid, String time) {
		if (uid == null || uid.equals(""))
			return null;
		String newUid = uid;

		for (int i = 0; i < (8 - uid.length() % 8); i++) {
			newUid += "8";
		}

		String str = newUid + time;
		char[] array = str.toCharArray();
		int len = array.length;
		StringBuilder sb = new StringBuilder(len);
		int i = 0;
		while (i < len) {
			sb.append(array[i + 1]);
			sb.append(array[i]);
			i += 2;
		}
		str = sb.toString();

		try {
			String code = encode(str);
			return code;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String encode(String data) throws Exception {
		DESKeySpec keySpec = new DESKeySpec(KEY_64);
		AlgorithmParameterSpec iv = new IvParameterSpec(IV_64);
		SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
		SecretKey key = keyFactory.generateSecret(keySpec);
		Cipher enCipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
		enCipher.init(Cipher.ENCRYPT_MODE, key, iv);
		byte[] pasByte = enCipher.doFinal(data.getBytes("utf-8"));
		return new String(Base64.encode(pasByte));
	}

	public static boolean validate(String code, String time, String uid) {
		if (code == null || Des.encrypt(uid, time) == null)
			return true;
		code = code.replace(' ', '+');
		return (encrypt(uid, time)).contains(code);
	}
}
