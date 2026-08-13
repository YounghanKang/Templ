import { useState, useRef, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import AuthScreen from './AuthScreen'
const INITIAL_TEAMS = [
  { id: 'T-001', name: 'Design System', members: 8, color: '#6b5cf6', mission: '모든 사용자가 직관적으로 제품을 사용할 수 있도록 일관된 디자인 언어를 구축한다.' },
  { id: 'T-002', name: 'Platform Engineering', members: 14, color: '#10b981', mission: '개발자 경험을 최우선으로, 확장 가능하고 안정적인 인프라 기반을 마련한다.' },
  { id: 'T-003', name: 'Growth & Marketing', members: 6, color: '#f59e0b', mission: '데이터 기반 실험으로 제품 성장을 가속화하고 시장 점유율을 확대한다.' },
  { id: 'T-004', name: 'Data Infrastructure', members: 11, color: '#ef4444', mission: '신뢰할 수 있는 데이터 파이프라인을 구축하여 전사적 의사결정의 품질을 높인다.' },
  { id: 'T-005', name: 'Mobile Experience', members: 9, color: '#3b82f6', mission: '네이티브 수준의 성능과 UX로 모바일 사용자 만족도를 업계 최고 수준으로 이끈다.' },
]

const AVATAR_COLORS = ['#6b5cf6', '#10b981', '#f59e0b', '#ef4444', '#3b82f6', '#ec4899', '#14b8a6', '#f97316']
const COLOR_SWATCHES = ['#6b5cf6', '#10b981', '#f59e0b', '#ef4444', '#3b82f6', '#ec4899', '#14b8a6', '#f97316']

function getInitials(name: string) {
  return name.split(' ').map(w => w[0]).join('').slice(0, 2).toUpperCase()
}

function Avatar({ name, size = 36 }: { name: string; size?: number }) {
  const idx = name.charCodeAt(0) % AVATAR_COLORS.length
  return (
    <div style={{
      width: size, height: size, borderRadius: '50%', background: AVATAR_COLORS[idx],
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      fontSize: size * 0.38, fontWeight: 600, color: '#fff',
      fontFamily: 'var(--font-display)', flexShrink: 0,
    }}>
      {getInitials(name)}
    </div>
  )
}

function TeamDot({ color }: { color: string }) {
  return <span style={{ width: 8, height: 8, borderRadius: '50%', background: color, flexShrink: 0, display: 'inline-block' }} />
}

type TeamType = { id: string; name: string; members: number; color: string; mission: string }
type View = { kind: 'team'; id: string } | { kind: 'create' }
type TabId = 'mission' | 'info' | 'integrations'

// ── Roadmap node data ─────────────────────────────────────────────────────────
type Status = 'done' | 'active' | 'todo'
type RComment = { id: number; author: string; text: string; time: string }
type RFile = { id: number; name: string; size: string; author: string; date: string; url?: string }
type NodeDetail = {
  label: string; code: string; status: Status; progress: number
  goal: string; dueDate: string; assignees: string[]; prerequisites: string[]; aiSummary: string
  issue?: string
  files?: RFile[]
}

const STATUS_META: Record<Status, { label: string; color: string }> = {
  done: { label: '완료', color: '#10b981' },
  active: { label: '진행중', color: '#6b5cf6' },
  todo: { label: '대기', color: '#6b6a72' },
}

const NODE_DATA: Record<string, NodeDetail> = {
  root: {
    label: '2D 게임 개발', code: 'GOAL', status: 'active', progress: 42,
    goal: '2D 액션 RPG 한 편을 기획부터 출시까지 완주한다.',
    dueDate: '2025-12-31', assignees: ['Alex Rivera', 'Jordan Kim'], prerequisites: [],
    aiSummary: '전체 로드맵은 4개 축(디자인·엔진·스킬·보스)으로 구성되며 현재 디자인 축이 완료 단계, 엔진 축이 진행 중입니다. 스킬·보스 축은 엔진 확정 이후 착수 예정이라 4월 엔진 결정이 전체 일정의 핵심 분기점입니다.',
    files: [
      { id: 1, name: '2D_RPG_기획서_v3.pdf', size: '4.2 MB', author: 'Alex Rivera', date: '2025-03-02' },
      { id: 2, name: '전체_일정표.xlsx', size: '318 KB', author: 'Jordan Kim', date: '2025-03-11' },
    ],
  },
  m1: {
    label: '게임 디자인', code: 'P-01', status: 'done', progress: 100,
    goal: '세계관·스토리·레벨 구성을 확정하고 GDD를 완성한다.',
    dueDate: '2025-03-31', assignees: ['Sam Chen'], prerequisites: [],
    aiSummary: 'GDD 초안이 완료되어 핵심 게임루프와 진행 시스템이 확정됐습니다. 1챕터 보스 패턴 섹션만 미완이며, 보스 축 착수 전까지 보완하면 후속 작업에 영향은 없습니다.',
    files: [
      { id: 1, name: 'GDD_최종.docx', size: '1.8 MB', author: 'Sam Chen', date: '2025-03-28' },
      { id: 2, name: '레벨_레이아웃.fig', size: '12.4 MB', author: 'Sam Chen', date: '2025-03-30' },
    ],
  },
  m2: {
    label: '물리엔진', code: 'P-02', status: 'active', progress: 55,
    goal: '충돌·중력·이동을 포함한 물리 시뮬레이션 레이어를 구축한다.',
    dueDate: '2025-05-15', assignees: ['Morgan Lee', 'Sam Chen'], prerequisites: ['게임 디자인'],
    aiSummary: '유니티 채택과 자체 개발 두 갈래를 병행 검토 중입니다. 프로토타입에서 경사면 이동 시 튀는 이슈가 남아 있어, 이 문제 해결 여부가 엔진 선택의 결정 근거가 될 전망입니다.',
    files: [
      { id: 1, name: '엔진_비교_리포트.pdf', size: '960 KB', author: 'Morgan Lee', date: '2025-04-08' },
    ],
  },
  m3: {
    label: '스킬 코딩', code: 'P-03', status: 'todo', progress: 12,
    goal: '검사·궁수·마법사 3개 직업의 스킬 시스템을 구현한다.',
    dueDate: '2025-07-31', assignees: ['Alex Rivera'], prerequisites: ['게임 디자인', '물리엔진'],
    aiSummary: '쿨타임·피격 판정·이펙트 연동이 물리 레이어에 강하게 의존합니다. 엔진 확정 전 착수한 코드는 재작업 가능성이 높아, 공통 인터페이스만 먼저 잡는 방식이 안전합니다.',
  },
  m4: {
    label: '보스몹 코딩', code: 'P-04', status: 'todo', progress: 0,
    goal: '보스 AI 패턴과 페이즈 전환 시스템을 구현한다.',
    dueDate: '2025-09-30', assignees: ['Jordan Park', 'Alex Rivera'], prerequisites: ['물리엔진', '스킬 코딩'],
    aiSummary: '오크·트롤 두 보스가 계획되어 있으며 각각 고유 패턴과 약점을 가집니다. 플레이어 스킬 밸런스가 확정되어야 패턴 난이도를 정할 수 있어 스킬 축 완료가 선행 조건입니다.',
  },
  l1: {
    label: '게임 장르', code: 'T-011', status: 'done', progress: 100,
    goal: '2D 액션 RPG로 장르를 확정하고 타깃 유저를 정의한다.',
    dueDate: '2025-02-28', assignees: ['Sam Chen'], prerequisites: [],
    aiSummary: '경쟁 타이틀 분석을 통해 2D 액션 RPG로 확정됐습니다. 차별화 축은 직업별 스킬 조합 다양성으로 설정되어 이후 스킬 설계 방향을 규정합니다.',
    files: [
      { id: 1, name: '장르_시장조사.pdf', size: '2.1 MB', author: 'Sam Chen', date: '2025-02-20' },
    ],
  },
  l2: {
    label: '유니티', code: 'T-021', status: 'active', progress: 70,
    goal: 'Unity 2022 LTS 기반 물리 환경을 세팅하고 검증한다.',
    dueDate: '2025-04-01', assignees: ['Morgan Lee'], prerequisites: ['게임 디자인'],
    aiSummary: 'Rigidbody2D 이동과 Physics2D 충돌 레이어 구성이 대부분 완료됐습니다. 개발 속도 측면에서 자체 개발보다 유리하나 세밀한 물리 제어에는 제약이 있습니다.',
    files: [
      { id: 1, name: 'unity_physics_proto.zip', size: '28.6 MB', author: 'Morgan Lee', date: '2025-03-25' },
      { id: 2, name: '경사면_버그_로그.txt', size: '46 KB', author: 'Morgan Lee', date: '2025-04-05' },
    ],
    issue: '경사면 이동 시 캐릭터가 튀는 물리 버그가 재현되고 있습니다. Physics2D 마찰 설정으로 해결되지 않아 엔진 채택 결정이 보류 중이며, 하위 작업(검사·궁수·마법사)도 함께 지연될 수 있습니다.',
  },
  l3: {
    label: '자체 개발', code: 'T-022', status: 'todo', progress: 25,
    goal: '외부 엔진 없이 AABB 충돌·중력 물리 레이어를 구현한다.',
    dueDate: '2025-06-30', assignees: ['Sam Chen', 'Morgan Lee'], prerequisites: ['게임 디자인'],
    aiSummary: '개념 증명 단계로 기본 충돌·중력만 동작합니다. 제어 자유도는 높지만 일정 리스크가 커서 유니티 안과 4월 중 택일하는 판단이 권장됩니다.',
  },
  l4: {
    label: '검사', code: 'T-031', status: 'todo', progress: 30,
    goal: '강공격·막기·카운터로 구성된 근접 직업을 구현한다.',
    dueDate: '2025-07-01', assignees: ['Alex Rivera'], prerequisites: ['물리엔진'],
    aiSummary: '높은 방어력과 중간 이동속도를 가진 기준 직업입니다. 카운터 판정 프레임이 넓다는 리뷰가 있어 밸런스 기준값 확정이 선행되어야 합니다.',
  },
  l5: {
    label: '궁수', code: 'T-032', status: 'todo', progress: 10,
    goal: '투사체 물리를 기반으로 원거리 직업을 구현한다.',
    dueDate: '2025-07-15', assignees: ['Alex Rivera'], prerequisites: ['물리엔진'],
    aiSummary: '화살 궤도와 충돌 처리가 물리 레이어에 직접 의존합니다. 기본·관통·폭발 3종 화살 중 폭발 화살만 광역 판정 설계가 추가로 필요합니다.',
  },
  l6: {
    label: '마법사', code: 'T-033', status: 'todo', progress: 0,
    goal: '광역 마법 3종과 VFX 연동을 구현한다.',
    dueDate: '2025-08-01', assignees: ['Jordan Park'], prerequisites: ['물리엔진'],
    aiSummary: '최저 방어력·최고 딜 포텐셜 구조로, 세 직업 중 밸런스 민감도가 가장 높습니다. VFX 에셋 제작이 코드 작업과 병렬 진행 가능합니다.',
  },
  l7: {
    label: '오크', code: 'T-041', status: 'todo', progress: 0,
    goal: '1챕터 보스의 돌진·지진 패턴과 분노 페이즈를 구현한다.',
    dueDate: '2025-08-31', assignees: ['Jordan Park'], prerequisites: ['스킬 코딩'],
    aiSummary: '패턴 상태머신이 단순해 보스 AI 구조의 레퍼런스 역할을 합니다. 여기서 정립한 페이즈 전환 규격을 트롤에 재사용하는 것이 효율적입니다.',
  },
  l8: {
    label: '트롤', code: 'T-042', status: 'todo', progress: 0,
    goal: '재생 능력과 약점 부위 판정을 갖춘 2챕터 보스를 구현한다.',
    dueDate: '2025-09-15', assignees: ['Alex Rivera', 'Jordan Park'], prerequisites: ['스킬 코딩', '오크'],
    aiSummary: '재생 쿨타임이 짧을 경우 근접 직업의 클리어가 사실상 불가능하다는 테스트 결과가 있습니다. 약점 판정 크기와 재생 주기를 함께 튜닝해야 합니다.',
  },
}

// ── Roadmap canvas ────────────────────────────────────────────────────────────
type Tier = 'root' | 'mid' | 'leaf'
type RNode = NodeDetail & { id: string; tier: Tier; x: number; y: number; w: number; h: number; comments: RComment[] }
type REdge = { id: string; from: string; to: string }

const LEAF_W = 128, LEAF_H = 58, MID_W = 182, MID_H = 66, ROOT_W = 240, ROOT_H = 74
const PAD_X = 46, GROUP_GAP = 46, LEAF_GAP = 16
const ROOT_Y = 22, MID_Y = 216, LEAF_Y = 396, MIN_CANVAS_H = 516
const SIZE: Record<Tier, { w: number; h: number }> = {
  root: { w: ROOT_W, h: ROOT_H }, mid: { w: MID_W, h: MID_H }, leaf: { w: LEAF_W, h: LEAF_H },
}

const GROUPS: { mid: string; leaves: string[] }[] = [
  { mid: 'm1', leaves: ['l1'] },
  { mid: 'm2', leaves: ['l2', 'l3'] },
  { mid: 'm3', leaves: ['l4', 'l5', 'l6'] },
  { mid: 'm4', leaves: ['l7', 'l8'] },
]

function buildInitialGraph(): { nodes: RNode[]; edges: REdge[] } {
  const nodes: RNode[] = []
  const edges: REdge[] = []
  let cursor = PAD_X
  for (const g of GROUPS) {
    const groupW = g.leaves.length * LEAF_W + (g.leaves.length - 1) * LEAF_GAP
    g.leaves.forEach((id, i) => {
      nodes.push({ ...NODE_DATA[id], id, tier: 'leaf', comments: [], x: cursor + i * (LEAF_W + LEAF_GAP), y: LEAF_Y, ...SIZE.leaf })
      edges.push({ id: `e-${g.mid}-${id}`, from: g.mid, to: id })
    })
    nodes.push({ ...NODE_DATA[g.mid], id: g.mid, tier: 'mid', comments: [], x: cursor + groupW / 2 - MID_W / 2, y: MID_Y, ...SIZE.mid })
    edges.push({ id: `e-root-${g.mid}`, from: 'root', to: g.mid })
    cursor += groupW + GROUP_GAP
  }
  const totalW = cursor - GROUP_GAP + PAD_X
  nodes.push({ ...NODE_DATA.root, id: 'root', tier: 'root', comments: [], x: totalW / 2 - ROOT_W / 2, y: ROOT_Y, ...SIZE.root })
  return { nodes, edges }
}

/** Orthogonal connector: down from `from`, across a bus line, down into `to`. */
function elbow(from: RNode, to: RNode, r = 12) {
  const x1 = from.x + from.w / 2, y1 = from.y + from.h
  const x2 = to.x + to.w / 2, y2 = to.y - 7
  if (y2 <= y1 + 12) {
    // target sits above/beside the source — route around the side
    const midX = (x1 + x2) / 2
    return `M ${x1} ${y1} L ${x1} ${y1 + 18} L ${midX} ${y1 + 18} L ${midX} ${y2 - 18} L ${x2} ${y2 - 18} L ${x2} ${y2}`
  }
  const busY = (y1 + y2) / 2
  if (Math.abs(x1 - x2) < 1) return `M ${x1} ${y1} L ${x2} ${y2}`
  const dir = x2 > x1 ? 1 : -1
  const rr = Math.max(2, Math.min(r, Math.abs(x2 - x1) / 2, (busY - y1) / 2, (y2 - busY) / 2))
  return [
    `M ${x1} ${y1}`,
    `L ${x1} ${busY - rr}`,
    `Q ${x1} ${busY} ${x1 + dir * rr} ${busY}`,
    `L ${x2 - dir * rr} ${busY}`,
    `Q ${x2} ${busY} ${x2} ${busY + rr}`,
    `L ${x2} ${y2}`,
  ].join(' ')
}

function RoadmapNode({ node, warn, selected, dimmed, editing, linking, scale, onSelect, onDelete, onStartLink, onMove }: {
  node: RNode; warn: string | null; selected: boolean; dimmed: boolean; editing: boolean; linking: boolean; scale: number
  onSelect: (id: string) => void; onDelete: (id: string) => void
  onStartLink: (id: string) => void; onMove: (id: string, x: number, y: number) => void
}) {
  const st = STATUS_META[node.status]
  const isRoot = node.tier === 'root'
  // A node either has its own problem (red) or inherits one from a downstream node (amber).
  const alert = node.issue ? 'issue' : warn ? 'warn' : null
  const drag = useRef<{ cx: number; cy: number; ox: number; oy: number; moved: boolean } | null>(null)

  function handlePointerDown(e: React.PointerEvent) {
    e.stopPropagation()
    if (!editing) return
    ;(e.currentTarget as HTMLElement).setPointerCapture(e.pointerId)
    drag.current = { cx: e.clientX, cy: e.clientY, ox: node.x, oy: node.y, moved: false }
  }
  function handlePointerMove(e: React.PointerEvent) {
    if (!drag.current) return
    const nx = Math.max(8, Math.round((drag.current.ox + (e.clientX - drag.current.cx) / scale) / 4) * 4)
    const ny = Math.max(8, Math.round((drag.current.oy + (e.clientY - drag.current.cy) / scale) / 4) * 4)
    if (Math.abs(nx - node.x) > 1 || Math.abs(ny - node.y) > 1) drag.current.moved = true
    onMove(node.id, nx, ny)
  }
  function handlePointerUp(e: React.PointerEvent) {
    const d = drag.current
    drag.current = null
    ;(e.currentTarget as HTMLElement).releasePointerCapture?.(e.pointerId)
    // Only the drag gesture (edit mode) resolves to a click here; plain clicks use onClick.
    if (d && !d.moved) onSelect(node.id)
  }

  return (
    <div
      onPointerDown={handlePointerDown}
      onPointerMove={handlePointerMove}
      onPointerUp={handlePointerUp}
      onClick={() => { if (!editing) onSelect(node.id) }}
      style={{
        position: 'absolute', left: node.x, top: node.y, width: node.w, height: node.h,
        display: 'flex', flexDirection: 'column', justifyContent: 'center', gap: 5,
        padding: isRoot ? '0 18px' : '0 14px',
        background: alert === 'issue' ? (selected ? '#2a1d20' : '#1f1518')
          : alert === 'warn' ? (isRoot ? '#6b5cf6' : selected ? '#2a2418' : '#1f1c12')
            : isRoot ? '#6b5cf6' : selected ? '#252533' : '#191922',
        border: `1.5px solid ${linking ? '#f59e0b' : alert === 'issue' ? '#ef4444' : alert === 'warn' ? '#eab308' : selected ? '#9b8cff' : isRoot ? '#9b8cff' : '#3a3a4a'}`,
        borderRadius: isRoot ? 14 : 10,
        boxShadow: alert === 'issue'
          ? '0 0 0 4px rgba(239,68,68,0.18), 0 6px 18px rgba(0,0,0,0.4)'
          : alert === 'warn'
            ? '0 0 0 4px rgba(234,179,8,0.15), 0 6px 18px rgba(0,0,0,0.4)'
          : selected
            ? '0 0 0 4px rgba(107,92,246,0.26), 0 10px 24px rgba(0,0,0,0.45)'
            : isRoot ? '0 10px 30px rgba(107,92,246,0.3)' : '0 2px 10px rgba(0,0,0,0.3)',
        opacity: dimmed ? 0.42 : 1,
        cursor: editing ? 'grab' : 'pointer', userSelect: 'none', touchAction: 'none',
        transition: 'opacity 0.18s, background 0.15s, border-color 0.15s, box-shadow 0.18s',
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 6, minWidth: 0 }}>
        <span style={{
          width: 6, height: 6, borderRadius: '50%', flexShrink: 0,
          background: isRoot ? '#ffffff' : st.color,
          boxShadow: node.status === 'active' ? `0 0 0 3px ${st.color}30` : 'none',
        }} />
        <span style={{
          fontFamily: 'var(--font-display)', fontWeight: isRoot ? 700 : 600,
          fontSize: isRoot ? 17 : node.tier === 'mid' ? 14 : 13,
          color: alert === 'issue' ? '#ffb4b4' : alert === 'warn' && !isRoot ? '#f7de8f' : isRoot ? '#ffffff' : '#f1f0f8',
          letterSpacing: '-0.01em', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
        }}>
          {node.label}
        </span>
        {alert && (
          <span title={alert === 'issue' ? node.issue : `하위 작업에 문제가 있습니다 — ${warn}`} style={{
            display: 'inline-flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
            width: 15, height: 15, borderRadius: '50%',
            background: alert === 'issue' ? '#ef4444' : '#eab308',
            animation: `${alert === 'issue' ? 'issuePulse' : 'warnPulse'} 1.6s ease-in-out infinite`,
          }}>
            <svg width="9" height="9" viewBox="0 0 10 10" fill="none">
              <path d="M5 2v3.4M5 7.4v.1" stroke={alert === 'issue' ? '#fff' : '#3d2f04'} strokeWidth="1.6" strokeLinecap="round" />
            </svg>
          </span>
        )}
        {node.status === 'done' && !isRoot && (
          <svg width="11" height="11" viewBox="0 0 12 12" fill="none" style={{ flexShrink: 0 }}>
            <path d="M1.5 6.3L4.4 9.2L10.5 3" stroke="#10b981" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        )}
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <span style={{ fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: '0.1em', color: isRoot ? 'rgba(255,255,255,0.8)' : '#9998ad' }}>
          {node.code}
        </span>
        <span style={{ flex: 1, height: 3, borderRadius: 2, background: isRoot ? 'rgba(255,255,255,0.25)' : '#33333f', overflow: 'hidden' }}>
          <span style={{
            display: 'block', height: '100%', width: `${node.progress}%`, borderRadius: 2,
            background: isRoot ? '#ffffff' : st.color, transition: 'width 0.4s ease',
          }} />
        </span>
        <span style={{ fontFamily: 'var(--font-mono)', fontSize: 9, color: isRoot ? 'rgba(255,255,255,0.8)' : '#9998ad' }}>
          {node.progress}%
        </span>
      </div>

      {editing && (
        <>
          {!isRoot && (
            <button
              onPointerDown={e => e.stopPropagation()}
              onClick={e => { e.stopPropagation(); onDelete(node.id) }}
              title="노드 삭제"
              style={{
                position: 'absolute', top: -9, right: -9, width: 20, height: 20, borderRadius: '50%',
                background: '#ef4444', border: '2px solid #0f0f14', color: '#fff', cursor: 'pointer',
                display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 0, lineHeight: 1,
              }}>
              <svg width="8" height="8" viewBox="0 0 8 8" fill="none">
                <path d="M1 1L7 7M7 1L1 7" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
              </svg>
            </button>
          )}
          <button
            onPointerDown={e => e.stopPropagation()}
            onClick={e => { e.stopPropagation(); onStartLink(node.id) }}
            title="여기서 화살표 연결 시작"
            style={{
              position: 'absolute', bottom: -9, left: '50%', marginLeft: -10, width: 20, height: 20, borderRadius: '50%',
              background: linking ? '#f59e0b' : '#6b5cf6', border: '2px solid #0f0f14', color: '#fff', cursor: 'pointer',
              display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 0,
            }}>
            <svg width="9" height="9" viewBox="0 0 10 10" fill="none">
              <path d="M5 1V9M5 9L2 6M5 9L8 6" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
          </button>
        </>
      )}
    </div>
  )
}

function RoadmapCanvas({ nodes, edges, selectedId, editing, linkFrom, onSelect, onMove, onDeleteNode, onStartLink, onDeleteEdge }: {
  nodes: RNode[]; edges: REdge[]; selectedId: string | null; editing: boolean; linkFrom: string | null
  onSelect: (id: string) => void; onMove: (id: string, x: number, y: number) => void
  onDeleteNode: (id: string) => void; onStartLink: (id: string) => void; onDeleteEdge: (id: string) => void
}) {
  const [hoverEdge, setHoverEdge] = useState<string | null>(null)
  const [panning, setPanning] = useState(false)
  const viewportRef = useRef<HTMLDivElement>(null)
  const pan = useRef<{ x: number; y: number; sl: number; st: number } | null>(null)
  const [scale, setScale] = useState(1)
  // Bubble a downstream node's problem up to every ancestor so parents flag it in amber.
  const warnMap = (() => {
    const parents: Record<string, string[]> = {}
    for (const e of edges) (parents[e.to] ??= []).push(e.from)
    const out: Record<string, string> = {}
    for (const n of nodes) {
      if (!n.issue) continue
      const seen = new Set<string>([n.id])
      const queue = [...(parents[n.id] ?? [])]
      while (queue.length) {
        const id = queue.shift()!
        if (seen.has(id)) continue
        seen.add(id)
        out[id] = out[id] ? `${out[id]}, ${n.label}` : n.label
        queue.push(...(parents[id] ?? []))
      }
    }
    return out
  })()

  const byId = Object.fromEntries(nodes.map(n => [n.id, n]))
  const width = Math.max(680, ...nodes.map(n => n.x + n.w + PAD_X))
  const height = Math.max(MIN_CANVAS_H, ...nodes.map(n => n.y + n.h + 40))

  // Highlight the ancestor path of the selected node.
  const activePath = (() => {
    const s = new Set<string>()
    if (!selectedId || editing) return s
    s.add(selectedId)
    let frontier = [selectedId]
    for (let depth = 0; depth < 6 && frontier.length; depth++) {
      const parents = edges.filter(e => frontier.includes(e.to)).map(e => e.from).filter(p => !s.has(p))
      parents.forEach(p => s.add(p))
      frontier = parents
    }
    return s.size > 1 ? s : new Set<string>()
  })()
  const isDim = (id: string) => activePath.size > 0 && !activePath.has(id)

  // Wheel zooms the canvas (anchored at the cursor) instead of scrolling it.
  useEffect(() => {
    const vp = viewportRef.current
    if (!vp) return
    function onWheel(e: WheelEvent) {
      e.preventDefault()
      const el = viewportRef.current
      if (!el) return
      setScale(prev => {
        const next = Math.min(2, Math.max(0.4, prev * (e.deltaY > 0 ? 0.92 : 1.08)))
        if (next === prev) return prev
        const rect = el.getBoundingClientRect()
        const px = e.clientX - rect.left, py = e.clientY - rect.top
        const ratio = next / prev
        el.scrollLeft = (el.scrollLeft + px) * ratio - px
        el.scrollTop = (el.scrollTop + py) * ratio - py
        return next
      })
    }
    vp.addEventListener('wheel', onWheel, { passive: false })
    return () => vp.removeEventListener('wheel', onWheel)
  }, [])

  function handlePanDown(e: React.PointerEvent) {
    const vp = viewportRef.current
    // Let edge hit-areas keep their own click handling.
    if (!vp || (e.target as Element).tagName === 'path') return
    pan.current = { x: e.clientX, y: e.clientY, sl: vp.scrollLeft, st: vp.scrollTop }
    setPanning(true)
    ;(e.currentTarget as HTMLElement).setPointerCapture(e.pointerId)
  }
  function handlePanMove(e: React.PointerEvent) {
    const vp = viewportRef.current
    if (!pan.current || !vp) return
    vp.scrollLeft = pan.current.sl - (e.clientX - pan.current.x)
    vp.scrollTop = pan.current.st - (e.clientY - pan.current.y)
  }
  function handlePanUp(e: React.PointerEvent) {
    pan.current = null
    setPanning(false)
    ;(e.currentTarget as HTMLElement).releasePointerCapture?.(e.pointerId)
  }

  return (
    <div style={{ position: 'relative', height: '100%' }}>
    <div
      ref={viewportRef}
      className="rm-viewport"
      onPointerDown={handlePanDown}
      onPointerMove={handlePanMove}
      onPointerUp={handlePanUp}
      onPointerCancel={handlePanUp}
      style={{
        overflow: 'auto', padding: '18px 0 0',
        height: '100%', display: 'flex',
        cursor: panning ? 'grabbing' : 'grab', touchAction: 'none',
      }}
    >
      <div style={{ width: width * scale, height: height * scale, position: 'relative', margin: 'auto', flexShrink: 0 }}>
      <div style={{
        position: 'relative', width, height, minWidth: width,
        transform: `scale(${scale})`, transformOrigin: '0 0',
      }}>

        <svg width={width} height={height} style={{ position: 'absolute', inset: 0 }}>
          <defs>
            <marker id="rm-arrow" markerWidth="9" markerHeight="9" refX="6.5" refY="3" orient="auto">
              <path d="M0,0 L0,6 L7,3 z" fill="#7d7c96" />
            </marker>
            <marker id="rm-arrow-on" markerWidth="9" markerHeight="9" refX="6.5" refY="3" orient="auto">
              <path d="M0,0 L0,6 L7,3 z" fill="#a99bff" />
            </marker>
            <marker id="rm-arrow-del" markerWidth="9" markerHeight="9" refX="6.5" refY="3" orient="auto">
              <path d="M0,0 L0,6 L7,3 z" fill="#ef4444" />
            </marker>
          </defs>

          {edges.map(e => {
            const a = byId[e.from], b = byId[e.to]
            if (!a || !b) return null
            const d = elbow(a, b)
            const on = activePath.has(e.from) && activePath.has(e.to)
            const del = editing && hoverEdge === e.id
            return (
              <g key={e.id}>
                <path d={d} fill="none" stroke={del ? '#ef4444' : on ? '#a99bff' : '#7d7c96'}
                  strokeWidth={del || on ? 2 : 1.4}
                  markerEnd={del ? 'url(#rm-arrow-del)' : on ? 'url(#rm-arrow-on)' : 'url(#rm-arrow)'}
                  opacity={activePath.size > 0 && !on ? 0.45 : 1}
                  style={{ transition: 'stroke 0.12s' }} />
                {editing && (
                  <path d={d} fill="none" stroke="transparent" strokeWidth={14} style={{ cursor: 'pointer' }}
                    onMouseEnter={() => setHoverEdge(e.id)} onMouseLeave={() => setHoverEdge(null)}
                    onClick={() => { setHoverEdge(null); onDeleteEdge(e.id) }}>
                    <title>클릭하면 이 화살표를 삭제합니다</title>
                  </path>
                )}
              </g>
            )
          })}
        </svg>

        {nodes.map(n => (
          <RoadmapNode key={n.id} node={n} warn={warnMap[n.id] ?? null}
            selected={selectedId === n.id} dimmed={isDim(n.id)}
            editing={editing} linking={linkFrom === n.id}
            onSelect={onSelect} onDelete={onDeleteNode} onStartLink={onStartLink} onMove={onMove}
            scale={scale} />
        ))}
      </div>
      </div>
    </div>

      {/* Zoom indicator */}
      <div style={{
        position: 'absolute', right: 16, bottom: 12, display: 'flex', alignItems: 'center', gap: 8,
        padding: '5px 8px 5px 11px', borderRadius: 100, background: '#191922e6',
        border: '1px solid #3a3a4a', pointerEvents: 'auto',
      }}>
        <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10, color: '#b0aec2' }}>
          {Math.round(scale * 100)}%
        </span>
        <button onClick={() => { setScale(1); if (viewportRef.current) { viewportRef.current.scrollLeft = 0; viewportRef.current.scrollTop = 0 } }}
          style={{
            padding: '3px 9px', borderRadius: 100, border: 'none', background: '#2c2c3a',
            color: '#e5e4ef', fontFamily: 'var(--font-display)', fontSize: 10, fontWeight: 600, cursor: 'pointer',
          }}>
          리셋
        </button>
      </div>
    </div>
  )
}

