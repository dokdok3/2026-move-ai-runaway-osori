import type { CargoType } from '@/api/fare/model'

const CARGO_TYPE_LABELS: Record<string, string> = {
  REFRIGERATED: '냉장',
  FROZEN: '냉동',
  GENERAL: '일반화물',
  CONSTRUCTION: '건설자재',
  HAZARDOUS: '위험물',
}

const CARGO_TYPES: readonly CargoType[] = [
  'REFRIGERATED',
  'GENERAL',
  'FROZEN',
  'CONSTRUCTION',
  'HAZARDOUS',
]

export function getCargoTypeLabel(cargoType: string | undefined): string {
  if (!cargoType) return '일반화물'
  return CARGO_TYPE_LABELS[cargoType] ?? cargoType
}

/** AI 파싱 결과는 자유 문자열이라, 백엔드가 실제로 받는 리터럴 유니언으로 좁혀준다. */
export function toCargoType(cargoType: string | undefined): CargoType | undefined {
  return (CARGO_TYPES as readonly string[]).includes(cargoType ?? '')
    ? (cargoType as CargoType)
    : undefined
}
