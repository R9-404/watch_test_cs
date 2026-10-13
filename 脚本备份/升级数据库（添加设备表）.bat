@echo off
cd /d "%~dp0"

rem ---- 如果 MySQL 账号密码不是 root/123456，请改下面两行 ----
set "DB_USER=root"
set "DB_PASS=123456"
rem ---- 如果 MySQL 服务名不一样，请改下面这行（一般不用改）----
set "MYSQL_SERVICE=MySQL2077"
rem ---- 数据库名 ----
set "DB_NAME=old"

echo ============================================
echo   智慧养老手表管理系统 - 升级：添加设备（手表）相关表
echo ============================================
echo.

rem 1. 确保 MySQL 已启动
sc query "%MYSQL_SERVICE%" >nul 2>&1
if errorlevel 1 (
    echo [提示] 没找到 MySQL 服务 %MYSQL_SERVICE%，跳过自动启动。
) else (
    sc query "%MYSQL_SERVICE%" | findstr /i "RUNNING" >nul
    if errorlevel 1 (
        echo 正在启动 MySQL 服务 ...
        net start "%MYSQL_SERVICE%" >nul 2>&1
    ) else (
        echo MySQL 服务已在运行。
    )
)

rem 2. 检查 mysql 命令
mysql --version >nul 2>&1
if errorlevel 1 (
    echo.
    echo [错误] 找不到 mysql 命令。
    echo 请先安装 MySQL，并确认它能用 mysql 命令打开。
    pause
    exit /b 1
)

rem 3. 检查脚本文件
if not exist "device_migration.sql" (
    echo [错误] 当前文件夹里找不到 device_migration.sql 文件。
    pause
    exit /b 1
)

echo.
echo 正在为数据库 %DB_NAME% 添加 device / device_location / bind_code 表 ...
mysql -u%DB_USER% -p%DB_PASS% --default-character-set=utf8mb4 %DB_NAME% < device_migration.sql
if errorlevel 1 (
    echo.
    echo [错误] 执行失败，请检查上面的账号密码和数据库名。
    pause
    exit /b 1
)

echo.
echo ============================================
echo   升级成功！
echo   现在可以在“基础管理 - 设备管理”里看到设备，
echo   也可以双击“启动模拟手表.bat”让页面数据动起来。
echo ============================================
echo.
pause
