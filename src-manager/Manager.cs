// 智慧养老手表管理系统 —— 统一管理程序（源码）
// 编译方式（Windows 自带 .NET 编译器，无需安装任何东西）：
//   C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe /nologo /optimize+ /out:..\智慧养老管理系统.exe src-manager\Manager.cs
// 说明：本文件故意使用 C# 5 语法，保证在 .NET Framework 4.0 的 csc 下也能编译通过。

using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Threading;

internal static class Manager
{
    // ==================== 可按需修改的配置 ====================
    private const string MYSQL_USER = "root";
    private const string MYSQL_PASSWORD = "123456";
    private const string MYSQL_SERVICE = "MySQL2077";
    private const string DB_NAME = "old";
    private const string JAVA_HOME = @"C:\Program Files\Java\jdk-17";
    private const int BACKEND_PORT = 8795;
    private const int FRONTEND_PORT = 8087;
    // ========================================================

    private static string root;
    private static Encoding consoleEncoding = Encoding.Default;
    private static readonly string[] JavaArgs = new string[]
    {
        "--add-opens", "java.base/java.lang=ALL-UNNAMED",
        "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED",
        "--add-opens", "java.base/java.util=ALL-UNNAMED",
        "--add-opens", "java.base/java.io=ALL-UNNAMED",
        "--add-opens", "java.base/java.net=ALL-UNNAMED",
        "--add-opens", "java.base/java.nio=ALL-UNNAMED",
        "--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED"
    };

    private static int Main(string[] args)
    {
        Console.Title = "智慧养老手表管理系统 - 管理台";
        root = AppDomain.CurrentDomain.BaseDirectory.TrimEnd('\\');
        DetectConsoleEncoding();

        if (args.Length > 0)
        {
            // 第二个参数写 yes 可以跳过确认（自动化测试用）
            bool force = args.Length > 1 && args[1].Trim().ToLower() == "yes";
            return RunOnce(args[0], force);
        }

        while (true)
        {
            ClearScreen();
            PrintBanner();
            PrintStatus();
            PrintMenu();

            string choice = Ask("请选择");
            Console.WriteLine();
            switch (choice)
            {
                case "1": StartSystem(); break;
                case "2": StopSystem(); break;
                case "3": ShowStatus(); break;
                case "4": InitDatabase(); break;
                case "5": UpgradeDatabase(); break;
                case "6": SimulatorMenu(); break;
                case "7": OpenManual(); break;
                case "8": ShowAccounts(); break;
                case "9": OpenFolder(); break;
                case "0": return 0;
                default:
                    Warn("没有这个选项，请输入 0-9");
                    Pause();
                    break;
            }
        }
    }

    /** 支持命令行直接执行：/start /stop /sim /init /upgrade /manual /status */
    private static int RunOnce(string cmd, bool force)
    {
        string c = cmd.TrimStart('-', '/').ToLower();
        if (c == "start") { StartSystem(); return 0; }
        if (c == "stop") { StopSystem(); return 0; }
        if (c == "sim") { StartSimulator(""); return 0; }
        if (c == "init") { InitDatabase(force); return 0; }
        if (c == "upgrade") { UpgradeDatabase(); return 0; }
        if (c == "manual") { OpenManual(); return 0; }
        if (c == "status") { ShowStatus(); return 0; }
        Warn("未知参数：" + cmd);
        return 1;
    }

    // ==================== 界面 ====================

    private static void PrintBanner()
    {
        Console.WriteLine("============================================================");
        Console.WriteLine("            智慧养老手表管理系统 · 管理台");
        Console.WriteLine("============================================================");
        Console.WriteLine("  目录：" + root);
        Console.WriteLine();
    }

