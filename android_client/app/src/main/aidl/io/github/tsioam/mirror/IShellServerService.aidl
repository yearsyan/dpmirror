// IShellServerService.aidl
package io.github.tsioam.mirror;

// Declare any non-default types here with import statements

interface IShellServerService {
    void destroy() = 16777114;

    void exit() = 1;

    void launchServer(String apkPath,String ldPath,String wsString, String ice) = 2;

    String test() = 3;
}