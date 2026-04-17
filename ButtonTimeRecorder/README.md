# 按钮时间记录器 - Android APP

> 原PC端 `按钮时间记录器.exe` 的 Android 移植版

## 功能完整对照

| 原PC端功能 | Android APP |
|-----------|-------------|
| 实时时钟显示 | ✅ 顶部时钟每秒刷新 |
| 添加自定义按钮 | ✅ 输入名称 → 点击添加 |
| 流式布局（自动换行） | ✅ FlowLayout自定义控件 |
| 按钮绿色=未按下 / 红色=已按下 | ✅ 颜色切换 |
| 删除单个按钮（×） | ✅ 每个按钮右上角×按钮 |
| 记录按下/弹起时间 | ✅ 含时间戳、HH:mm显示 |
| 计算持续时长 | ✅ 时分秒格式 |
| 表格展示记录 | ✅ RecyclerView |
| 搜索/过滤按钮 | ✅ 实时搜索 |
| 清空记录 | ✅ 带确认对话框 |
| 删除全部按钮 | ✅ 带确认对话框 |
| 从Excel导入按钮名称 | ✅ D列+L列，第4行起 |
| 导出到Excel | ✅ Apache POI，可分享 |

---

## 打包 APK 步骤

### 方法一：使用 Android Studio（推荐）

#### 1. 前置条件
- 安装 [Android Studio](https://developer.android.com/studio)（2023.x 或更高版本）
- JDK 17（Android Studio自带）

#### 2. 打开项目
1. 启动 Android Studio
2. 选择 **File → Open**
3. 导航到本项目文件夹：`C:\Users\111\WorkBuddy\Claw\ButtonTimeRecorder`
4. 点击 **OK**，等待 Gradle 同步（首次需要下载依赖，可能需要几分钟）

#### 3. 生成 Debug APK（快速测试）
1. 菜单：**Build → Build Bundle(s) / APK(s) → Build APK(s)**
2. 等待构建完成
3. 点击右下角通知 **locate** 定位APK
4. APK路径：`app/build/outputs/apk/debug/app-debug.apk`

#### 4. 生成 Release APK（发布版）
1. 菜单：**Build → Generate Signed Bundle / APK**
2. 选择 **APK**
3. 创建或选择签名文件（.jks）
4. 填写密码信息
5. 选择 **release** 构建类型
6. 点击 **Finish**
7. APK路径：`app/build/outputs/apk/release/app-release.apk`

---

### 方法二：命令行构建

#### 前置条件
确认 Android SDK 已安装，并在 `local.properties` 中配置正确路径。

```bash
# 进入项目目录
cd C:\Users\111\WorkBuddy\Claw\ButtonTimeRecorder

# 下载 Gradle Wrapper（如果没有gradlew.bat）
# 或者直接使用已安装的 Gradle

# 构建 Debug APK
gradlew.bat assembleDebug

# 构建 Release APK（需要签名配置）
gradlew.bat assembleRelease
```

APK输出位置：`app\build\outputs\apk\debug\app-debug.apk`

---

### 方法三：在线构建（无需本地环境）

使用 [GitHub Actions](https://github.com) 或 [Bitrise](https://bitrise.io) 等CI平台，
将项目上传后自动构建 APK。

---

## 安装APK到手机

### 方式1：直接传输
1. 用USB连接手机
2. 将APK文件复制到手机存储
3. 在手机文件管理器中找到APK并安装

### 方式2：adb安装（推荐）
```bash
adb install app-debug.apk
```

### 注意事项
- 手机需要开启"**安装未知来源应用**"（设置→安全）
- Android 5.0 (API 21) 及以上系统均支持

---

## 项目结构

```
ButtonTimeRecorder/
├── app/
│   ├── build.gradle              # 依赖配置
│   └── src/main/
│       ├── AndroidManifest.xml   # 权限&组件声明
│       ├── java/com/example/buttontimerecorder/
│       │   ├── MainActivity.java       # 主界面逻辑
│       │   ├── ToggleButtonView.java   # 切换按钮组件
│       │   ├── FlowLayout.java         # 流式布局
│       │   ├── RecordAdapter.java      # 记录表格适配器
│       │   ├── TimeRecord.java         # 时间记录数据模型
│       │   └── ExcelHelper.java        # Excel导入导出工具
│       └── res/
│           ├── layout/
│           │   ├── activity_main.xml       # 主界面布局
│           │   ├── item_toggle_button.xml  # 按钮item布局
│           │   └── item_record.xml         # 记录行布局
│           ├── drawable/         # 图形资源
│           ├── values/           # 颜色、字符串、样式
│           └── xml/file_paths.xml  # FileProvider配置
├── build.gradle                  # 根配置
├── settings.gradle               # 项目配置
├── local.properties              # SDK路径（本地）
└── gradle/wrapper/
    └── gradle-wrapper.properties  # Gradle版本
```

---

## 依赖说明

| 依赖 | 用途 |
|------|------|
| `androidx.appcompat` | Android向后兼容 |
| `material` | Material Design组件 |
| `constraintlayout` | 约束布局 |
| `recyclerview` | 记录列表 |
| `cardview` | 卡片UI |
| `poi + poi-ooxml` | Excel读写（Apache POI） |

---

## 常见问题

**Q: Gradle同步失败？**  
A: 检查网络，或配置国内镜像：在 `build.gradle` 的 repositories 中添加阿里云镜像。

**Q: POI相关编译错误？**  
A: 确保 `packagingOptions` 中排除了 META-INF 冲突文件（已在build.gradle中配置）。

**Q: 导出的Excel在哪里？**  
A: 导出后会弹出对话框显示路径，也可点击"分享文件"发送到微信/邮件等。
路径通常为：`/sdcard/Android/data/com.example.buttontimerecorder/files/`

---

## 版本信息
- App版本：1.0
- 最低Android版本：Android 7.0 (API 24)
- 目标Android版本：Android 14 (API 34)
