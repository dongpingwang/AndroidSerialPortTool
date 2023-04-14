package com.wdp.serial.business;

import java.util.List;

/**
 * 作者：王东平
 * 功能：数据转换类
 * 说明：
 * 版本：1.0.0
 */
public final class DataUtils {

    private DataUtils() {

    }

    public static int toHex(String data) {
        return Integer.parseInt(data, 16);
    }

    public static byte toByte(String data, int radix) {
        return (byte) Integer.parseInt(data, radix);
    }

    public static String toHex(byte data) {
        // 1字节转2个Hex字符
        return String.format("%02x", data).toUpperCase();
    }

    public static String toHex(byte[] data) {
        StringBuilder strBuilder = new StringBuilder();
        for (byte b : data) {
            strBuilder.append(toHex(b));
            strBuilder.append(" ");
        }
        return strBuilder.toString();
    }

    public static int isOdd(int num) {
        return num & 0x1;
    }

    public static byte[] hexToBytes(String hexStr) {
        int len = hexStr.length();
        byte[] result;
        if (isOdd(len) == 1) {
            // 奇数时补0
            len++;
            hexStr = "0" + hexStr;
        }
        result = new byte[len / 2];
        int j = 0;
        for (int i = 0; i < len; i += 2) {
            result[j] = toByte(hexStr.substring(i, i + 2), 16);
            j++;
        }
        return result;
    }


    public static byte[] arrayCopy(List<byte[]> data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        int len = 0;
        for (int i = 0; i < data.size(); i++) {
            len += data.get(i).length;
        }
        byte[] total = new byte[len];
        int curIndex = 0;
        for (int i = 0; i < data.size(); i++) {
            for (int j = 0; j < data.get(i).length; j++) {
                total[curIndex] = data.get(i)[j];
                curIndex++;
            }
        }
        return total;
    }
}
