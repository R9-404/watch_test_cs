@echo off
cd /d "%~dp0"

rem ---- 如果 MySQL 账号密码不是 root/123456，请改下面两行 ----
set "DB_USER=root"
set "DB_PASS=123456"
rem ---- 如果 MySQL 服务名不一样，请改下面这行（一般不用改）----
set "MYSQL_SERVICE=MySQL2077"

echo ============================================
echo   智慧养老手表管理系统 - 一键导入数据库
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

rem 3. 检查 old.sql
if not exist "old.sql" (
    echo [错误] 当前文件夹里找不到 old.sql 文件。
    pause
    exit /b 1
)

rem 4. 创建数据库
echo.
echo 第 1 步：创建数据库 old ...
mysql -u%DB_USER% -p%DB_PASS% -e "CREATE DATABASE IF NOT EXISTS `old` DEFAULT CHARACTER SET utf8mb4;"
if errorlevel 1 (
    echo [错误] 创建数据库失败，请检查上面的账号密码。
    pause
    exit /b 1
)

rem 5. 导入数据
echo 第 2 步：导入数据（old.sql）...
mysql -u%DB_USER% -p%DB_PASS% --default-character-set=utf8mb4 old < old.sql
if errorlevel 1 (
    echo [错误] 导入失败，请检查 old.sql 和账号密码。
    pause
    exit /b 1
)

echo.
echo ============================================
echo   导入成功！
echo   现在可以双击 启动系统.bat 打开系统了。
echo ============================================
echo.
pause

