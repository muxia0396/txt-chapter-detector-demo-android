# TXT Chapter Detector Demo (Android)

> 基于离线 TXT 目录识别算法的简单 Android 阅读 Demo。

项目不含小说正文、不访问网络，也不请求存储权限；用户通过系统文件选择器授予当前 TXT 的只读访问权限。

## 功能

- 纯离线 TXT 目录识别：传统章节、数字紧贴标题、空标题编号、上/下篇、番外和完结感言。
- 支持 UTF-8、UTF-16LE、UTF-16BE 与 GB18030。
- 目录点击跳转、上一章、下一章。
- 面向 Android 16：`compileSdk 36`、`targetSdk 36`、`minSdk 23`。
- 无广告、无分析 SDK、无网络权限、无第三方运行时依赖。

## UI 设计

- 长文件标题最多显示三行，标题区域为内容自适应高度，不会因固定单行高度而裁切。
- 四个操作采用两行两列等宽按钮：`打开 TXT / 目录 / 上一章 / 下一章`。
- 文件读取、解码和目录识别在后台线程执行，避免阻塞界面。

## 构建

需要 JDK 17、Android SDK Platform 36 和 Gradle：

```bash
gradle test assembleDebug
```

Windows：

```powershell
gradle test assembleDebug
```

APK 输出到：`app/build/outputs/apk/debug/app-debug.apk`。

## 使用

1. 安装 debug APK。
2. 点击“打开 TXT”，在系统文件选择器中选择本地 TXT。
3. 点击“目录”查看识别结果；点击目录项跳转正文。
4. 使用“上一章 / 下一章”顺序阅读。

本项目采用 [MIT License](LICENSE)。
