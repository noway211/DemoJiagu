#!/usr/bin/env python
# -*- coding: utf-8 -*-

import os
import sys
import game_conf
import game_repack
import getopt


def usageDoc(argv):
    print "USAGE: {0} -s source  -o outputlib -n name ".format(
        os.path.basename(argv[0]))
    print "-s   --source     source apk"
    print "-o   --output     new apk output dir"
    print "-n   --output     new apk name"



def init(exedir):
    game_conf.KEYSTOREPAHT = exedir + "/" + game_conf.KEYSTOREPAHT
    game_conf.APKTOOL = exedir + "/" + game_conf.APKTOOL



def main(argv):

    options,args = getopt.getopt(sys.argv[1:],"hs:o:n:",["help","source=","output=","name="])
    for name,value in options:
        if name in ("-h","--help"):
            usageDoc(argv)
            game_conf.exit(game_conf.EXIT_SUCCESS, "help")
        elif name in("-s","--source"):
            game_conf.SOURCEAPK= value
            if not os.path.exists(game_conf.SOURCEAPK):
                print "source {0} is not exist".format(game_conf.SOURCEAPK)
                game_conf.exit(game_conf.EXIT_ARGUMENT_ERR, "source file not exist")
        elif name in("-o","--output"):
            game_conf.OUTPUTDIR = value
            if not os.path.exists(game_conf.OUTPUTDIR):
                os.makedirs(game_conf.OUTPUTDIR)
        elif name in("-n","--name"):
            game_conf.OUTPUTNAME = value
            if not value.endswith(".apk"):
                game_conf.exit(game_conf.EXIT_ARGUMENT_ERR, "output file name is not end with \"apk\"")


    if game_conf.SOURCEAPK == '' or game_conf.OUTPUTDIR == '':
        game_conf.exit(game_conf.EXIT_ARGUMENT_ERR, "params error  look  --help")

    init(os.path.dirname(argv[0]))
    game_repack.repackApk(game_conf.SOURCEAPK)

    print '''End successful.'''
    game_conf.exit(game_conf.EXIT_SUCCESS)


if __name__ == '__main__':
    try:
        main(sys.argv)
    except:
        print("""An internal error occured.""" )
        raise
