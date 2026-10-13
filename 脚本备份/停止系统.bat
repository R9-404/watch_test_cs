@echo off
echo 正在停止 智慧养老手表管理系统 ...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8087 :8795" ^| findstr "LISTENING"') do (
    echo   结束进程 PID=%%a
    taskkill /f /pid %%a >nul 2>&1
)
echo 完成！
pause
