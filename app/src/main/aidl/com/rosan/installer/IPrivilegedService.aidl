package com.rosan.installer;

import android.content.ComponentName;

interface IPrivilegedService {
    void delete(in String[] paths);

    void setDefaultInstaller(in ComponentName component, boolean enable);

    /**
     * 执行命令
     */
    String execLine(String command);

    /**
     * 执行数组中分离的命令
     */
    String execArr(in String[] command);
}
