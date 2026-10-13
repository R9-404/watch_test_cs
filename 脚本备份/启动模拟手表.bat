@echo off
cd /d "%~dp0watch-sim"

rem ---- 内置 Node 10 便携版（无需另外安装 Node）----
set "NODE=%~dp0tools\node-v10.24.1-win-x64\node.exe"
if not exist "%NODE%" set "NODE=node"

if not exist "watch-sim.js" (
    echo [错误] 找不到 watch-sim\watch-sim.js
    pause
    exit /b 1
)

echo ============================================
echo   模拟手表端（阶段2）
echo ============================================
echo.
echo 服务器地址、设备号、密钥在 watch-sim\watch-sim.config.json 里改。
echo 设备号/密钥可在系统「基础管理 - 设备管理」里查看。
echo 演示时看「位置信息」「心率血压」「睡眠步数」页面，数据会实时变化。
echo 按 Ctrl+C 停止。
echo.

"%NODE%" watch-sim.js %*
pause
