package com.wdp.serial;

import android.os.ParcelFileDescriptor;

import java.io.FileDescriptor;
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

    private ISerialManager serialManager;
    private String path;
    private int speed;
    private ParcelFileDescriptor pfd;
    // use by jni code
    private int mNativeContext;

    public SerialPort(String path, int speed) {
        this.path = path;
        this.speed = speed;
        serialManager = new SerialService();
    }

    public void open() throws IOException {
        pfd = serialManager.openSerialPort(path);
        if (pfd != null) {
            native_open(pfd.getFileDescriptor(), speed);
        } else {
            throw new IOException("Could not open serial port " + path);
        }
    }

    public void close() throws IOException {
        if (pfd != null) {
            pfd.close();
            pfd = null;
        }
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

    private native void native_open(FileDescriptor fd, int speed) throws IOException;

    private native void native_close();

    private native int native_read_array(byte[] buffer, int length) throws IOException;

    private native int native_read_direct(ByteBuffer buffer, int length) throws IOException;

    private native void native_write_array(byte[] buffer, int length) throws IOException;

    private native void native_write_direct(ByteBuffer buffer, int length) throws IOException;

    private native void native_send_break();
}
