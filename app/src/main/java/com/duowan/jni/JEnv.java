package com.duowan.jni;

public class JEnv {

    public static native String getCryptSeed();
	static{
		System.loadLibrary("jenv");
	}
}