// ── Node detail panel ─────────────────────────────────────────────────────────
function daysLeft(due: string) {
  const t = new Date(due).getTime()
  if (Number.isNaN(t)) return '—'
  const diff = Math.ceil((t - new Date('2025-04-01').getTime()) / 86400000)
  return diff >= 0 ? `D-${diff}` : `D+${-diff}`
}

// Uploaded files carry a blob URL; seeded sample files fall back to a generated stub.
function downloadFile(f: RFile) {
  const href = f.url ?? URL.createObjectURL(new Blob(
    [`${f.name}\n제출자: ${f.author}\n제출일: ${f.date}\n\n(샘플 파일입니다)`],
    { type: 'text/plain;charset=utf-8' },
  ))
  const a = document.createElement('a')
  a.href = href
  a.download = f.name
  document.body.appendChild(a)
  a.click()
  a.remove()
  if (!f.url) setTimeout(() => URL.revokeObjectURL(href), 1000)
}

function formatBytes(n: number) {
  if (n < 1024) return `${n} B`
  if (n < 1024 * 1024) return `${Math.round(n / 1024)} KB`
  return `${(n / (1024 * 1024)).toFixed(1)} MB`
}

const fieldStyle: React.CSSProperties = {
  width: '100%', padding: '10px 13px', borderRadius: 10,
  border: '1.5px solid var(--color-border)', background: '#fafaf8',
  fontSize: 13, lineHeight: 1.7, color: 'var(--color-foreground)',
  fontFamily: 'var(--font-body)', outline: 'none', boxSizing: 'border-box', resize: 'none',
}

