#include <jni.h>
#include <cstdio>
#include <cerrno>
#include <string>

void throwErrnoException(JNIEnv *env, const char *functionName, int err) {
    jclass exClass = env->FindClass("android/system/ErrnoException");
    if (exClass == nullptr) return;

    jmethodID ctor = env->GetMethodID(exClass, "<init>", "(Ljava/lang/String;I)V");
    if (ctor == nullptr) return;

    jstring jFunctionName = env->NewStringUTF(functionName);
    jobject exception = env->NewObject(exClass, ctor, jFunctionName, err);

    env->Throw((jthrowable)exception);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_rosan_installer_Natives_deleteFile(JNIEnv *env, jobject thiz, jstring file_path) {
    if (file_path == nullptr) {
        throwErrnoException(env, "deleteFile", EINVAL);
        return;
    }

    const char *path = env->GetStringUTFChars(file_path, nullptr);

    int result = remove(path);

    env->ReleaseStringUTFChars(file_path, path);

    if (result != 0) {
        throwErrnoException(env, "deleteFile", errno);
        return;
    }
}