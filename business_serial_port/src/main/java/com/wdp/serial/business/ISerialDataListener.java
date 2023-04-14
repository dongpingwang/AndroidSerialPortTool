package com.wdp.serial.business;

/**
 * 作者：王东平
 * 功能：
 * 说明：
 * 版本：1.0.0
 */
public interface ISerialDataListener {
    void onDataReceived(byte[] data);
}