function ChipEditor({ items, color, placeholder, onChange }: {
  items: string[]; color: string; placeholder: string; onChange: (next: string[]) => void
}) {
  const [draft, setDraft] = useState('')
  function add() {
    const v = draft.trim()
    if (v && !items.includes(v)) onChange([...items, v])
    setDraft('')
  }
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
      {items.length > 0 && (
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
          {items.map(it => (
            <span key={it} style={{
              display: 'inline-flex', alignItems: 'center', gap: 6, padding: '5px 8px 5px 11px',
              borderRadius: 100, background: `${color}12`, border: `1px solid ${color}30`,
              fontSize: 12, fontWeight: 500, color: 'var(--color-foreground)',
            }}>
              {it}
              <button onClick={() => onChange(items.filter(x => x !== it))}
                style={{ border: 'none', background: 'none', padding: 0, cursor: 'pointer', display: 'flex', opacity: 0.45 }}>
                <svg width="10" height="10" viewBox="0 0 10 10" fill="none">
                  <path d="M1.5 1.5L8.5 8.5M8.5 1.5L1.5 8.5" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" />
                </svg>
              </button>
            </span>
          ))}
        </div>
      )}
      <div style={{ display: 'flex', gap: 6 }}>
        <input value={draft} onChange={e => setDraft(e.target.value)}
          onKeyDown={e => { if (e.key === 'Enter') { e.preventDefault(); add() } }}
          placeholder={placeholder} style={{ ...fieldStyle, padding: '8px 12px', fontSize: 12.5 }} />
        <button onClick={add} disabled={!draft.trim()} style={{
          padding: '8px 14px', borderRadius: 10, border: 'none', flexShrink: 0,
          background: draft.trim() ? color : '#e0dfd9', color: '#fff',
          fontFamily: 'var(--font-display)', fontSize: 12, fontWeight: 600,
          cursor: draft.trim() ? 'pointer' : 'not-allowed',
        }}>
          추가
        </button>
      </div>
    </div>
  )
}

function NodeDetailPanel({ node, color, onChange, onClose }: {
  node: RNode; color: string; onChange: (patch: Partial<RNode>) => void; onClose: () => void
}) {
  const st = STATUS_META[node.status]
  const [view, setView] = useState<'info' | 'comments'>('info')
  const [draft, setDraft] = useState('')
  const comments = node.comments ?? []
  const files = node.files ?? []

  function submitComment() {
    const text = draft.trim()
    if (!text) return
    onChange({ comments: [...comments, { id: Date.now(), author: 'Jordan Kim', text, time: '방금 전' }] })
    setDraft('')
  }

  const section = (ko: string, en: string) => (
    <div style={{ display: 'flex', alignItems: 'baseline', gap: 8, marginBottom: 10 }}>
      <span style={{ fontFamily: 'var(--font-display)', fontSize: 12, fontWeight: 700, color: 'var(--color-foreground)', letterSpacing: '-0.01em' }}>
        {ko}
      </span>
      <span style={{ fontFamily: 'var(--font-mono)', fontSize: 9, color: 'var(--color-muted-foreground)', textTransform: 'uppercase', letterSpacing: '0.1em' }}>
        {en}
      </span>
    </div>
  )

  return (
    <div style={{
      width: 348, minWidth: 348, background: '#ffffff',
      borderTop: '1px solid var(--color-border)', borderLeft: '1px solid var(--color-border)',
      borderRadius: 0, display: 'flex', flexDirection: 'column',
      height: '100%', overflowY: 'auto',
      animation: 'slideInRight 0.18s ease',
    }}>
      {/* Header — name is editable inline */}
      <div style={{
        padding: '16px 18px', borderBottom: '1px solid var(--color-border)',
        display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 10,
        background: '#0f0f14', flexShrink: 0,
        position: 'sticky', top: 0, zIndex: 1,
      }}>
        <div style={{ minWidth: 0, flex: 1 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 7, marginBottom: 7 }}>
            <span style={{ fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: '0.12em', color: '#8b8a9e' }}>
              {node.code}
            </span>
            <span style={{
              display: 'inline-flex', alignItems: 'center', gap: 5, padding: '2px 8px', borderRadius: 100,
              background: `${st.color}22`, border: `1px solid ${st.color}4d`,
              fontFamily: 'var(--font-mono)', fontSize: 9, color: st.color, letterSpacing: '0.05em',
            }}>
              <span style={{ width: 5, height: 5, borderRadius: '50%', background: st.color }} />
              {st.label}
            </span>
          </div>
          <input
            value={node.label} onChange={e => onChange({ label: e.target.value })}
            placeholder="노드 이름"
            style={{
              width: '100%', background: 'transparent', border: '1.5px solid transparent', borderRadius: 8,
              padding: '3px 6px', margin: '0 0 0 -6px',
              fontFamily: 'var(--font-display)', fontWeight: 700, fontSize: 18, color: '#ffffff',
              letterSpacing: '-0.02em', outline: 'none',
            }}
            onFocus={e => { e.target.style.borderColor = '#6b5cf6'; e.target.style.background = '#1a1a24' }}
            onBlur={e => { e.target.style.borderColor = 'transparent'; e.target.style.background = 'transparent' }}
          />
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexShrink: 0 }}>
        <button onClick={() => setView(v => (v === 'comments' ? 'info' : 'comments'))}
          title="댓글"
          style={{
            display: 'flex', alignItems: 'center', gap: 6, height: 26, padding: '0 10px', borderRadius: 7,
            border: `1px solid ${view === 'comments' ? '#ffffff' : '#4a4a5c'}`, flexShrink: 0,
            background: view === 'comments' ? '#ffffff' : 'transparent',
            color: view === 'comments' ? '#0f0f14' : '#ffffff', cursor: 'pointer',
            fontFamily: 'var(--font-display)', fontSize: 11, fontWeight: 600,
          }}>
          <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
            <path d="M1.5 2.5h9v6h-5l-2.5 2v-2h-1.5v-6Z" stroke="currentColor" strokeWidth="1.2" strokeLinejoin="round" />
          </svg>
          댓글{comments.length > 0 ? ` ${comments.length}` : ''}
        </button>
        <button onClick={onClose} style={{
          width: 26, height: 26, borderRadius: 7, border: '1px solid #33333f', flexShrink: 0,
          background: 'transparent', display: 'flex', alignItems: 'center', justifyContent: 'center',
          cursor: 'pointer', color: '#a9a8ba',
        }}>
          <svg width="11" height="11" viewBox="0 0 12 12" fill="none">
            <path d="M1 1L11 11M11 1L1 11" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
          </svg>
        </button>
        </div>
      </div>

      {view === 'comments' ? (
        <div style={{ padding: 18, display: 'flex', flexDirection: 'column', gap: 14 }}>
          {comments.length === 0 && (
            <div style={{
              padding: '18px 14px', borderRadius: 12, border: '1px dashed var(--color-border)',
              fontSize: 12.5, color: 'var(--color-muted-foreground)', textAlign: 'center', lineHeight: 1.6,
            }}>
              아직 댓글이 없습니다.<br />이 작업에 대한 의견을 남겨보세요.
            </div>
          )}

          {comments.map(c => (
            <div key={c.id} style={{ display: 'flex', gap: 9 }}>
              <Avatar name={c.author} size={26} />
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ display: 'flex', alignItems: 'baseline', gap: 7, marginBottom: 4 }}>
                  <span style={{ fontSize: 12.5, fontWeight: 600, color: 'var(--color-foreground)' }}>{c.author}</span>
                  <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10, color: 'var(--color-muted-foreground)' }}>{c.time}</span>
                  <button onClick={() => onChange({ comments: comments.filter(x => x.id !== c.id) })}
                    style={{ marginLeft: 'auto', border: 'none', background: 'none', padding: 0, cursor: 'pointer', opacity: 0.35, display: 'flex' }}>
                    <svg width="10" height="10" viewBox="0 0 10 10" fill="none">
                      <path d="M1.5 1.5L8.5 8.5M8.5 1.5L1.5 8.5" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" />
                    </svg>
                  </button>
                </div>
                <p style={{
                  margin: 0, fontSize: 12.5, lineHeight: 1.7, color: 'var(--color-foreground)',
                  background: '#f5f4f0', borderRadius: 10, padding: '8px 11px',
                }}>
                  {c.text}
                </p>
              </div>
            </div>
          ))}

          <div style={{ display: 'flex', gap: 8, alignItems: 'flex-end', marginTop: 4 }}>
            <Avatar name="Jordan Kim" size={26} />
            <textarea
              value={draft} onChange={e => setDraft(e.target.value)}
              onKeyDown={e => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); submitComment() } }}
              placeholder="댓글 작성… (Enter로 등록)" rows={2}
              style={{ ...fieldStyle, fontSize: 12.5, padding: '9px 12px' }}
            />
            <button onClick={submitComment} disabled={!draft.trim()} style={{
              width: 34, height: 34, borderRadius: 10, border: 'none', flexShrink: 0,
              background: draft.trim() ? color : '#e0dfd9', color: '#fff',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              cursor: draft.trim() ? 'pointer' : 'not-allowed',
            }}>
              <svg width="13" height="13" viewBox="0 0 11 11" fill="none">
                <path d="M5.5 9V2M5.5 2L2.5 5M5.5 2L8.5 5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            </button>
          </div>
        </div>
      ) : (
      <div style={{ padding: 18, display: 'flex', flexDirection: 'column', gap: 22 }}>

        {/* 문제 발생 */}
        {node.issue !== undefined && (
          <div style={{
            borderRadius: 14, border: '1.5px solid #ef444440', background: '#ef44440d', padding: '14px 15px',
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 9 }}>
              <span style={{
                width: 17, height: 17, borderRadius: '50%', background: '#ef4444', flexShrink: 0,
                display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
              }}>
                <svg width="10" height="10" viewBox="0 0 10 10" fill="none">
                  <path d="M5 2v3.4M5 7.4v.1" stroke="#fff" strokeWidth="1.7" strokeLinecap="round" />
                </svg>
              </span>
              <span style={{ fontFamily: 'var(--font-display)', fontSize: 12.5, fontWeight: 700, color: '#b91c1c' }}>
                문제 발생
              </span>
              <span style={{ fontFamily: 'var(--font-mono)', fontSize: 9, color: '#c2504f', letterSpacing: '0.1em', textTransform: 'uppercase' }}>
                Blocked
              </span>
              <button onClick={() => onChange({ issue: undefined })}
                style={{
                  marginLeft: 'auto', padding: '4px 10px', borderRadius: 7, border: '1px solid #ef444440',
                  background: 'transparent', color: '#b91c1c', cursor: 'pointer',
                  fontFamily: 'var(--font-display)', fontSize: 11, fontWeight: 600,
                }}>
                해결됨
              </button>
            </div>
            <textarea value={node.issue} onChange={e => onChange({ issue: e.target.value })} rows={4}
              placeholder="어떤 문제가 발생했는지 적어주세요"
              style={{ ...fieldStyle, background: '#ffffff', border: '1.5px solid #ef444433', fontSize: 12.5 }} />
          </div>
        )}

        {/* 제출된 파일 */}
        <div>
          {section('제출된 파일', 'Files')}
          {files.length === 0 ? (
            <p style={{
              margin: 0, padding: '14px 0', textAlign: 'center', borderRadius: 10,
              border: '1.5px dashed var(--color-border)', fontSize: 12,
              color: 'var(--color-muted-foreground)',
            }}>
              제출된 파일이 없습니다
            </p>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 7 }}>
              {files.map(f => (
                <div key={f.id} onClick={() => downloadFile(f)} title={`${f.name} 다운로드`}
                  onMouseEnter={e => { e.currentTarget.style.background = '#f2f1ec'; e.currentTarget.style.borderColor = `${color}55` }}
                  onMouseLeave={e => { e.currentTarget.style.background = '#fafaf8'; e.currentTarget.style.borderColor = 'var(--color-border)' }}
                  style={{
                    display: 'flex', alignItems: 'center', gap: 10, padding: '9px 11px',
                    borderRadius: 10, borderWidth: 1, borderStyle: 'solid', borderColor: 'var(--color-border)',
                    background: '#fafaf8', cursor: 'pointer', transition: 'background 0.12s, border-color 0.12s',
                  }}>
                  <span style={{
                    width: 30, height: 30, borderRadius: 8, flexShrink: 0,
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    background: `${color}14`, color,
                    fontFamily: 'var(--font-mono)', fontSize: 8.5, fontWeight: 500, letterSpacing: '0.04em',
                  }}>
                    {(f.name.split('.').pop() ?? '').slice(0, 4).toUpperCase()}
                  </span>
                  <div style={{ minWidth: 0, flex: 1 }}>
                    <div style={{
                      fontFamily: 'var(--font-display)', fontSize: 12.5, fontWeight: 600,
                      color: 'var(--color-foreground)',
                      whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
                    }}>
                      {f.name}
                    </div>
                    <div style={{ fontFamily: 'var(--font-mono)', fontSize: 9.5, color: 'var(--color-muted-foreground)', marginTop: 2 }}>
                      {f.size} · {f.author} · {f.date}
                    </div>
                  </div>
                  <svg width="13" height="13" viewBox="0 0 12 12" fill="none" style={{ flexShrink: 0, color: 'var(--color-muted-foreground)' }}>
                    <path d="M6 1.5v6M6 7.5L3.6 5.1M6 7.5l2.4-2.4M1.8 9.6h8.4" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" strokeLinejoin="round" />
                  </svg>
                  <button onClick={e => { e.stopPropagation(); onChange({ files: files.filter(x => x.id !== f.id) }) }}
                    title="파일 삭제"
                    style={{
                      border: 'none', background: 'none', padding: 4, cursor: 'pointer',
                      color: 'var(--color-muted-foreground)', display: 'flex', flexShrink: 0,
                    }}>
                    <svg width="10" height="10" viewBox="0 0 10 10" fill="none">
                      <path d="M1.5 1.5L8.5 8.5M8.5 1.5L1.5 8.5" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" />
                    </svg>
                  </button>
                </div>
              ))}
            </div>
          )}
          <label style={{
            display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6,
            marginTop: 8, padding: '8px 0', borderRadius: 9,
            border: '1.5px dashed var(--color-border)', cursor: 'pointer',
            fontFamily: 'var(--font-display)', fontSize: 11.5, fontWeight: 600,
            color: 'var(--color-muted-foreground)',
          }}>
            <input type="file" multiple style={{ display: 'none' }}
              onChange={e => {
                const picked = Array.from(e.target.files ?? [])
                if (picked.length === 0) return
                const today = new Date().toISOString().slice(0, 10)
                let next = Math.max(0, ...files.map(f => f.id)) + 1
                onChange({
                  files: [...files, ...picked.map(f => ({
                    id: next++, name: f.name, size: formatBytes(f.size), author: 'Jordan Kim', date: today,
                    url: URL.createObjectURL(f),
                  }))],
                })
                e.target.value = ''
              }} />
            + 파일 제출
          </label>
        </div>

        {/* 진행 상태 */}
        <div>
          {section('진행 상태', 'Status')}
          <div style={{ display: 'flex', gap: 6, marginBottom: 12 }}>
            {(['todo', 'active', 'done'] as const).map(k => {
              const on = node.status === k
              const m = STATUS_META[k]
              return (
                <button key={k}
                  onClick={() => onChange({ status: k, progress: k === 'done' ? 100 : k === 'todo' && node.progress === 100 ? 0 : node.progress })}
                  style={{
                    flex: 1, padding: '9px 0', borderRadius: 9,
                    border: `1.5px solid ${on ? m.color : 'var(--color-border)'}`,
                    background: on ? `${m.color}14` : '#ffffff',
                    color: on ? m.color : 'var(--color-muted-foreground)',
                    fontFamily: 'var(--font-display)', fontSize: 12, fontWeight: on ? 700 : 500,
                    cursor: 'pointer', transition: 'all 0.12s',
                  }}>
                  {k === 'done' ? '✓ 완료' : m.label}
                </button>
              )
            })}
          </div>
          {node.issue === undefined && (
            <button onClick={() => onChange({ issue: '' })} style={{
              width: '100%', marginBottom: 12, padding: '8px 0', borderRadius: 9,
              border: '1.5px dashed #ef444455', background: 'transparent', color: '#b91c1c',
              fontFamily: 'var(--font-display)', fontSize: 11.5, fontWeight: 600, cursor: 'pointer',
            }}>
              ! 문제 발생으로 표시
            </button>
          )}
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <input type="range" min={0} max={100} step={5} value={node.progress}
              onChange={e => onChange({ progress: Number(e.target.value) })}
              style={{ flex: 1, accentColor: color }} />
            <span style={{ fontFamily: 'var(--font-mono)', fontSize: 11, color: 'var(--color-muted-foreground)', width: 34, textAlign: 'right' }}>
              {node.progress}%
            </span>
          </div>
        </div>

        {/* 목표 */}
        <div>
          {section('목표', 'Objective')}
          <textarea value={node.goal} onChange={e => onChange({ goal: e.target.value })} rows={3}
            placeholder="이 작업이 달성해야 하는 것"
            style={{ ...fieldStyle, fontWeight: 500, background: `${color}0a`, border: `1.5px solid ${color}33` }} />
        </div>

        {/* 시간 제한 */}
        <div>
          {section('시간 제한', 'Deadline')}
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <input type="date" value={node.dueDate} onChange={e => onChange({ dueDate: e.target.value })}
              style={{ ...fieldStyle, fontFamily: 'var(--font-mono)', flex: 1 }} />
            <span style={{
              fontFamily: 'var(--font-mono)', fontSize: 11, fontWeight: 500, padding: '6px 10px', borderRadius: 7,
              background: '#0f0f14', color: '#ffffff', letterSpacing: '0.04em', flexShrink: 0,
            }}>
              {daysLeft(node.dueDate)}
            </span>
          </div>
        </div>

        {/* 진행 멤버 */}
        <div>
          {section('진행 멤버', 'Members')}
          {node.assignees.length > 0 && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginBottom: 10 }}>
              {node.assignees.map(name => (
                <div key={name} style={{
                  display: 'flex', alignItems: 'center', gap: 10,
                  padding: '8px 11px', borderRadius: 10, border: '1px solid var(--color-border)',
                }}>
                  <Avatar name={name} size={24} />
                  <span style={{ flex: 1, fontSize: 13, fontWeight: 500, color: 'var(--color-foreground)' }}>{name}</span>
                  <button onClick={() => onChange({ assignees: node.assignees.filter(a => a !== name) })}
                    style={{ border: 'none', background: 'none', cursor: 'pointer', padding: 0, display: 'flex', opacity: 0.4 }}>
                    <svg width="11" height="11" viewBox="0 0 10 10" fill="none">
                      <path d="M1.5 1.5L8.5 8.5M8.5 1.5L1.5 8.5" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" />
                    </svg>
                  </button>
                </div>
              ))}
            </div>
          )}
          <ChipEditor items={[]} color={color} placeholder="멤버 이름 입력 후 Enter"
            onChange={next => { if (next[0]) onChange({ assignees: [...node.assignees, next[0]] }) }} />
        </div>

        {/* 전 단계 할 일 */}
        <div>
          {section('전 단계 할 일', 'Prerequisites')}
          {node.prerequisites.length === 0 && (
            <div style={{
              padding: '9px 12px', borderRadius: 9, border: '1px dashed var(--color-border)',
              fontSize: 12, color: 'var(--color-muted-foreground)', marginBottom: 8,
            }}>
              선행 작업 없음 · 바로 시작 가능
            </div>
          )}
          <ChipEditor items={node.prerequisites} color={color} placeholder="선행 작업 입력 후 Enter"
            onChange={next => onChange({ prerequisites: next })} />
        </div>

        {/* AI 요약 */}
        <div>
          {section('AI 요약', 'AI Summary')}
          <div style={{ padding: '14px 15px', borderRadius: 14, background: '#0f0f14', border: '1px solid #2a2a38' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 7, marginBottom: 9 }}>
              <svg width="13" height="13" viewBox="0 0 14 14" fill="none">
                <path d="M7 1L8.3 5.2L12.5 6.5L8.3 7.8L7 12L5.7 7.8L1.5 6.5L5.7 5.2L7 1Z" fill="#a99bff" />
              </svg>
              <span style={{ fontFamily: 'var(--font-mono)', fontSize: 9, color: '#a99bff', letterSpacing: '0.12em' }}>
                GENERATED
              </span>
            </div>
            <textarea value={node.aiSummary} onChange={e => onChange({ aiSummary: e.target.value })} rows={5}
              placeholder="AI 요약이 여기에 생성됩니다"
              style={{
                width: '100%', background: 'transparent', border: '1.5px solid transparent', borderRadius: 8,
                padding: '2px 4px', margin: '0 0 0 -4px', outline: 'none', resize: 'none',
                fontSize: 12.5, lineHeight: 1.8, color: '#d3d2e0', fontFamily: 'var(--font-body)',
                boxSizing: 'border-box',
              }}
              onFocus={e => { e.target.style.borderColor = '#6b5cf6'; e.target.style.background = '#181822' }}
              onBlur={e => { e.target.style.borderColor = 'transparent'; e.target.style.background = 'transparent' }}
            />
          </div>
        </div>
      </div>
      )}
    </div>
  )
}

