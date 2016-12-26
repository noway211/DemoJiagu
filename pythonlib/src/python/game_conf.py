#!/usr/bin/env python
# -*- coding: utf-8 -*-
import sys
import os
import os.path
import tempfile


def _isWindows():
    return sys.platform == 'win32'

def _isLinux():
    return sys.platform.startswith('linux')

def _is_mac():
    return sys.platform == 'darwin'


if _isWindows():
    JARSIGNERPATH = u'jarsigner'
    APKTOOL = u'apktool.bat'
    PETERSHELL = u'petershell.bat'
elif _isLinux():
    JARSIGNERPATH = u'$JAVA_HOME/bin/jarsigner'
    APKTOOL = u'apktool'
    PETERSHELL = u'petershell'

elif _is_mac():
    JARSIGNERPATH = u'$JAVA_HOME/bin/jarsigner'
    APKTOOL = u'apktool'
    PETERSHELL = u'petershell'

CACHE_DIR = tempfile.gettempdir()
ASSETSDIR = u'assets'
RESDIR = u'res'
JNIDIR = u'jni'
LIBDIR = u'lib'
AIDLDIR = u'aidl'
KEYSTOREPAHT = u'duowan.keystore'
KEYSTOREALIAS = u'duowan.keystore'
STOREPASS = u'duowan123'
SOURCEAPK =""
OUTPUTDIR = ""
OUTPUTNAME =""
CURDIR = os.getcwd()


###################Exit code ########################
#exit success
EXIT_SUCCESS		= 0
#unknown error
EXIT_UNKNOWN		= 1
#argument error
EXIT_ARGUMENT_ERR	= 2
#fail to copy apk to cache
EXIT_COPYING_ERR	= 3
#fail to delete signed files from apk
EXIT_UPZIP_LIB_ERR	= 4
#fail to repackage apk
EXIT_REPACKAGE_ERR	= 5
#fail to DECODE_APK
EXIT_DECODE_APK_ERR= 6
#fail to sign a apk
EXIT_SIGNAPK_ERR	= 7
#fail to sign a apk
EXIT_DX_ERR	= 8
#fail to sign a apk
EXIT_SMALI_ERR	= 9

#fail to DECODE_APK
EXIT_CREATE_DEX_ERR= 10



def exit(rcode=EXIT_SUCCESS,msg=None):
    global CURDIR
    os.chdir(CURDIR)
    if msg != None :
        print msg
    sys.exit(rcode)


