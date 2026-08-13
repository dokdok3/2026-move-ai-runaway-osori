import driversCsv from './drivers.csv?raw'
import freightsCsv from './freights.csv?raw'
import faresCsv from './fares.csv?raw'
import { parseCsv } from './csv'

export interface DriverRow {
  id: string
  name: string
  vehicleCargoTypes: string[]
  currentSido: string
  currentSigungu: string
  preferredOriginSido: string
  preferredOriginSigungu: string
  preferredDestinationSido: string
  preferredDestinationSigungu: string
  minimumAcceptFareKrw: number
  rating: number
  completionRate: number
}

export interface FreightRow {
  id: string
  cargoType: string
  cargoDescription: string
  originSido: string
  originSigungu: string
  destinationSido: string
  destinationSigungu: string
  weightTon: number
  offeredFareKrw: number
  loadingAt: string
  unloadingAt: string
  status: string
}

export interface FareRow {
  id: string
  originSido: string
  originSigungu: string
  destinationSido: string
  destinationSigungu: string
  cargoType: string
  averageFareKrw: number
  lowFareThresholdKrw: number
}

/** id는 CSV 원본의 `driver-01`/`freight-001` 형태에서 숫자만 뽑아 API의 int64 id로 쓴다. */
export const idNumber = (id: string): number => Number(id.split('-')[1])

export const drivers: DriverRow[] = parseCsv(driversCsv).map((row) => ({
  id: row.id,
  name: row.name,
  vehicleCargoTypes: row.vehicleCargoTypes.split('|'),
  currentSido: row.currentSido,
  currentSigungu: row.currentSigungu,
  preferredOriginSido: row.preferredOriginSido,
  preferredOriginSigungu: row.preferredOriginSigungu,
  preferredDestinationSido: row.preferredDestinationSido,
  preferredDestinationSigungu: row.preferredDestinationSigungu,
  minimumAcceptFareKrw: Number(row.minimumAcceptFareKrw),
  rating: Number(row.rating),
  completionRate: Number(row.completionRate),
}))

export const freights: FreightRow[] = parseCsv(freightsCsv).map((row) => ({
  id: row.id,
  cargoType: row.cargoType,
  cargoDescription: row.cargoDescription,
  originSido: row.originSido,
  originSigungu: row.originSigungu,
  destinationSido: row.destinationSido,
  destinationSigungu: row.destinationSigungu,
  weightTon: Number(row.weightTon),
  offeredFareKrw: Number(row.offeredFareKrw),
  loadingAt: row.loadingAt,
  unloadingAt: row.unloadingAt,
  status: row.status,
}))

export const fares: FareRow[] = parseCsv(faresCsv).map((row) => ({
  id: row.id,
  originSido: row.originSido,
  originSigungu: row.originSigungu,
  destinationSido: row.destinationSido,
  destinationSigungu: row.destinationSigungu,
  cargoType: row.cargoType,
  averageFareKrw: Number(row.averageFareKrw),
  lowFareThresholdKrw: Number(row.lowFareThresholdKrw),
}))

export const findDriverByDriverId = (driverId: number): DriverRow | undefined =>
  drivers.find((driver) => idNumber(driver.id) === driverId)

export const findFreightByCargoId = (cargoId: number): FreightRow | undefined =>
  freights.find((freight) => idNumber(freight.id) === cargoId)

export const lookupFareReference = (
  originSido: string,
  destinationSido: string,
  cargoType: string,
): FareRow | undefined =>
  fares.find(
    (fare) =>
      fare.originSido === originSido &&
      fare.destinationSido === destinationSido &&
      fare.cargoType === cargoType,
  )

/** 시도 -> 시군구 목록. drivers.csv/freights.csv에 등장하는 모든 지역을 모아 만든다. */
export const regionsBySido = (): Map<string, Set<string>> => {
  const map = new Map<string, Set<string>>()

  const add = (sido: string, sigungu: string) => {
    if (!map.has(sido)) map.set(sido, new Set())
    map.get(sido)?.add(sigungu)
  }

  for (const driver of drivers) {
    add(driver.currentSido, driver.currentSigungu)
    add(driver.preferredOriginSido, driver.preferredOriginSigungu)
    add(driver.preferredDestinationSido, driver.preferredDestinationSigungu)
  }
  for (const freight of freights) {
    add(freight.originSido, freight.originSigungu)
    add(freight.destinationSido, freight.destinationSigungu)
  }

  return map
}