// ── Mission input tab ─────────────────────────────────────────────────────────
function TeamMissionInput({ team, onSave }: { team: TeamType; onSave: (mission: string) => void }) {
  const [value, setValue] = useState(team.mission)
  const [saved, setSaved] = useState(!!team.mission)
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null)
  const [graph, setGraph] = useState(buildInitialGraph)
  const [editing, setEditing] = useState(false)
  const [specOpen, setSpecOpen] = useState(false)
  const [linkFrom, setLinkFrom] = useState<string | null>(null)
  const nextIdRef = useRef(1)
  const textareaRef = useRef<HTMLTextAreaElement>(null)
  const [alertNodeId, setAlertNodeId] = useState<string | null>(null)
  const announcedRef = useRef<Set<string>>(new Set())

  // Guard against a malformed/stale graph value (e.g. hot-reload hook shuffling).
  const validGraph = !!graph && Array.isArray(graph.nodes) && Array.isArray(graph.edges)
  useEffect(() => { if (!validGraph) setGraph(buildInitialGraph()) }, [validGraph])
  const safeGraph = validGraph ? graph : { nodes: [] as RNode[], edges: [] as REdge[] }

  const selectedNode = safeGraph.nodes.find(n => n.id === selectedNodeId) ?? null
  const alertNode = safeGraph.nodes.find(n => n.id === alertNodeId) ?? null

  // Announce each problem node once with a popup so the issue isn't missed.
  useEffect(() => {
    const seen = announcedRef.current
    for (const n of safeGraph.nodes) {
      if (n.issue && n.issue.trim() && !seen.has(n.id)) {
        seen.add(n.id)
        setAlertNodeId(n.id)
        return
      }
      if (!n.issue?.trim()) seen.delete(n.id)
    }
  }, [safeGraph.nodes])

  function patchNode(id: string, patch: Partial<RNode>) {
    setGraph(g => ({ ...g, nodes: g.nodes.map(n => n.id === id ? { ...n, ...patch } : n) }))
  }

  function moveNode(id: string, x: number, y: number) {
    setGraph(g => ({ ...g, nodes: g.nodes.map(n => n.id === id ? { ...n, x, y } : n) }))
  }

  function addNode() {
    const id = `n${Date.now()}${nextIdRef.current++}`
    const maxY = Math.max(0, ...safeGraph.nodes.map(n => n.y + n.h))
    const newNode: RNode = {
      id, tier: 'leaf', x: 60, y: maxY + 46, ...SIZE.leaf,
      label: '새 작업', code: `T-${String(900 + nextIdRef.current)}`, status: 'todo', progress: 0,
      goal: '', dueDate: '2025-12-31', assignees: [], prerequisites: [], aiSummary: '', comments: [],
    }
    setGraph(g => ({ ...g, nodes: [...g.nodes, newNode] }))
    setSelectedNodeId(id)
  }

  function deleteNode(id: string) {
    setGraph(g => ({
      nodes: g.nodes.filter(n => n.id !== id),
      edges: g.edges.filter(e => e.from !== id && e.to !== id),
    }))
    setLinkFrom(f => (f === id ? null : f))
    setSelectedNodeId(p => (p === id ? null : p))
  }

  function deleteEdge(id: string) {
    setGraph(g => ({ ...g, edges: g.edges.filter(e => e.id !== id) }))
  }

  /** In edit mode a node click either starts or completes an arrow. */
  function handleNodeSelect(id: string) {
    if (editing && linkFrom) {
      if (linkFrom !== id) {
        setGraph(g => {
          const exists = g.edges.some(e => e.from === linkFrom && e.to === id)
          return exists ? g : { ...g, edges: [...g.edges, { id: `e-${linkFrom}-${id}-${Date.now()}`, from: linkFrom, to: id }] }
        })
      }
      setLinkFrom(null)
      return
    }
    setSelectedNodeId(prev => (prev === id ? null : id))
  }

  useEffect(() => {
    if (!saved) textareaRef.current?.focus()
  }, [])

  function handleSave() {
    if (!value.trim()) return
    onSave(value.trim())
    setSaved(true)
  }

  function handleEdit() {
    setSaved(false)
    setSelectedNodeId(null)
    setEditing(false)
    setLinkFrom(null)
    setTimeout(() => textareaRef.current?.focus(), 50)
  }

  if (saved) {
    return (
      <div style={{ flex: 1, display: 'flex', minHeight: 0, overflow: 'hidden' }}>

        {/* Roadmap canvas */}
        <div style={{
          flex: 1, minWidth: 0, alignSelf: 'stretch',
          display: 'flex', flexDirection: 'column',
          background: '#0f0f14', borderRadius: 0, borderTop: '1px solid #1e1e28',
          overflow: 'hidden',
          backgroundImage: 'radial-gradient(#1e1e2a 1px, transparent 1px)',
          backgroundSize: '22px 22px',
        }}>
            {/* Canvas toolbar */}
            <div style={{
              display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 16,
              padding: '16px 22px', borderBottom: '1px solid #1e1e28', background: '#0f0f14',
            }}>
              <div>
                <div style={{ fontFamily: 'var(--font-mono)', fontSize: 9, color: '#8b8a9e', letterSpacing: '0.16em', textTransform: 'uppercase', marginBottom: 5 }}>
                  Roadmap · Auto-generated
                </div>
                <h2 style={{ fontFamily: 'var(--font-display)', fontSize: 18, fontWeight: 700, color: '#ffffff', margin: 0, letterSpacing: '-0.02em' }}>
                  목표 달성 로드맵
                </h2>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 14, flexShrink: 0 }}>
                {!editing && (['done', 'active', 'todo'] as const).map(k => (
                  <span key={k} style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                    <span style={{ width: 6, height: 6, borderRadius: '50%', background: STATUS_META[k].color }} />
                    <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10, color: '#9998ad' }}>{STATUS_META[k].label}</span>
                  </span>
                ))}
                {editing && (
                  <button onClick={addNode} style={{
                    display: 'flex', alignItems: 'center', gap: 6, padding: '7px 14px', borderRadius: 9,
                    border: '1px solid #3a3a4a', background: '#191922', color: '#e5e4ef',
                    fontFamily: 'var(--font-display)', fontSize: 12, fontWeight: 600, cursor: 'pointer',
                  }}>
                    <span style={{ fontSize: 14, lineHeight: 1 }}>+</span> 노드 추가
                  </button>
                )}
                <button onClick={() => setSpecOpen(v => !v)} style={{
                  display: 'flex', alignItems: 'center', gap: 7, padding: '7px 14px', borderRadius: 9,
                  border: `1px solid ${specOpen ? '#6b5cf6' : '#3a3a4a'}`,
                  background: specOpen ? '#6b5cf620' : '#191922',
                  color: specOpen ? '#b7abff' : '#e5e4ef',
                  fontFamily: 'var(--font-display)', fontSize: 12, fontWeight: 600, cursor: 'pointer',
                  transition: 'background 0.15s, border-color 0.15s, color 0.15s',
                }}>
                  <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
                    <path d="M2.5 1.5h5L10 4v6.5H2.5V1.5Z" stroke="currentColor" strokeWidth="1.2" strokeLinejoin="round" />
                    <path d="M4.4 6h3.2M4.4 8h2.2" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" />
                  </svg>
                  명세서 확인 및 재설정
                </button>
                <button onClick={() => { setEditing(v => !v); setLinkFrom(null) }} style={{
                  padding: '7px 16px', borderRadius: 9, border: 'none',
                  background: editing ? '#10b981' : team.color, color: '#ffffff',
                  fontFamily: 'var(--font-display)', fontSize: 12, fontWeight: 600, cursor: 'pointer',
                  transition: 'background 0.15s',
                }}>
                  {editing ? '편집 완료' : '로드맵 편집'}
                </button>
              </div>
            </div>

            {editing && (
              <div style={{
                display: 'flex', alignItems: 'center', gap: 10, padding: '10px 22px',
                background: linkFrom ? '#f59e0b16' : '#16161e', borderBottom: '1px solid #1e1e28',
              }}>
                <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10, color: linkFrom ? '#f5b849' : '#9998ad', letterSpacing: '0.04em', lineHeight: 1.7 }}>
                  {linkFrom
                    ? '연결할 대상 노드를 클릭하세요 · ESC 대신 시작 노드의 화살표 버튼을 다시 눌러 취소'
                    : '노드를 드래그해 이동 · 아래 화살표 버튼으로 연결 시작 · × 로 노드 삭제 · 화살표를 클릭하면 삭제'}
                </span>
              </div>
            )}

            <div style={{ position: 'relative', flex: 1, minHeight: 0 }}>
              {specOpen && (
                <div style={{
                  position: 'absolute', top: 0, left: 0, right: 0, zIndex: 20,
                  padding: '16px 22px', borderBottom: '1px solid #23232f',
                  background: 'rgba(18,18,26,0.96)', backdropFilter: 'blur(6px)',
                  boxShadow: '0 14px 32px rgba(0,0,0,0.45)',
                  animation: 'slideDown 0.16s ease',
                }}>
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 14, marginBottom: 10 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      <span style={{ width: 7, height: 7, borderRadius: '50%', background: team.color }} />
                      <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10, color: '#b7abff', letterSpacing: '0.12em', textTransform: 'uppercase' }}>
                        Spec · 최종 목표
                      </span>
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      <button onClick={handleEdit} style={{
                        padding: '6px 14px', borderRadius: 8, border: 'none', background: team.color,
                        color: '#ffffff', fontFamily: 'var(--font-display)', fontSize: 12, fontWeight: 600, cursor: 'pointer',
                      }}>
                        재설정하기
                      </button>
                      <button onClick={() => setSpecOpen(false)} style={{
                        width: 26, height: 26, borderRadius: 7, border: '1px solid #33333f',
                        background: 'transparent', color: '#a9a8ba', cursor: 'pointer',
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                      }}>
                        <svg width="11" height="11" viewBox="0 0 12 12" fill="none">
                          <path d="M1 1L11 11M11 1L1 11" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
                        </svg>
                      </button>
                    </div>
                  </div>
                  <p style={{ margin: 0, fontSize: 14, fontWeight: 500, lineHeight: 1.75, color: '#e5e4ef' }}>
                    {value}
                  </p>
                  <div style={{ display: 'flex', gap: 20, marginTop: 12, paddingTop: 12, borderTop: '1px solid #23232f' }}>
                    {[
                      { k: '노드', v: safeGraph.nodes.length },
                      { k: '연결', v: safeGraph.edges.length },
                      { k: '완료', v: safeGraph.nodes.filter(n => n.status === 'done').length },
                    ].map(m => (
                      <div key={m.k} style={{ display: 'flex', alignItems: 'baseline', gap: 6 }}>
                        <span style={{ fontFamily: 'var(--font-display)', fontSize: 16, fontWeight: 700, color: '#ffffff' }}>{m.v}</span>
                        <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10, color: '#8b8a9e' }}>{m.k}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}
              <RoadmapCanvas
                nodes={safeGraph.nodes} edges={safeGraph.edges}
                selectedId={selectedNodeId} editing={editing} linkFrom={linkFrom}
                onSelect={handleNodeSelect} onMove={moveNode}
                onDeleteNode={deleteNode} onDeleteEdge={deleteEdge}
                onStartLink={id => setLinkFrom(f => (f === id ? null : id))}
              />
            </div>

            <div style={{
              marginTop: 'auto', flexShrink: 0,
              padding: '11px 22px', borderTop: '1px solid #1e1e28', background: '#0f0f14',
              fontFamily: 'var(--font-mono)', fontSize: 10, color: '#8b8a9e', letterSpacing: '0.04em',
            }}>
              {selectedNode
                ? `SELECTED · ${selectedNode.code} — ${selectedNode.label}`
                : `${safeGraph.nodes.length} NODES · ${safeGraph.edges.length} LINKS · 노드를 클릭하면 상세 정보가 열립니다`}
            </div>
        </div>

      {alertNode && (
        <div onClick={() => setAlertNodeId(null)} style={{
          position: 'fixed', inset: 0, zIndex: 90, display: 'flex', alignItems: 'center', justifyContent: 'center',
          background: 'rgba(15,15,20,0.5)', backdropFilter: 'blur(3px)', animation: 'fadeIn 0.14s ease',
        }}>
          <div onClick={e => e.stopPropagation()} style={{
            width: 400, maxWidth: '90vw', borderRadius: 18, background: '#ffffff', overflow: 'hidden',
            border: '1.5px solid #ef444433', boxShadow: '0 24px 60px rgba(0,0,0,0.32)',
            animation: 'slideDown 0.16s ease',
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '16px 18px', background: '#ef44440f', borderBottom: '1px solid #ef444426' }}>
              <span style={{
                width: 26, height: 26, borderRadius: '50%', background: '#ef4444', flexShrink: 0,
                display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                animation: 'issuePulse 1.6s ease-in-out infinite',
              }}>
                <svg width="13" height="13" viewBox="0 0 10 10" fill="none">
                  <path d="M5 2v3.4M5 7.4v.1" stroke="#fff" strokeWidth="1.8" strokeLinecap="round" />
                </svg>
              </span>
              <div style={{ minWidth: 0 }}>
                <div style={{ fontFamily: 'var(--font-display)', fontSize: 14.5, fontWeight: 700, color: '#b91c1c' }}>
                  문제가 발생했습니다
                </div>
                <div style={{ fontFamily: 'var(--font-mono)', fontSize: 9.5, letterSpacing: '0.1em', color: '#c2504f', marginTop: 2 }}>
                  {alertNode.code} · BLOCKED
                </div>
              </div>
            </div>
            <div style={{ padding: '16px 18px 18px' }}>
              <p style={{ margin: '0 0 8px', fontFamily: 'var(--font-display)', fontSize: 15, fontWeight: 700, color: 'var(--color-foreground)' }}>
                {alertNode.label}
              </p>
              <p style={{ margin: 0, fontSize: 13, lineHeight: 1.75, color: 'var(--color-muted-foreground)' }}>
                {alertNode.issue}
              </p>
              <div style={{ display: 'flex', gap: 8, marginTop: 18 }}>
                <button onClick={() => setAlertNodeId(null)} style={{
                  flex: 1, padding: '10px 0', borderRadius: 10, border: '1.5px solid var(--color-border)',
                  background: '#ffffff', color: 'var(--color-muted-foreground)', cursor: 'pointer',
                  fontFamily: 'var(--font-display)', fontSize: 12.5, fontWeight: 600,
                }}>
                  닫기
                </button>
                <button onClick={() => { setSelectedNodeId(alertNode.id); setAlertNodeId(null) }} style={{
                  flex: 1.4, padding: '10px 0', borderRadius: 10, border: 'none',
                  background: '#ef4444', color: '#ffffff', cursor: 'pointer',
                  fontFamily: 'var(--font-display)', fontSize: 12.5, fontWeight: 700,
                }}>
                  해당 노드 보기
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

        {/* Right: detail panel */}
        {selectedNode && (
          <NodeDetailPanel key={selectedNode.id} node={selectedNode} color={team.color}
            onChange={patch => patchNode(selectedNode.id, patch)}
            onClose={() => setSelectedNodeId(null)} />
        )}
      </div>
    )
  }

  return (
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
      <div style={{ width: '100%', maxWidth: 560 }}>

        {/* Team badge */}
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 12, marginBottom: 36 }}>
          <div style={{
            width: 56, height: 56, borderRadius: 16, background: team.color,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontFamily: 'var(--font-display)', fontWeight: 700, fontSize: 20, color: '#fff',
            boxShadow: `0 8px 28px ${team.color}40`,
          }}>
            {getInitials(team.name)}
          </div>
          <div style={{ textAlign: 'center' }}>
            <div style={{ fontFamily: 'var(--font-display)', fontSize: 18, fontWeight: 700, color: 'var(--color-foreground)', letterSpacing: '-0.02em' }}>
              {team.name}
            </div>
            <div style={{ fontFamily: 'var(--font-mono)', fontSize: 11, color: 'var(--color-muted-foreground)', marginTop: 4 }}>
              최종 목표를 한 문장으로 정의해주세요
            </div>
          </div>
        </div>

        {/* Edit state */}
        <div style={{
          background: '#ffffff', borderRadius: 20,
          border: `2px solid ${team.color}`,
          boxShadow: `0 0 0 4px ${team.color}18, 0 4px 20px ${team.color}20`,
          overflow: 'hidden',
        }}>
          <textarea
            ref={textareaRef}
            value={value}
            onChange={e => setValue(e.target.value)}
            onKeyDown={e => { if (e.key === 'Enter' && (e.metaKey || e.ctrlKey)) handleSave() }}
            placeholder="예) 모든 사용자가 직관적으로 제품을 사용할 수 있도록 일관된 디자인 언어를 구축한다."
            rows={4}
            style={{
              width: '100%', border: 'none', outline: 'none', resize: 'none',
              padding: '24px 28px', fontSize: 16, fontWeight: 500, lineHeight: 1.75,
              color: 'var(--color-foreground)', background: 'transparent',
              fontFamily: 'var(--font-body)', boxSizing: 'border-box',
            }}
          />
          <div style={{
            padding: '12px 20px', borderTop: `1px solid ${team.color}18`,
            display: 'flex', alignItems: 'center', justifyContent: 'space-between',
            background: `${team.color}06`,
          }}>
            <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10, color: value.length > 200 ? '#ef4444' : 'var(--color-muted-foreground)' }}>
              {value.length} / 200 · ⌘Enter로 저장
            </span>
            <button onClick={handleSave} disabled={!value.trim()} style={{
              padding: '8px 20px', borderRadius: 10, border: 'none',
              background: value.trim() ? team.color : '#e0dfd9',
              color: '#ffffff', fontSize: 13, fontWeight: 600,
              fontFamily: 'var(--font-display)', cursor: value.trim() ? 'pointer' : 'not-allowed',
              transition: 'background 0.15s', letterSpacing: '-0.01em',
            }}>
              저장
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

