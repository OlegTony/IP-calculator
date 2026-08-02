# 📱 IP Subnet Calculator

A modern, fast, and high-contrast Android application designed to calculate network parameters based on an IP address and subnet prefix. Built entirely with Kotlin and follow modern Android UI practices.

---

## ✨ Features
* 🔍 **Instant Calculation**: Computes Subnet Mask, Network ID, Broadcast Address, and Host Ranges on the fly.
* 🛡️ **Edge-case Support**: Properly handles special networks like `/31` (RFC 3021) and `/32` with "N/A" placeholders.
* ♿ **Accessibility & Contrast**: Built using fully compliant WCAG contrast text colors (`#212121` on `#FFFFFF`) for seamless reading.
* 📱 **Adaptive Layout**: Optimized layout to look clean on various screens, from standard phones to large foldable devices.

---

## 🛠️ Tech Stack & Architecture
* **Language**: [Kotlin](https://kotlinlang.org)
* **UI Framework**: Native XML Layouts with Material Design 3 components.
* **Minimum SDK**: Android 7.0 (API 24) or higher.
* **Build System**: Gradle (Kotlin DSL).

---

## ⚙️ Upcoming Features (In Development)
* 🔄 **Reset Button**: One-tap action to clear all text fields and reset analysis values.
* 🚫 **Input Validation**: Strict formatting checks to prevent incorrect IP addresses or prefix bounds out of range.
* 🌐 **Localization**: Automatic language switching between English and Russian based on system settings.
