<div align="center">
 
# Little Relay
 
<img src="https://github.com/UmerCodez/LittleRelay/blob/main/assets/app-icon.png" width="150">

![GitHub License](https://img.shields.io/github/license/UmerCodez/LittleRelay?style=for-the-badge) ![Android Badge](https://img.shields.io/badge/Android-6.0+-34A853?logo=android&logoColor=fff&style=for-the-badge) ![Jetpack Compose Badge](https://img.shields.io/badge/Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=fff&style=for-the-badge) ![Material 3](https://img.shields.io/badge/Material%203-ebe89d?style=for-the-badge&logo=materialdesign&logoColor=white) ![GitHub Release](https://img.shields.io/github/v/release/UmerCodez/LittleRelay?include_prereleases&style=for-the-badge)

[<img src="https://github.com/UmerCodez/LittleRelay/blob/main/assets/gio-github.png" height="80">](https://github.com/UmerCodez/LittleRelay/releases) [<img src="https://github.com/UmerCodez/LittleRelay/blob/main/assets/gio-fdroid.png" height="80">](https://f-droid.org/packages/app.umerfarooq.littlerelay)

### An Android application that acts as a bidirectional bridge between BLE devices and an MQTT broker, supporting both Central and Peripheral roles.

<img src="https://github.com/UmerCodez/LittleRelay/blob/main/fastlane/metadata/android/en-US/images/phoneScreenshots/1.jpg" width="250" heigth="250"> <img src="https://github.com/UmerCodez/LittleRelay/blob/main/fastlane/metadata/android/en-US/images/phoneScreenshots/3.jpg" width="250" heigth="250"> <img src="https://github.com/UmerCodez/LittleRelay/blob/main/fastlane/metadata/android/en-US/images/phoneScreenshots/2.jpg" width="250" heigth="250"> <img src="https://github.com/UmerCodez/LittleRelay/blob/main/fastlane/metadata/android/en-US/images/phoneScreenshots/4.jpg" width="250" heigth="250"> <img src="https://github.com/UmerCodez/LittleRelay/blob/main/fastlane/metadata/android/en-US/images/phoneScreenshots/5.jpg" width="250" heigth="250"> <img src="https://github.com/UmerCodez/LittleRelay/blob/main/fastlane/metadata/android/en-US/images/phoneScreenshots/6.jpg" width="250" heigth="250"> <img src="https://github.com/UmerCodez/LittleRelay/blob/main/fastlane/metadata/android/en-US/images/phoneScreenshots/7.jpg" width="250" heigth="250"> <img src="https://github.com/UmerCodez/LittleRelay/blob/main/fastlane/metadata/android/en-US/images/phoneScreenshots/8.jpg" width="250" heigth="250">

</div>


# BLE ↔ MQTT Bridge for Android

An Android application that acts as a **bidirectional data bridge between Bluetooth Low Energy (BLE) devices and an MQTT broker**.

The app supports **both BLE roles**:

* **Peripheral (GATT Server)**
* **Central (GATT Client)**

This allows BLE devices to **publish data to MQTT topics** and **receive MQTT data back over BLE**, with the Android device acting as the intermediary.

## Architecture Overview

```
+-------------+       BLE        +----------------+       MQTT       +--------------+
| BLE Device  | <--------------> | Android Bridge | <--------------> | MQTT Broker |
+-------------+                  +----------------+                  +--------------+
```

The Android app:

* Translates BLE writes & notifications into MQTT messages
* Translates MQTT messages into BLE notifications or writes


## BLE Roles

The app can operate in **one BLE role at a time**.


# Peripheral Role (GATT Server)

In **Peripheral Mode**, the Android app **advertises itself** and allows other BLE devices to connect to it.

```mermaid
graph LR
    BLE[BLE Device]
    APP[Android App]
    MQTT[MQTT Broker]

    BLE -->|WRITE or WRITE_NO_RESPONSE| APP
    APP -->|Publish| MQTT

    MQTT -->|Subscribed Data| APP
    APP -->|NOTIFY| BLE
```

## Advertised BLE Service

**Service UUID**

```
d2dfc1a2-9f53-494b-a2a1-1efd6148aa81
```

### Characteristics

| Purpose    | UUID                                   | Properties               |
| ---------- | -------------------------------------- | ------------------------ |
| MQTT → BLE | `007a7899-dc76-4778-b33b-3460bf250eed` | NOTIFY                   |
| BLE → MQTT | `eeb6d18b-863b-4fb5-81b6-a38902b985df` | WRITE, WRITE_NO_RESPONSE |



## Data Flow (Peripheral Role)

### BLE Device → MQTT Broker

1. Remote BLE device connects to the app
2. Remote BLE device writes data to following characteristic exposed by the app

```
eeb6d18b-863b-4fb5-81b6-a38902b985df
```

3. App publishes the received payload to the configured MQTT topic(s)

✔ Supported write types:

* `WRITE`
* `WRITE_NO_RESPONSE`



### MQTT Broker → BLE Device

1. Remote BLE device subscribes to notifications on:

```
007a7899-dc76-4778-b33b-3460bf250eed
```

2. App subscribes to MQTT topic(s)
3. When MQTT data arrives:

   * App sends it via **BLE Notification**
   * Device receives data instantly

ℹ️ **Indications are not supported** (for now) , only Notifications.



## Peripheral Role Summary

| Direction  | Mechanism                                 |
| ---------- | ----------------------------------------- |
| BLE → MQTT | Write to writable characteristic          |
| MQTT → BLE | Notification on notifiable characteristic |



# Central Role (GATT Client)

In **Central Mode**, the Android app scans for nearby BLE devices and connects to them.

This mode is ideal when:

* BLE devices already advertise services
* Android initiates and manages connections

```mermaid
graph LR
    BLE[BLE Device]
    APP[Android App]
    MQTT[MQTT Broker]

    BLE -->|NOTIFY or INDICATE| APP
    APP -->|Publish| MQTT

    MQTT -->|Subscribed Data| APP
    APP -->|WRITE_NO_RESPONSE| BLE
```


## Device Requirements (Central Role)

### BLE Device → MQTT Broker

The BLE device must expose **at least one characteristic** with:

* `NOTIFY` **or**
* `INDICATE`

Once found, the app UI allows enabling notifications/indications.

When data arrives:

* App publishes it to the configured MQTT topic(s)



### MQTT Broker → BLE Device

The BLE device must expose a characteristic supporting:

* `WRITE_NO_RESPONSE`

When MQTT data arrives:

* App writes the payload to the selected characteristic



## Central Role Summary

| Direction  | Requirement                            |
| ---------- | -------------------------------------- |
| BLE → MQTT | Characteristic with NOTIFY or INDICATE |
| MQTT → BLE | Characteristic with WRITE_NO_RESPONSE  |



## MQTT Configuration

The app allows configuring:

* Broker URL
* Port
* Credentials (if required)
* Publish topics
* Subscribe topics

### Topic Mapping

| Source                   | Destination        |
| ------------------------ | ------------------ |
| BLE Write / Notification | MQTT Publish       |
| MQTT Subscribe           | BLE Notify / Write |



## Payload Handling

* Payloads are forwarded as **bytes**
* No transformation is applied


### Android 11 and Lower – Location Permission Requirement

On devices running **Android 11 (API level 30) or lower**, the system requires **location permission** to perform Bluetooth Low Energy (BLE) scans. This is an Android platform restriction, as BLE scanning can potentially be used to infer a user’s location. To use the app in **Central role** and scan for nearby BLE devices, you must grant the appropriate location permission (typically **ACCESS_FINE_LOCATION**) and ensure that **location services are turned ON** on the device. If location permission is denied or location services are disabled, BLE scanning will not return any results, even if Bluetooth is enabled.