// ── Team info tab ─────────────────────────────────────────────────────────────
function TeamInfo({ team }: { team: TeamType }) {
  const [mission, setMission] = useState(team.mission)
  const [editingMission, setEditingMission] = useState(!team.mission)

  const MOCK_MEMBERS = [
    { name: 'Alex Rivera', role: 'Lead', email: 'alex@acmecorp.io' },
    { name: 'Sam Chen', role: 'Member', email: 'sam@acmecorp.io' },
    { name: 'Jordan Park', role: 'Member', email: 'jordan@acmecorp.io' },
    { name: 'Morgan Lee', role: 'Viewer', email: 'morgan@acmecorp.io' },
  ].slice(0, Math.min(team.members, 4))

  return (
    <div style={{ maxWidth: 640, margin: '0 auto' }}>

      {/* Mission card */}
      <div style={{
        background: '#ffffff', borderRadius: 16, border: `1.5px solid ${team.color}33`,
        overflow: 'hidden', marginBottom: 20, boxShadow: `0 0 0 4px ${team.color}0a`,
      }}>
        <div style={{
          padding: '16px 24px', borderBottom: `1px solid ${team.color}22`,
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          background: `${team.color}08`,
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <div style={{ width: 6, height: 6, borderRadius: '50%', background: team.color }} />
            <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10, color: team.color, textTransform: 'uppercase', letterSpacing: '0.1em', fontWeight: 500 }}>
              Mission
            </span>
          </div>
          <button onClick={() => setEditingMission(v => !v)}
            style={{
              padding: '4px 10px', borderRadius: 6, border: `1px solid ${team.color}33`,
              background: 'transparent', color: team.color, fontSize: 11, fontWeight: 600,
              fontFamily: 'var(--font-display)', cursor: 'pointer', transition: 'background 0.12s',
            }}
            onMouseEnter={e => { (e.currentTarget as HTMLElement).style.background = `${team.color}14` }}
            onMouseLeave={e => { (e.currentTarget as HTMLElement).style.background = 'transparent' }}>
            {editingMission ? '저장' : '편집'}
          </button>
        </div>
        <div style={{ padding: '24px' }}>
          {editingMission ? (
            <>
              <textarea autoFocus value={mission} onChange={e => setMission(e.target.value)}
                placeholder="팀이 궁극적으로 달성하려는 목표를 한 문장으로 정의해주세요." rows={3}
                style={{
                  width: '100%', padding: '12px 16px', borderRadius: 10,
                  border: `1.5px solid ${team.color}44`, fontSize: 15, fontWeight: 500, lineHeight: 1.7,
                  color: 'var(--color-foreground)', background: `${team.color}05`,
                  outline: 'none', fontFamily: 'var(--font-body)', resize: 'none',
                  boxShadow: `0 0 0 3px ${team.color}18`, boxSizing: 'border-box',
                }} />
              <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 6 }}>
                <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10, color: mission.length > 200 ? '#ef4444' : 'var(--color-muted-foreground)' }}>
                  {mission.length} / 200
                </span>
              </div>
            </>
          ) : (
            <p style={{ margin: 0, fontSize: 16, fontWeight: 500, lineHeight: 1.75, color: mission ? 'var(--color-foreground)' : 'var(--color-muted-foreground)', fontStyle: mission ? 'normal' : 'italic' }}>
              {mission || '아직 팀 미션이 없습니다. 편집 버튼을 눌러 추가해보세요.'}
            </p>
          )}
        </div>
      </div>

      {/* Stats */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 10, marginBottom: 20 }}>
        {[
          { label: 'Members', value: team.members },
          { label: 'Projects', value: Math.floor(team.members * 0.8) },
          { label: 'Active this week', value: Math.floor(team.members * 0.6) },
        ].map(stat => (
          <div key={stat.label} style={{ background: '#fff', borderRadius: 12, border: '1px solid var(--color-border)', padding: '16px 20px' }}>
            <div style={{ fontFamily: 'var(--font-display)', fontSize: 26, fontWeight: 700, color: 'var(--color-foreground)', letterSpacing: '-0.02em' }}>
              {stat.value}
            </div>
            <div style={{ fontFamily: 'var(--font-mono)', fontSize: 10, color: 'var(--color-muted-foreground)', marginTop: 4, textTransform: 'uppercase', letterSpacing: '0.06em' }}>
              {stat.label}
            </div>
          </div>
        ))}
      </div>

      {/* Members */}
      <div style={{ background: '#fff', borderRadius: 16, border: '1px solid var(--color-border)', overflow: 'hidden' }}>
        <div style={{ padding: '20px 24px', borderBottom: '1px solid var(--color-border)', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10, color: 'var(--color-muted-foreground)', textTransform: 'uppercase', letterSpacing: '0.08em' }}>Members</span>
          <button style={{ padding: '6px 14px', borderRadius: 8, border: '1.5px solid var(--color-border)', background: 'transparent', fontSize: 12, fontWeight: 600, color: 'var(--color-foreground)', cursor: 'pointer', fontFamily: 'var(--font-display)' }}>
            Invite
          </button>
        </div>
        {MOCK_MEMBERS.map((m, i) => (
          <div key={m.email} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '14px 24px', borderBottom: i < MOCK_MEMBERS.length - 1 ? '1px solid var(--color-border)' : 'none' }}>
            <Avatar name={m.name} size={34} />
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 13, fontWeight: 600, color: 'var(--color-foreground)' }}>{m.name}</div>
              <div style={{ fontSize: 12, color: 'var(--color-muted-foreground)', marginTop: 1 }}>{m.email}</div>
            </div>
            <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10, color: 'var(--color-muted-foreground)', background: '#f0efe9', borderRadius: 5, padding: '3px 8px', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              {m.role}
            </span>
          </div>
        ))}
        {team.members > 4 && (
          <div style={{ padding: '12px 24px', background: '#fafaf8' }}>
            <span style={{ fontSize: 12, color: 'var(--color-muted-foreground)' }}>+{team.members - 4} more members</span>
          </div>
        )}
      </div>
    </div>
  )
}

