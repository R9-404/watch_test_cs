@echo off
cd /d "%~dp0careold"

rem ---- 可修改：JDK 路径（后端运行需要 JDK8 或 JDK17）----
if not defined JAVA_HOME set "JAVA_HOME=C:\Program Files\Java\jdk-17"

if not exist "%JAVA_HOME%\bin\java.exe" (
    echo [错误] 找不到 Java：%JAVA_HOME%\bin\java.exe
    echo 请安装 JDK，或编辑本脚本顶部的 JAVA_HOME 路径。
    pause
    exit /b 1
)

if not exist "target\careold-0.0.1-SNAPSHOT.jar" (
    echo [错误] 找不到后端 jar。
    echo 请先编译：cd careold ^&^& mvn -DskipTests package
    pause
    exit /b 1
)

echo 正在启动后端（端口 8795）...
"%JAVA_HOME%\bin\java" --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED --add-opens java.base/java.net=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED -jar target\careold-0.0.1-SNAPSHOT.jar
pause
