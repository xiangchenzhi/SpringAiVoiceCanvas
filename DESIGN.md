# 纯语音控制的绘图工具 — 设计文档

## 一、项目概述

**目标：** 开发一款用户完全不能使用鼠标或键盘、仅通过语音指令完成绘图创作的绘图工具。

**核心命题拆解：**

```
语音 → 文字 → 意图解析 → 绘图命令 → Canvas/SVG
```

**职责边界：**

| 层 | 技术 | 职责 |
|---|---|---|
| 语音采集 | 浏览器 Web Speech API | 将用户语音转为文字 |
| 意图解析 | Spring AI + 大模型 | 将自然语言转为结构化绘图命令 |
| 图形渲染 | Vue 3 + SVG | 根据命令数组渲染图形对象 |
| 命令执行 | Vue 前端状态管理 | 解析 JSON 命令，操作 shapes 数组 |

---

## 二、技术架构

```
┌─────────────────────────────────────────────────┐
│                    浏览器                        │
│  ┌───────────┐    ┌──────────────┐              │
│  │ Web Speech │ ->│   Vue 3 App │               │
│  │   API     │    │  (SVG 渲染)   │              │
│  └───────────┘    └──────┬───────┘              │
│                          │ HTTP POST /api/voice │
└──────────────────────────┼──────────────────────┘
                           │
┌──────────────────────────┼──────────────────────┐
│                   Spring Boot 后端               │
│  ┌───────────────────────▼────────────────────┐ │
│  │         VoiceController                    │ │
│  │         POST /api/voice                    │ │
│  └───────────────────────┬────────────────────┘ │
│  ┌───────────────────────▼────────────────────┐ │
│  │         VoiceService                       │ │
│  │  构造 Prompt → Spring AI → 大模型  │ │
│  │  返回 List<ShapeCommand>                   │ │
│  └───────────────────────┬────────────────────┘ │
│  ┌───────────────────────▼────────────────────┐ │
│  │      Spring AI (ChatClient)                │ │
│  │  调用 OpenAI/通义千问 等大模型               │ │
│  └────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────┘
```

---

## 三、核心数据结构

### 3.1 前后端统一命令格式

```json
{
  "action": "circle",
  "params": {
    "cx": 500,
    "cy": 300,
    "r": 100,
    "color": "blue"
  }
}
```

**action 枚举值：**

| action | 说明 | 关键参数 |
|---|---|---|
| `circle` | 圆形 | cx, cy, r, color |
| `rect` | 矩形 | x, y, width, height, color |
| `triangle` | 三角形 | cx, cy, size, color |
| `line` | 线段 | x1, y1, x2, y2, color |
| `text` | 文本 | x, y, content, color, fontSize |
| `clear` | 清空画布 | 无 |
| `undo` | 撤销最后图形 | 无 |
| `redo` | 重做已撤销图形 | 无 |
| `set_bg` | 设置背景色 | color |

### 3.2 复杂指令 → 命令数组

单条语音可拆解为多条命令，前端依次执行：

```json
[
  {"action": "circle",  "params": {"cx": 300, "cy": 300, "r": 50,  "color": "blue"}},
  {"action": "rect",   "params": {"x": 400,  "y": 250, "width": 100, "height": 100, "color": "red"}},
  {"action": "line",   "params": {"x1": 350, "y1": 300, "x2": 400, "y2": 300, "color": "black"}}
]
```

---

## 四、计划支持的指令能力

### 4.1 基础图形（Phase 1）

| 指令示例 | 解析结果 | 状态 |
|---|---|---|
| "画一个圆" | `circle` 默认半径50、画布中央 | 计划实现 |
| "画一个半径100的蓝色圆" | `circle` r=100 color=blue | 计划实现 |
| "画一个矩形" | `rect` 默认100x80、画布中央 | 计划实现 |
| "画一个红色三角形" | `triangle` color=red | 计划实现 |
| "画一条线" | `line` 默认斜线 | 计划实现 |
| "写一段文字" | `text` | 计划实现 |

### 4.2 颜色控制（Phase 1）

| 指令示例 | 说明 | 状态 |
|---|---|---|
| "红色/蓝色/绿色/黄色/黑色/白色" | 基础颜色词 | 计划实现 |
| "深蓝色 / 浅绿色" | 复合颜色 | 计划实现 |
| "改成红色" | 修改上一个图形颜色 | 计划实现 |

### 4.3 位置控制（Phase 1）

