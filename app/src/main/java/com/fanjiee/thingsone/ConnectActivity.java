package com.fanjiee.thingsone;

import android.app.Activity;
import android.os.Bundle;


import android.bluetooth.BluetoothDevice;
import com.google.android.things.bluetooth.BluetoothConnectionManager;
import com.google.android.things.bluetooth.BluetoothConnectionCallback;
import com.google.android.things.bluetooth.BluetoothProfile;
import com.google.android.things.bluetooth.ConnectionParams;
/**
 * Skeleton of an Android Things activity.
 * <p>
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 * <p>
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 */


//设备配对成功后，连接到远程设备

public class ConnectActivity extends Activity {

    BluetoothConnectionManager bluetoothConnectionManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bluetoothConnectionManager = BluetoothConnectionManager.getInstance();
        bluetoothConnectionManager.registerConnectionCallback(bluetoothConnectionCallback);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothConnectionManager.unregisterConnectionCallback(bluetoothConnectionCallback);
    }

    private void connectToA2dp(BluetoothDevice bluetoothDevice) {
        bluetoothConnectionManager.connect(bluetoothDevice, BluetoothProfile.A2DP_SINK);
    }

    // 为配置文件连接进程设置回调
    private final BluetoothConnectionCallback bluetoothConnectionCallback = new BluetoothConnectionCallback() {
        @Override
        public void onConnectionRequested(BluetoothDevice bluetoothDevice, ConnectionParams connectionParams) {
            // 处理传入的连接请求
            handleConnectionRequest(bluetoothDevice, connectionParams);
        }

        @Override
        public void onConnectionRequestCancelled(BluetoothDevice bluetoothDevice, int requestType) {
            // 请求取消
        }

        @Override
        public void onConnected(BluetoothDevice bluetoothDevice, int profile) {
            // 连接成功完成
        }

        @Override
        public void onDisconnected(BluetoothDevice bluetoothDevice, int profile) {
            // 远程设备断开连接
        }
    };


    private void handleConnectionRequest(BluetoothDevice bluetoothDevice, ConnectionParams connectionParams) {
        // 确定是否接受连接请求
        boolean accept = false;
        if (connectionParams.getRequestType() == ConnectionParams.REQUEST_TYPE_PROFILE_CONNECTION) {
            accept = true;
        }

        // 将结果传递给蓝牙连接管理器
        bluetoothConnectionManager.confirmOrDenyConnection(bluetoothDevice, connectionParams, accept);
    }
}
