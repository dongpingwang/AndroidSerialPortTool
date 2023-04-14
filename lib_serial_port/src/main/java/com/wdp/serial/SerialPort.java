package com.wdp.serial;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 作者：王东平
 * 功能：入口类，可以用此打开关闭串口，数据读取业务由自己包装
 * 说明：
 * 版本：1.0.0
 */
public class SerialPort {

    {
        System.loadLibrary("serial");
    }

    private final String path;
    private final int speed;

    private final int flag;

    // use by jni code
    private int mNativeContext;

    public SerialPort(String path, int speed) {
        this(path, speed, 0);
    }

    public SerialPort(String path, int speed, int flag) {
        this.path = path;
        this.speed = speed;
        this.flag = flag;
    }

    public void open() throws IOException {
        native_open(path, speed, flag);
    }

    public void close() {
        native_close();
    }

    public int read(ByteBuffer buffer) throws IOException {
        if (buffer.isDirect()) {
            return native_read_direct(buffer, buffer.remaining());
        } else if (buffer.hasArray()) {
            return native_read_array(buffer.array(), buffer.remaining());
        } else {
            throw new IllegalArgumentException("buffer is not direct and has no array");
        }
    }

    public void write(ByteBuffer buffer, int length) throws IOException {
        if (buffer.isDirect()) {
            native_write_direct(buffer, length);
        } else if (buffer.hasArray()) {
            native_write_array(buffer.array(), length);
        } else {
            throw new IllegalArgumentException("buffer is not direct and has no array");
        }
    }

    public void sendBreak() {
        native_send_break();
    }

    private native void native_open(String path, int speed, int flag) throws IOException;

    private native void native_close();

    private native int native_read_array(byte[] buffer, int length) throws IOException;

    private native int native_read_direct(ByteBuffer buffer, int length) throws IOException;

    private native void native_write_array(byte[] buffer, int length) throws IOException;

    private native void native_write_direct(ByteBuffer buffer, int length) throws IOException;

    private native void native_send_break();
}
