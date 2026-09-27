# 📱 IP Subnet Calculator

A simple and practical Android application for calculating IPv4 subnet parameters from an IP address and CIDR prefix.

The project was created as a learning and portfolio project while developing practical Android development skills with Kotlin.

## ✨ Features

* **IPv4 subnet calculation**

    * Subnet Mask
    * Network ID
    * Broadcast Address
    * First and Last Host
    * Total Usable Hosts
    * Wildcard Mask

* **CIDR support**

    * Prefixes from `/0` to `/32`
    * Special handling for `/31` and `/32`

* **Network information**

    * Network Class
    * Private/Public network type
    * RFC 1918 private address detection

* **Input validation**

    * IPv4 address validation
    * CIDR prefix validation
    * Protection against invalid and incomplete addresses

* **Calculation history**

    * Local history stored with Android `SharedPreferences`
    * View recent calculations
    * Clear history

* **Clipboard support**

    * Copy calculation results
    * Copy public IP address

* **Public IP monitoring**

    * Displays the device's current public IPv4 address when network access is available
    * Automatically refreshes the displayed public IP when the network connection changes
    * Detects network availability changes using Android `ConnectivityManager.NetworkCallback`
    * Handles loss and restoration of network connectivity
    * Useful for observing public IP changes when switching between Wi-Fi and mobile networks

* **Localization**

    * English
    * Russian
    * Uses the device language settings

* **Dark interface**

    * High-contrast dark UI
    * Orange accent elements

## 🧮 Example

For:

```text
IP address: 192.168.1.100
Prefix: /24
```

The application calculates:

```text
Subnet Mask:       255.255.255.0
Network ID:        192.168.1.0
Broadcast:         192.168.1.255
First Host:        192.168.1.1
Last Host:         192.168.1.254
Total Hosts:       254
Wildcard Mask:     0.0.0.255
```

## 🛠️ Tech Stack

* **Language:** Kotlin
* **UI:** Android XML layouts
* **Build system:** Gradle
* **Android Gradle Plugin:** 8.10.1
* **Kotlin:** 2.0.21
* **Compile SDK:** 35
* **Target SDK:** 35
* **Minimum SDK:** 26
* **Local storage:** SharedPreferences
* **Networking:** `HttpURLConnection`
* **Asynchronous operations:** Kotlin Coroutines

## 📂 Project Structure

The project follows a standard Android application structure:

```text
IPCalculator/
├── app/
│   └── src/
│       └── main/
│           ├── java/
│           │   └── com/oleg/ipcalculator/
│           └── res/
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## 🚀 Installation

1. Download the APK from the GitHub Release.
2. Install it on an Android device running Android 8.0 (API 26) or newer.
3. Launch **IP Calculator**.
4. Enter an IPv4 address and CIDR prefix.
5. Press **Calculate**.

## 📌 Version

**Current version: 1.0.0**

This release represents the completed version of the project.

## 📄 License

This project is provided for educational and portfolio purposes.
