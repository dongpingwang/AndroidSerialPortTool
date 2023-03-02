package com.wdp.serial;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.wdp.serial.util.DataUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 作者：王东平
 * 功能：尽量保证结果回调准确
 * --------------------状态过程--------------------
 * 预期状态：START --> READING --> COMPLETE
 * 超时状态：START --> TIMEOUT
 * 时间点状态：START --> END
 * <p>
 * 说明：
 * 版本：1.0.0
 */
class SerialDataListenerBusiness {

    private static final String TAG = "ListenerBusiness";

    private static final int MSG_START = 1;
    private static final int MSG_END = 2;
    private static final int MSG_READING = 3;

    private static final int MSG_TIMEOUT = 4;

    private static final int MSG_COMPLETE = 5;

    private Handler handler;

    // todo 性能问题，不用CopyOnWriteArrayList
    private CopyOnWriteArrayList<ListenerMachineState> listeners;

    SerialDataListenerBusiness() {
        listeners = new CopyOnWriteArrayList<>();
        HandlerThread thread = new HandlerThread("thread_serial_data_listener");
        thread.start();
        handler = new Handler(thread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                SerialDataListenerBusiness.this.handleMessage(msg);
            }
        };
    }

    private void handleMessage(Message receive) {
        ListenerMachineState bean = (ListenerMachineState) receive.obj;
        int msgId = receive.what;
        if (msgId == MSG_START) {
            // 开始处理
            bean.state = ListenerMachineState.State.START;
            bean.startTime = System.currentTimeMillis();
            loopEvent(bean, MSG_END, bean.timeout);
        } else if (msgId == MSG_END) {
            // 到点
            listeners.remove(bean);
            bean.state = ListenerMachineState.State.END;
            // TODO 有时驱动回复并不是预期的，这里补发
            if (bean.readCount != bean.datas.size() && !bean.datas.isEmpty()) {
                bean.listener.onDataReceived(DataUtils.arrayCopy(bean.datas));
            }
        } else if (msgId == MSG_TIMEOUT) {
            // 已超时
            bean.state = ListenerMachineState.State.TIMEOUT;
            listeners.remove(bean);
        } else if (msgId == MSG_READING) {
            // 收到一次数据
            if (System.currentTimeMillis() > (bean.startTime + bean.timeout)) {
                // 已超时
                loopEvent(bean, MSG_TIMEOUT, 0);
            } else {
                if (bean.readCount == bean.datas.size()) {
                    // 读取完成
                    handler.removeMessages(MSG_COMPLETE, bean);
                    loopEvent(bean, MSG_COMPLETE, 0);
                }
            }
        } else if (msgId == MSG_COMPLETE) {
            // 提前结束
            if (System.currentTimeMillis() <= (bean.startTime + bean.timeout)) {
                handler.removeMessages(MSG_END, bean);
            }
            listeners.remove(bean);
            // 预期数据读取完成
            bean.state = ListenerMachineState.State.COMPLETE;
            bean.listener.onDataReceived(DataUtils.arrayCopy(bean.datas));
        }
    }

    public void registerSerialDataListener(ISerialDataListener listener, long timeout, int readCount) {
        ListenerMachineState bean = new ListenerMachineState(listener, timeout, readCount);
        if (!listeners.contains(bean)) {
            listeners.add(bean);
            loopEvent(bean, MSG_START, 0);
        }
    }

    public void onDataRead(byte[] data) {
        // 如果每次写入数据后串口都能有回复，回调最先添加的监听即可
        if (!listeners.isEmpty()) {
            ListenerMachineState head = listeners.get(0);
            head.datas.add(data);
            loopEvent(head, MSG_READING, 0);
        }
    }

    private void loopEvent(ListenerMachineState event, int eventId, long delayMillis) {
        Message msg = Message.obtain();
        msg.what = eventId;
        msg.obj = event;
        if (delayMillis > 0) {
            handler.sendMessageDelayed(msg, delayMillis);
        } else {
            handler.sendMessage(msg);
        }
    }

    private static class ListenerMachineState {
        enum State {
            START,
            COMPLETE,
            END,
            TIMEOUT
        }

        private List<byte[]> datas = new ArrayList<>();
        private ISerialDataListener listener;
        private int readCount;
        private long timeout;
        private long startTime;
        private State state;

        public ListenerMachineState(ISerialDataListener listener, long timeout, int readCount) {
            this.listener = listener;
            this.readCount = readCount;
            this.timeout = timeout;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ListenerMachineState that = (ListenerMachineState) o;
            return readCount == that.readCount && timeout == that.timeout && Objects.equals(listener, that.listener);
        }

        @Override
        public int hashCode() {
            return Objects.hash(listener, readCount, timeout);
        }
    }

}
