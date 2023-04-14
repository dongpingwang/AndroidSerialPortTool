package com.wdp.serial.business;

import android.util.Log;

import com.wdp.serial.SerialPort;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 作者：王东平
 * 功能：便捷的业务类，easy to use
 * 说明：
 * 版本：1.0.0
 */
public class SerialBusiness implements Runnable {

    private static final String TAG = "SerialBusiness";

    private final SerialPort serialPort;
    private final ByteBuffer inputBuffer;
    private final ByteBuffer outputBuffer;

    private final String path;
    private final int speed;
    private final boolean isDirect;
    private final int bufferSize;
    private volatile long timeout;

    private volatile int readCount;

    private boolean isAutoCheck;

    private boolean openStatus = false;

    private SerialDataListenerBusiness listenerBusiness;

    private SerialBusiness(String path, int speed, boolean isDirect, int bufferSize, long timeout, boolean isAutoCheck, int readCount) {
        this.path = path;
        this.speed = speed;
        this.isDirect = isDirect;
        this.bufferSize = bufferSize;
        this.timeout = timeout;
        this.isAutoCheck = isAutoCheck;
        this.readCount = readCount;
        this.serialPort = new SerialPort(path, speed);
        if (isDirect) {
            inputBuffer = ByteBuffer.allocateDirect(bufferSize);
            outputBuffer = ByteBuffer.allocateDirect(bufferSize);
        } else {
            inputBuffer = ByteBuffer.allocate(bufferSize);
            outputBuffer = ByteBuffer.allocate(bufferSize);
        }
        checkStatus();
        new Thread(this).start();
        listenerBusiness = new SerialDataListenerBusiness();
    }

    public boolean openSerialPort() {
        if (openStatus) {
            return true;
        }
        try {
            serialPort.open();
            openStatus = true;
        } catch (Exception e) {
            e.printStackTrace();
            openStatus = false;
        }
        return openStatus;
    }

    public void closeSerialPort() {
        try {
            serialPort.close();
            openStatus = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void write(byte[] data) {
        if (openStatus) {
            try {
                outputBuffer.clear();
                outputBuffer.put(data);
                Log.d(TAG, "write");
                serialPort.write(outputBuffer, data.length);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            checkStatus();
        }
    }

    public void write(byte[] data, ISerialDataListener dataListener) {
        write(data);
        listenerBusiness.registerSerialDataListener(dataListener, timeout, readCount);
    }

    public void sendBreak() {
        if (openStatus) {
            try {
                serialPort.sendBreak();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            checkStatus();
        }
    }

    private void checkStatus() {
        if (isAutoCheck) {
            openSerialPort();
        }
    }

    @Override
    public void run() {
        Log.d(TAG, "run");
        int ret = 0;
        while (ret >= 0) {
            try {
                inputBuffer.clear();
                ret = serialPort.read(inputBuffer);
                Log.d(TAG, "read data: " + ret);
                if (ret > 0) {
                    byte[] buffer = new byte[ret];
                    inputBuffer.get(buffer, 0, buffer.length);
                    listenerBusiness.onDataRead(buffer);
                }
            } catch (IOException e) {
                Log.e(TAG, "read failed", e);
                break;
            }
        }
        Log.d(TAG, "thread out");
    }


    public static class Builder {

        private final String path;
        private final int speed;
        private boolean isDirect = false;
        private int bufferSize = 1024;

        // 读取次数，有些写入串口驱动后进行多次读取
        private int readCount = 1;

        // 是否自动打开串口
        private boolean isAutoCheck = true;

        // 每次写入后，最长等待串口驱动回复的时间,单位：ms
        private long timeout = 500L;

        public Builder(String path, int speed) {
            this.path = path;
            this.speed = speed;
        }

        public Builder setDirect(boolean isDirect) {
            this.isDirect = isDirect;
            return this;
        }

        public Builder setBufferSize(int bufferSize) {
            this.bufferSize = bufferSize;
            return this;
        }

        public Builder setReadCount(int readCount) {
            this.readCount = readCount;
            return this;
        }

        public Builder setAutoCheck(boolean isAutoCheck) {
            this.isAutoCheck = isAutoCheck;
            return this;
        }

        public Builder setTimeout(long timeout) {
            this.timeout = timeout;
            return this;
        }

        public SerialBusiness build() {
            return new SerialBusiness(path, speed, isDirect, bufferSize, timeout, isAutoCheck, readCount);
        }
    }
}
