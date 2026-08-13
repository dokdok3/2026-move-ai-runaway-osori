import {
  type DriverRow,
  type FareRow,
  type FreightRow,
  idNumber,
  lookupFareReference,
} from './dataset'
import type { components } from '@/api/schema.gen'

type DriverResponse = components['schemas']['DriverResponse']
type LoadResponse = components['schemas']['LoadResponse']
type FareQuoteResponse = components['schemas']['FareQuoteResponse']
type CargoDetailResponse = components['schemas']['CargoDetailResponse']
type RegionPoint = components['schemas']['RegionPoint']

const VEHICLE_TYPES = ['1톤 트럭', '2.5톤 트럭', '5톤 트럭', '11톤 트럭', '카고트레일러']
const CAPACITY_TONS = [1, 2.5, 5, 11, 18]

/**
 * drivers.csv에는 vehicleType/bodyType/capacityTon/plateNumber/totalTrips가 없어서
 * driver id를 시드로 결정적으로 합성한다 (새로고침해도 같은 값이 나와야 데모가 안정적이다).
 */
export const toDriverResponse = (row: DriverRow): DriverResponse => {
  const n = idNumber(row.id)
  const isColdChain = row.vehicleCargoTypes.some((type) =>
    ['REFRIGERATED', 'FROZEN'].includes(type),
  )

  return {
    driverId: n,
    name: row.name,
    plateNumber: `${10 + (n % 80)}${['가', '나', '다', '라', '마'][n % 5]}${1000 + n}`,
    vehicleType: VEHICLE_TYPES[n % VEHICLE_TYPES.length],
    bodyType: isColdChain ? '냉동탑차' : '카고',
    capacityTon: CAPACITY_TONS[n % CAPACITY_TONS.length],
    rating: row.rating,
    totalTrips: 40 + n * 7,
    completionRate: row.completionRate,
    minAcceptFare: row.minimumAcceptFareKrw,
    routePreferences: {
      origins: [{ sido: row.preferredOriginSido, sigungu: row.preferredOriginSigungu }],
      destinations: [
        { sido: row.preferredDestinationSido, sigungu: row.preferredDestinationSigungu },
      ],
    },
  }
}

const BODY_TYPE_BY_CARGO_TYPE: Record<string, string> = {
  REFRIGERATED: '냉동탑차',
  FROZEN: '냉동탑차',
  HAZARDOUS: '탱크로리',
  CONSTRUCTION: '카고',
  GENERAL: '카고',
}

export const fareVerdict = (offeredFare: number, fareRef: FareRow | undefined): string => {
  if (!fareRef) return 'UNKNOWN'
  return offeredFare < fareRef.lowFareThresholdKrw ? 'LOW' : 'FAIR'
}

/** 실제 거리 데이터가 없어 화물 id를 시드로 대략적인 값을 만든다. 데모용 근사치다. */
const approximateDistanceKm = (freight: FreightRow): number => {
  const sameSido = freight.originSido === freight.destinationSido
  const base = sameSido ? 20 : 250
  return base + (idNumber(freight.id) % 150)
}

export const toLoadResponse = (freight: FreightRow): LoadResponse => {
  const fareRef = lookupFareReference(
    freight.originSido,
    freight.destinationSido,
    freight.cargoType,
  )
  const badge = fareVerdict(freight.offeredFareKrw, fareRef)
  const belowPercent = fareRef
    ? Math.round((1 - freight.offeredFareKrw / fareRef.averageFareKrw) * 100)
    : undefined

  return {
    cargoId: idNumber(freight.id),
    origin: `${freight.originSido} ${freight.originSigungu}`,
    destination: `${freight.destinationSido} ${freight.destinationSigungu}`,
    loadingAt: freight.loadingAt,
    unloadingAt: freight.unloadingAt,
    distanceKm: approximateDistanceKm(freight),
    vehicleType: BODY_TYPE_BY_CARGO_TYPE[freight.cargoType] ?? '카고',
    weightTon: freight.weightTon,
    bodyType: BODY_TYPE_BY_CARGO_TYPE[freight.cargoType] ?? '카고',
    cargoType: freight.cargoType,
    fare: freight.offeredFareKrw,
    matchScore: matchScore(freight, fareRef),
    badge,
    regionAverageFare: fareRef?.averageFareKrw,
    belowPercent,
    rankingMode: 'RULE_BASE',
    matchReasons: [],
  }
}

/**
 * 백엔드 랭킹 알고리즘이 아직 없어서(스텁) 데모용으로 만든 단순 점수다.
 * 운임이 시세보다 유리할수록, 기사 선호 구간과 겹칠수록 점수가 올라간다.
 */
const matchScore = (freight: FreightRow, fareRef: FareRow | undefined): number => {
  let score = 60

  if (fareRef) {
    const ratio = freight.offeredFareKrw / fareRef.averageFareKrw
    score += Math.round((ratio - 1) * 100)
  }

  return Math.max(0, Math.min(100, score))
}

