#include <jni.h>
#include <string>
#include <termios.h>
#include <fcntl.h>
#include "utils.h"
#include <unistd.h>

static const char *LOG_TAG = "SerialPortJNI";

static jfieldID field_context;

extern "C"
JNIEXPORT void JNICALL
Java_com_wdp_serial_SerialPort_native_1open(JNIEnv *env, jobject thiz, jstring path,
                                            jint baudrate, int flag) {

    const char *pathStr = env->GetStringUTFChars(path, nullptr);
    int fd = open(pathStr, O_RDWR | O_NOCTTY | flag);
    if (fd < 0) {
        LOGD(LOG_TAG, "could not open %s", pathStr);
        env->ReleaseStringUTFChars(path, pathStr);
        return;
    }
    LOGD(LOG_TAG, "open() fd = %d", fd);
    env->ReleaseStringUTFChars(path, pathStr);

    speed_t speed = getSpeed(baudrate);
    if (speed < 0) {
        jniThrowException(env, "java/lang/IllegalArgumentException",
                          "Unsupported serial port speed");
        return;
    }
    fd = fcntl(fd, F_DUPFD_CLOEXEC, 0);
    if (fd < 0) {
        jniThrowException(env, "java/io/IOException", "Could not open serial port");
        return;
    }
    field_context = env->GetFieldID(env->FindClass("com/wdp/serial/SerialPort"), "mNativeContext",
                                    "I");
    env->SetIntField(thiz, field_context, fd);

    struct termios tio{};
    if (tcgetattr(fd, &tio)) {
        memset(&tio, 0, sizeof(tio));
    }
    tio.c_cflag = speed | CS8 | CLOCAL | CREAD;
    tio.c_oflag &= ~OPOST;
    tio.c_iflag = IGNPAR;
    tio.c_lflag = 0;
    tio.c_cc[VTIME] = 0;
    tio.c_cc[VMIN] = 1;
    tcsetattr(fd, TCSANOW, reinterpret_cast<const termios *>(&tio));
    tcflush(fd, TCIFLUSH);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_wdp_serial_SerialPort_native_1close(JNIEnv *env, jobject thiz) {
    jint fd = env->GetIntField(thiz, field_context);
    LOGD(LOG_TAG, "close() fd = %d", fd);
    close(fd);
    env->SetIntField(thiz, field_context, -1);

}
extern "C"
JNIEXPORT jint JNICALL
Java_com_wdp_serial_SerialPort_native_1read_1array(JNIEnv *env, jobject thiz, jbyteArray buffer,
                                                   jint length) {
    jint fd = env->GetIntField(thiz, field_context);
    auto *buf = (jbyte *) malloc(length);
    if (!buf) {
        jniThrowException(env, "java/lang/OutOfMemoryError", nullptr);
        return -1;
    }
    int ret = read(fd, buf, length);
    if (ret > 0) {
        env->SetByteArrayRegion(buffer, 0, ret, buf);
    }
    free(buf);
    if (ret < 0) {
        jniThrowException(env, "java/io/IOException", nullptr);
    }
    return ret;

}
extern "C"
JNIEXPORT jint JNICALL
Java_com_wdp_serial_SerialPort_native_1read_1direct(JNIEnv *env, jobject thiz, jobject buffer,
                                                    jint length) {
    jint fd = env->GetIntField(thiz, field_context);
    auto *buf = (jbyte *) env->GetDirectBufferAddress(buffer);
    if (!buf) {
        jniThrowException(env, "java/lang/IllegalArgumentException", "ByteBuffer not direct");
        return -1;
    }
    int ret = read(fd, buf, length);
    if (ret < 0) {
        jniThrowException(env, "java/io/IOException", nullptr);
    }
    return ret;
}
extern "C"
JNIEXPORT void JNICALL
Java_com_wdp_serial_SerialPort_native_1write_1array(JNIEnv *env, jobject thiz, jbyteArray buffer,
                                                    jint length) {
    jint fd = env->GetIntField(thiz, field_context);
    auto *buf = (jbyte *) malloc(length);
    if (!buf) {
        jniThrowException(env, "java/lang/OutOfMemoryError", nullptr);
        return;
    }
    env->GetByteArrayRegion(buffer, 0, length, buf);
    jint ret = write(fd, buf, length);
    free(buf);
    if (ret < 0) {
        jniThrowException(env, "java/io/IOException", nullptr);
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_wdp_serial_SerialPort_native_1write_1direct(JNIEnv *env, jobject thiz, jobject buffer,
                                                     jint length) {
    jint fd = env->GetIntField(thiz, field_context);
    auto *buf = (jbyte *) env->GetDirectBufferAddress(buffer);
    if (!buf) {
        jniThrowException(env, "java/lang/IllegalArgumentException", "ByteBuffer not direct");
        return;
    }
    int ret = write(fd, buf, length);
    if (ret < 0) {
        jniThrowException(env, "java/io/IOException", nullptr);
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_wdp_serial_SerialPort_native_1send_1break(JNIEnv *env, jobject thiz) {
    jint fd = env->GetIntField(thiz, field_context);
    tcsendbreak(fd, 0);
}