// ── Chat integration settings tab ─────────────────────────────────────────────
type Accounts = {
  slack: { connected: boolean; handle: string }
  github: { connected: boolean; handle: string }
}

const PROVIDER_MARK: Record<string, { bg: string; glyph: React.ReactNode }> = {
  slack: {
    bg: '#3f0f3f',
    glyph: <span style={{ fontFamily: 'var(--font-mono)', fontSize: 15, fontWeight: 700, color: '#fff' }}>#</span>,
  },
  github: {
    bg: '#18181f',
    glyph: (
      <svg width="17" height="17" viewBox="0 0 16 16" fill="#fff">
        <path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82a7.4 7.4 0 0 1 2-.27c.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.01 8.01 0 0 0 16 8c0-4.42-3.58-8-8-8Z" />
      </svg>
    ),
  },
}

function ProviderMark({ id, size = 38 }: { id: string; size?: number }) {
  const m = PROVIDER_MARK[id]
  return (
    <div style={{
      width: size, height: size, borderRadius: 10, background: m.bg, flexShrink: 0,
      display: 'flex', alignItems: 'center', justifyContent: 'center',
    }}>
      {m.glyph}
    </div>
  )
}

function AccountRow({ id, name, hint, required, account, color, onConnect, onDisconnect }: {
  id: 'slack' | 'github'; name: string; hint: string; required: boolean
  account: { connected: boolean; handle: string }; color: string
  onConnect: (handle: string) => void; onDisconnect: () => void
}) {
  const [editing, setEditing] = useState(false)
  const [specOpen, setSpecOpen] = useState(false)
  const [draft, setDraft] = useState('')

  const chip = (text: string, fg: string, bg: string, bd: string) => (
    <span style={{
      fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: '0.08em',
      padding: '2px 7px', borderRadius: 5, color: fg, background: bg, border: `1px solid ${bd}`,
    }}>
      {text}
    </span>
  )

  return (
    <div style={{
      border: `1.5px solid ${account.connected ? '#10b98140' : required ? '#f59e0b40' : 'var(--color-border)'}`,
      borderRadius: 14, background: '#ffffff', padding: '14px 16px',
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <ProviderMark id={id} />
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 7, marginBottom: 3 }}>
            <span style={{ fontFamily: 'var(--font-display)', fontSize: 14, fontWeight: 700, color: 'var(--color-foreground)' }}>
              {name}
            </span>
            {required
              ? chip('필수', '#b45309', '#f59e0b14', '#f59e0b33')
              : chip('선택', 'var(--color-muted-foreground)', '#f0efe9', 'var(--color-border)')}
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <span style={{ width: 6, height: 6, borderRadius: '50%', background: account.connected ? '#10b981' : '#c9c8c2', flexShrink: 0 }} />
            <span style={{
              fontFamily: 'var(--font-mono)', fontSize: 11,
              color: account.connected ? '#10b981' : 'var(--color-muted-foreground)',
              overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
            }}>
              {account.connected ? account.handle : '미연결'}
            </span>
          </div>
        </div>

        {account.connected ? (
          <button onClick={onDisconnect} style={{
            padding: '7px 14px', borderRadius: 9, border: '1.5px solid var(--color-border)',
            background: 'transparent', fontFamily: 'var(--font-display)', fontSize: 12, fontWeight: 600,
            color: 'var(--color-muted-foreground)', cursor: 'pointer', flexShrink: 0,
          }}>
            연동 해제
          </button>
        ) : (
          <button onClick={() => { setEditing(v => !v); setDraft('') }} style={{
            padding: '7px 16px', borderRadius: 9, border: 'none', background: editing ? '#0f0f14' : color,
            fontFamily: 'var(--font-display)', fontSize: 12, fontWeight: 600, color: '#ffffff',
            cursor: 'pointer', flexShrink: 0, transition: 'background 0.15s',
          }}>
            {editing ? '취소' : '연동하기'}
          </button>
        )}
      </div>

      {editing && !account.connected && (
        <div style={{ display: 'flex', gap: 8, marginTop: 13, paddingTop: 13, borderTop: '1px solid var(--color-border)' }}>
          <input
            autoFocus value={draft} onChange={e => setDraft(e.target.value)}
            onKeyDown={e => { if (e.key === 'Enter' && draft.trim()) { onConnect(draft.trim()); setEditing(false) } }}
            placeholder={hint}
            style={{
              flex: 1, padding: '9px 13px', borderRadius: 9, border: '1.5px solid var(--color-border)',
              background: '#fafaf8', fontSize: 13, fontFamily: 'var(--font-mono)',
              color: 'var(--color-foreground)', outline: 'none',
            }}
          />
          <button onClick={() => { if (draft.trim()) { onConnect(draft.trim()); setEditing(false) } }} disabled={!draft.trim()}
            style={{
              padding: '9px 18px', borderRadius: 9, border: 'none',
              background: draft.trim() ? color : '#e0dfd9', color: '#ffffff',
              fontFamily: 'var(--font-display)', fontSize: 12, fontWeight: 600,
              cursor: draft.trim() ? 'pointer' : 'not-allowed', flexShrink: 0,
            }}>
            연결
          </button>
        </div>
      )}
    </div>
  )
}

const CHAT_CHANNELS = [
  { id: 'slack', name: 'Slack', hint: 'https://hooks.slack.com/services/…' },
]

