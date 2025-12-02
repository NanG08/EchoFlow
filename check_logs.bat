@echo off
REM Quick script to check ASL model loading logs

echo ========================================
echo Checking ASL Model Loading Logs
echo ========================================
echo.

echo Clearing old logs...
adb logcat -c

echo.
echo Open Sign Language mode in the app now!
echo Press Ctrl+C to stop viewing logs
echo.
echo ========================================
echo.

adb logcat -s System.out:I *:E | findstr /C:"ASL" /C:"model" /C:"assets" /C:"tflite"
