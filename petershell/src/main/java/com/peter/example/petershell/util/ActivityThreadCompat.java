package com.peter.example.petershell.util;

import java.lang.reflect.InvocationTargetException;

public class ActivityThreadCompat {

    private static Object sActivityThread;

    private static Class sClass;

    public synchronized static final Object instance() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (sActivityThread == null) {
            sActivityThread = RefInvoke.invokeStaticMethod("android.app.ActivityThread", "currentActivityThread", new Class[] {}, new Object[] {});
        }
        return sActivityThread;
    }


}
