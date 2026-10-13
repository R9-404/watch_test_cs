@echo off
cd /d "%~dp0oldIot"

rem ---- 内置 Node 10 便携版（webpack3 项目需要旧版 Node）----
set "NODE=%~dp0tools\node-v10.24.1-win-x64\node.exe"
if not exist "%NODE%" set "NODE=node"

if not exist "node_modules" (
    echo [错误] 找不到 node_modules。
    echo 请先安装依赖：cd oldIot ^&^& npm install --ignore-scripts
    pause
    exit /b 1
)

echo 正在启动前端（端口 8087）...
"%NODE%" node_modules\webpack-dev-server\bin\webpack-dev-server.js --inline --progress --config build\webpack.dev.conf.js
pause
