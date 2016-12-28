# DemoJiagu apk 加固。

简单的加固dex的demo，支持多dex。 采用 android +jar +python。
将原始的apk的dex隐藏在壳的dex中，在app初始化时，解密dex，采用新的dexclassload替换系统的pathclassloader
