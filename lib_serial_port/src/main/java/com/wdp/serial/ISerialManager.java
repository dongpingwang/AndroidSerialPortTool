package com.wdp.serial;

import android.os.ParcelFileDescriptor;

/**
 * 作者：王东平
 * 功能：
 * 说明：
 * 版本：1.0.0
 */
interface ISerialManager {
    ParcelFileDescriptor openSerialPort(String path);
}
