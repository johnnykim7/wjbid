import { useMemo, type KeyboardEvent } from 'react'
import type { BaseStat } from '../../types/awardeeBase'
import { BOUNDS, DMZ_POINTS, JEJU, KOREA_OUTLINE } from './koreaOutline'

/** 위도 1도의 그림 길이. 경도는 위도 38도 부근 비율(COS_LAT)을 곱해 줄인다 */
const SCALE = 118
const COS_LAT = 0.81
const PAD = 10
/** 점 크기. 공고 0건도 보이게 바탕 반지름을 두고, 공고 수의 제곱근에 비례해 키운다 */
const DOT_BASE = 4.5
const DOT_GROW = 1.9
const LABEL_GAP = 5
const LABEL_SIZE = { normal: 12.5, selected: 14 }

const W = Math.round((BOUNDS.lonMax - BOUNDS.lonMin) * SCALE * COS_LAT + PAD * 2)
const H = Math.round((BOUNDS.latMax - BOUNDS.latMin) * SCALE + PAD * 2)

const px = (lon: number) => (lon - BOUNDS.lonMin) * SCALE * COS_LAT + PAD
const py = (lat: number) => (BOUNDS.latMax - lat) * SCALE + PAD
const pathOf = (pts: readonly (readonly [number, number])[], close: boolean) =>
  pts.map(([lon, lat], i) => `${i === 0 ? 'M' : 'L'}${px(lon).toFixed(1)} ${py(lat).toFixed(1)}`).join(' ') + (close ? ' Z' : '')

const LAND = pathOf(KOREA_OUTLINE, true)
const DMZ = pathOf(KOREA_OUTLINE.slice(0, DMZ_POINTS), false)

type Box = { x1: number; y1: number; x2: number; y2: number }
type Dot = { base: BaseStat; x: number; y: number; r: number }
type Label = { id: string; x: number; y: number; anchor: 'start' | 'end'; text: string; selected: boolean }

const overlaps = (a: Box, b: Box) => a.x1 < b.x2 && b.x1 < a.x2 && a.y1 < b.y2 && b.y1 < a.y2

/** 글자 폭 어림. 한글은 글자 크기만큼, 그 밖은 6할 */
function textWidth(text: string, size: number) {
  let w = 0
  for (const ch of text) w += /[ㄱ-힝]/.test(ch) ? size : size * 0.6
  return w
}

/**
 * 이름표 자리를 고른다. 공고가 많은 기지부터 점 오른쪽, 왼쪽, 아래, 위 순으로 두어 보고,
 * 다른 이름표나 점과 겹치거나 그림 밖으로 나가면 다음 자리를 본다. 둘 곳이 없으면 이름표 없이 점만 둔다(점 위에 올리면 이름이 뜬다).
 */
function placeLabels(dots: Dot[], selectedId: string | null, labelOf: (b: BaseStat) => string): Label[] {
  const order = [...dots].sort((a, b) =>
    (a.base.id === selectedId ? -1 : 0) - (b.base.id === selectedId ? -1 : 0)
    || b.base.opportunityCount - a.base.opportunityCount)
  const dotBoxes = dots.map((d) => ({ id: d.base.id, box: { x1: d.x - d.r, y1: d.y - d.r, x2: d.x + d.r, y2: d.y + d.r } }))
  const labelBoxes: Box[] = []
  const out: Label[] = []
  for (const d of order) {
    const selected = d.base.id === selectedId
    const size = selected ? LABEL_SIZE.selected : LABEL_SIZE.normal
    const text = labelOf(d.base)
    const w = textWidth(text, size)
    const g = d.r + LABEL_GAP
    const tries: { x: number; y: number; anchor: 'start' | 'end' }[] = [
      { x: d.x + g, y: d.y + size * 0.35, anchor: 'start' },
      { x: d.x - g, y: d.y + size * 0.35, anchor: 'end' },
      { x: d.x + g * 0.6, y: d.y + g + size * 0.8, anchor: 'start' },
      { x: d.x - g * 0.6, y: d.y + g + size * 0.8, anchor: 'end' },
      { x: d.x + g * 0.6, y: d.y - g, anchor: 'start' },
      { x: d.x - g * 0.6, y: d.y - g, anchor: 'end' },
    ]
    for (const t of tries) {
      const box: Box = t.anchor === 'start'
        ? { x1: t.x, y1: t.y - size, x2: t.x + w, y2: t.y + 3 }
        : { x1: t.x - w, y1: t.y - size, x2: t.x, y2: t.y + 3 }
      const inside = box.x1 >= 2 && box.x2 <= W - 2 && box.y1 >= 2 && box.y2 <= H - 2
      const free = !labelBoxes.some((o) => overlaps(o, box))
        && !dotBoxes.some((o) => o.id !== d.base.id && overlaps(o.box, box))
      if (inside && free) {
        labelBoxes.push(box)
        out.push({ id: d.base.id, x: t.x, y: t.y, anchor: t.anchor, text, selected })
        break
      }
    }
  }
  return out
}

type Props = {
  bases: BaseStat[]
  selectedId: string | null
  onSelect: (id: string) => void
  ariaLabel: string
  /** 점 옆 이름표 */
  labelOf: (b: BaseStat) => string
  /** 점에 올리면 뜨는 이름과 읽기 프로그램용 이름 */
  titleOf: (b: BaseStat) => string
}

/** 기지별 지도 (CR-136). 위치가 있는 기지만 점으로 그리고, 점은 공고 수만큼 크다. 점을 누르면 그 기지를 고른다 */
export function KoreaMap({ bases, selectedId, onSelect, ariaLabel, labelOf, titleOf }: Props) {
  const dots = useMemo<Dot[]>(() => bases
    .filter((b) => b.latitude != null && b.longitude != null)
    .map((b) => ({ base: b, x: px(b.longitude as number), y: py(b.latitude as number), r: DOT_BASE + Math.sqrt(b.opportunityCount) * DOT_GROW }))
    .sort((a, b) => b.r - a.r), [bases])
  const labels = useMemo(() => placeLabels(dots, selectedId, labelOf), [dots, selectedId, labelOf])
  const drawOrder = [...dots.filter((d) => d.base.id !== selectedId), ...dots.filter((d) => d.base.id === selectedId)]

  const onKey = (id: string) => (e: KeyboardEvent<SVGCircleElement>) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault()
      onSelect(id)
    }
  }

  return (
    <svg className="kmap" viewBox={`0 0 ${W} ${H}`} role="img" aria-label={ariaLabel}>
      <rect className="sea" x="0" y="0" width={W} height={H} rx="10" />
      <path className="land" d={LAND} />
      <ellipse className="land" cx={px(JEJU.lon)} cy={py(JEJU.lat)} rx={JEJU.rxDeg * SCALE * COS_LAT} ry={JEJU.ryDeg * SCALE} />
      <path className="dmz" d={DMZ} />
      {drawOrder.map((d) => (
        <circle
          key={d.base.id}
          className={`dot${d.base.id === selectedId ? ' on' : ''}`}
          cx={d.x}
          cy={d.y}
          r={d.r}
          role="button"
          tabIndex={0}
          aria-pressed={d.base.id === selectedId}
          aria-label={titleOf(d.base)}
          onClick={() => onSelect(d.base.id)}
          onKeyDown={onKey(d.base.id)}
        >
          <title>{titleOf(d.base)}</title>
        </circle>
      ))}
      {labels.map((l) => (
        <text key={l.id} className={`lbl${l.selected ? ' on' : ''}`} x={l.x} y={l.y} textAnchor={l.anchor} aria-hidden="true">{l.text}</text>
      ))}
    </svg>
  )
}