    private static void PrintStatus()
    {
        bool mysql = IsPortOpen(3306);
        bool backend = IsPortOpen(BACKEND_PORT);
        bool frontend = IsPortOpen(FRONTEND_PORT);
        int simCount = CountSimulatorProcesses();

        Console.WriteLine("  当前状态：MySQL " + Tag(mysql)
            + "    后端 " + Tag(backend)
            + "    前端 " + Tag(frontend)
            + "    模拟手表 " + (simCount > 0 ? "运行中(" + simCount + ")" : "已停止"));
        if (frontend)
        {
            Console.WriteLine("  访问地址：http://localhost:" + FRONTEND_PORT + "/   （管理员 15179158704 / 123456）");
        }
        Console.WriteLine();
    }

    private static string Tag(bool on)
    {
        return on ? "[运行中]" : "[已停止]";
    }

    private static void PrintMenu()
    {
        Console.WriteLine("  [1] 启动系统          MySQL + 后端 + 前端，并自动打开浏览器");
        Console.WriteLine("  [2] 停止系统          关闭后端与前端");
        Console.WriteLine("  [3] 查看系统状态      刷新上面的状态信息");
        Console.WriteLine("  [4] 初始化/导入数据库  首次使用：导入 old.sql（会覆盖现有数据）");
        Console.WriteLine("  [5] 升级数据库        老库补表：邀请码表、设备（手表）表");
        Console.WriteLine("  [6] 启动模拟手表      没有真机时，让页面数据实时变化");
        Console.WriteLine("  [7] 打开使用手册      用记事本打开《系统使用手册.md》");
        Console.WriteLine("  [8] 查看登录账号与地址");
        Console.WriteLine("  [9] 打开程序所在文件夹");
        Console.WriteLine("  [0] 退出");
        Console.WriteLine();
    }

    private static void ShowStatus()
    {
        PrintStatus();
        Console.WriteLine("  数据库连接检查：");
        // 列名用英文，避免 mysql 客户端输出列名时控制台编码不一致出现乱码
        int code = RunProcess(Path.Combine(GetMysqlBin(), "mysql.exe"),
            "-u" + MYSQL_USER + " -p" + MYSQL_PASSWORD + " -e \"select count(*) as cnt from oldperson;\" " + DB_NAME, true);
        if (code == 0) { Info("数据库 " + DB_NAME + " 可以正常连接（上面 cnt 即老人数量）。"); }
        else { Warn("数据库连接失败，请检查 MySQL 账号密码与库是否已导入。"); }
        Pause();
    }

    private static void ShowAccounts()
    {
        Info("管理员：15179158704 / 123456");
        Info("家属：  15179158702 / 123456（田有村，关联老人「田风」）");
        Info("演示设备：WATCH0001 / demo-secret-1（绑定 姜涛）");
        Info("          WATCH0002 / demo-secret-2（绑定 王明）");
        Info("          WATCH0003 / demo-secret-3（绑定 田风）");
        Info("网页地址：http://localhost:" + FRONTEND_PORT + "/");
        Info("手表/模拟手表服务器地址：http://<电脑局域网IP>:" + BACKEND_PORT);
        Pause();
    }

    // ==================== 1. 启动系统 ====================