| 指令示例 | 说明 | 状态 |
|---|---|---|
| "在画布中央" | cx=500, cy=300（假设1000x600画布） | 计划实现 |
| "在左边 / 在右边" | 相对定位 | 计划实现 |
| "在上面 / 在下面" | 相对定位 | 计划实现 |
| "在左上角 / 在右下角" | 角落定位 | 计划实现 |

### 4.4 修改与控制（Phase 1）

| 指令示例 | 说明 | 状态 |
|---|---|---|
| "撤销 / 返回上一步" | 删除最后图形 | 计划实现 |
| "重做" | 恢复已撤销图形 | 计划实现 |
| "清空画布 / 全部删除" | 清除所有图形 | 计划实现 |
| "删除最后一个图形" | 等同撤销 | 计划实现 |

### 4.5 复杂指令拆解（Phase 2）

| 指令示例 | 拆解结果 | 状态 |
|---|---|---|
| "先画一个蓝色圆形，然后在圆形右边画一个红色矩形，最后用黑线连接它们" | `[circle, rect, line]` | 计划实现 |
| "画一个笑脸" | `[circle(脸), circle(左眼), circle(右眼), arc(嘴)]` | 计划实现 |
| "画一座房子" | `[rect(房体), triangle(屋顶), rect(门)]` | 计划实现 |
| "在画布上画三个不同颜色的圆" | `[circle, circle, circle]` | 计划实现 |

### 4.6 高级能力（Phase 3 / 选做）

| 指令示例 | 说明 | 状态 |
|---|---|---|
| "把刚才的圆变大一点" | 修改已有图形属性 | 选做 |
| "把第二个矩形移到右边" | 按索引定位修改 | 选做 |
| "画一个渐变色圆形" | 复杂样式 | 选做 |
| "放大 / 缩小画布" | 画布操作 | 选做 |
| "导出图片" | 保存 PNG/SVG | 选做 |

---

## 五、后端 Prompt 设计（Spring AI 核心）

### 5.1 System Prompt

```
你是一个绘图命令解析器。用户会用中文描述他们想要绘制的图形。
你需要将用户的自然语言描述转换为 JSON 命令数组。

规则：
1. 画布尺寸为 1000x600
2. 如果用户没有指定位置，默认放在画布中央 (cx=500, cy=300 或 x=500, y=300)
3. 如果用户没有指定尺寸，圆形默认 r=50，矩形默认 100x80，三角形默认 size=50
4. 如果用户没有指定颜色，默认使用黑色
5. 如果用户说"左边"，默认 x=200；"右边"默认 x=800；"上面"默认 y=150；"下面"默认 y=450
6. 复杂指令请拆解为多个命令，按执行顺序排列
7. 只返回 JSON 数组，不要任何其他文字

支持的命令类型：
- circle: {"action":"circle","params":{"cx":500,"cy":300,"r":100,"color":"blue"}}
- rect: {"action":"rect","params":{"x":400,"y":250,"width":100,"height":100,"color":"red"}}
- triangle: {"action":"triangle","params":{"cx":500,"cy":300,"size":60,"color":"green"}}
- line: {"action":"line","params":{"x1":0,"y1":0,"x2":100,"y2":100,"color":"black"}}
- text: {"action":"text","params":{"x":500,"y":300,"content":"你好","color":"black","fontSize":24}}
- clear: {"action":"clear","params":{}}
- undo: {"action":"undo","params":{}}
```

### 5.2 User Prompt 构造

```java
String userPrompt = "用户指令：" + transcript + "\n请转换成JSON命令数组。";
```

---

## 六、前端设计（Vue 3 + SVG）

### 6.1 组件树

```
App.vue
├── VoiceButton.vue        // 语音录制按钮（按住说话 / 点击开始）
├── Canvas.vue              // SVG 画布渲染
│   ├── ShapeCircle.vue     // 圆形组件（可选拆分）
│   ├── ShapeRect.vue       // 矩形组件
│   ├── ShapeTriangle.vue   // 三角形组件
│   ├── ShapeLine.vue       // 线段组件
│   └── ShapeText.vue       // 文本组件
└── CommandLog.vue          // 命令历史日志（调试/展示用）
```

### 6.2 状态管理

```javascript
const shapes = ref([])       // 当前画布图形列表
const undoStack = ref([])    // 撤销栈
const isListening = ref(false) // 是否正在录音
const transcript = ref('')   // 识别文字
```

### 6.3 核心流程

