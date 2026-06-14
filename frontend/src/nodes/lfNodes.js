import {
  RectNode,
  RectNodeModel,
  EllipseNode,
  EllipseNodeModel,
  DiamondNode,
  DiamondNodeModel,
  CircleNode,
  CircleNodeModel,
  h
} from '@logicflow/core'

// ==================== 卡片式通用节点（白底 + 圆角 + 轻阴影感） ====================
class CardNode extends RectNode {
  getShape() {
    const { model } = this.props
    const { x, y, width, height } = model
    const fill = model.properties?.fill || '#ffffff'
    const stroke = model.properties?.stroke || '#e2e8f0'
    return h('g', {}, [
      // 底阴影
      h('rect', {
        x: x - width / 2 + 2, y: y - height / 2 + 2,
        width, height, rx: 12, ry: 12,
        fill: 'rgba(0,0,0,0.04)'
      }),
      // 主体
      h('rect', {
        x: x - width / 2, y: y - height / 2,
        width, height, rx: 12, ry: 12,
        fill, stroke, strokeWidth: 1.5
      })
    ])
  }
}
class CardModel extends RectNodeModel {
  initNodeData(data) {
    super.initNodeData(data)
    this.radius = 12
    this.text = data.text || data.properties?.label || ''
  }
}

// ==================== 开始/结束 ====================
class StartEndNode extends EllipseNode {
  getShape() {
    const { model } = this.props
    const { x, y, rx, ry } = model
    const fill = model.properties?.fill || '#f0fdf4'
    const stroke = model.properties?.stroke || '#22c55e'
    return h('ellipse', {
      cx: x, cy: y, rx: rx ?? 50, ry: ry ?? 28,
      fill, stroke, strokeWidth: 2
    })
  }
}
class StartEndModel extends EllipseNodeModel {
  initNodeData(data) {
    super.initNodeData(data)
    this.rx = 50; this.ry = 28
    this.text = data.text || data.properties?.label || ''
  }
}

// ==================== 决策菱形 ====================
class DecisionNode extends DiamondNode {
  getShape() {
    const { model } = this.props
    const { x, y, rx, ry } = model
    return h('polygon', {
      points: `${x},${y - ry} ${x + rx},${y} ${x},${y + ry} ${x - rx},${y}`,
      fill: '#fefce8',
      stroke: '#f59e0b',
      strokeWidth: 2
    })
  }
}
class DecisionModel extends DiamondNodeModel {
  initNodeData(data) {
    super.initNodeData(data)
    this.rx = 55; this.ry = 36
    this.text = data.text || data.properties?.label || ''
  }
}

// ==================== 思维导图圆形根节点 ====================
class RootNode extends CircleNode {
  getShape() {
    const { model } = this.props
    const { x, y, r } = model
    return h('circle', {
      cx: x, cy: y, r: r ?? 38,
      fill: '#ffffff',
      stroke: '#6366f1',
      strokeWidth: 2.5
    })
  }
}
class RootModel extends CircleNodeModel {
  initNodeData(data) {
    super.initNodeData(data)
    this.r = 38
    this.text = data.text || data.properties?.label || ''
  }
}

// ==================== 用例图：Actor（火柴人） ====================
class ActorNode extends RectNode {
  getShape() {
    const { model } = this.props
    const { x, y } = model
    const headR = 10
    const headY = y - 24
    const bodyEndY = y + 4
    const armY = y - 10

    return h('g', {}, [
      // 头（圆）
      h('circle', { cx: x, cy: headY, r: headR, fill: 'none', stroke: '#1e293b', strokeWidth: 2 }),
      // 身体
      h('line', { x1: x, y1: headY + headR, x2: x, y2: bodyEndY, stroke: '#1e293b', strokeWidth: 2, strokeLinecap: 'round' }),
      // 左臂
      h('line', { x1: x - 14, y1: armY, x2: x, y2: armY + 2, stroke: '#1e293b', strokeWidth: 2, strokeLinecap: 'round' }),
      // 右臂
      h('line', { x1: x, y1: armY + 2, x2: x + 14, y2: armY, stroke: '#1e293b', strokeWidth: 2, strokeLinecap: 'round' }),
      // 左腿
      h('line', { x1: x, y1: bodyEndY, x2: x - 10, y2: y + 22, stroke: '#1e293b', strokeWidth: 2, strokeLinecap: 'round' }),
      // 右腿
      h('line', { x1: x, y1: bodyEndY, x2: x + 10, y2: y + 22, stroke: '#1e293b', strokeWidth: 2, strokeLinecap: 'round' })
    ])
  }
}
class ActorModel extends RectNodeModel {
  initNodeData(data) {
    super.initNodeData(data)
    this.radius = 0
    this.width = 40
    this.height = 60
    this.text = {
      value: data.text?.value || data.properties?.label || '',
      x: data.text?.x || data.x || 100,
      y: (data.text?.y || data.y || 100) + 36
    }
    this.text.x = (data.text?.x || data.x || 100)
    this.text.y = (data.text?.y || data.y || 100) + 36
  }
}

// ==================== 用例图：Usecase（椭圆） ====================
class UsecaseNode extends RectNode {
  getShape() {
    const { model } = this.props
    const { x, y, width, height } = model
    const rx = width / 2
    const ry = height / 2
    return h('ellipse', {
      cx: x, cy: y,
      rx, ry,
      fill: '#fff7ed',
      stroke: '#f97316',
      strokeWidth: 2
    })
  }
}
class UsecaseModel extends RectNodeModel {
  initNodeData(data) {
    super.initNodeData(data)
    this.radius = 0
    this.width = 140
    this.height = 60
    this.text = data.text || data.properties?.label || ''
  }
}
export function registerCardNodes(lf) {
  const types = ['process', 'service', 'gateway', 'message_queue', 'database', 'entity', 'cache', 'branch', 'leaf']
  types.forEach(t => lf.register({ type: t, view: CardNode, model: CardModel }))
  lf.register({ type: 'start',   view: StartEndNode, model: StartEndModel })
  lf.register({ type: 'end',     view: StartEndNode, model: StartEndModel })
  lf.register({ type: 'decision', view: DecisionNode, model: DecisionModel })
  lf.register({ type: 'root',    view: RootNode, model: RootModel })

  // 类图：class / interface / abstract 都是矩形卡片
  lf.register({ type: 'class',     view: CardNode, model: CardModel })
  lf.register({ type: 'interface', view: CardNode, model: CardModel })
  lf.register({ type: 'abstract',  view: CardNode, model: CardModel })

  // 用例图：actor 火柴人，usecase 椭圆
  lf.register({ type: 'actor',   view: ActorNode, model: ActorModel })
  lf.register({ type: 'usecase', view: UsecaseNode, model: UsecaseModel })
}
