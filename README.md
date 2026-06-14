> **演示视频**：[七牛云2026暑假实训第四批AI绘画作品 — Bilibili](https://b23.tv/GFYwTpl)

# DiagramGPT — 语音驱动的 AI 画图系统

多模态 AI 画图工具。用户通过**语音或文字**描述需求，AI 自动生成**自由绘图**、**图表**（流程图/ER图/架构图/思维导图/类图/用例图）或 **AI 生图**。

支持**版本树分支管理**，每次操作自动生成快照，可从任意历史节点分叉出新的创作路径。

---

## 技术栈

| 层 | 技术 |
|----|------|
| 后端框架 | Spring Boot 3.5 + Spring AI |
| 前端 | Vue 3 + Vite |
| 图表渲染 | LogicFlow 2.x |
| 数据库 | MySQL + Redis |
| 大模型 | DeepSeek（对话/分类/指令解析） |
| 生图模型 | SiliconFlow Kolors |
| 语音识别 | 讯飞语音识别 API |

---

## 环境要求

- **JDK 17**+
- **Node.js 18**+
- **MySQL 8.0**+
- **Redis 7.0**+（或 Windows 版）
- 麦克风设备（语音功能）

---

## 快速部署

### 1. 克隆仓库

```bash
git clone https://github.com/xiangchenzhi/SpringAiVoiceCanvas.git
cd SpringAIVoiceCanvas
```

### 2. 创建 MySQL 数据库

```sql
CREATE DATABASE voice_canvas DEFAULT CHARACTER SET utf8mb4;
```

JPA 会自动建表，不需要手动导入 SQL。

### 3. 启动 Redis

```bash
# Linux / macOS
redis-server

# Windows — 下载 Redis for Windows 或使用 WSL
redis-server.exe
```

### 4. 配置环境变量

需要设置以下环境变量（API Key 不能硬编码在配置文件中）：

| 变量名 | 说明 | 获取地址 |
|--------|------|----------|
| `DEEPSEEK_API_KEY` | DeepSeek API Key | https://platform.deepseek.com |
| `SILICONFLOW_API_KEY` | SiliconFlow API Key | https://siliconflow.cn |
| `XF_APP_ID` | 讯飞语音应用 ID | https://console.xfyun.cn |
| `XF_API_KEY` | 讯飞语音 API Key | https://console.xfyun.cn |
| `XF_API_SECRET` | 讯飞语音 API Secret | https://console.xfyun.cn |

**Windows 设置方法（PowerShell 管理员模式）：**

```powershell
[System.Environment]::SetEnvironmentVariable('DEEPSEEK_API_KEY', 'sk-xxx', 'User')
[System.Environment]::SetEnvironmentVariable('SILICONFLOW_API_KEY', 'sk-xxx', 'User')
[System.Environment]::SetEnvironmentVariable('XF_APP_ID', 'xxx', 'User')
[System.Environment]::SetEnvironmentVariable('XF_API_KEY', 'xxx', 'User')
[System.Environment]::SetEnvironmentVariable('XF_API_SECRET', 'xxx', 'User')
```

设置后**需要重启终端或 IDE** 才能生效。

### 5. 启动后端

```bash
cd backend

# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

后端默认运行在 `http://localhost:8080`。

### 6. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认运行在 `http://localhost:5173`。

### 7. 打开浏览器

访问 http://localhost:5173，注册账号后即可使用。

---

## 项目结构

```
SpringAI/
├── backend/                          # Spring Boot 后端
│   ├── src/main/java/.../
│   │   ├── advisor/                  # AI Chat Advisor（提示词注入）
│   │   │   ├── MemoryAdvisor         # 对话记忆注入
│   │   │   ├── IntentAdvisor         # 意图分类提示词
│   │   │   ├── DiagramAdvisor        # 图表生成提示词
│   │   │   ├── DrawingCommandAdvisor # 绘图命令提示词
│   │   │   └── PromptEnhanceAdvisor  # 生图提示词优化
│   │   ├── asr/                      # 讯飞语音识别
│   │   │   ├── XunfeiAsrHandler      # WebSocket 音频中继
│   │   │   └── XunfeiSignUtil        # HMAC-SHA256 签名工具
│   │   ├── config/
│   │   │   ├── WebConfig             # 拦截器注册
│   │   │   ├── WebSocketConfig       # WebSocket 端点
│   │   │   └── AuthInterceptor       # Token 鉴权拦截器
│   │   ├── controller/
│   │   │   ├── UnifiedController     # 统一入口（意图处理+版本树）
│   │   │   └── UserController        # 登录注册
│   │   ├── memory/                   # Redis 记忆管理
│   │   │   └── RedisChatMemoryService
│   │   ├── model/                    # JPA 实体 + DTO
│   │   ├── repository/               # Spring Data JPA
│   │   └── Service/
│   │       ├── AIService             # AI 调用封装
│   │       ├── ConversationService   # 会话管理
│   │       ├── DiagramVersionService # 版本树管理
│   │       ├── ImageGenerateService  # 生图服务
│   │       └── UserService           # 用户认证
│   └── src/main/resources/application.yml
│
├── frontend/                         # Vue 3 前端
│   ├── src/
│   │   ├── api/                      # API 客户端
│   │   │   ├── authApi.js            # 登录注册
│   │   │   ├── conversationApi.js    # 会话 & 版本
│   │   │   ├── intentApi.js          # 意图 API
│   │   │   ├── xunfeiAsrClient.js    # 讯飞 ASR 客户端
│   │   │   ├── voiceApi.js           # 语音命令
│   │   │   └── imageApi.js           # 图片生成
│   │   ├── components/
│   │   │   ├── LoginView.vue         # 登录/注册页
│   │   │   ├── Sidebar.vue           # 会话侧边栏
│   │   │   ├── DrawingCanvas.vue     # SVG 自由绘图
│   │   │   ├── DiagramCanvas.vue     # LogicFlow 图表
│   │   │   ├── ImageCanvas.vue       # AI 生图展示
│   │   │   ├── ProcessLog.vue        # 执行日志
│   │   │   ├── VersionTree.vue       # 版本树弹窗
│   │   │   └── VersionTreeNode.vue   # 版本树节点（递归）
│   │   ├── nodes/                    # LogicFlow 自定义节点
│   │   │   └── lfNodes.js
│   │   ├── App.vue                   # 主组件
│   │   └── main.js
│   └── vite.config.js
│
└── voice-asr-dev-log.md              # 语音开发采坑记录
```

---

## 核心功能架构

### 意图分流 Pipeline

```
用户输入 → AuthInterceptor → 意图分类 → 三选一执行
                                         ├── shape → AI 解析绘图命令 → SVG 渲染
                                         ├── diagram → AI 生成节点/边 → LogicFlow 渲染
                                         └── image → AI 优化 prompt → Kolors 生图
```

### 版本树 + 分支记忆管理

```
操作流程：
1. copyMemory(version:{父版本} → branch:{uuid})     // 从冻存快照复制
2. AI 读写 branch:{uuid}                              // 临时 key
3. moveMemory(branch:{uuid} → version:{新版本})       // 固化，删临时 key
```

每个版本独立保存图快照 + AI 对话记忆快照，分支间记忆完全隔离。

### 语音状态机

```
PAUSED ←→ LISTENING → PROCESSING → PAUSED
  ↑         ↑            ↑           ↑
 手动暂停  自动开启   VAD 触发     AI 返回结果
```

---

## 常见问题

**Q: 语音识别没有反应？**

检查：① 浏览器是否允许麦克风权限 ② 后端是否在运行 ③ 讯飞 API 环境变量是否设置正确。如果浏览器控制台有 `[ASR] AudioContext 无法恢复` 日志，点击一下语音按钮即可。

**Q: 端口 5173 被占用？**

先杀掉占用进程：
```bash
netstat -ano | findstr :5173          # 查看 PID
taskkill /PID <PID> /F               # 强制结束
```
然后重新 `npm run dev`。

**Q: 如何清空数据库重新演示？**

```sql
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE execution_logs;
TRUNCATE TABLE diagram_versions;
TRUNCATE TABLE conversations;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;
```
Redis：`redis-cli FLUSHALL`  
浏览器：F12 → Application → 删 `diagram_token` 和 `diagram_username`

---

## 演示脚本（约 9 分钟）

1. **登录注册** — 展示 BCrypt + Redis token 鉴权
2. **语音自由绘图** — 说"画一个红色三角形"，VAD + 讯飞识别 + SVG 渲染
3. **6 种图表** — 流程图/ER图/架构图/类图/用例图，LogicFlow 自适应布局
4. **AI 生图** — 三路分流，中译英 prompt 优化 + Kolors 模型
5. **版本树分支** — 回溯历史版本，从任意节点分叉，记忆隔离
6. **沉默容错** — VAD 误触发空结果，状态自动恢复