```
1. 用户点击麦克风按钮 → 开始录音
2. Web Speech API 返回文字 → 显示在界面上
3. 前端 POST /api/voice → 后端解析
4. 后端返回 JSON 命令数组 → 前端依次执行
5. 每个命令操作 shapes 数组 → Vue 响应式渲染 SVG
```

---

## 七、后端 API 设计

### POST /api/voice

**Request:**
```json
{
  "transcript": "在画布中央画一个蓝色圆形",
  "canvasWidth": 1000,
  "canvasHeight": 600
}
```

**Response:**
```json
{
  "commands": [
    {
      "action": "circle",
      "params": {
        "cx": 500,
        "cy": 300,
        "r": 50,
        "color": "blue"
      }
    }
  ],
  "originalText": "在画布中央画一个蓝色圆形"
}
```

### GET /api/health

健康检查接口。

---

## 八、项目结构

```
SpringAIVoiceCanvas/                  # Git 仓库根目录（monorepo）
├── DESIGN.md                         # 本文档
├── README.md
├── .gitignore
├── .gitattributes
├── backend/                          # Spring Boot 后端模块
│   ├── pom.xml
│   ├── mvnw / mvnw.cmd
│   ├── HELP.md
│   └── src/
│       ├── main/
│       │   ├── java/com/xcodez/springaivoicecanvas/
│       │   │   ├── SpringAiVoiceCanvasApplication.java
│       │   │   ├── Service/
│       │   │   │   └── AIService.java
│       │   │   ├── config/
│       │   │   │   └── AIConfig.java
│       │   │   ├── controller/       # 待添加
│       │   │   │   └── VoiceController.java
│       │   │   ├── service/          # 待添加
│       │   │   │   └── VoiceService.java
│       │   │   └── model/            # 待添加
│       │   │       ├── VoiceRequest.java
│       │   │       ├── VoiceResponse.java
│       │   │       └── ShapeCommand.java
│       │   └── resources/
│       │       └── application.properties
│       └── test/
└── frontend/                         # Vue 3 + Vite 前端模块
    ├── index.html
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── App.vue
        ├── main.js
        ├── components/
        │   ├── VoiceButton.vue
        │   ├── DrawingCanvas.vue
        │   └── CommandLog.vue
        └── api/
            └── voiceApi.js
```

---

## 九、推进计划

| 阶段 | 内容 | 产出 |
|---|---|---|
| **Phase 1** | 后端核心：Spring AI 集成、VoiceController、基础 Prompt | 单条基础指令可解析执行 |
| **Phase 2** | 前端核心：Vue 3 + SVG 渲染、Web Speech 集成 | 语音输入 → 图形渲染全链路 |
| **Phase 3** | 复杂指令：数组命令、连续执行、相对定位 | 多条指令拆解执行 |
| **Phase 4** | 完善：撤销重做、清空、错误处理、中文优化 | 交互闭环 |
| **Phase 5** | 选做：修改属性、渐变、导出、预设模板 | 锦上添花 |

---

## 十、关键设计决策

1. **为什么用 SVG 而不是 Canvas？**
   SVG 的每个图形都是独立 DOM 元素，天然支持 Vue 响应式渲染和撤销重做，符合"形状即对象"的理念。

2. **为什么命令用数组而不是单个对象？**
   题目明确要求"复杂指令拆解能力"。一次语音可能对应多个绘图操作（如"画笑脸"→ 4个命令）。数组格式优雅地解决了这个问题。

3. **为什么位置用绝对坐标而不是相对坐标？**
   让大模型根据画布尺寸计算绝对坐标，前端只需执行。避免前端维护复杂的相对定位逻辑。

4. **Spring AI 的定位？**
   Spring AI 只负责"自然语言 → 结构化命令"这一步，不碰语音也不碰渲染。职责清晰，边界明确。

5. **语音识别为什么不用后端？**
   浏览器原生 Web Speech API 延迟最低（本地识别），无需上传音频流。中文识别准确率足够。

---

## 十一、未完成/待定项

| 项目 | 原因 |
|---|---|
| 多轮对话上下文 | 复杂度高，基础单轮指令已满足题目要求 |
| 语音唤醒词 | Web Speech API 不原生支持，需额外集成 |
| 手绘/自由画笔 | 语音难以描述自由曲线，不在题目范围内 |
| 图形拖动修改 | 违反"不能用鼠标"的约束 |
| 多人协同 | 超出题目范围 |
