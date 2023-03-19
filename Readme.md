# Android串口工具

参考Android系统实现方式，串口操作采用native实现，比java层用文件流的方式更高效，不需要延时和睡眠处理，**大量自测不会出现读取不到数据等问题**。

特点：
1.提供基本的串口操作，支持由自己去实现读取数据等业务
2.默认实现一个串口操作业务，支持数据回调，支持数据拼接，支持超时等

使用方式1：
```java
SerialPort serialPort = new SerialPort(path, speed)
```

使用方式2：
```java
SerialBusiness serialBusiness = new SerialBusiness.Builder(path, speed)
        .setReadCount(2)
        .setAutoCheck(true)
        .build();
```
demo工程界面效果：
[图片](./serial_port_demo.png)

github：https://github.com/dongpingwang/AndroidSerialPortTool
