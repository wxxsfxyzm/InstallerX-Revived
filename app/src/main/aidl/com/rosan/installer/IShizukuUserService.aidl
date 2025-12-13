package com.rosan.installer;

import com.rosan.installer.IPrivilegedService;

interface IShizukuUserService {
    oneway void destroy() = 16777114;

    IPrivilegedService getPrivilegedService() = 1;
}
