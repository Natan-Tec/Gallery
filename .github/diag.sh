#!/bin/bash
PKG=com.natantec.levegaleria
APK=$(ls apk/*.apk | head -1)
adb install -r "$APK"

echo "=== 1) Abrindo sem permissao ==="
adb logcat -c
adb shell am start -W -n $PKG/.MainActivity
sleep 8
adb shell pidof $PKG && echo "PROCESSO VIVO" || echo "PROCESSO MORTO"
adb logcat -d | grep -E "AndroidRuntime|FATAL|Caused by|at com.natantec|at androidx|at coil|ClassNotFound|NoSuchMethod" | head -80

echo "=== 2) Abrindo com permissao ==="
adb shell pm grant $PKG android.permission.READ_EXTERNAL_STORAGE
adb shell am force-stop $PKG
adb logcat -c
adb shell am start -W -n $PKG/.MainActivity
sleep 8
adb shell pidof $PKG && echo "PROCESSO VIVO" || echo "PROCESSO MORTO"
adb logcat -d | grep -E "AndroidRuntime|FATAL|Caused by|at com.natantec|at androidx|at coil|ClassNotFound|NoSuchMethod" | head -80
exit 0
