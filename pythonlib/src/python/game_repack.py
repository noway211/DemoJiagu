#!/usr/bin/env python
# -*- coding: utf-8 -*-

import game_conf
import os
import shutil
import time
import game_cmd
import game_menifest_op

rpaklib = os.path.abspath(game_conf.CACHE_DIR + "/repack")


def del_file(filepath):
    if os.path.isdir(filepath):
        shutil.rmtree(filepath)
    elif os.path.isfile(filepath):
        os.remove(filepath)



def cleanEnv():
    del_file(rpaklib+'/'+"source.apk")
    del_file(rpaklib+'/'+"new.apk")
    del_file(rpaklib+'/'+"source")
    del_file(rpaklib+'/'+"classes.dex")

def repackApk(inputApk):
    print "sourceapk==="+inputApk
    #临时目录创建
    if os.path.exists(rpaklib) == False:
        os.makedirs(rpaklib)

    #拷贝文件到临时文件夹
    try:
        cacheApkFile = rpaklib+'/'+"source.apk"
        del_file(cacheApkFile)
        shutil.copy(inputApk, cacheApkFile)
    except:
        game_conf.exit(game_conf.EXIT_COPYING_ERR, "copy apk to temp dir fail")


    #删除反编译的目录
    temp_apk_recompile_dir = rpaklib+"/source"
    del_file(temp_apk_recompile_dir)

    #反编译apk
    if game_cmd.run_shell_cmd_nopipe(game_conf.APKTOOL+ " d "+cacheApkFile + " -o "+temp_apk_recompile_dir) == False:
        game_conf.exit(game_conf.EXIT_DECODE_APK_ERR, "decompile apk fail")

    print "begin create new dex"
    #合并新的dex
    if game_cmd.shell_run_ok(game_conf.PETERSHELL+" -s "+cacheApkFile+" -o "+rpaklib) == False:
        game_conf.exit(game_conf.EXIT_CREATE_DEX_ERR,"create new dex fail")


    #删除原工程的无用文件
    file_array = os.listdir(temp_apk_recompile_dir)
    for dex_file in file_array:
        if dex_file.startswith('smali'):
            del_file(temp_apk_recompile_dir+"/"+dex_file);
    # if os.path.exists(temp_apk_recompile_dir+"/smali"):
    #     del_file(temp_apk_recompile_dir+"/smali")
    # if os.path.exists(temp_apk_recompile_dir+"/assets"):
    #     del_file(temp_apk_recompile_dir+"/assets")
    # if os.path.exists(temp_apk_recompile_dir+"/lib"):
    #     del_file(temp_apk_recompile_dir+"/lib")

    #拷贝新的dex到编译目录
    shutil.copy(rpaklib+"/classes.dex", temp_apk_recompile_dir+"/classes.dex")


    #修改androidmenifest
    game_menifest_op.modifypackage(temp_apk_recompile_dir+"/AndroidManifest.xml")



    if game_cmd.run_shell_cmd_nopipe(game_conf.APKTOOL+ " b "+temp_apk_recompile_dir + " -o "+rpaklib+ "/new.apk") == False:
        game_conf.exit(game_conf.EXIT_REPACKAGE_ERR, "apk repack fail")

    #签名
    #jarsigner -sigalg MD5withRSA -digestalg SHA1 -keystore ./duowan.keystore -storepass duowan123 %rpaklib%\new.apk duowan.keystore
    command = "{0} -sigalg MD5withRSA -digestalg SHA1 -keystore {1} -storepass {2} {3} {4}".format(game_conf.JARSIGNERPATH, game_conf.KEYSTOREPAHT, game_conf.STOREPASS, rpaklib + "/new.apk", game_conf.KEYSTOREALIAS)
    if game_cmd.shell_run_ok(command) == False:
        cleanEnv()
        game_conf.exit(game_conf.EXIT_SIGNAPK_ERR, "jarsigner apk fail")

    #添加新new到目标目录
    if game_conf.OUTPUTNAME == '':
        os.rename(rpaklib +"/new.apk", game_conf.OUTPUTDIR + "/repack_" + str(time.time()) + ".apk")
    else:
        if os.path.exists(os.path.join(game_conf.OUTPUTDIR,game_conf.OUTPUTNAME)):
            os.remove(os.path.join(game_conf.OUTPUTDIR,game_conf.OUTPUTNAME))
        os.rename(rpaklib +"/new.apk", os.path.join(game_conf.OUTPUTDIR,game_conf.OUTPUTNAME))
    #清除目录
    cleanEnv()