    private static void StartSystem()
    {
        Console.WriteLine("[1/5] 检查 MySQL ...");
        EnsureMysqlService();

        Console.WriteLine("[2/5] 检查后端程序 ...");
        string jar = Path.Combine(root, @"careold\target\careold-0.0.1-SNAPSHOT.jar");
        string java = Path.Combine(JAVA_HOME, @"bin\java.exe");
        if (!File.Exists(jar))
        {
            Error("找不到后端程序：" + jar);
            Error("请先重新编译后端：cd careold && mvn -DskipTests package");
            Pause();
            return;
        }
        if (!File.Exists(java))
        {
            Error("找不到 Java：" + java);
            Error("请安装 JDK，或修改本程序（Manager.cs）顶部的 JAVA_HOME 后重新编译。");
            Pause();
            return;
        }

        Console.WriteLine("[3/5] 启动后端（端口 " + BACKEND_PORT + "）...");
        if (IsPortOpen(BACKEND_PORT))
        {
            Info("后端已经在运行，跳过。");
        }
        else
        {
            StartProcess(java, BuildJavaArgs(jar), root);
            Console.Write("      等待后端启动");
            bool up = WaitPort(BACKEND_PORT, 60);
            Console.WriteLine();
            if (up) { Info("后端已启动。"); }
            else { Warn("后端 60 秒内没有起来，请看后端窗口里的报错。"); }
        }

        Console.WriteLine("[4/5] 启动前端（端口 " + FRONTEND_PORT + "）...");
        if (IsPortOpen(FRONTEND_PORT))
        {
            Info("前端已经在运行，跳过。");
        }
        else
        {
            string node = Path.Combine(root, @"tools\node-v10.24.1-win-x64\node.exe");
            if (!File.Exists(node)) { node = "node"; }
            string wds = Path.Combine(root, @"oldIot\node_modules\webpack-dev-server\bin\webpack-dev-server.js");
            if (!File.Exists(wds))
            {
                Error("前端依赖缺失：" + wds);
                Error("请先在 oldIot 目录执行：npm install --ignore-scripts");
                Pause();
                return;
            }
            string frontDir = Path.Combine(root, "oldIot");
            StartProcess(node, "\"" + wds + "\" --inline --progress --config build\\webpack.dev.conf.js", frontDir);
            Console.Write("      等待前端编译");
            bool up = WaitPort(FRONTEND_PORT, 120);
            Console.WriteLine();
            if (up) { Info("前端已启动。"); }
            else { Warn("前端还没起来，第一次编译比较慢，稍等片刻再刷新浏览器。"); }
        }

        Console.WriteLine("[5/5] 打开浏览器 ...");
        try { Process.Start("http://localhost:" + FRONTEND_PORT + "/"); }
        catch (Exception e) { Warn("自动打开浏览器失败：" + e.Message); }

        Console.WriteLine();
        Info("启动完成！访问地址 http://localhost:" + FRONTEND_PORT + "/");
        Info("提示：想看实时数据，回到菜单选 6 启动模拟手表。");
        Console.WriteLine();
        Info("后端、前端各开了一个窗口，请不要关闭它们。");
        Pause();
    }

    private static string BuildJavaArgs(string jar)
    {
        List<string> list = new List<string>();
        list.AddRange(JavaArgs);
        list.Add("-jar");
        list.Add("\"" + jar + "\"");
        return string.Join(" ", list.ToArray());
    }

    private static void EnsureMysqlService()
    {
        if (IsPortOpen(3306))
        {
            Info("MySQL 已在运行。");
            return;
        }
        Info("MySQL 未运行，尝试启动服务 " + MYSQL_SERVICE + " ...");
        int code = RunProcess("net", "start " + MYSQL_SERVICE, true);
        if (WaitPort(3306, 30))
        {
            Info("MySQL 已启动。");
        }
        else
        {
            Warn("MySQL 启动失败（退出码 " + code + "），请手动启动，或检查服务名是否为 " + MYSQL_SERVICE);
        }
    }

    // ==================== 2. 停止系统 ====================

    private static void StopSystem()
    {
        bool any = false;
        any |= KillByPort(BACKEND_PORT, "后端");
        any |= KillByPort(FRONTEND_PORT, "前端");
        any |= KillSimulators();
        Console.WriteLine();
        if (any) { Info("已停止。"); }
        else { Info("本来就没有在运行。"); }
        Pause();
    }

    private static bool KillByPort(int port, string name)
    {
        List<int> pids = GetPortPids(port);
        if (pids.Count == 0)
        {
            Info(name + "（端口 " + port + "）没有在运行。");
            return false;
        }
        foreach (int pid in pids)
        {
            Console.WriteLine("      正在关闭" + name + "（PID " + pid + "）...");
            KillProcess(pid);
        }
        return true;
    }

    private static bool KillSimulators()
    {
        List<int> pids = GetSimulatorPids();
        if (pids.Count == 0)
        {
            Info("模拟手表没有在运行。");
            return false;
        }
        foreach (int pid in pids)
        {
            Console.WriteLine("      正在关闭模拟手表（PID " + pid + "）...");
            KillProcess(pid);
        }
        return true;
    }

