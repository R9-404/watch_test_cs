@echo off

set "MYSQL_SERVICE=MySQL2077"

echo ============================================
echo    智慧养老手表管理系统 一键启动
echo ============================================
echo.

echo [1/3] 检查 MySQL 服务 ...
sc query "%MYSQL_SERVICE%" >nul 2>&1
if errorlevel 1 (
    echo   [!] 未找到服务 %MYSQL_SERVICE%，请确认 MySQL 已安装
) else (
    sc query "%MYSQL_SERVICE%" | findstr /i "RUNNING" >nul
    if errorlevel 1 (
        echo   正在启动 MySQL ...
        net start "%MYSQL_SERVICE%" >nul 2>&1
    ) else (
        echo   MySQL 已在运行
    )
)

echo [2/3] 启动后端与前端（各开一个窗口）...
start "careold-backend-8795" "%~dp0启动后端.bat"
start "oldiot-frontend-8087" "%~dp0启动前端.bat"

echo [3/3] 等待服务启动并打开浏览器 ...
timeout /t 15 /nobreak >nul
start "" "http://localhost:8087/"

echo.
echo 启动完成！访问地址 http://localhost:8087/
echo 账号：15179158704 / 123456
echo.
pause
