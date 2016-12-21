package com.peter.example.petershell;

import android.app.Application;
import android.content.ContentProvider;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.Log;

import com.peter.example.petershell.util.ActivityThreadCompat;
import com.peter.example.petershell.util.Reflect;
import com.peter.example.petershell.util.ReflectException;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import dalvik.system.DexClassLoader;

/**
 * Created by zhukui on 2016/12/19.
 */

public class StudApplication extends Application {

    private static final String appkey = "APPLICATION_CLASS_NAME";
    private String apkFileName;
    private String odexPath;
    private String libPath;

    //这是context 赋值
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        try {
            //创建两个文件夹payload_odex，payload_lib 私有的，可写的文件目录
            File odex = this.getDir("source_odex", MODE_PRIVATE);
            File libs = this.getDir("source_lib", MODE_PRIVATE);
            odexPath = odex.getAbsolutePath();
            libPath = libs.getAbsolutePath();
            apkFileName = odex.getAbsolutePath() + "/source.apk";
            File dexFile = new File(apkFileName);
            if (!dexFile.exists())
            {
                dexFile.createNewFile();  //在payload_odex文件夹内，创建payload.apk
                // 读取程序classes.dex文件
                byte[] dexdata = this.readDexFileFromApk();

                // 分离出解壳后的apk文件已用于动态加载
                this.splitPayLoadFromDex(dexdata);
            }
            // 配置动态加载环境
            Object currentActivityThread = ActivityThreadCompat.instance();
            String packageName = this.getPackageName();//当前apk的包名
            //下面两句不是太理解
            ArrayMap mPackages = Reflect.on(currentActivityThread).field("mPackages").get();
            WeakReference wr = (WeakReference) mPackages.get(packageName);
//            DexClassLoader dLoader = new DexClassLoader(apkFileName, odexPath,
//                    libPath,getClassLoader());
            //设置父classload为systemclassload 这个与壳classload完全隔离
            DexClassLoader dLoader = new DexClassLoader(apkFileName, odexPath,
                    libPath, ClassLoader.getSystemClassLoader());
            Reflect.on(wr.get()).set("mClassLoader",dLoader);
            Log.i("peterLog","classloader:"+dLoader);

            try{
                Object actObj = dLoader.loadClass("com.peter.example.petershell.MainActivity");
                Log.i("peterLog", "actObj:"+actObj);
            }catch(Exception e){
                Log.i("peterLog", "activity:"+Log.getStackTraceString(e));
            }


        } catch (Exception e) {
            Log.i("peterLog", "error:"+Log.getStackTraceString(e));
            e.printStackTrace();
        }

    }

    @Override
    public void onCreate() {
        {
            loadResources(apkFileName);

            Log.i("peterLog", "onCreate");
            // 如果源应用配置有Appliction对象，则替换为源应用Applicaiton，以便不影响源程序逻辑。
            String appClassName = null;
            try {
                ApplicationInfo ai = this.getPackageManager()
                        .getApplicationInfo(this.getPackageName(),
                                PackageManager.GET_META_DATA);
                Bundle bundle = ai.metaData;
                if (bundle != null && bundle.containsKey("APPLICATION_CLASS_NAME")) {
                    appClassName = bundle.getString("APPLICATION_CLASS_NAME");//className 是配置在xml文件中的。
                } else {
                    Log.i("peterLog", "have no application class name");
                    return;
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.i("peterLog", "error:"+Log.getStackTraceString(e));
                e.printStackTrace();
            }
            //有值的话调用该Applicaiton

            Object currentActivityThread = null;
            try {
                currentActivityThread = ActivityThreadCompat.instance();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Object mBoundApplication = Reflect.on(currentActivityThread).field("mBoundApplication").get();
            Object loadedApkInfo = Reflect.on(mBoundApplication).field("info").get();
            Log.d("peterLog",loadedApkInfo.getClass().toString());
            //把当前进程的mApplication 设置成了null
            Reflect.on(loadedApkInfo).set("mApplication",null);
            Object oldApplication = Reflect.on(currentActivityThread).field("mInitialApplication").get();
            ArrayList<Application> mAllApplications =Reflect.on(currentActivityThread).field("mAllApplications").get();
            mAllApplications.remove(oldApplication);//删除oldApplication
            ApplicationInfo appinfo_In_LoadedApk = Reflect.on(loadedApkInfo).field("mApplicationInfo").get();
            ApplicationInfo appinfo_In_AppBindData = Reflect.on(mBoundApplication).field("appInfo").get();
            appinfo_In_LoadedApk.className = appClassName;
            appinfo_In_AppBindData.className = appClassName;

            Application app = Reflect.on(loadedApkInfo).call("makeApplication",false,null).get();

            Reflect.on(currentActivityThread).set("mInitialApplication",app);

            ArrayMap mProviderMap = Reflect.on(currentActivityThread).field("mProviderMap").get();
            Iterator it = mProviderMap.keySet().iterator();
            while (it.hasNext()) {
                Object providerClientRecord = it.next();
                Log.d("peterLog",providerClientRecord.toString());
                try {
                    ContentProvider contentProvider = Reflect.on(providerClientRecord).field("mLocalProvider").get();
                    Reflect.on(contentProvider).set("mContext",app);
                } catch (ReflectException e) {
                    e.printStackTrace();
                }

            }
            Log.i("peterLog", "app:"+app);
            app.onCreate();
        }
    }

    /**
     * 释放被加壳的apk文件，so文件
     * @param apkdata
     */
    private void splitPayLoadFromDex(byte[] apkdata) throws IOException {
        int ablen = apkdata.length;
        //取被加壳apk的长度   这里的长度取值，对应加壳时长度的赋值都可以做些简化
        byte[] dexlen = new byte[4];
        System.arraycopy(apkdata, ablen - 4, dexlen, 0, 4);
        ByteArrayInputStream bais = new ByteArrayInputStream(dexlen);
        DataInputStream in = new DataInputStream(bais);
        int readInt = in.readInt();
        System.out.println(Integer.toHexString(readInt));
        byte[] newdex = new byte[readInt];
        //把被加壳apk内容拷贝到newdex中
        System.arraycopy(apkdata, ablen - 4 - readInt, newdex, 0, readInt);
        //这里应该加上对于apk的解密操作，若加壳是加密处理的话
        //?

        //对源程序Apk进行解密
        newdex = decrypt(newdex);

        //写入apk文件
        File file = new File(apkFileName);
        try {
            FileOutputStream localFileOutputStream = new FileOutputStream(file);
            localFileOutputStream.write(newdex);
            localFileOutputStream.close();
        } catch (IOException localIOException) {
            throw new RuntimeException(localIOException);
        }

        //分析被加壳的apk文件
        ZipInputStream localZipInputStream = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(file)));
        while (true) {
            ZipEntry localZipEntry = localZipInputStream.getNextEntry();//不了解这个是否也遍历子目录，看样子应该是遍历的
            if (localZipEntry == null) {
                localZipInputStream.close();
                break;
            }
            //取出被加壳apk用到的so文件，放到 libPath中（data/data/包名/payload_lib)
            String name = localZipEntry.getName();
            if (name.startsWith("lib/") && name.endsWith(".so")) {
                File storeFile = new File(libPath + "/"
                        + name.substring(name.lastIndexOf('/')));
                storeFile.createNewFile();
                FileOutputStream fos = new FileOutputStream(storeFile);
                byte[] arrayOfByte = new byte[1024];
                while (true) {
                    int i = localZipInputStream.read(arrayOfByte);
                    if (i == -1)
                        break;
                    fos.write(arrayOfByte, 0, i);
                }
                fos.flush();
                fos.close();
            }
            localZipInputStream.closeEntry();
        }
        localZipInputStream.close();


    }

    /**
     * 从apk包里面获取dex文件内容（byte）
     * @return
     * @throws IOException
     */
    private byte[] readDexFileFromApk() throws IOException {
        ByteArrayOutputStream dexByteArrayOutputStream = new ByteArrayOutputStream();
        ZipInputStream localZipInputStream = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(
                        this.getApplicationInfo().sourceDir)));
        while (true) {
            ZipEntry localZipEntry = localZipInputStream.getNextEntry();
            if (localZipEntry == null) {
                localZipInputStream.close();
                break;
            }
            if (localZipEntry.getName().equals("classes.dex")) {
                byte[] arrayOfByte = new byte[1024];
                while (true) {
                    int i = localZipInputStream.read(arrayOfByte);
                    if (i == -1)
                        break;
                    dexByteArrayOutputStream.write(arrayOfByte, 0, i);
                }
            }
            localZipInputStream.closeEntry();
        }
        localZipInputStream.close();
        return dexByteArrayOutputStream.toByteArray();
    }


    // //直接返回数据，读者可以添加自己解密方法
    private byte[] decrypt(byte[] srcdata) {
        for(int i=0;i<srcdata.length;i++){
            srcdata[i] = (byte)(0xFF ^ srcdata[i]);
        }
        return srcdata;
    }

    protected void loadResources(String dexPath) {
        AssetManager assetManager = null;
        try {
            assetManager = AssetManager.class.newInstance();
            Reflect.on(assetManager).call("addAssetPath",dexPath);
            setAPKResources(assetManager);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private void setAPKResources(AssetManager newAssetManager) throws  Exception{
        Reflect.on(newAssetManager).call("ensureStringBlocks");
        Collection<WeakReference<Resources>> references = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Object resourcesManager = Reflect.on("android.app.ResourcesManager").call("getInstance").get();
            try {
                ArrayMap<?, WeakReference<Resources>> arrayMap = Reflect.on(resourcesManager).field("mActiveResources").get();
                references = arrayMap.values();
            } catch (ReflectException e) {
                e.printStackTrace();
                references = (Collection) Reflect.on(resourcesManager).field("mResourceReferences").get();
            }
        } else {
            try {
                HashMap<?, WeakReference<Resources>> map = Reflect.on(ActivityThreadCompat.instance()).field("mActiveResources").get();
                references = map.values();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        for (WeakReference<Resources> wr : references) {
            Resources resources = wr.get();
            if (resources == null) continue;

            try {
                Reflect.on(resources).set("mAssets", newAssetManager);
            } catch (Throwable ignore) {
                Reflect.on(resources).field("mResourcesImpl").set("mAssets", newAssetManager);
            }

            resources.updateConfiguration(resources.getConfiguration(), resources.getDisplayMetrics());
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            for (WeakReference<Resources> wr : references) {
                Resources resources = wr.get();
                if (resources == null) continue;

                // android.util.Pools$SynchronizedPool<TypedArray>
                Object typedArrayPool = Reflect.on(resources).field("mTypedArrayPool").get();
                // Clear all the pools
                while(Reflect.on(typedArrayPool).call("acquire").get() != null);
            }
        }
    }



}
