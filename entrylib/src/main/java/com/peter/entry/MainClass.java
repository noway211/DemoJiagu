package com.peter.entry;

import com.sun.nio.zipfs.ZipFileStore;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.Map;
import java.util.zip.Adler32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class MainClass {


    public static void main(String[] args) {

        Map<String,String> paramg = ParamOpt.parseParam(args);
        String curdir = System.getProperty("user.dir");
        if(paramg.containsKey("")){
            System.out.println("cmd like this:  java -jar xxxx.jar -s file.apk -o dir");
            return ;
        }

        String sourceapk = paramg.get("s");
        String desDir = paramg.get("o");

        try {
            saveSourceDexFile(sourceapk,desDir);
            InputStream dexFileStream = Class.class.getResourceAsStream("/source/petershell.dex");
            if(dexFileStream == null){
                throw new Exception("shell dex file is not find");
            }
            byte[] payloadArray = encrpt(readFileBytes(new File(desDir,"/tmp/classes")));//以二进制形式读出dex，并进行加密处理//对源Apk进行加密操作
            System.out.println("datlength:"+payloadArray.length);
            byte[] unShellDexArray = readDexfileBytes(dexFileStream);//以二进制形式读出dex
            dexFileStream.close();
            int payloadLen = payloadArray.length;
            int unShellDexLen = unShellDexArray.length;
            int totalLen = payloadLen + unShellDexLen + 4;//多出4字节是存放长度的。
            byte[] newdex = new byte[totalLen]; // 申请了新的长度
            //添加解壳代码
            System.arraycopy(unShellDexArray, 0, newdex, 0, unShellDexLen);//先拷贝dex内容
            //添加加密后的解壳数据
            System.arraycopy(payloadArray, 0, newdex, unShellDexLen, payloadLen);//再在dex内容后面拷贝apk的内容
            //添加解壳数据长度
            System.arraycopy(intToByte(payloadLen), 0, newdex, totalLen - 4, 4);//最后4为长度
            //修改DEX file size文件头,
            fixFileSizeHeader(newdex);
            //修改DEX SHA1 文件头
            fixSHA1Header(newdex);
            //修改DEX CheckSum文件头
            fixCheckSumHeader(newdex);

            String str =desDir+"/classes.dex";
            File file = new File(str);
            File parentfile = file.getParentFile();
            if(!parentfile.exists()){
                parentfile.mkdirs();
            }
            FileOutputStream localFileOutputStream = new FileOutputStream(file);
            localFileOutputStream.write(newdex);
            localFileOutputStream.flush();
            localFileOutputStream.close();
            deleteFile(new File(desDir,"tmp"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void deleteFile(File file){
        if(file.isFile()){//表示该文件不是文件夹
            file.delete();
            System.out.println("删除delet file"+file.getPath());
        }else{
            //首先得到当前的路径
            String[] childFilePaths = file.list();
            for(String childFilePath : childFilePaths){
                File childFile=new File(file.getAbsolutePath()+"\\"+childFilePath);
                deleteFile(childFile);
            }
            file.delete();
        }
    }
    //直接返回数据，读者可以添加自己加密方法
    private static byte[] encrpt(byte[] srcdata) {
        for (int i = 0; i < srcdata.length; i++) {
            srcdata[i] = (byte) (0xFF ^ srcdata[i]);
        }
        return srcdata;
    }



    private static void saveSourceDexFile(String sourcefilepath,String outputDir) {
        ZipInputStream localZipInputStream = null;
        try {
            localZipInputStream = new ZipInputStream(
                    new BufferedInputStream(new FileInputStream(new File(sourcefilepath))));
            while (true){
                ZipEntry localZipEntry = localZipInputStream.getNextEntry();
                if (localZipEntry == null) {
                    localZipInputStream.close();
                    break;
                }
                String name = localZipEntry.getName();
                if(name.startsWith("classes")&&name.endsWith(".dex")){
                    File storeFile = new File(outputDir + "/tmp/"
                            + name);
                    File pfile = storeFile.getParentFile();
                    if(!pfile.exists()){
                        pfile.mkdirs();
                    }
                    storeFile.createNewFile();
                    FileOutputStream fos = new FileOutputStream(storeFile);
                    byte[] arrayOfByte = new byte[2048];
                    while (true) {
                        int i = localZipInputStream.read(arrayOfByte);
                        if (i == -1)
                            break;
                        fos.write(arrayOfByte, 0, i);
                    }
                    fos.flush();
                    fos.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                localZipInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        File file = new File(outputDir,"tmp");
        File[] fileslist = file.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if(name.endsWith(".dex")){
                 return  true;
                }
                return false;
            }
        });
        if(file != null && fileslist.length > 0){
            try {
                File mulDexFile = new File(outputDir,"/tmp/classes");
                RandomAccessFile savedFile = new RandomAccessFile(mulDexFile, "rw");
                savedFile.seek(0);
                savedFile.writeByte(fileslist.length);
                for(File subfile:fileslist){
                    writeFile(savedFile,subfile);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
            }
        }


    }

    private static  void writeFile(RandomAccessFile accessFile, File file){
        try {
            FileInputStream fis = new FileInputStream(file);
            accessFile.writeInt(fis.available());
            byte[] tempb = new byte[2048];
            int length = 0;
            while((length = fis.read(tempb)) >0){
                accessFile.write(tempb,0,length);
            }
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

        }


    }
    /**
     * 修改dex头，CheckSum 校验码
     *
     * @param dexBytes
     */
    private static void fixCheckSumHeader(byte[] dexBytes) {
        Adler32 adler = new Adler32();
        adler.update(dexBytes, 12, dexBytes.length - 12);//从12到文件末尾计算校验码
        long value = adler.getValue();
        int va = (int) value;
        byte[] newcs = intToByte(va);
        //高位在前，低位在前掉个个
        byte[] recs = new byte[4];
        for (int i = 0; i < 4; i++) {
            recs[i] = newcs[newcs.length - 1 - i];
            System.out.println(Integer.toHexString(newcs[i]));
        }
        System.arraycopy(recs, 0, dexBytes, 8, 4);//效验码赋值（8-11）
        System.out.println(Long.toHexString(value));
        System.out.println();
    }


    /**
     * int 转byte[]
     *
     * @param number
     * @return
     */
    public static byte[] intToByte(int number) {
        byte[] b = new byte[4];
        for (int i = 3; i >= 0; i--) {
            b[i] = (byte) (number % 256);
            number >>= 8;
        }
        return b;
    }

    /**
     * 修改dex头 sha1值
     *
     * @param dexBytes
     * @throws NoSuchAlgorithmException
     */
    private static void fixSHA1Header(byte[] dexBytes)
            throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(dexBytes, 32, dexBytes.length - 32);//从32为到结束计算sha--1
        byte[] newdt = md.digest();
        System.arraycopy(newdt, 0, dexBytes, 12, 20);//修改sha-1值（12-31）
        //输出sha-1值，可有可无
        String hexstr = "";
        for (int i = 0; i < newdt.length; i++) {
            hexstr += Integer.toString((newdt[i] & 0xff) + 0x100, 16)
                    .substring(1);
        }
        System.out.println(hexstr);
    }

    /**
     * 修改dex头 file_size值
     *
     * @param dexBytes
     */
    private static void fixFileSizeHeader(byte[] dexBytes) {
        //新文件长度
        byte[] newfs = intToByte(dexBytes.length);
        System.out.println(Integer.toHexString(dexBytes.length));
        byte[] refs = new byte[4];
        //高位在前，低位在前掉个个
        for (int i = 0; i < 4; i++) {
            refs[i] = newfs[newfs.length - 1 - i];
            System.out.println(Integer.toHexString(newfs[i]));
        }
        System.arraycopy(refs, 0, dexBytes, 32, 4);//修改（32-35）
    }


    /**
     * 以二进制读出文件内容
     *
     * @param file
     * @return
     * @throws IOException
     */
    private static byte[] readFileBytes(File file) throws IOException {
        byte[] arrayOfByte = new byte[1024];
        ByteArrayOutputStream localByteArrayOutputStream = new ByteArrayOutputStream();
        FileInputStream fis = new FileInputStream(file);
        while (true) {
            int i = fis.read(arrayOfByte);
            if (i != -1) {
                localByteArrayOutputStream.write(arrayOfByte, 0, i);
            } else {
                fis.close();
                return localByteArrayOutputStream.toByteArray();
            }
        }
    }

    private static byte[] readDexfileBytes(InputStream fis) throws IOException {
        byte[] arrayOfByte = new byte[1024];
        ByteArrayOutputStream localByteArrayOutputStream = new ByteArrayOutputStream();
        //  FileInputStream fis = new FileInputStream(file);
        while (true) {
            int i = fis.read(arrayOfByte);
            if (i != -1) {
                localByteArrayOutputStream.write(arrayOfByte, 0, i);
            } else {
                return localByteArrayOutputStream.toByteArray();
            }
        }
    }
}
