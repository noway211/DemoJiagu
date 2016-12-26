#!/usr/bin/env python
# -*- coding: utf-8 -*-

import subprocess

def run_shell_cmd(command):
    print "command==========="+command
    p = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    output = p.stdout.read()
    p.wait()
    errout = p.stderr.read()
    p.stdout.close()
    p.stderr.close()
    return (output.strip(), errout.strip())


#输出不要管道数据
def run_shell_cmd_nopipe(command):
    print "command==========="+command
    p = subprocess.Popen(command, shell=True, stdout=None, stderr=None)
    p.wait()
    return "",""

def shell_run_ok(command):
    output,errput =run_shell_cmd(command)
    if (errput != "") or (output.lower().find("error") >= 0):
        print output
        print errput
        return False
    return True
    pass

def nopipe_shell_run_ok(command):
    output,errput =run_shell_cmd_nopipe(command)
    if (errput != "") or (output.lower().find("error") >= 0):
        print output
        print errput
        return False
    return True
    pass