/** 기사가 취급 가능하고, 최소수락운임을 넘고, 아직 배차되지 않은 화물만 남긴다. */
export const loadsForDriver = (driver: DriverRow, freights: FreightRow[]): FreightRow[] =>
  freights
    .filter((freight) => freight.status === 'PENDING')
    .filter((freight) => driver.vehicleCargoTypes.includes(freight.cargoType))
    .filter((freight) => freight.offeredFareKrw >= driver.minimumAcceptFareKrw)

export const toFareQuoteResponse = (
  originSido: string | undefined,
  destSido: string | undefined,
  cargoType: string | undefined,
  desiredFare: number | undefined,
): FareQuoteResponse => {
  const fareRef =
    originSido && destSido && cargoType
      ? lookupFareReference(originSido, destSido, cargoType)
      : undefined
  if (!fareRef) {
    return { verdict: 'UNKNOWN', message: '해당 구간·화물 종류의 시세 데이터가 없습니다.' }
  }

  const verdict =
    desiredFare === undefined
      ? fareVerdict(fareRef.averageFareKrw, fareRef)
      : fareVerdict(desiredFare, fareRef)

  return {
    averageFare: fareRef.averageFareKrw,
    sameDayThreshold: fareRef.averageFareKrw,
    distanceKm: undefined,
    verdict,
    message:
      verdict === 'LOW'
        ? '제시 운임이 구간 평균보다 낮습니다.'
        : verdict === 'FAIR'
          ? '구간 평균 대비 적정한 운임입니다.'
          : undefined,
  }
}

export const freightToCargoDetail = (
  freight: FreightRow,
  assignedDriver?: DriverResponse,
): CargoDetailResponse => {
  const origin: RegionPoint = { sido: freight.originSido, sigungu: freight.originSigungu }
  const destination: RegionPoint = {
    sido: freight.destinationSido,
    sigungu: freight.destinationSigungu,
  }

  return {
    cargoId: idNumber(freight.id),
    status: freight.status,
    origin,
    destination,
    cargoType: freight.cargoType,
    cargoDescription: freight.cargoDescription,
    weightTon: freight.weightTon,
    vehicleType: BODY_TYPE_BY_CARGO_TYPE[freight.cargoType] ?? '카고',
    bodyType: BODY_TYPE_BY_CARGO_TYPE[freight.cargoType] ?? '카고',
    desiredFare: freight.offeredFareKrw,
    loadingAt: freight.loadingAt,
    unloadingAt: freight.unloadingAt,
    distanceKm: approximateDistanceKm(freight),
    fare: toFareQuoteResponse(
      freight.originSido,
      freight.destinationSido,
      freight.cargoType,
      freight.offeredFareKrw,
    ),
    assignedDriver,
  }
}

const CARGO_TYPE_KEYWORDS: Record<string, string> = {
  냉장: 'REFRIGERATED',
  냉동: 'FROZEN',
  위험물: 'HAZARDOUS',
  화학: 'HAZARDOUS',
  건설: 'CONSTRUCTION',
  자재: 'CONSTRUCTION',
  철근: 'CONSTRUCTION',
}

const SIDO_NAMES = [
  '서울특별시',
  '부산광역시',
  '대구광역시',
  '인천광역시',
  '광주광역시',
  '대전광역시',
  '울산광역시',
  '세종특별자치시',
  '경기도',
  '강원특별자치도',
  '충청북도',
  '충청남도',
  '전라북도',
  '전라남도',
  '경상북도',
  '경상남도',
  '제주특별자치도',
]

export interface ParsedFreeText {
  originSido?: string
  destinationSido?: string
  cargoType: string
  weightTon?: number
  desiredFare?: number
}

/**
 * 자연어 문장에서 시도명·톤수·운임·화물 종류를 정규식으로 추출하는 데모용 파서다.
 * 실제 AI 파싱이 아니라, 백엔드가 아직 없을 때 화면을 보여주기 위한 근사치다.
 */
export const parseFreeText = (rawText: string): ParsedFreeText => {
  const foundSidos = SIDO_NAMES.filter((sido) => rawText.includes(sido.slice(0, 2)))
  const weightMatch = rawText.match(/(\d+(?:\.\d+)?)\s*톤/)
  const fareMatch = rawText.match(/(\d+(?:\.\d+)?)\s*(만원|원)/)
  const cargoTypeEntry = Object.entries(CARGO_TYPE_KEYWORDS).find(([keyword]) =>
    rawText.includes(keyword),
  )

  const fareValue = fareMatch ? Number(fareMatch[1]) : undefined
  const desiredFare =
    fareValue === undefined ? undefined : fareMatch?.[2] === '만원' ? fareValue * 10000 : fareValue

  return {
    originSido: foundSidos[0],
    destinationSido: foundSidos[1],
    cargoType: cargoTypeEntry?.[1] ?? 'GENERAL',
    weightTon: weightMatch ? Number(weightMatch[1]) : undefined,
    desiredFare,
  }
}
