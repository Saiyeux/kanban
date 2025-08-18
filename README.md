# Kanban Task Management

一个现代化的Android看板任务管理应用，采用Material Design 3设计风格。

## ✨ 功能特性

### 📋 任务管理
- 创建、编辑、删除任务
- 任务进度自动计算
- 状态自动同步（待处理 → 进行中 → 已完成）

### 🎯 事件管理
- 为任务创建具体事件
- 点击事件展开状态变更按钮
- 拖拽排序事件优先级
- 时间跟踪（小时）

### 📊 进度跟踪
- 实时统计面板（2x2布局）
- 点击统计卡片筛选事件
- 优先级颜色标识
- 完成度可视化进度条

### 💾 数据持久化
- SharedPreferences本地存储
- JSON序列化支持
- 数据版本管理与迁移
- 导入/导出功能

## 🎨 设计亮点

- **Material Design 3** 现代化设计风格
- **流畅动画** 微交互反馈效果
- **响应式布局** 优雅的卡片设计
- **直观操作** 侧滑面板 + 主内容区域

## 🛠️ 技术架构

### 开发环境
- **语言**: Kotlin
- **最低SDK**: 34 (Android 14)
- **目标SDK**: 36 (Android 15)
- **构建工具**: Gradle with Kotlin DSL

### 核心技术
- **UI框架**: Android Views + View Binding
- **架构模式**: MVVM + Repository Pattern
- **数据库**: SharedPreferences (JSON存储)
- **异步处理**: Kotlin Coroutines + Flow
- **UI组件**: Material Design Components

### 项目结构
```
app/src/main/java/com/example/kanban/
├── data/
│   ├── entity/          # 数据实体类
│   └── SimpleRepository.kt # 数据仓库
├── ui/
│   ├── adapter/         # RecyclerView适配器
│   ├── kanban/          # 主界面Fragment + ViewModel
│   └── helper/          # 拖拽助手类
└── MainActivity.kt      # 主Activity
```

## 🚀 快速开始

### 环境要求
- Android Studio Hedgehog | 2023.1.1+
- JDK 17+
- Android SDK 34+

### 构建应用
```bash
# 克隆项目
git clone [repository-url]
cd kanban

# 构建调试版本
./gradlew assembleDebug

# 安装到设备
./gradlew installDebug

# 运行测试
./gradlew test
```

## 📱 使用说明

1. **任务管理**
   - 点击右上角 ➕ 创建新任务
   - 长按任务可编辑或删除
   - 点击任务查看关联事件

2. **事件操作**
   - 点击事件卡片展开状态按钮
   - 待处理 → 进行中（无需输入时间）
   - 进行中 → 完成（需要输入总用时）
   - 拖拽事件重新排序

3. **进度查看**
   - 顶部统计卡片显示进度概览
   - 点击统计卡片筛选对应状态事件
   - 任务进度条显示完成百分比

## 🎯 状态流程

### 任务状态
- **待处理**: 所有事件未开始
- **进行中**: 有事件开始但未全部完成
- **已完成**: 所有事件完成

### 事件状态
- **待处理** → **进行中** → **已完成**
- 支持状态回退操作

## 📦 数据存储

应用使用SharedPreferences进行本地数据持久化：
- JSON格式序列化任务和事件数据
- 支持数据版本管理和迁移
- 应用更新后数据自动保留

## 🤝 贡献指南

欢迎提交Issue和Pull Request！

## 📄 开源协议

MIT License - 详见 [LICENSE](LICENSE) 文件

---

> 一个专业的看板应用，让任务管理变得简单高效 ✨