import React from 'react';
import {
  SectionList,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
// import RTNBluetooth from 'rtn-Bluetooth/js/NativeBluetooth';
import RTNBluetooth from 'rtn-bluetooth/js/NativeBluetooth';

interface BondedDeviceItem {
  deviceName: string;
  deviceId: string;
}

interface Props {
  bondedDevices: BondedDeviceItem[];
  isServer: boolean;
}

const BondedDeviceSectionList = ({bondedDevices, isServer}: Props) => {
  // Organize the data into sections (if needed). For this example, we will have a single section.
  const sections = [{title: 'Bonded Devices', data: bondedDevices}];
  const handleItemPress = async (item: BondedDeviceItem) => {
    const res = await RTNBluetooth?.pairDevice(item.deviceId, isServer);
    console.log({res});
  };

  console.log(bondedDevices.length);

  // Render each item within a section
  const renderItem = ({item}: {item: BondedDeviceItem}) => (
    <TouchableOpacity onPress={() => handleItemPress(item)}>
      <View style={styles.itemContainer}>
        <Text style={styles.deviceName}>{item.deviceName}</Text>
        <Text style={styles.deviceId}>Device ID: {item.deviceId}</Text>
      </View>
    </TouchableOpacity>
  );

  // Render the header for each section
  const renderSectionHeader = ({
    section,
  }: {
    section: {
      title: string;
      data: BondedDeviceItem[];
    };
  }) => (
    <View style={styles.sectionHeader}>
      <Text style={styles.sectionHeaderText}>{section.title}</Text>
    </View>
  );

  return (
    <SectionList
      sections={sections}
      keyExtractor={(item, index) => item.deviceId + index}
      renderItem={renderItem}
      renderSectionHeader={renderSectionHeader}
    />
  );
};

const styles = StyleSheet.create({
  sectionHeader: {
    backgroundColor: '#f2f2f2',
    paddingVertical: 10,
    paddingHorizontal: 20,
  },
  sectionHeaderText: {
    fontSize: 18,
    fontWeight: 'bold',
  },
  itemContainer: {
    backgroundColor: '#fff',
    padding: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#ccc',
  },
  deviceName: {
    fontSize: 16,
    fontWeight: 'bold',
  },
  deviceId: {
    fontSize: 14,
    color: '#555',
  },
});

export default BondedDeviceSectionList;
