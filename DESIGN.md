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

---

## 七、Phase 2 — 结构化图表（AI 生成图结构）

### 7.0 架构洞察：从「AI 画图」到「AI 生成图结构」

| | Phase 1（自由绘图） | Phase 2（结构化图表） |
|---|---|---|
| LLM 职责 | 理解 + 布局 + 绘制 | **仅理解语义** |
| 前端职责 | 照坐标渲染 | **布局引擎 + 渲染引擎** |
| 坐标谁算 | 大模型猜 | 布局算法算 |
| 为什么画房子丑 | 大模型不知道像素 | 布局引擎知道 |

**核心思想：不要让 LLM 去做确定性算法的事。**

```
语音 → ASR → Spring AI → 结构化图模型 → 布局引擎 → SVG

LLM 只负责：
  - 节点有哪些
  - 关系是什么
```

### 7.1 统一 DSL：Diagram

四种图表类型在底层统一为 Graph 模型：

```
流程图(Graph) ⊃ 架构图(Graph) ⊃ ER图(Graph) ⊃ 思维导图(Tree ⊂ Graph)
```

**统一 Java 模型：**

```java
class Diagram {
    String type;           // flowchart | mindmap | er | architecture
    List<Node> nodes;
    List<Edge> edges;
}

class Node {
    String id;             // 唯一标识
    String label;          // 显示文本
    String nodeType;       // start / process / decision / end / entity / service / ...
}

class Edge {
    String source;         // 源节点 id
    String target;         // 目标节点 id
    String label;          // 边标签（可选）
}
```

### 7.2 四种图表类型

#### 流程图（flowchart）

用户："帮我画一个请假审批流程"

```json
{
  "type": "flowchart",
  "nodes": [
    {"id": "start",   "label": "开始",     "nodeType": "start"},
    {"id": "apply",   "label": "提交申请",   "nodeType": "process"},
    {"id": "approve", "label": "审批",      "nodeType": "decision"},
    {"id": "end",     "label": "结束",      "nodeType": "end"}
  ],
  "edges": [
    {"source": "start",   "target": "apply"},
    {"source": "apply",   "target": "approve"},
    {"source": "approve", "target": "end",   "label": "通过"}
  ]
}
```

#### 思维导图（mindmap）

用户："生成 SpringCloud 学习路线"

```json
{
  "type": "mindmap",
  "nodes": [
    {"id": "root",      "label": "SpringCloud"},
    {"id": "registry",  "label": "注册中心",    "nodeType": "branch"},
    {"id": "nacos",     "label": "Nacos"},
    {"id": "invoke",    "label": "服务调用",    "nodeType": "branch"},
    {"id": "feign",     "label": "OpenFeign"}
  ],
  "edges": [
    {"source": "root",     "target": "registry"},
    {"source": "registry", "target": "nacos"},
    {"source": "root",     "target": "invoke"},
    {"source": "invoke",   "target": "feign"}
  ]
}
```

#### ER 图（er）

用户："画一个电商系统 ER 图"

```json
{
  "type": "er",
  "nodes": [
    {"id": "User",    "label": "User\n─────\nid\nname",    "nodeType": "entity"},
    {"id": "Order",   "label": "Order\n─────\nid\nuser_id", "nodeType": "entity"}
  ],
  "edges": [
    {"source": "User",  "target": "Order", "label": "1:N"}
  ]
}
```

#### 系统架构图（architecture）

用户："画一个 SpringCloud 微服务架构"

```json
{
  "type": "architecture",
  "nodes": [
    {"id": "gateway",   "label": "Gateway",       "nodeType": "service"},
    {"id": "nacos",     "label": "Nacos",          "nodeType": "service"},
    {"id": "orderSvc",  "label": "OrderService",   "nodeType": "service"},
    {"id": "userSvc",   "label": "UserService",    "nodeType": "service"},
    {"id": "mysql",     "label": "MySQL",           "nodeType": "database"},
    {"id": "redis",     "label": "Redis",           "nodeType": "cache"}
  ],
  "edges": [
    {"source": "gateway",  "target": "orderSvc"},
    {"source": "gateway",  "target": "userSvc"},
    {"source": "orderSvc", "target": "mysql"},
    {"source": "orderSvc", "target": "redis"}
  ]
}
```

