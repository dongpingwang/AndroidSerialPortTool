package com.wdp.serial;

import android.os.ParcelFileDescriptor;

/**
 * 作者：王东平
 * 功能：
 * 说明：
 * 版本：1.0.0
 */
class SerialService implements ISerialManager {

    @Override
    public ParcelFileDescriptor openSerialPort(String path) {
        return native_open(path);
    }

    private native ParcelFileDescriptor native_open(String path);
}
