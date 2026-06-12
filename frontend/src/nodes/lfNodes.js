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

// ==================== 注册所有类型（卡片风） ====================
export function registerCardNodes(lf) {
  const types = ['process', 'service', 'gateway', 'message_queue', 'database', 'entity', 'cache', 'branch', 'leaf']
  types.forEach(t => lf.register({ type: t, view: CardNode, model: CardModel }))
  lf.register({ type: 'start',   view: StartEndNode, model: StartEndModel })
  lf.register({ type: 'end',     view: StartEndNode, model: StartEndModel })
  lf.register({ type: 'decision', view: DecisionNode, model: DecisionModel })
  lf.register({ type: 'root',    view: RootNode, model: RootModel })
}
