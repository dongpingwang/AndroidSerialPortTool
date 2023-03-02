#include <jni.h>
#include <string>
#include <fcntl.h>
#include "utils.h"

static const char *LOG_TAG = "SerialServiceJNI";

extern "C" JNIEXPORT jobject JNICALL
Java_com_wdp_serial_SerialService_native_1open(JNIEnv *env, jobject thiz, jstring path) {

    const char *pathStr = env->GetStringUTFChars(path, nullptr);
    int fd = open(pathStr, O_RDWR | O_NOCTTY);
    if (fd < 0) {
        LOGD(LOG_TAG, "could not open %s", pathStr);
        env->ReleaseStringUTFChars(path, pathStr);
        return nullptr;
    }
    LOGD(LOG_TAG, "open() fd = %d", fd);
    env->ReleaseStringUTFChars(path, pathStr);

    jclass cFd = env->FindClass("java/io/FileDescriptor");
    jmethodID cFdInit = env->GetMethodID(cFd, "<init>", "(I)V");
    jobject fleDescriptor = env->NewObject(cFd, cFdInit, fd);

    jclass cPfd = env->FindClass("android/os/ParcelFileDescriptor");
    jmethodID mPfdInit = env->GetMethodID(cPfd, "<init>", "(Ljava/io/FileDescriptor;)V");
    jobject parcelFileDescriptor = env->NewObject(cPfd, mPfdInit, fleDescriptor);
    return parcelFileDescriptor;
}