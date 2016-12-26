#!/usr/bin/env python
# -*- coding: utf-8 -*-

import xml.etree.ElementTree as ET
import game_conf
import os



#修改包名,修改文件名称
def modifypackage(file):
    doc=ET.parse(file)
    ET.register_namespace("android","http://schemas.android.com/apk/res/android")
    ET.register_namespace("tools","http://schemas.android.com/tools")
    root = doc.getroot()
    for elem in root.iterfind('application'):
        sourceapp =elem.attrib['{http://schemas.android.com/apk/res/android}name'];
        elem.set("{http://schemas.android.com/apk/res/android}name","com.peter.example.petershell.StudApplication")
        if sourceapp != '':
            newelement = ET.Element('meta-data', {"{http://schemas.android.com/apk/res/android}name":"APPLICATION_CLASS_NAME","{http://schemas.android.com/apk/res/android}value":sourceapp})
            elem.append(newelement)


    doc.write(file, encoding="utf-8",xml_declaration=True)





if __name__=="__main__":
    modifypackage("D:\\test\\AndroidManifest.xml")
    # dict = menifest("D:\\test\\AndroidManifest.xml")
    # print  dict