    /**
     * 结束进程并确认真的退出。
     * 只用 taskkill 有时会失败（权限/占用），这里再补一次 WMI Terminate，
     * 并等到进程消失、文件句柄释放为止——否则紧接着重新打包 jar 会因为文件被占用而失败。
     */
    private static void KillProcess(int pid)
    {
        if (!IsProcessAlive(pid))
        {
            return;
        }
        RunProcess("taskkill", "/PID " + pid + " /T /F", false);
        if (WaitProcessExit(pid, 8))
        {
            return;
        }
        CaptureProcess("wmic", "process where processid=" + pid + " call terminate");
        if (WaitProcessExit(pid, 10))
        {
            return;
        }
        Warn("进程 " + pid + " 没有退出，可能仍在占用文件，请手动在任务管理器结束它。");
    }

    private static bool IsProcessAlive(int pid)
    {
        try
        {
            Process p = Process.GetProcessById(pid);
            return p != null;
        }
        catch
        {
            return false;
        }
    }

    private static bool WaitProcessExit(int pid, int seconds)
    {
        try
        {
            Process p = Process.GetProcessById(pid);
            if (p.WaitForExit(seconds * 1000))
            {
                return true;
            }
            return !IsProcessAlive(pid);
        }
        catch
        {
            return true;
        }
    }

    /** 通过 wmic 找出命令行里带 watch-sim.js 的 node 进程 */
    private static List<int> GetSimulatorPids()
    {
        List<int> result = new List<int>();
        string output = CaptureProcess("wmic",
            "process where \"name='node.exe'\" get ProcessId,CommandLine /format:csv");
        string[] lines = output.Split('\n');
        foreach (string line in lines)
        {
            if (line.IndexOf("watch-sim.js", StringComparison.OrdinalIgnoreCase) < 0)
            {
                continue;
            }
            string[] parts = line.Trim().Split(',');
            int pid;
            if (parts.Length >= 2 && int.TryParse(parts[parts.Length - 1].Trim(), out pid))
            {
                result.Add(pid);
            }
        }
        return result;
    }

    private static int CountSimulatorProcesses()
    {
        return GetSimulatorPids().Count;
    }

    /** 用 netstat 找占用端口的进程号 */
    private static List<int> GetPortPids(int port)
    {
        List<int> pids = new List<int>();
        string output = CaptureProcess("netstat", "-ano -p tcp");
        string[] lines = output.Split('\n');
        string portTag = ":" + port;
        foreach (string line in lines)
        {
            string text = line.Trim();
            if (text.Length == 0 || text.IndexOf("LISTENING", StringComparison.OrdinalIgnoreCase) < 0)
            {
                continue;
            }
            string[] parts = text.Split(new char[] { ' ', '\t' }, StringSplitOptions.RemoveEmptyEntries);
            if (parts.Length < 4)
            {
                continue;
            }
            if (!parts[1].EndsWith(portTag, StringComparison.Ordinal))
            {
                continue;
            }
            int pid;
            if (int.TryParse(parts[parts.Length - 1], out pid) && !pids.Contains(pid))
            {
                pids.Add(pid);
            }
        }
        return pids;
    }

    // ==================== 3. 数据库 ====================

    private static void InitDatabase()
    {
        InitDatabase(false);
    }

