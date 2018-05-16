~/Android/Sdk/platform-tools/adb logcat | grep `~/Android/Sdk/platform-tools/adb shell ps | grep fi.ct.mist.mist | cut -c10-15` | awk '{ print strftime("%Y-%m-%d %H:%M:%S"), $0; fflush(); }'
