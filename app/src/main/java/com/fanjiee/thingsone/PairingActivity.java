package com.fanjiee.thingsone;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.util.Log;

import com.google.android.things.bluetooth.BluetoothConnectionManager;
import com.google.android.things.bluetooth.BluetoothPairingCallback;
import com.google.android.things.bluetooth.PairingParams;

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
public class PairingActivity extends Activity {

    private BluetoothConnectionManager bluetoothConnectionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        bluetoothConnectionManager = BluetoothConnectionManager.getInstance();
//        bluetoothConnectionManager.registerConnectionCallback(bluetoothPairingCallback);

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        bluetoothConnectionManager.unregisterPairingCallback(bluetoothPairingCallback);
    }

    private void startPairing(BluetoothDevice remoteDevice) {
        bluetoothConnectionManager.initiatePairing(remoteDevice);
    }

    private BluetoothPairingCallback bluetoothPairingCallback = new BluetoothPairingCallback() {

        @Override
        public void onPairingInitiated(BluetoothDevice bluetoothDevice,
                                       PairingParams pairingParams) {
            // 处理传入配对请求或传出配对请求的确认
            handlePairingRequest(bluetoothDevice, pairingParams);
        }

        @Override
        public void onPaired(BluetoothDevice bluetoothDevice) {
            // 设备配对完成
        }

        @Override
        public void onUnpaired(BluetoothDevice bluetoothDevice) {
            // 设备未配对
        }

        @Override
        public void onPairingError(BluetoothDevice bluetoothDevice,
                                   BluetoothPairingCallback.PairingError pairingError) {
            // 失败
        }
    };


    private void handlePairingRequest(BluetoothDevice bluetoothDevice, PairingParams pairingParams) {
        switch (pairingParams.getPairingType()) {
            case PairingParams.PAIRING_VARIANT_DISPLAY_PIN:
            case PairingParams.PAIRING_VARIANT_DISPLAY_PASSKEY:
                // 向用户显示所需的PIN
                    Log.d("==", "Display Passkey - " + pairingParams.getPairingPin());
                break;
            case PairingParams.PAIRING_VARIANT_PIN:
            case PairingParams.PAIRING_VARIANT_PIN_16_DIGITS:
                // 从用户处获取PIN
//                String pin = ...;
                //将结果传递给完成配对
//                bluetoothConnectionManager.finishPairing(bluetoothDevice, pin);
                break;
            case PairingParams.PAIRING_VARIANT_CONSENT:
            case PairingParams.PAIRING_VARIANT_PASSKEY_CONFIRMATION:
                // 向用户显示配对的确认
//            ...
                // 完成配对过程
                bluetoothConnectionManager.finishPairing(bluetoothDevice);
                break;
        }
    }

}