    private static void InitDatabase(bool skipConfirm)
    {
        Warn("注意：导入会重建数据库里的表并写入示例数据，现有数据会被覆盖！");
        if (!skipConfirm && !Confirm("确定要继续吗")) { Info("已取消。"); Pause(); return; }

        string mysql = Path.Combine(GetMysqlBin(), "mysql.exe");
        if (!File.Exists(mysql)) { Error("找不到 MySQL 客户端：" + mysql); Pause(); return; }
        string sqlFile = Path.Combine(root, "old.sql");
        if (!File.Exists(sqlFile)) { Error("找不到 old.sql"); Pause(); return; }

        Console.WriteLine("第 1 步：重建数据库 " + DB_NAME + "（先删后建，保证干净）...");
        int code = RunProcess(mysql,
            "-u" + MYSQL_USER + " -p" + MYSQL_PASSWORD
            + " -e \"DROP DATABASE IF EXISTS `" + DB_NAME + "`; CREATE DATABASE `" + DB_NAME
            + "` DEFAULT CHARACTER SET utf8mb4;\"", true);
        if (code != 0)
        {
            Error("创建数据库失败，请检查账号密码（当前配置：" + MYSQL_USER + "/" + MYSQL_PASSWORD + "）。");
            Pause();
            return;
        }

        Console.WriteLine("第 2 步：导入 " + Path.GetFileName(sqlFile) + " ...");
        code = PipeFileToMysql(mysql, sqlFile);
        if (code != 0)
        {
            Error("导入失败，请检查 MySQL 是否可登录。");
            Pause();
            return;
        }

        Console.WriteLine("第 3 步：补充新增功能用到的表和字段（邀请码、设备、告警处置）...");
        string[] extra = ListMigrationFiles();
        if (extra.Length == 0) { Warn("没有找到 *migration*.sql，跳过。"); }
        foreach (string name in extra)
        {
            string file = Path.Combine(root, name);
            int c2 = PipeFileToMysql(mysql, file);
            if (c2 == 0) { Info(name + " 执行成功。"); }
            else { Warn(name + " 执行失败，可以稍后用菜单 5 单独升级。"); }
        }

        Console.WriteLine();
        Info("导入成功！现在可以选 1 启动系统了。");
        Info("如果系统之前就在运行，请先选 2 停止，再选 1 重新启动。");
        Pause();
    }

    private static void UpgradeDatabase()
    {
        string mysql = Path.Combine(GetMysqlBin(), "mysql.exe");
        if (!File.Exists(mysql)) { Error("找不到 MySQL 客户端：" + mysql); Pause(); return; }
        if (!IsPortOpen(3306)) { EnsureMysqlService(); }

        string[] files = ListMigrationFiles();
        if (files.Length == 0) { Warn("程序目录下没有找到 *migration*.sql 文件。"); Pause(); return; }
        foreach (string name in files)
        {
            string file = Path.Combine(root, name);
            Console.WriteLine("正在执行：" + name);
            int code = PipeFileToMysql(mysql, file);
            if (code == 0) { Info(name + " 执行成功。"); }
            else { Error(name + " 执行失败（退出码 " + code + "）。"); }
        }
        Console.WriteLine();
        Info("升级完成（重复执行也不会出错）。");
        Pause();
    }

    /** 程序目录下所有 *migration*.sql，按文件名排序执行（可重复执行） */
    private static string[] ListMigrationFiles()
    {
        try
        {
            List<string> result = new List<string>();
            string[] files = Directory.GetFiles(root, "*migration*.sql");
            Array.Sort(files, StringComparer.OrdinalIgnoreCase);
            foreach (string file in files)
            {
                result.Add(Path.GetFileName(file));
            }
            return result.ToArray();
        }
        catch (Exception e)
        {
            Warn("读取升级脚本失败：" + e.Message);
            return new string[0];
        }
    }

    /**
     * 把一个 .sql 文件导入数据库。
     * 实现方式：用 cmd 的输入重定向（mysql ... < 文件），而不是往 mysql 的 stdin 写字符串——
     * 后者在 SQL 文件较大时容易写不完就关闭管道，报"管道已结束"。
     */
    private static int PipeFileToMysql(string mysql, string sqlFile)
    {
        try
        {
            string command = "\"\"" + mysql + "\" -u" + MYSQL_USER + " -p" + MYSQL_PASSWORD
                + " --default-character-set=utf8mb4 " + DB_NAME + " < \"" + sqlFile + "\"\"";
            ProcessStartInfo info = new ProcessStartInfo("cmd.exe", "/c " + command);
            info.WorkingDirectory = root;
            info.UseShellExecute = false;
            info.RedirectStandardOutput = true;
            info.RedirectStandardError = true;
            info.CreateNoWindow = true;
            using (Process p = Process.Start(info))
            {
                string stdout = p.StandardOutput.ReadToEnd();
                string stderr = p.StandardError.ReadToEnd();
                p.WaitForExit();
                if (stdout.Trim().Length > 0) { Console.Write(Encode(stdout)); }
                if (p.ExitCode != 0 && stderr.Trim().Length > 0) { Console.Write(Encode(stderr)); }
                return p.ExitCode;
            }
        }
        catch (Exception e)
        {
            Error("执行失败：" + e.Message);
            return -1;
        }
    }