### 7.3 Prompt 设计

```
你是一个图表结构生成器。用户会用中文描述他们想要的图表（流程图、思维导图、ER 图、架构图）。
你需要返回图的结构（nodes + edges），不要计算任何坐标或布局。

规则：
1. 根据用户描述判断图表类型：flowchart / mindmap / er / architecture
2. 每个节点需有唯一 id 和描述性 label
3. 流程图节点需标注 nodeType：start / process / decision / end
4. ER 图节点为实体，需在 label 中包含字段列表
5. 架构图节点需标注 nodeType：service / database / cache / gateway
6. 只返回 JSON，不要任何其他文字

返回格式：
{
  "type": "flowchart",
  "nodes": [
    {"id": "start", "label": "开始", "nodeType": "start"}
  ],
  "edges": [
    {"source": "start", "target": "next", "label": "可选标签"}
  ]
}
```

### 7.4 前端渲染方案

前端使用**自动布局算法**，不依赖 LLM 计算坐标：

```
Diagram(JSON)
  ↓
前端自动布局（按 type 选择布局策略）
  ↓
SVG 渲染
```

**布局策略：**

| 图表类型 | 布局策略 |
|---|---|
| flowchart | 纵向分层布局（layered top-down） |
| mindmap | 径向树布局（radial tree） |
| er | 网格布局（grid） |
| architecture | 力导向布局（force-directed） |

**组件树更新：**

```
App.vue
├── TextInput.vue            # 文字输入框（自由绘图 + 图表共用）
├── DrawingCanvas.vue        # Phase 1 自由绘图 SVG 渲染
├── DiagramCanvas.vue        # Phase 2 结构化图表 SVG 渲染（新增）
│   └── 内置布局算法（layered / radial / grid / force）
└── CommandLog.vue           # 命令历史日志
```

### 7.5 意图路由

前端根据后端返回的响应类型自动切换渲染模式：

```
后端响应:
  { commands: [...] }        → DrawingCanvas（自由绘图模式）
  { diagram: {...} }         → DiagramCanvas（图表模式）
```

或者统一响应格式（推荐）：

```json
{
  "type": "shape" | "diagram",
  "commands": [...],        // type=shape 时有值
  "diagram": {...},         // type=diagram 时有值
  "originalText": "..."
}
```

---

## 八、后端 API 设计

### POST /api/voice（Phase 1 — 自由绘图）

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

### POST /api/diagram（Phase 2 — 结构化图表）

**Request:**
```json
{
  "transcript": "帮我画一个请假审批流程"
}
```

**Response:**
```json
{
  "type": "diagram",
  "diagram": {
    "type": "flowchart",
    "nodes": [
      {"id": "start", "label": "开始", "nodeType": "start"},
      {"id": "apply", "label": "提交申请", "nodeType": "process"},
      {"id": "approve", "label": "审批", "nodeType": "decision"},
      {"id": "end", "label": "结束", "nodeType": "end"}
    ],
    "edges": [
      {"source": "start", "target": "apply"},
      {"source": "apply", "target": "approve"},
      {"source": "approve", "target": "end", "label": "通过"}
    ]
  },
  "originalText": "帮我画一个请假审批流程"
}
```

### GET /api/health

健康检查接口。

---

## 九、项目结构

