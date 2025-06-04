<h1 align="center">🗝 Ping Pong Smartlock 🗝</h1>

A custom smart lock solution based on the Ping Pong protocol running on raspberry devices.

## ✨ Key Features
- Support for multiple Keys
- Fullscreen QR visualization
- Simplistic QR authentication
- Intuitive unlocking procedure

## 📦 Requirements
### Verifier [*`lock`*]
```bash
pip install -r requirements.txt
```

### Prover [*`key`*]
Minimum Android SDK version of Android 11 is required.

## 🖥 Setup
### Verifier [*`lock`*]
```bash
git clone git@github.com:IImpaq/ping-pong.git
mv ping-pong/verifier/verifier lock/verifier
rm -rf ping-pong
```

### Prover [*`key`*]
*Setup is already done and is implemented in the code for the Smartphone app!*

The setup requires an import of the prover
library release under [prover-releases](https://github.com/IImpaq/ping-pong/releases).
This is done by downloading the latest library version and placing it in *`key/app/libs`* where it is able to be used as a dependency in the build.gradle.kts file of the application with:
```gradle
implementation(files("libs/pingpongprover-release.aar"))
```

## ⚙ Usage
### Verifier [*`lock`*]
```bash
cd lock
python -m main
```

### Prover [*`key`*]
The **`key`** project can simply be opened in Android Studio and loaded onto a physical smartphone device with Android 11 or higher via developer mode USB debugging, or by using the built-in device emulation. The code should compile and build out of the box.

## 🧠 Team & Roles
- **Christian Burtscher:** Prover rapid prototyping
- **Marcus Gugacs:** Verifier rapid prototyping
- **Tobias Marehart:** Testing/Debugging & Enhancements

## 📝 License
MIT License (see [LICENSE](LICENSE.md)).

## 📞 Contact
If you have any questions or want to get in touch, just [send an email](mailto:iimpaq@proton.me)

---
Made with 💛