    private static string GetMysqlBin()
    {
        string[] candidates = new string[]
        {
            @"C:\Program Files\MySQL\MySQL Server 8.0\bin",
            @"C:\Program Files\MySQL\MySQL Server 5.7\bin",
            @"D:\Program Files\MySQL\MySQL Server 8.0\bin"
        };
        foreach (string dir in candidates)
        {
            if (Directory.Exists(dir)) { return dir; }
        }
        return candidates[0];
    }

    // ==================== 4. 模拟手表 ====================

    private static void SimulatorMenu()
    {
        Console.WriteLine("  模拟手表选项：");
        Console.WriteLine("    [1] 标准演示（每 3 秒上报，循环演示 正常/心率异常/摔倒/SOS）");
        Console.WriteLine("    [2] 只报正常数据");
        Console.WriteLine("    [3] 高频上报（每 1 秒）");
        Console.WriteLine("    [4] 换一台设备（WATCH0002）");
        Console.WriteLine("    [5] 停止模拟手表");
        Console.WriteLine("    [0] 返回主菜单");
        Console.WriteLine();
        string choice = Ask("请选择");
        string extra;
        switch (choice)
        {
            case "1": extra = ""; break;
            case "2": extra = " --scenario normal"; break;
            case "3": extra = " --interval 1"; break;
            case "4": extra = " --deviceNo WATCH0002 --secret demo-secret-2"; break;
            case "5": KillSimulators(); Pause(); return;
            case "0": return;
            default: Warn("没有这个选项"); Pause(); return;
        }
        StartSimulator(extra);
    }

    private static void StartSimulator(string extraArgs)
    {
        string node = Path.Combine(root, @"tools\node-v10.24.1-win-x64\node.exe");
        if (!File.Exists(node)) { node = "node"; }
        string script = Path.Combine(root, @"watch-sim\watch-sim.js");
        if (!File.Exists(script))
        {
            Error("找不到模拟手表脚本：" + script);
            Pause();
            return;
        }
        string workDir = Path.Combine(root, "watch-sim");
        StartProcess(node, "\"" + script + "\"" + extraArgs, workDir);
        Info("模拟手表已在一个新窗口启动。");
        Info("打开 http://localhost:" + FRONTEND_PORT + "/ 的「设备管理」「心率血压」「位置信息」就能看到数据在变。");
        Info("关闭那个窗口，或回到这里选 6 -> 5 即可停止。");
        Pause();
    }

    // ==================== 5. 手册与文件夹 ====================

    private static void OpenManual()
    {
        string manual = Path.Combine(root, "系统使用手册.md");
        if (!File.Exists(manual))
        {
            Error("找不到《系统使用手册.md》");
            Pause();
            return;
        }
        try
        {
            Process.Start(new ProcessStartInfo(manual) { UseShellExecute = true });
            Info("已用系统默认程序打开《系统使用手册.md》。");
        }
        catch (Exception e)
        {
            Error("打开失败：" + e.Message);
            Info("文件位置：" + manual);
        }
        Pause();
    }

    private static void OpenFolder()
    {
        try { Process.Start("explorer.exe", "\"" + root + "\""); }
        catch (Exception e) { Error(e.Message); }
    }

    // ==================== 工具方法 ====================

    private static bool IsPortOpen(int port)
    {
        TcpClient client = null;
        try
        {
            client = new TcpClient();
            IAsyncResult ar = client.BeginConnect(IPAddress.Loopback, port, null, null);
            bool ok = ar.AsyncWaitHandle.WaitOne(400);
            if (ok && client.Connected) { client.EndConnect(ar); return true; }
            return false;
        }
        catch
        {
            return false;
        }
        finally
        {
            if (client != null) { client.Close(); }
        }
    }