function TeamIntegrations({ team, accounts, onAccountsChange }: {
  team: TeamType; accounts: Accounts; onAccountsChange: (a: Accounts) => void
}) {
  const [channel, setChannel] = useState<string | null>('slack')
  const [webhooks, setWebhooks] = useState<Record<string, string>>({})
  const [savedAt, setSavedAt] = useState<string | null>(null)

  const MEMBER_LINKS = [
    { name: 'Alex Rivera', slack: '@alex', github: 'alex-rivera' },
    { name: 'Sam Chen', slack: '@sam', github: null },
    { name: 'Jordan Park', slack: '@jordan.park', github: 'jpark-dev' },
    { name: 'Morgan Lee', slack: null, github: null },
  ].slice(0, Math.min(team.members, 4))

  const label = (ko: string, en: string) => (
    <div style={{ display: 'flex', alignItems: 'baseline', gap: 8, marginBottom: 12 }}>
      <span style={{ fontFamily: 'var(--font-display)', fontSize: 14, fontWeight: 700, color: 'var(--color-foreground)', letterSpacing: '-0.01em' }}>
        {ko}
      </span>
      <span style={{ fontFamily: 'var(--font-mono)', fontSize: 9, color: 'var(--color-muted-foreground)', textTransform: 'uppercase', letterSpacing: '0.12em' }}>
        {en}
      </span>
    </div>
  )

  return (
    <div style={{ maxWidth: 640, margin: '0 auto', paddingBottom: 40 }}>
      <div style={{ marginBottom: 28 }}>
        <div style={{ fontFamily: 'var(--font-mono)', fontSize: 10, color: 'var(--color-muted-foreground)', letterSpacing: '0.14em', textTransform: 'uppercase', marginBottom: 6 }}>
          Integrations
        </div>
        <h1 style={{ fontFamily: 'var(--font-display)', fontSize: 26, fontWeight: 700, color: 'var(--color-foreground)', margin: 0, letterSpacing: '-0.02em' }}>
          채팅방 연동 설정
        </h1>
        <p style={{ fontSize: 13.5, color: 'var(--color-muted-foreground)', marginTop: 8, lineHeight: 1.65 }}>
          AI 요약과 프로젝트 알림을 받을 외부 채팅방을 선택하고 연결 링크를 입력하세요.
        </p>
      </div>

      {/* 내 계정 연결 */}
      <div style={{ marginBottom: 28 }}>
        {label('내 계정 연결', 'My account')}

        {!accounts.slack.connected && (
          <div style={{
            display: 'flex', alignItems: 'flex-start', gap: 9, marginBottom: 12,
            padding: '11px 14px', borderRadius: 11, background: '#f59e0b12', border: '1px solid #f59e0b33',
          }}>
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none" style={{ marginTop: 1, flexShrink: 0 }}>
              <path d="M7 1.5 13 12H1L7 1.5Z" stroke="#b45309" strokeWidth="1.3" strokeLinejoin="round" />
              <path d="M7 5.5V8.2M7 10.1v.1" stroke="#b45309" strokeWidth="1.4" strokeLinecap="round" />
            </svg>
            <span style={{ fontSize: 12.5, color: '#92400e', lineHeight: 1.6 }}>
              Slack 계정 연동은 <strong style={{ fontWeight: 600 }}>필수</strong>입니다. 연동 전에는 팀 알림과 AI 요약이 전달되지 않습니다.
            </span>
          </div>
        )}

        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          <AccountRow
            id="slack" name="Slack" required hint="@handle 또는 slack 이메일" color={team.color}
            account={accounts.slack}
            onConnect={handle => onAccountsChange({ ...accounts, slack: { connected: true, handle } })}
            onDisconnect={() => onAccountsChange({ ...accounts, slack: { connected: false, handle: '' } })}
          />
          <AccountRow
            id="github" name="GitHub" required={false} hint="github username" color={team.color}
            account={accounts.github}
            onConnect={handle => onAccountsChange({ ...accounts, github: { connected: true, handle } })}
            onDisconnect={() => onAccountsChange({ ...accounts, github: { connected: false, handle: '' } })}
          />
        </div>
      </div>

      {/* 팀 채팅방 */}
      <div style={{ marginBottom: 28 }}>
        {label('팀 채팅방', 'Channel')}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {CHAT_CHANNELS.map(ch => {
            const on = channel === ch.id
            return (
              <div key={ch.id} style={{
                border: `1.5px solid ${on ? team.color : 'var(--color-border)'}`,
                borderRadius: 14, background: '#ffffff', overflow: 'hidden',
                boxShadow: on ? `0 0 0 3px ${team.color}14` : 'none',
                transition: 'border-color 0.15s, box-shadow 0.15s',
              }}>
                <button onClick={() => setChannel(on ? null : ch.id)}
                  style={{
                    width: '100%', display: 'flex', alignItems: 'center', gap: 12,
                    padding: '14px 16px', background: 'transparent', border: 'none', cursor: 'pointer', textAlign: 'left',
                  }}>
                  <ProviderMark id={ch.id} />
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontFamily: 'var(--font-display)', fontSize: 14, fontWeight: 700, color: 'var(--color-foreground)', marginBottom: 3 }}>
                      {ch.name}
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                      <span style={{ width: 6, height: 6, borderRadius: '50%', background: on && webhooks[ch.id] ? '#10b981' : '#c9c8c2' }} />
                      <span style={{ fontFamily: 'var(--font-mono)', fontSize: 11, color: on && webhooks[ch.id] ? '#10b981' : 'var(--color-muted-foreground)' }}>
                        {on && webhooks[ch.id] ? '연결됨' : '미연결'}
                      </span>
                    </div>
                  </div>
                  <span style={{
                    width: 18, height: 18, borderRadius: '50%', flexShrink: 0,
                    border: `2px solid ${on ? team.color : '#d6d5cf'}`,
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                  }}>
                    {on && <span style={{ width: 8, height: 8, borderRadius: '50%', background: team.color }} />}
                  </span>
                </button>
                {on && (
                  <div style={{ padding: '0 16px 14px' }}>
                    <input
                      value={webhooks[ch.id] || ''}
                      onChange={e => setWebhooks(prev => ({ ...prev, [ch.id]: e.target.value }))}
                      placeholder={ch.hint}
                      style={{
                        width: '100%', padding: '9px 13px', borderRadius: 9,
                        border: '1.5px solid var(--color-border)', background: '#fafaf8',
                        fontSize: 12, fontFamily: 'var(--font-mono)', color: 'var(--color-foreground)',
                        outline: 'none', boxSizing: 'border-box',
                      }}
                    />
                  </div>
                )}
              </div>
            )
          })}
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 14, marginTop: 16 }}>
          <button onClick={() => setSavedAt(new Date().toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' }))}
            disabled={!accounts.slack.connected}
            style={{
              padding: '10px 22px', borderRadius: 10, border: 'none',
              background: accounts.slack.connected ? team.color : '#d1d0cc', color: '#ffffff',
              fontFamily: 'var(--font-display)', fontSize: 13, fontWeight: 600,
              cursor: accounts.slack.connected ? 'pointer' : 'not-allowed', letterSpacing: '-0.01em',
            }}>
            설정 저장
          </button>
          <span style={{ fontFamily: 'var(--font-mono)', fontSize: 11, color: savedAt ? '#10b981' : 'var(--color-muted-foreground)' }}>
            {savedAt ? `✓ ${savedAt}에 저장됨` : accounts.slack.connected ? '변경 사항 없음' : 'Slack 계정 연동 후 저장할 수 있어요'}
          </span>
        </div>
      </div>

      {/* Members */}
      <div>
        {label('멤버 연동 현황', 'Members')}
        <div style={{ background: '#ffffff', borderRadius: 14, border: '1px solid var(--color-border)', overflow: 'hidden' }}>
          {MEMBER_LINKS.map((m, i) => (
            <div key={m.name} style={{
              display: 'flex', alignItems: 'center', gap: 12, padding: '13px 16px',
              borderBottom: i < MEMBER_LINKS.length - 1 ? '1px solid var(--color-border)' : 'none',
            }}>
              <Avatar name={m.name} size={30} />
              <span style={{ flex: 1, fontSize: 13, fontWeight: 500, color: 'var(--color-foreground)' }}>{m.name}</span>
              {(['slack', 'github'] as const).map(p => (
                <span key={p} style={{
                  display: 'inline-flex', alignItems: 'center', gap: 5, padding: '3px 9px', borderRadius: 100,
                  background: m[p] ? '#10b98112' : '#f0efe9',
                  border: `1px solid ${m[p] ? '#10b98130' : 'var(--color-border)'}`,
                  fontFamily: 'var(--font-mono)', fontSize: 10,
                  color: m[p] ? '#0f8f6c' : 'var(--color-muted-foreground)',
                }}>
                  <span style={{ width: 5, height: 5, borderRadius: '50%', background: m[p] ? '#10b981' : '#c9c8c2' }} />
                  {m[p] || (p === 'slack' ? 'slack 미연결' : 'github 미연결')}
                </span>
              ))}
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

// ── Team view (tabs) ──────────────────────────────────────────────────────────
function TeamView({ team, tab, onMissionSave, accounts, onAccountsChange }: {
  team: TeamType; tab: TabId; onMissionSave: (id: string, mission: string) => void
  accounts: Accounts; onAccountsChange: (a: Accounts) => void
}) {
  return (
    <div style={{ flex: 1, minHeight: 0, overflowY: tab === 'mission' ? 'hidden' : 'auto', display: 'flex', flexDirection: 'column' }}>
      {tab === 'mission' && <TeamMissionInput team={team} onSave={m => onMissionSave(team.id, m)} />}
      {tab === 'info' && <TeamInfo team={team} />}
      {tab === 'integrations' && <TeamIntegrations team={team} accounts={accounts} onAccountsChange={onAccountsChange} />}
    </div>
  )
}

// ── Create team form ──────────────────────────────────────────────────────────
function CreateTeamForm({ teams, onCreated, accounts, onAccountsChange }: {
  teams: TeamType[]; onCreated: (team: TeamType) => void
  accounts: Accounts; onAccountsChange: (a: Accounts) => void
}) {
  const [slackHandle, setSlackHandle] = useState(accounts.slack.handle)
  const [teamName, setTeamName] = useState('')
  const [teamDesc, setTeamDesc] = useState('')
  const [selectedColor, setSelectedColor] = useState(COLOR_SWATCHES[0])
  const [inviteInput, setInviteInput] = useState('')
  const [invitees, setInvitees] = useState<string[]>([])
  const [creating, setCreating] = useState(false)
  const [success, setSuccess] = useState(false)

  function addInvitee() {
    const email = inviteInput.trim()
    if (email && !invitees.includes(email)) setInvitees(prev => [...prev, email])
    setInviteInput('')
  }

  function handleCreate(e: React.FormEvent) {
    e.preventDefault()
    if (!teamName.trim() || !slackHandle.trim()) return
    onAccountsChange({ ...accounts, slack: { connected: true, handle: slackHandle.trim() } })
    setCreating(true)
    setTimeout(() => {
      const newId = `T-${String(teams.length + 1).padStart(3, '0')}`
      const next: TeamType = { id: newId, name: teamName.trim(), members: 1 + invitees.length, color: selectedColor, mission: '' }
      setSuccess(true)
      setCreating(false)
      setTimeout(() => { setSuccess(false); onCreated(next) }, 800)
    }, 600)
  }

  const inputStyle: React.CSSProperties = {
    width: '100%', padding: '10px 14px', borderRadius: 10,
    border: '1.5px solid var(--color-border)', fontSize: 14,
    color: 'var(--color-foreground)', background: '#fafaf8',
    outline: 'none', fontFamily: 'var(--font-body)',
    transition: 'border-color 0.15s, box-shadow 0.15s',
  }
  const onFocus = (e: React.FocusEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    e.target.style.borderColor = 'var(--color-primary)'
    e.target.style.boxShadow = '0 0 0 3px rgba(107,92,246,0.1)'
    e.target.style.background = '#ffffff'
  }
  const onBlur = (e: React.FocusEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    e.target.style.borderColor = 'var(--color-border)'
    e.target.style.boxShadow = 'none'
  }

  return (
    <div style={{ maxWidth: 640, margin: '0 auto' }}>
      <div style={{ marginBottom: 36 }}>
        <h1 style={{ fontFamily: 'var(--font-display)', fontSize: 30, fontWeight: 700, color: 'var(--color-foreground)', margin: 0, letterSpacing: '-0.02em', lineHeight: 1.1 }}>
          New team
        </h1>
        <p style={{ fontSize: 14, color: 'var(--color-muted-foreground)', marginTop: 8, lineHeight: 1.6 }}>
          팀을 만들면 첫 화면에서 팀 미션을 바로 입력할 수 있어요.
        </p>
      </div>

      <div style={{ background: '#ffffff', borderRadius: 16, border: '1px solid var(--color-border)', overflow: 'hidden' }}>
        <form onSubmit={handleCreate}>
          <div style={{ padding: '28px 32px', borderBottom: '1px solid var(--color-border)' }}>
            <div style={{ fontFamily: 'var(--font-mono)', fontSize: 10, color: 'var(--color-muted-foreground)', letterSpacing: '0.08em', textTransform: 'uppercase', marginBottom: 18 }}>
              Identity
            </div>
            <div style={{ display: 'flex', gap: 14, alignItems: 'flex-end', marginBottom: 20 }}>
              <div>
                <div style={{ fontSize: 12, fontWeight: 500, color: 'var(--color-foreground)', marginBottom: 8 }}>Color</div>
                <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', maxWidth: 140 }}>
                  {COLOR_SWATCHES.map(c => (
                    <button key={c} type="button" onClick={() => setSelectedColor(c)} style={{
                      width: 28, height: 28, borderRadius: 8, background: c,
                      border: selectedColor === c ? `2px solid ${c}` : '2px solid transparent',
                      outline: selectedColor === c ? '2px solid #ffffff' : '2px solid transparent',
                      outlineOffset: 1, cursor: 'pointer',
                      boxShadow: selectedColor === c ? `0 0 0 3px ${c}44` : 'none',
                      transition: 'box-shadow 0.1s',
                    }} />
                  ))}
                </div>
              </div>
              <div style={{ width: 48, height: 48, borderRadius: 12, background: selectedColor, display: 'flex', alignItems: 'center', justifyContent: 'center', fontFamily: 'var(--font-display)', fontWeight: 700, fontSize: 18, color: '#fff', flexShrink: 0, transition: 'background 0.2s', marginBottom: 2 }}>
                {teamName ? getInitials(teamName) : '?'}
              </div>
            </div>
            <div style={{ marginBottom: 16 }}>
              <label style={{ fontSize: 12, fontWeight: 500, color: 'var(--color-foreground)', display: 'block', marginBottom: 8 }}>
                Team name <span style={{ color: '#ef4444' }}>*</span>
              </label>
              <input type="text" value={teamName} onChange={e => setTeamName(e.target.value)}
                placeholder="e.g. Platform Engineering" required style={inputStyle} onFocus={onFocus} onBlur={onBlur} />
            </div>
            <div>
              <label style={{ fontSize: 12, fontWeight: 500, color: 'var(--color-foreground)', display: 'block', marginBottom: 8 }}>
                Description <span style={{ fontWeight: 400, color: 'var(--color-muted-foreground)' }}>(optional)</span>
              </label>
              <textarea value={teamDesc} onChange={e => setTeamDesc(e.target.value)}
                placeholder="What does this team work on?" rows={3}
                style={{ ...inputStyle, resize: 'none', lineHeight: 1.6 } as React.CSSProperties}
                onFocus={onFocus} onBlur={onBlur} />
            </div>
          </div>

          <div style={{ padding: '24px 32px', borderBottom: '1px solid var(--color-border)' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 14 }}>
              <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10, color: 'var(--color-muted-foreground)', letterSpacing: '0.08em', textTransform: 'uppercase' }}>
                Slack account
              </span>
              <span style={{ fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: '0.08em', padding: '2px 7px', borderRadius: 5, color: '#b45309', background: '#f59e0b14', border: '1px solid #f59e0b33' }}>
                필수
              </span>
            </div>
            <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
              <ProviderMark id="slack" />
              <input type="text" value={slackHandle} onChange={e => setSlackHandle(e.target.value)}
                placeholder="@handle 또는 slack 이메일" required
                style={{ ...inputStyle, fontFamily: 'var(--font-mono)', fontSize: 13 } as React.CSSProperties}
                onFocus={onFocus} onBlur={onBlur} />
            </div>
            <p style={{ fontSize: 12, color: 'var(--color-muted-foreground)', margin: '10px 0 0', lineHeight: 1.55 }}>
              팀 알림과 AI 요약을 받으려면 Slack 계정 연동이 필요합니다. 팀 생성 시 자동으로 연동됩니다.
            </p>
          </div>

          <div style={{ padding: '24px 32px', borderBottom: '1px solid var(--color-border)' }}>
            <div style={{ fontFamily: 'var(--font-mono)', fontSize: 10, color: 'var(--color-muted-foreground)', letterSpacing: '0.08em', textTransform: 'uppercase', marginBottom: 14 }}>
              Invite members
            </div>
            <div style={{ display: 'flex', gap: 8, marginBottom: 12 }}>
              <input type="email" value={inviteInput} onChange={e => setInviteInput(e.target.value)}
                onKeyDown={e => { if (e.key === 'Enter' || e.key === ',') { e.preventDefault(); addInvitee() } }}
                placeholder="name@company.com" style={inputStyle} onFocus={onFocus} onBlur={onBlur} />
              <button type="button" onClick={addInvitee} disabled={!inviteInput.trim()}
                style={{
                  padding: '10px 16px', borderRadius: 10, border: '1.5px solid var(--color-border)',
                  background: inviteInput.trim() ? 'var(--color-primary)' : '#f0efe9',
                  color: inviteInput.trim() ? '#ffffff' : 'var(--color-muted-foreground)',
                  fontSize: 13, fontWeight: 600, fontFamily: 'var(--font-display)',
                  cursor: inviteInput.trim() ? 'pointer' : 'not-allowed',
                  transition: 'background 0.15s, color 0.15s', whiteSpace: 'nowrap',
                }}>
                Add
              </button>
            </div>
            <p style={{ fontSize: 12, color: 'var(--color-muted-foreground)', margin: '0 0 14px', lineHeight: 1.5 }}>
              이메일 입력 후 <kbd style={{ fontFamily: 'var(--font-mono)', fontSize: 11, background: '#f0efe9', border: '1px solid var(--color-border)', borderRadius: 4, padding: '1px 5px' }}>Enter</kbd> 또는 Add를 클릭하세요.
            </p>
            {invitees.length > 0 && (
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                {invitees.map(email => (
                  <div key={email} style={{ display: 'flex', alignItems: 'center', gap: 6, padding: '5px 10px 5px 8px', borderRadius: 100, background: 'rgba(107,92,246,0.08)', border: '1px solid rgba(107,92,246,0.2)' }}>
                    <div style={{ width: 20, height: 20, borderRadius: '50%', background: 'var(--color-primary)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 9, fontWeight: 700, color: '#fff', fontFamily: 'var(--font-display)' }}>
                      {email[0].toUpperCase()}
                    </div>
                    <span style={{ fontSize: 12, color: 'var(--color-foreground)', fontWeight: 500 }}>{email}</span>
                    <button type="button" onClick={() => setInvitees(prev => prev.filter(e => e !== email))}
                      style={{ background: 'none', border: 'none', cursor: 'pointer', padding: 0, display: 'flex', alignItems: 'center', opacity: 0.4, transition: 'opacity 0.1s' }}
                      onMouseEnter={e => { (e.currentTarget as HTMLElement).style.opacity = '1' }}
                      onMouseLeave={e => { (e.currentTarget as HTMLElement).style.opacity = '0.4' }}>
                      <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
                        <path d="M2 2L10 10M10 2L2 10" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
                      </svg>
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div style={{ padding: '20px 32px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', background: '#fafaf8' }}>
            <div style={{ fontFamily: 'var(--font-mono)', fontSize: 11, color: 'var(--color-muted-foreground)' }}>
              {success
                ? <span style={{ color: '#10b981', fontWeight: 500 }}>✓ 팀이 생성됐습니다</span>
                : <span>생성 후 미션을 바로 입력할 수 있어요</span>}
            </div>
            <button type="submit" disabled={!teamName.trim() || !slackHandle.trim() || creating}
              style={{
                padding: '10px 24px', borderRadius: 10, border: 'none',
                background: !teamName.trim() || !slackHandle.trim() || creating ? '#d1d0cc' : 'var(--color-primary)',
                color: '#ffffff', fontSize: 14, fontWeight: 600, fontFamily: 'var(--font-display)',
                cursor: !teamName.trim() || !slackHandle.trim() || creating ? 'not-allowed' : 'pointer',
                display: 'flex', alignItems: 'center', gap: 8, letterSpacing: '-0.01em',
                transition: 'background 0.15s',
              }}
              onMouseEnter={e => { if (teamName.trim() && slackHandle.trim() && !creating) (e.currentTarget as HTMLElement).style.background = '#5a4de0' }}
              onMouseLeave={e => { if (teamName.trim() && slackHandle.trim() && !creating) (e.currentTarget as HTMLElement).style.background = 'var(--color-primary)' }}>
              {creating ? (
                <>
                  <svg width="14" height="14" viewBox="0 0 14 14" style={{ animation: 'spin 0.8s linear infinite' }}>
                    <circle cx="7" cy="7" r="5" stroke="rgba(255,255,255,0.3)" strokeWidth="2" fill="none" />
                    <path d="M7 2A5 5 0 0 1 12 7" stroke="white" strokeWidth="2" strokeLinecap="round" fill="none" />
                  </svg>
                  Creating…
                </>
              ) : 'Create team'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

// ── Personal settings ─────────────────────────────────────────────────────────
type Profile = { region: string; language: string; company: string }

const REGIONS = ['대한민국 · 서울', '대한민국 · 부산', '일본 · 도쿄', '미국 · 샌프란시스코', '독일 · 베를린', '싱가포르']
const LANGUAGES = ['한국어', 'English', '日本語', '中文(简体)']

function PersonalSettings({ profile, onChange, onClose }: {
  profile: Profile; onChange: (p: Profile) => void; onClose: () => void
}) {
  const [draft, setDraft] = useState<Profile>(profile)

  const label = (ko: string, en: string) => (
    <div style={{ display: 'flex', alignItems: 'baseline', gap: 8, marginBottom: 8 }}>
      <span style={{ fontSize: 12.5, fontWeight: 600, color: 'var(--color-foreground)' }}>{ko}</span>
      <span style={{ fontFamily: 'var(--font-mono)', fontSize: 9, color: 'var(--color-muted-foreground)', textTransform: 'uppercase', letterSpacing: '0.1em' }}>{en}</span>
    </div>
  )
  const control: React.CSSProperties = {
    width: '100%', padding: '10px 13px', borderRadius: 10,
    border: '1.5px solid var(--color-border)', background: '#fafaf8',
    fontSize: 13.5, color: 'var(--color-foreground)', fontFamily: 'var(--font-body)',
    outline: 'none', boxSizing: 'border-box', appearance: 'none',
  }

  return (
    <div onClick={onClose} style={{
      position: 'fixed', inset: 0, background: 'rgba(15,15,20,0.5)', zIndex: 100,
      display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 24,
      animation: 'fadeIn 0.14s ease',
    }}>
      <div onClick={e => e.stopPropagation()} style={{
        width: '100%', maxWidth: 420, background: '#ffffff', borderRadius: 20,
        border: '1px solid var(--color-border)', overflow: 'hidden',
        boxShadow: '0 24px 64px rgba(0,0,0,0.28)', animation: 'slideDown 0.18s ease',
      }}>
        <div style={{
          padding: '18px 22px', background: '#0f0f14',
          display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12,
        }}>
          <div>
            <div style={{ fontFamily: 'var(--font-mono)', fontSize: 9, color: '#8b8a9e', letterSpacing: '0.14em', textTransform: 'uppercase', marginBottom: 5 }}>
              Personal settings
            </div>
            <div style={{ fontFamily: 'var(--font-display)', fontSize: 18, fontWeight: 700, color: '#ffffff', letterSpacing: '-0.02em' }}>
              개인 설정
            </div>
          </div>
          <button onClick={onClose} style={{
            width: 28, height: 28, borderRadius: 8, border: '1px solid #33333f', background: 'transparent',
            color: '#a9a8ba', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
          }}>
            <svg width="11" height="11" viewBox="0 0 12 12" fill="none">
              <path d="M1 1L11 11M11 1L1 11" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
            </svg>
          </button>
        </div>

        <div style={{ padding: '22px', display: 'flex', flexDirection: 'column', gap: 18 }}>
          <div>
            {label('거주 지역', 'Region')}
            <select value={draft.region} onChange={e => setDraft({ ...draft, region: e.target.value })} style={control}>
              {REGIONS.map(r => <option key={r} value={r}>{r}</option>)}
            </select>
          </div>
          <div>
            {label('선호 언어', 'Language')}
            <select value={draft.language} onChange={e => setDraft({ ...draft, language: e.target.value })} style={control}>
              {LANGUAGES.map(l => <option key={l} value={l}>{l}</option>)}
            </select>
          </div>
          <div>
            {label('현재 회사', 'Company')}
            <input value={draft.company} onChange={e => setDraft({ ...draft, company: e.target.value })}
              placeholder="회사명을 입력하세요" style={control} />
          </div>
        </div>

        <div style={{
          padding: '14px 22px', background: '#fafaf8', borderTop: '1px solid var(--color-border)',
          display: 'flex', justifyContent: 'flex-end', gap: 8,
        }}>
          <button onClick={onClose} style={{
            padding: '9px 16px', borderRadius: 10, border: '1.5px solid var(--color-border)',
            background: 'transparent', color: 'var(--color-muted-foreground)',
            fontFamily: 'var(--font-display)', fontSize: 13, fontWeight: 600, cursor: 'pointer',
          }}>
            취소
          </button>
          <button onClick={() => { onChange(draft); onClose() }} style={{
            padding: '9px 20px', borderRadius: 10, border: 'none', background: 'var(--color-primary)',
            color: '#ffffff', fontFamily: 'var(--font-display)', fontSize: 13, fontWeight: 600, cursor: 'pointer',
          }}>
            저장
          </button>
        </div>
      </div>
    </div>
  )
}

// ── App ───────────────────────────────────────────────────────────────────────
export default function App() {
  const { t } = useTranslation()
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const [teams, setTeams] = useState<TeamType[]>(INITIAL_TEAMS)
  const [view, setView] = useState<View>({ kind: 'team', id: INITIAL_TEAMS[INITIAL_TEAMS.length - 1].id })
  const [tab, setTab] = useState<TabId>('mission')
  const [accounts, setAccounts] = useState<Accounts>({
    slack: { connected: false, handle: '' },
    github: { connected: false, handle: '' },
  })
  const [profileOpen, setProfileOpen] = useState(false)
  const [settingsOpen, setSettingsOpen] = useState(false)
  const [profile, setProfile] = useState({ region: '대한민국 · 서울', language: '한국어', company: 'Acme Corp' })
  const [sidebarOpen, setSidebarOpen] = useState(true)

  function handleCreated(team: TeamType) {
    setTeams(prev => [...prev, team])
    setView({ kind: 'team', id: team.id })
    setTab('mission')
  }

  function handleMissionSave(id: string, mission: string) {
    setTeams(prev => prev.map(t => t.id === id ? { ...t, mission } : t))
  }

  const activeTeamId = view.kind === 'team' ? view.id : null
  const topbarTitle = view.kind === 'create'
    ? t('header.newTeam')
    : teams.find(t => t.id === activeTeamId)?.name ?? ''

  if (!isAuthenticated) {
    return <AuthScreen onSuccess={() => setIsAuthenticated(true)} />
  }

  return (
    <div style={{ display: 'flex', height: '100vh', overflow: 'hidden', fontFamily: 'var(--font-body)' }}>

      {/* ── Sidebar ── */}
      <aside style={{
        width: sidebarOpen ? 260 : 0, minWidth: sidebarOpen ? 260 : 0,
        background: 'var(--color-sidebar)', display: 'flex', flexDirection: 'column',
        borderRight: sidebarOpen ? '1px solid var(--color-border-sidebar)' : 'none',
        overflow: 'hidden', transition: 'width 0.22s cubic-bezier(0.4,0,0.2,1), min-width 0.22s cubic-bezier(0.4,0,0.2,1)',
      }}>
        <div style={{ padding: '24px 20px 20px', borderBottom: '1px solid var(--color-border-sidebar)', flexShrink: 0 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <div style={{ width: 30, height: 30, background: 'var(--color-primary)', borderRadius: 8, flexShrink: 0, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                <path d="M8 2L14 5.5V10.5L8 14L2 10.5V5.5L8 2Z" stroke="white" strokeWidth="1.5" fill="none" />
                <circle cx="8" cy="8" r="2" fill="white" />
              </svg>
            </div>
            <span style={{ fontFamily: 'var(--font-display)', fontWeight: 700, fontSize: 16, color: '#ffffff', letterSpacing: '-0.01em', whiteSpace: 'nowrap' }}>
              Nexus
            </span>
          </div>
        </div>

        <div style={{ padding: '20px 20px 10px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexShrink: 0 }}>
          <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10, color: 'var(--color-sidebar-muted)', letterSpacing: '0.1em', textTransform: 'uppercase', whiteSpace: 'nowrap' }}>
            Teams
          </span>
          <button onClick={() => setView({ kind: 'create' })} title="Create new team"
            style={{
              width: 22, height: 22, borderRadius: 6,
              background: view.kind === 'create' ? 'var(--color-primary)' : 'transparent',
              border: '1px solid', borderColor: view.kind === 'create' ? 'var(--color-primary)' : 'var(--color-border-sidebar)',
              color: view.kind === 'create' ? '#ffffff' : 'var(--color-sidebar-muted)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              cursor: 'pointer', fontSize: 16, fontWeight: 400,
              transition: 'background 0.15s, border-color 0.15s, color 0.15s', padding: 0,
            }}
            onMouseEnter={e => { if (view.kind !== 'create') { (e.currentTarget as HTMLElement).style.background = 'var(--color-sidebar-hover)'; (e.currentTarget as HTMLElement).style.color = '#ffffff' } }}
            onMouseLeave={e => { if (view.kind !== 'create') { (e.currentTarget as HTMLElement).style.background = 'transparent'; (e.currentTarget as HTMLElement).style.color = 'var(--color-sidebar-muted)' } }}>
            +
          </button>
        </div>

        <nav style={{ flex: 1, overflowY: 'auto', padding: '0 10px' }}>
          {teams.map(team => {
            const isActive = view.kind === 'team' && view.id === team.id
            return (
              <button key={team.id} onClick={() => setView({ kind: 'team', id: team.id })}
                style={{
                  width: '100%', display: 'flex', alignItems: 'center', gap: 10,
                  padding: '9px 10px', borderRadius: 8, border: 'none',
                  background: isActive ? 'var(--color-sidebar-active)' : 'transparent',
                  cursor: 'pointer', textAlign: 'left', transition: 'background 0.12s', marginBottom: 2, whiteSpace: 'nowrap',
                }}
                onMouseEnter={e => { if (!isActive) (e.currentTarget as HTMLElement).style.background = 'var(--color-sidebar-hover)' }}
                onMouseLeave={e => { if (!isActive) (e.currentTarget as HTMLElement).style.background = 'transparent' }}>
                <TeamDot color={team.color} />
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 13, fontWeight: 500, color: isActive ? '#ffffff' : '#c8c7c1', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    {team.name}
                  </div>
                  <div style={{ fontFamily: 'var(--font-mono)', fontSize: 10, color: 'var(--color-sidebar-muted)', marginTop: 1 }}>
                    {team.members} members
                  </div>
                </div>
                {isActive && <div style={{ width: 3, height: 3, borderRadius: '50%', background: 'var(--color-primary)', flexShrink: 0 }} />}
              </button>
            )
          })}
        </nav>

        <div style={{ padding: '14px 20px', borderTop: '1px solid var(--color-border-sidebar)', flexShrink: 0 }}>
          <div style={{ fontFamily: 'var(--font-mono)', fontSize: 10, color: 'var(--color-sidebar-muted)', marginBottom: 6, letterSpacing: '0.06em', textTransform: 'uppercase' }}>Workspace</div>
          <div style={{ fontSize: 12, color: '#9998a4', fontWeight: 500, whiteSpace: 'nowrap' }}>Acme Corp</div>
          <div style={{ fontFamily: 'var(--font-mono)', fontSize: 10, color: 'var(--color-sidebar-muted)', marginTop: 2 }}>
            {teams.reduce((s, t) => s + t.members, 0)} total members
          </div>
        </div>
      </aside>

      {/* ── Main ── */}
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden', minWidth: 0 }}>
        <header style={{
          height: 60, background: '#ffffff', borderBottom: '1px solid var(--color-border)',
          display: 'grid', gridTemplateColumns: '1fr auto 1fr', alignItems: 'center',
          padding: '0 24px 0 16px', flexShrink: 0,
        }}>
          {/* Left: toggle + title */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <button onClick={() => setSidebarOpen(o => !o)} title={sidebarOpen ? t('header.collapseSidebar') : t('header.expandSidebar')}
              style={{
                width: 30, height: 30, borderRadius: 8, border: '1px solid var(--color-border)',
                background: 'transparent', display: 'flex', alignItems: 'center', justifyContent: 'center',
                cursor: 'pointer', color: 'var(--color-muted-foreground)',
                transition: 'background 0.12s, color 0.12s', flexShrink: 0,
              }}
              onMouseEnter={e => { (e.currentTarget as HTMLElement).style.background = '#f0efe9'; (e.currentTarget as HTMLElement).style.color = 'var(--color-foreground)' }}
              onMouseLeave={e => { (e.currentTarget as HTMLElement).style.background = 'transparent'; (e.currentTarget as HTMLElement).style.color = 'var(--color-muted-foreground)' }}>
              <svg width="14" height="14" viewBox="0 0 14 14" fill="none" style={{ transition: 'transform 0.22s', transform: sidebarOpen ? 'none' : 'rotate(180deg)' }}>
                <path d="M9 3L5 7L9 11" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            </button>
            <span style={{ fontFamily: 'var(--font-display)', fontWeight: 600, fontSize: 15, color: 'var(--color-foreground)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
              {topbarTitle}
            </span>
          </div>

          {/* Center: tabs (only when viewing a team) */}
          {view.kind === 'team' ? (
            <div style={{ display: 'flex', alignItems: 'center', gap: 2, background: '#f0efe9', borderRadius: 10, padding: '3px' }}>
              {([{ id: 'mission', label: t('tabs.mission') }, { id: 'info', label: t('tabs.info') }, { id: 'integrations', label: t('tabs.integrations') }] as const).map(t2 => {
                const activeTeam = teams.find(tm => tm.id === view.id)
                const isActive = tab === t2.id
                return (
                  <button key={t2.id} onClick={() => setTab(t2.id)}
                    style={{
                      padding: '6px 14px', borderRadius: 7, border: 'none',
                      background: isActive ? '#ffffff' : 'transparent',
                      color: isActive ? 'var(--color-foreground)' : 'var(--color-muted-foreground)',
                      fontSize: 13, fontWeight: isActive ? 600 : 500,
                      fontFamily: 'var(--font-display)', cursor: 'pointer',
                      boxShadow: isActive ? '0 1px 4px rgba(0,0,0,0.08)' : 'none',
                      transition: 'background 0.12s, color 0.12s, box-shadow 0.12s',
                      whiteSpace: 'nowrap',
                    }}>
                    {t2.id === 'integrations' ? (
                      <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                        {t2.label}
                        {!accounts.slack.connected && (
                          <span style={{ width: 6, height: 6, borderRadius: '50%', background: '#f59e0b', display: 'inline-block', flexShrink: 0 }} />
                        )}
                      </span>
                    ) : t2.id === 'info' && activeTeam ? (
                      <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                        <span style={{ width: 6, height: 6, borderRadius: '50%', background: activeTeam.color, display: 'inline-block', flexShrink: 0 }} />
                        {t2.label}
                      </span>
                    ) : t2.label}
                  </button>
                )
              })}
            </div>
          ) : <div />}

          <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
          <div style={{ position: 'relative', flexShrink: 0 }}>
            <button onClick={() => setProfileOpen(o => !o)}
              style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '6px 12px 6px 6px', borderRadius: 100, border: '1px solid var(--color-border)', background: 'transparent', cursor: 'pointer', transition: 'background 0.12s' }}
              onMouseEnter={e => { (e.currentTarget as HTMLElement).style.background = '#f5f4f0' }}
              onMouseLeave={e => { (e.currentTarget as HTMLElement).style.background = 'transparent' }}>
              <Avatar name="Jordan Kim" size={28} />
              <div style={{ textAlign: 'left' }}>
                <div style={{ fontSize: 13, fontWeight: 600, color: 'var(--color-foreground)', lineHeight: 1.2 }}>Jordan Kim</div>
                <div style={{ fontFamily: 'var(--font-mono)', fontSize: 10, color: 'var(--color-muted-foreground)' }}>Admin</div>
              </div>
              <svg width="14" height="14" viewBox="0 0 14 14" fill="none" style={{ marginLeft: 2, opacity: 0.4, transform: profileOpen ? 'rotate(180deg)' : 'none', transition: 'transform 0.15s' }}>
                <path d="M3 5L7 9L11 5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
              </svg>
            </button>
            {profileOpen && (
              <div style={{ position: 'absolute', right: 0, top: 'calc(100% + 8px)', background: '#fff', border: '1px solid var(--color-border)', borderRadius: 12, boxShadow: '0 8px 24px rgba(0,0,0,0.08)', width: 200, overflow: 'hidden', zIndex: 50 }}>
                <div style={{ padding: '14px 16px', borderBottom: '1px solid var(--color-border)' }}>
                  <div style={{ fontSize: 13, fontWeight: 600, color: 'var(--color-foreground)' }}>Jordan Kim</div>
                  <div style={{ fontSize: 12, color: 'var(--color-muted-foreground)', marginTop: 2 }}>jordan@acmecorp.io</div>
                </div>
                {([
                  { label: t('userMenu.personalSettings'), danger: false, onSelect: () => setSettingsOpen(true) },
                  { label: t('userMenu.logout'), danger: true, onSelect: () => {} },
                ]).map(item => (
                  <button key={item.label} onClick={() => { setProfileOpen(false); item.onSelect() }}
                    style={{ width: '100%', textAlign: 'left', padding: '10px 16px', fontSize: 13, fontWeight: 500, color: item.danger ? '#ef4444' : 'var(--color-foreground)', background: 'transparent', border: 'none', borderTop: item.danger ? '1px solid var(--color-border)' : 'none', cursor: 'pointer', transition: 'background 0.1s' }}
                    onMouseEnter={e => { (e.currentTarget as HTMLElement).style.background = '#f5f4f0' }}
                    onMouseLeave={e => { (e.currentTarget as HTMLElement).style.background = 'transparent' }}>
                    {item.label}
                  </button>
                ))}
              </div>
            )}
          </div>
          </div>
        </header>

        <main
          style={{ flex: 1, overflow: 'hidden', padding: view.kind === 'team' && tab === 'mission' ? '0' : '22px 20px 18px', background: 'var(--color-background)', display: 'flex', flexDirection: 'column' }}
          onClick={() => profileOpen && setProfileOpen(false)}
        >
          {view.kind === 'create' ? (
            <div style={{ flex: 1, overflowY: 'auto' }}>
              <CreateTeamForm teams={teams} onCreated={handleCreated} accounts={accounts} onAccountsChange={setAccounts} />
            </div>
          ) : (
            (() => {
              const team = teams.find(t => t.id === view.id)
              return team ? (
                <TeamView key={team.id} team={team} tab={tab} onMissionSave={handleMissionSave}
                  accounts={accounts} onAccountsChange={setAccounts} />
              ) : null
            })()
          )}
        </main>
      </div>

      {settingsOpen && (
        <PersonalSettings profile={profile} onChange={setProfile} onClose={() => setSettingsOpen(false)} />
      )}

      <style>{`
        @keyframes spin { to { transform: rotate(360deg); } }
        @keyframes bounce {
          0%, 60%, 100% { transform: translateY(0); }
          30% { transform: translateY(-5px); }
        }
        @keyframes fadeIn { from { opacity: 0 } to { opacity: 1 } }
        @keyframes warnPulse {
          0%, 100% { box-shadow: 0 0 0 0 rgba(234,179,8,0.5); }
          50%      { box-shadow: 0 0 0 5px rgba(234,179,8,0); }
        }
        @keyframes issuePulse {
          0%, 100% { box-shadow: 0 0 0 0 rgba(239,68,68,0.55); }
          50%      { box-shadow: 0 0 0 5px rgba(239,68,68,0); }
        }
        @keyframes slideDown {
          from { opacity: 0; transform: translateY(-8px); }
          to   { opacity: 1; transform: translateY(0); }
        }
        @keyframes slideInRight {
          from { opacity: 0; transform: translateX(24px); }
          to   { opacity: 1; transform: translateX(0); }
        }
      `}</style>
    </div>
  )
}
