# 这个目录原来放着「Node 10 便携版」

前端的 `oldIot` 是 webpack 3 + webpack-dev-server 2.9 的老项目，**必须用 Node 10 运行**
（新版 Node 会因为 OpenSSL 问题直接报错）。

为了不把 25MB 的 `node.exe` 二进制放进 Git 仓库，它已被 `.gitignore` 忽略。
克隆本项目后，请按下面任一方式把运行时补回来：

## 方式一：下载官方 Node 10（推荐）

1. 下载：https://nodejs.org/dist/v10.24.1/node-v10.24.1-win-x64.zip
2. 解压，把里面的 `node.exe` 放到本目录下，最终路径为：
   ```
   tools/node-v10.24.1-win-x64/node.exe
   ```
3. 双击项目根目录的 `智慧养老管理系统.exe`，菜单选 `1` 启动系统即可。

## 方式二：用系统里已装的 Node 10

如果电脑上已经装了 Node 10 并加进了 PATH，把 `node.exe` 复制到上面的路径也可以，
或者直接改 `src-manager/Manager.cs` 里的 node 路径后重新编译 exe。

## 前端依赖也要装一次

`oldIot/node_modules` 同样没有入库，第一次使用需要安装依赖（用 Node 10 的 npm）：

```
cd oldIot
npm install --ignore-scripts
```

> `--ignore-scripts` 是为了跳过 node-sass 之类的原生模块编译，前端界面不需要它。
> 如果安装很慢，可以先配置国内镜像：`npm config set registry https://registry.npmmirror.com`
