/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 */

import React, {useEffect, useState} from 'react';
import {
  Button,
  NativeEventEmitter,
  SafeAreaView,
  StatusBar,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import RTNBluetooth from 'rtn-bluetooth/js/NativeBluetooth';
import BondedDeviceSectionList from './src/components/BondedDeviceList';

interface BondedDeviceItem {
  deviceName: string;
  deviceId: string;
}

function App(): JSX.Element {
  const [isFetchingBondedDevice, setIsFetchingBondedDevice] = useState(false);
  const [isServer, setIsServer] = useState(false);
  const [bondedDevices, setBondedDevicesData] = useState<BondedDeviceItem[]>(
    [],
  );

  useEffect(() => {
    const bluetoothEventEmitter = new NativeEventEmitter(RTNBluetooth as any);
    const bluetoothStatus = bluetoothEventEmitter.addListener(
      'BLUETOOTH_ENABLED',
      event => {
        console.log(event);
      },
    );
    return () => bluetoothStatus.remove();
  }, []);

  const checkBlueToothSupport = async () => {
    const res = await RTNBluetooth?.checkBluetoothSupport();
    console.log(res);
    const isEnabled = await RTNBluetooth?.enableBluetooth();
    console.log({isEnabled});
  };

  useEffect(() => {
    checkBlueToothSupport();
  }, []);

  const getBondedDevices = async () => {
    setIsFetchingBondedDevice(true);
    const devices = await RTNBluetooth?.getBondedPeripherals();
    console.log({devices});
    if (devices?.length) {
      const data: BondedDeviceItem[] = devices?.map(item => {
        const list = item.split('#');
        return {
          deviceName: list[0],
          deviceId: list[1],
        };
      });
      setBondedDevicesData(data);
      setIsFetchingBondedDevice(false);
    }
  };

  const startDiscovery = async () => {
    const res = await RTNBluetooth?.startDiscovery();
    console.log({res});
  };
  const cancelDiscovery = async () => {
    const res = await RTNBluetooth?.cancelDiscovery();
    console.log({res});
  };
  const startAcceptServer = async () => {
    const res = await RTNBluetooth?.startAcceptServer();
    console.log({res});
  };

  const handleToggleBluetooth = async () => {
    const res = await RTNBluetooth?.toggleBluetooth();
    console.log({res});
  };

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle={'dark-content'} />
      <View style={styles.header}>
        <Text style={styles.headerText}>Bluetooth Classic Module</Text>
      </View>
      <View style={styles.btnWrapper} />
      <Button title="toggle Bluetooth" onPress={handleToggleBluetooth} />
      <View style={styles.btnWrapper} />
      <Button title="start discovery" onPress={startDiscovery} />

      <View style={styles.btnWrapper} />
      <Button title="cancel discovery" onPress={cancelDiscovery} />

      <View style={styles.btnWrapper} />
      <Button title="Get bonded devices" onPress={getBondedDevices} />

      <View style={styles.btnWrapper} />
      <Button title="Act as Server" onPress={() => setIsServer(true)} />

      <View style={styles.btnWrapper} />
      <Button title="Start Server" onPress={startAcceptServer} />

      {isFetchingBondedDevice ? (
        <View>
          <Text>loading bonded devices data...</Text>
        </View>
      ) : (
        <BondedDeviceSectionList
          bondedDevices={bondedDevices}
          isServer={isServer}
        />
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#dbdbdb',
  },
  header: {
    backgroundColor: '#fff',
    paddingVertical: 15,
    alignItems: 'center',
    justifyContent: 'center',
  },
  headerText: {
    color: 'black',
    fontSize: 20,
    fontWeight: 'bold',
  },
  btnWrapper: {
    marginTop: 20,
  },
});

export default App;
