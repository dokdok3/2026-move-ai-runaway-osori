import { findFreightByCargoId, freights, idNumber } from './dataset'
import { freightToCargoDetail } from './transform'
import type { components } from '@/api/schema.gen'

type CargoDetailResponse = components['schemas']['CargoDetailResponse']

/**
 * 화물 상세는 freights.csv 시드로 지연 초기화한 뒤 메모리에서 mutate한다.
 * create/update/accept가 서로 다른 요청이어도 같은 세션 안에서는 일관되게 보이게 하려는 목적이다.
 * 새로고침하면 초기화된다 — 진짜 DB가 아니다.
 */
const cargoStore = new Map<number, CargoDetailResponse>()

let nextCargoId = Math.max(...freights.map((freight) => idNumber(freight.id))) + 1

const getOrSeed = (cargoId: number): CargoDetailResponse | undefined => {
  if (cargoStore.has(cargoId)) return cargoStore.get(cargoId)

  const freight = findFreightByCargoId(cargoId)
  if (!freight) return undefined

  const detail = freightToCargoDetail(freight)
  cargoStore.set(cargoId, detail)
  return detail
}

export const getCargoDetail = (cargoId: number): CargoDetailResponse | undefined =>
  getOrSeed(cargoId)

export const getCreatedCargoDetails = (): CargoDetailResponse[] => Array.from(cargoStore.values())

export const updateCargoDetail = (
  cargoId: number,
  patch: Partial<CargoDetailResponse>,
): CargoDetailResponse | undefined => {
  const current = getOrSeed(cargoId)
  if (!current) return undefined

  const updated = { ...current, ...patch }
  cargoStore.set(cargoId, updated)
  return updated
}

export const createCargoDetail = (
  detail: Omit<CargoDetailResponse, 'cargoId'>,
): CargoDetailResponse => {
  const cargoId = nextCargoId
  nextCargoId += 1

  const created = { ...detail, cargoId }
  cargoStore.set(cargoId, created)
  return created
}