    private static bool WaitPort(int port, int seconds)
    {
        for (int i = 0; i < seconds; i++)
        {
            if (IsPortOpen(port)) { return true; }
            Console.Write(".");
            Thread.Sleep(1000);
        }
        return IsPortOpen(port);
    }

    private static void StartProcess(string exe, string arguments, string workDir)
    {
        try
        {
            ProcessStartInfo info = new ProcessStartInfo(exe, arguments);
            info.WorkingDirectory = workDir;
            info.UseShellExecute = true;
            Process.Start(info);
        }
        catch (Exception e)
        {
            Error("启动失败：" + exe + " —— " + e.Message);
        }
    }

    private static void StartProcess(string exe, string[] arguments, string workDir)
    {
        StartProcess(exe, string.Join(" ", arguments), workDir);
    }

    private static int RunProcess(string exe, string arguments, bool showOutput)
    {
        try
        {
            ProcessStartInfo info = new ProcessStartInfo(exe, arguments);
            info.UseShellExecute = false;
            info.CreateNoWindow = true;
            info.RedirectStandardOutput = true;
            info.RedirectStandardError = true;
            using (Process p = Process.Start(info))
            {
                string stdout = p.StandardOutput.ReadToEnd();
                string stderr = p.StandardError.ReadToEnd();
                p.WaitForExit();
                if (showOutput)
                {
                    if (stdout.Trim().Length > 0) { Console.Write(Encode(stdout)); }
                    if (stderr.Trim().Length > 0) { Console.Write(Encode(stderr)); }
                }
                return p.ExitCode;
            }
        }
        catch (Exception e)
        {
            if (showOutput) { Error("执行 " + exe + " 失败：" + e.Message); }
            return -1;
        }
    }

    private static string CaptureProcess(string exe, string arguments)
    {
        try
        {
            ProcessStartInfo info = new ProcessStartInfo(exe, arguments);
            info.UseShellExecute = false;
            info.CreateNoWindow = true;
            info.RedirectStandardOutput = true;
            info.RedirectStandardError = true;
            using (Process p = Process.Start(info))
            {
                string stdout = p.StandardOutput.ReadToEnd();
                p.StandardError.ReadToEnd();
                p.WaitForExit();
                return stdout;
            }
        }
        catch
        {
            return "";
        }
    }

    private static string Encode(string text)
    {
        byte[] bytes = Encoding.Default.GetBytes(text);
        return consoleEncoding.GetString(bytes);
    }

    private static void DetectConsoleEncoding()
    {
        try
        {
            uint cp = GetConsoleOutputCP();
            if (cp == 0) { cp = 936; }
            consoleEncoding = Encoding.GetEncoding((int)cp);
        }
        catch
        {
            consoleEncoding = Encoding.Default;
        }
    }

    /** 输出被重定向时没有控制台缓冲区，Clear 会抛异常，这里吞掉即可 */
    private static void ClearScreen()
    {
        try { Console.Clear(); }
        catch { }
    }

    [System.Runtime.InteropServices.DllImport("kernel32.dll")]
    private static extern uint GetConsoleOutputCP();

    private static string Ask(string prompt)
    {
        Console.Write("  " + prompt + "：");
        string line = Console.ReadLine();
        // 输入被重定向或直接回车（EOF）时 ReadLine 返回 null，这里按"退出"处理，避免死循环
        return line == null ? "0" : line.Trim();
    }

    private static bool Confirm(string prompt)
    {
        Console.Write("  " + prompt + "？(y/N) ");
        string line = Console.ReadLine();
        return line != null && line.Trim().ToLower().StartsWith("y");
    }

    private static void Pause()
    {
        Console.WriteLine();
        Console.Write("  按回车键返回菜单 ...");
        Console.ReadLine();
    }

    private static void Info(string text) { Console.WriteLine("  " + text); }
    private static void Warn(string text) { Console.WriteLine("  [注意] " + text); }
    private static void Error(string text) { Console.WriteLine("  [错误] " + text); }
}
