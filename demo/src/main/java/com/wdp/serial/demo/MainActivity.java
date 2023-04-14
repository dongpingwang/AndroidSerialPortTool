package com.wdp.serial.demo;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.wdp.serial.business.SerialBusiness;
import com.wdp.serial.business.DataUtils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 作者：王东平
 * 功能：
 * 说明：
 * 版本：1.0.0
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final Integer[] SPEEDS = {9600, 19200, 115200};
    private static final String TAG = "MainActivity";

    private ScheduledExecutorService executor;
    private ScheduledFuture<?> timerTask;
    private Handler uiHandler = new Handler(Looper.getMainLooper());

    private TextView tvSerialPath;
    private TextView tvSerialSpeed;
    private Spinner spSerialPath;
    private Spinner spSerialSpeed;
    private Button btnOpen;
    private Button btnSend;
    private CheckBox cbHex;
    private CheckBox cbTimer;
    private EditText etTimer;
    private EditText etInput;
    private TextView tvOutput;

    private ArrayAdapter<Integer> speedAdapter;
    private ArrayAdapter<String> pathAdapter;
    private int curSpeedIndex = 0;
    private int curPathIndex = 0;

    private volatile boolean openStatus = false;
    private SerialBusiness serialBusiness;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        Log.d(TAG, "onCreate");
        tvSerialPath = findViewById(R.id.tv_serial_path);
        tvSerialSpeed = findViewById(R.id.tv_serial_speed);
        spSerialPath = findViewById(R.id.sp_serial_path);
        spSerialSpeed = findViewById(R.id.sp_serial_speed);
        btnOpen = findViewById(R.id.btn_open_serial);
        btnSend = findViewById(R.id.btn_send);
        cbHex = findViewById(R.id.cb_is_hex);
        cbTimer = findViewById(R.id.cb_timer);
        etInput = findViewById(R.id.et_input_data);
        etTimer = findViewById(R.id.et_timer);
        tvOutput = findViewById(R.id.tv_out_data);
        btnOpen.setOnClickListener(this);
        btnSend.setOnClickListener(this);
        spSerialSpeed.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                curSpeedIndex = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        spSerialPath.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                curPathIndex = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        speedAdapter = new ArrayAdapter<>(getApplicationContext(), R.layout.item, SPEEDS);
        spSerialSpeed.setAdapter(speedAdapter);

        executor = Executors.newSingleThreadScheduledExecutor();

        findSerialPorts();

    }

    private void findSerialPorts() {
        executor.execute(() -> {
            try {
                SerialPortFinder finder = new SerialPortFinder();
                String[] allDevicesPath = finder.getAllDevicesPath();
                uiHandler.post(() -> {
                    pathAdapter = new ArrayAdapter<>(getApplicationContext(), R.layout.item, allDevicesPath);
                    spSerialPath.setAdapter(pathAdapter);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onClick(View v) {
        if (v == btnOpen) {
            if (!openStatus) {
                String path = pathAdapter.getItem(curPathIndex);
                int speed = speedAdapter.getItem(curSpeedIndex);
                serialBusiness = new SerialBusiness.Builder(path, speed)
                        .setReadCount(2)
                        .setAutoCheck(true)
                        .build();
                openStatus = serialBusiness.openSerialPort();
                Log.d(TAG, "open: " + path + " | " + speed + " result=" + openStatus);
                btnOpen.setText("关闭");
            } else {
                openStatus = false;
                serialBusiness.closeSerialPort();
                btnOpen.setText("打开");
            }

        } else if (v == btnSend) {
            if (!openStatus) {
                Toast.makeText(this, "需要先打开串口", Toast.LENGTH_SHORT).show();
                return;
            }
            tvOutput.setText("");
            String data = etInput.getText().toString();
            if (timerTask != null && (!timerTask.isCancelled() || !timerTask.isDone())) {
                timerTask.cancel(true);
            }
            if (!TextUtils.isEmpty(data)) {
                boolean isHex = cbHex.isChecked();
                if (cbTimer.isChecked() && !TextUtils.isEmpty(etTimer.getText().toString())) {
                    // 定时发送
                    int delay =Integer.parseInt(etTimer.getText().toString());
                    timerTask = executor.scheduleWithFixedDelay(() -> {
                        serialBusiness.write(isHex ? DataUtils.hexToBytes(data) : data.getBytes(), response -> {
                            uiHandler.post(() -> {
                                tvOutput.setText(isHex ? DataUtils.toHex(response) : new String(response));
                                Log.d(TAG, "output:" + tvOutput.getText());
                            });
                        });
                    }, 0, delay, TimeUnit.MILLISECONDS);
                } else {
                    executor.execute(() -> {
                        serialBusiness.write(isHex ? DataUtils.hexToBytes(data) : data.getBytes(), response -> {
                            uiHandler.post(() -> {
                                tvOutput.setText(isHex ? DataUtils.toHex(response) : new String(response));
                                Log.d(TAG, "output:" + tvOutput.getText());
                            });
                        });
                    });
                }
            } else {
                Toast.makeText(this, "不能发送空数据", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
