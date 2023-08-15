package com.rtnbluetooth;

import androidx.annotation.NonNull;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import java.util.Map;
import java.util.HashMap;
import android.content.Context;
import com.rtnbluetooth.NativeBluetoothSpec;

public class BluetoothModule extends NativeBluetoothSpec {

    public static String NAME = "RTNBluetooth";
    private Context context;    
    final private BlcManager mRNCBluetoothManagerImpl;
    

    BluetoothModule(ReactApplicationContext context) {
        super(context);               
        mRNCBluetoothManagerImpl = new BlcManager(context);
    }

    @Override
    @NonNull
    public String getName() {
        return NAME;
    }

    @Override
    public void add(double a, double b, Promise promise) {
        promise.resolve(a + b);
    }
    @Override
    public void subtract(double a, double b, Promise promise) {
        promise.resolve(a - b);
    }

    @Override
    public void checkBluetoothSupport(Promise promise) {        
        mRNCBluetoothManagerImpl.checkBluetoothSupport(promise);
    }

    @Override
    public void toggleBluetooth(Promise promise) {
        mRNCBluetoothManagerImpl.toggleBlueTooth(promise);
    }

    @Override
    public void enableBluetooth(Promise promise) {
       mRNCBluetoothManagerImpl.enableBluetooth(promise);
    }

    @Override
    public void getBondedPeripherals(Promise promise) {
       mRNCBluetoothManagerImpl.getBondedPeripherals(promise);
    }
    @Override    
    public void pairDevice(String deviceId, boolean isServer, Promise promise) {
       mRNCBluetoothManagerImpl.pairDevice(deviceId, isServer, promise);
    }
    @Override    
    public void startDiscovery( Promise promise) {
       mRNCBluetoothManagerImpl.startDiscovery(promise);
    }

    @Override    
    public void cancelDiscovery( Promise promise) {
       mRNCBluetoothManagerImpl.cancelDiscovery(promise);
    }
    @Override    
    public void startAcceptServer( Promise promise) {
       mRNCBluetoothManagerImpl.startAcceptServer(promise);
    }

    @Override
    public void addListener(String eventType) {
    // NOOP
    }

    @Override
    public void removeListeners(double count) {
        // NOOP
    }
}