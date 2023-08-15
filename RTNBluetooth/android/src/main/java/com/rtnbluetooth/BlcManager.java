package com.rtnbluetooth;

import static android.app.Activity.RESULT_OK;
import static android.bluetooth.BluetoothProfile.GATT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;

import android.Manifest;
import android.annotation.SuppressLint;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.BaseActivityEventListener;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.rtnbluetooth.EventType;


import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


class BlcManager extends ReactContextBaseJavaModule implements LifecycleEventListener, ActivityEventListener {
    public static final String LOG_TAG = "ReactNativeBlcManager";
    private final UUID MY_UUID = UUID.fromString("6088D2B3-983A-4EED-9F94-5AD1256816B7");

    private static final int ENABLE_REQUEST = 539;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothManager bluetoothManager;
    private Context context;
    private ReactApplicationContext reactContext; 
    private Promise enableBluetoothPromise;

    public ReactApplicationContext getReactContext() {
        return reactContext;
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                Log.d(LOG_TAG, "onReceive: name " + deviceName);
                Log.d(LOG_TAG, "onReceive: address " + deviceHardwareAddress);

                // Attempt connecting to device if other device is listening for connections.
                ConnectThread connect = new ConnectThread(device);
                connect.start();
            }else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {                   
            context.unregisterReceiver(this);
        }
        }
    };


    public BlcManager(ReactApplicationContext reactContext) {
        super(reactContext);
        context = reactContext;
        this.reactContext = reactContext;
        reactContext.addActivityEventListener(this);
        reactContext.addLifecycleEventListener(this);
        Log.d(LOG_TAG, "BlcManager created");
    }


    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent intent) {
        Log.d(LOG_TAG, "onActivityResult  called "+ requestCode);

        if (requestCode == ENABLE_REQUEST && enableBluetoothPromise != null) {
            if (resultCode == RESULT_OK) {
                WritableMap eventData = Arguments.createMap();
                eventData.putString("status", "Enabled");
                sendEvent(EventType.BLUETOOTH_ENABLED,eventData);
                Toast toast = Toast.makeText( reactContext , "bluetooth enabled" ,Toast.LENGTH_SHORT);
                enableBluetoothPromise.resolve("user accepted the request!");
            } else {
                enableBluetoothPromise.resolve("User refused to enable");
            }
            enableBluetoothPromise = null;
        }
    }



    /**
     * Makes this device discoverable for 120 seconds (2 minutes).
     */
    private void ensureDiscoverable() {
        if (getBluetoothAdapter().getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 60);
           getReactApplicationContext().startActivity(discoverableIntent);
        }
    }



    @Override
    public void onNewIntent(Intent intent) {

    }
    @Override
    public String getName() {
        return "BlcManager";
    }

    private BluetoothAdapter getBluetoothAdapter() {
        if (bluetoothAdapter == null) {
            BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            bluetoothAdapter = manager.getAdapter();
        }
        return bluetoothAdapter;
    }



    private BluetoothManager getBluetoothManager() {
        if (bluetoothManager == null) {
            bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        }
        return bluetoothManager;
    }

    public void startDiscovery(Promise promise){
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&  ContextCompat.checkSelfPermission(reactContext,Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ){
            ActivityCompat.requestPermissions(getCurrentActivity() , new String[]{Manifest.permission.BLUETOOTH_SCAN}, 1);
        }else {
            getBluetoothAdapter().startDiscovery();
            ensureDiscoverable();
            promise.resolve(true);
        }
    }

    public void startAcceptServer(Promise promise){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(reactContext,Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ){
            ActivityCompat.requestPermissions(getCurrentActivity() , new String[]{Manifest.permission.BLUETOOTH_SCAN}, 1);
        }else {
            AcceptThread accept = new AcceptThread();
            accept.start();
        }
        promise.resolve("started accept server");
    }

    public void cancelDiscovery(Promise promise){
        getBluetoothAdapter().cancelDiscovery();
        promise.resolve(true);
    }

    public void getBondedPeripherals(Promise promise) {               
        enableDeviceBluetooth();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&  ContextCompat.checkSelfPermission(reactContext,Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ){

            ActivityCompat.requestPermissions(getCurrentActivity() , new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);

        }else {
            Set<BluetoothDevice> deviceSet = getBluetoothAdapter().getBondedDevices();
            List<String> deviceNamesList = new ArrayList<>();
            for (BluetoothDevice device : deviceSet) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address

                Log.d(LOG_TAG, deviceName + "#" + deviceHardwareAddress);

                deviceNamesList.add(deviceName + "#" + deviceHardwareAddress);
            }
            // Convert the list to a WritableArray
            WritableArray deviceNamesArray = Arguments.createArray();
            for (String deviceName : deviceNamesList) {
                deviceNamesArray.pushString(deviceName);
            }
            Log.d(LOG_TAG, deviceSet.toString());
            promise.resolve(deviceNamesArray);
        }
    }

    public void checkBluetoothSupport(Promise promise){
        if(getBluetoothAdapter() == null){
            Log.d(LOG_TAG, "No bluetooth support");
            promise.resolve(false);
        }else{
            promise.resolve(true);
        }
    }

    public void enableDeviceBluetooth() {
        if (getBluetoothAdapter() == null) {
            Log.d(LOG_TAG, "No bluetooth support");
            return;
        }
        if (!getBluetoothAdapter().isEnabled()) {
            Intent intentEnable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);            
            if (getCurrentActivity() == null)
                Log.d(LOG_TAG, "Current activity not available####");
            else
            {
                try{
                    getCurrentActivity().startActivityForResult(intentEnable, ENABLE_REQUEST);
                }catch(Exception e){
                    Log.d(LOG_TAG, e.getStackTrace().toString());
                }
            }

        }
    }

    public void toggleBlueTooth(Promise promise) {
        if(getBluetoothAdapter() == null){

        }else{
            if(getBluetoothAdapter().isEnabled()){
                getBluetoothAdapter().disable();
                Toast toast = Toast.makeText( reactContext , "bluetooth disabled" ,Toast.LENGTH_SHORT);
                toast.show();
                promise.resolve("disabled");
            }else{
                enableBluetooth(promise);
            }
        }
    }
    @SuppressLint("MissingPermission")
    public void enableBluetooth(Promise promise) {
        if (getBluetoothAdapter() == null) {
            Log.d(LOG_TAG, "No bluetooth support");
            promise.resolve("No bluetooth support");
            return;
        }
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(reactContext,Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ){

            ActivityCompat.requestPermissions(getCurrentActivity() , new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);

        }else {
            if (!getBluetoothAdapter().isEnabled()) {
                Intent intentEnable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                if (getCurrentActivity() == null)
                    promise.resolve("Current activity not available####");
                else {
                    try {
                        enableBluetoothPromise = promise;
                        getCurrentActivity().startActivityForResult(intentEnable, ENABLE_REQUEST);
                    } catch (Exception e) {
                        Log.d(LOG_TAG, e.getStackTrace().toString());
                        promise.resolve("Current activity not available!!!");
                    }

                }

            } else
                promise.resolve("default case");
        }
    }

    public void pairDevice(String deviceID, boolean isServer, Promise promise) {
           
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(reactContext,Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ){
            
            ActivityCompat.requestPermissions(getCurrentActivity() , new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
             
        }else {
            if(!getBluetoothAdapter().isEnabled()){
                getBluetoothAdapter().enable();

            }
            Log.d(LOG_TAG, "pairDevice");            

            BluetoothDevice device = getBluetoothAdapter().getRemoteDevice(deviceID);
            if(isServer) {
                AcceptThread accept = new AcceptThread();
                accept.start();
            }else {
                ConnectThread connect = new ConnectThread(device);
                connect.start();
            }
            Log.d(LOG_TAG, "Connecting to device: " + device.getName() + " (" + device.getAddress() + ")");
            promise.resolve("pairing ....");
        }
    }

    @Override
    public void onHostPause() {

    }
    @Override
    public void onHostResume() {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        getReactApplicationContext().registerReceiver(receiver, filter);
    };

    @Override
    public void onHostDestroy() {
        getReactApplicationContext().unregisterReceiver(receiver);
    }

    /**
     * Sends a {@link EventType} to the React Native JS module
     * {@link com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter}.
     * <p>
     * Currently having no active {@link com.facebook.react.bridge.CatalystInstance} will not cause
     * the application to crash, although I'm not sure if it should.
     *
     * @param event the {@link EventType} being sent
     * @param body the content of the event
     */
    private void sendEvent(EventType event, WritableMap body) {
        ReactContext context = getReactApplicationContext();        
        if (context.hasActiveCatalystInstance()) {
            Log.d(LOG_TAG, "sendEvent called "+ event.name());
            context
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(event.name(), body);
        } else {
            Log.e(LOG_TAG, "There is currently no active Catalyst instance");
        }
    }


    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;


        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.
            BluetoothServerSocket tmp = null;
            Log.d(LOG_TAG,"Accepting" + MY_UUID.toString());
            try {
                // MY_UUID is the app's UUID string, also used by the client code.
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord("myApp", MY_UUID);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Socket's listen() method failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned.
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Socket's accept() method failed", e);
                    break;
                }



                if (socket != null) {
                    // A connection was accepted. Perform work associated with
                    // the connection in a separate thread.

                    try {
                        //    manageMyConnectedSocket(socket);
                        mmServerSocket.close();
                    } catch (IOException e) {
                        Log.e(LOG_TAG, "Error closing the server socket", e);
                    }
                    break;
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Could not close the connect socket", e);
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                Log.d(LOG_TAG,"connect " + MY_UUID.toString());                                             
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                Log.d(LOG_TAG, "#### connecting #### "  );
            } catch (IOException e) {
                Log.e(LOG_TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.d(LOG_TAG, "trying Connection!");
            // Cancel discovery because it otherwise slows down the connection.
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(reactContext,Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ){

                ActivityCompat.requestPermissions(getCurrentActivity() , new String[]{Manifest.permission.BLUETOOTH_SCAN}, 1);

            }else {
                bluetoothAdapter.cancelDiscovery();

                try {
                    // Connect to the remote device through the socket. This call blocks
                    // until it succeeds or throws an exception.
                    Log.d(LOG_TAG, "trying Connection!");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(reactContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        Log.d(LOG_TAG, "permision check Connection!");
                        ActivityCompat.requestPermissions(getCurrentActivity(), new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);

                    }
                    if(!mmSocket.isConnected()){
                        mmSocket.connect();
                    }
                    Log.d(LOG_TAG, "Connection successful!");
                } catch (IOException connectException) {
                    // Unable to connect; close the socket and return.
                    Log.e(LOG_TAG, "unable to connect", connectException);
                    try {
                        mmSocket.close();
                    } catch (IOException closeException) {
                        Log.e(LOG_TAG, "Could not close the client socket", closeException);
                    }
                    return;
                }

                // The connection attempt succeeded. Perform work associated with
                // the connection in a separate thread.
                //    manageMyConnectedSocket(mmSocket);
            }
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Could not close the client socket", e);
            }
        }
    }
}