```
SpringAIVoiceCanvas/                  # Git 仓库根目录（monorepo）
├── DESIGN.md                         # 本文档
├── README.md
├── .gitignore
├── .gitattributes
├── backend/                          # Spring Boot 后端模块
│   ├── pom.xml
│   └── src/main/java/.../
│       ├── SpringAiVoiceCanvasApplication.java
│       ├── config/
│       │   └── AIConfig.java         # ChatClient Bean
│       ├── advisor/
│       │   ├── DrawingCommandAdvisor.java   # Phase 1: 自由绘图 System Prompt
│       │   └── DiagramAdvisor.java          # Phase 2: 图表结构 System Prompt
│       ├── controller/
│       │   ├── VoiceController.java         # POST /api/voice  (Phase 1)
│       │   └── DiagramController.java       # POST /api/diagram (Phase 2)
│       ├── Service/
│       │   └── AIService.java               # ChatClient 调用 + JSON 解析
│       └── model/
│           ├── ShapeCommand.java            # Phase 1 命令 DTO
│           ├── VoiceRequest.java            # Phase 1 请求 DTO
│           ├── VoiceResponse.java           # Phase 1 响应 DTO
│           ├── DiagramNode.java             # Phase 2 节点
│           ├── DiagramEdge.java             # Phase 2 边
│           ├── Diagram.java                 # Phase 2 图结构
│           ├── DiagramRequest.java          # Phase 2 请求 DTO
│           └── DiagramResponse.java         # Phase 2 响应 DTO
├── frontend/                         # Vue 3 + Vite 前端模块
│   ├── index.html
│   ├── package.json
│   ├── vite.config.js
│   └── src/
│       ├── App.vue                   # 双模式入口（自由绘图 + 图表）
│       ├── main.js
│       ├── api/
│       │   ├── voiceApi.js           # POST /api/voice
│       │   └── diagramApi.js         # POST /api/diagram
│       └── components/
│           ├── DrawingCanvas.vue     # Phase 1: SVG 自由绘图渲染
│           ├── DiagramCanvas.vue     # Phase 2: SVG 图表渲染（内置布局引擎）
│           └── CommandLog.vue        # 命令历史日志
```

---

## 十、推进计划

| 阶段 | 内容 | 产出 | 状态 |
|---|---|---|---|
| **Phase 1** | 后端核心 + 自由绘图 | VoiceController + DrawingCommandAdvisor + SVG 渲染 | 已完成 |
| **Phase 2** | 结构化图表 | DiagramController + DiagramAdvisor + DiagramCanvas（内置布局引擎） | 进行中 |
| **Phase 3** | 语音输入 | Web Speech API 集成，替代文字输入框 | 待开始 |
| **Phase 4** | 复杂指令 | 意图路由：自动判断自由绘图 vs 图表，统一入口 | 待开始 |
| **Phase 5** | 选做 | 修改属性、渐变、导出、预设模板 | 待开始 |

---

## 十一、关键设计决策

1. **为什么用 SVG 而不是 Canvas？**
   SVG 的每个图形都是独立 DOM 元素，天然支持 Vue 响应式渲染和撤销重做，符合"形状即对象"的理念。

2. **为什么命令用数组而不是单个对象？**
   题目明确要求"复杂指令拆解能力"。一次语音可能对应多个绘图操作。数组格式优雅地解决了这个问题。

3. **Phase 1：为什么位置用绝对坐标而不是相对坐标？**
   让大模型根据画布尺寸计算绝对坐标，前端只需执行。避免前端维护复杂的相对定位逻辑。

4. **Phase 2：为什么 LLM 不负责算坐标？**
   核心架构洞察：LLM 是「图结构生成器」而非「绘图引擎」。让 LLM 只输出 nodes + edges，前端用布局算法算坐标，生成质量大幅提升。流程图/ER图/架构图/思维导图在底层统一为 Graph + Tree 模型。

5. **Spring AI 的定位？**
   Spring AI 只负责"自然语言 → 结构化命令/图结构"这一步，不碰语音也不碰渲染。职责清晰，边界明确。

6. **语音识别为什么不用后端？**
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
