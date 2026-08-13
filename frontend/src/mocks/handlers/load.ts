import { HttpResponse } from 'msw'
import { createOpenApiHttp } from 'openapi-msw'
import type { paths } from '@/api/schema.gen'
import { findDriverByDriverId, freights } from '../data/dataset'
import { loadsForDriver, toLoadResponse } from '../data/transform'
import { getCargoDetail, updateCargoDetail } from '../data/store'

const http = createOpenApiHttp<paths>()
const hiddenCargoIds = new Set<number>()
const acceptedCargoIds = new Set<number>()

export const loadHandlers = [
  http.get('/api/v1/loads', ({ query }) => {
    const driverId = Number(query.get('driverId'))
    const driver = findDriverByDriverId(driverId)
    if (!driver) {
      return HttpResponse.json(
        { success: false, message: '기사를 찾을 수 없습니다.' },
        { status: 404 },
      )
    }

    const rankingMode = query.get('preferenceText') ? 'HYBRID' : 'RULE_BASE'
    const filter = query.get('filter') ?? 'ALL'
    const data = loadsForDriver(driver, freights)
      .map(toLoadResponse)
      .filter((load) => {
        if (load.cargoId === undefined) return false
        if (filter === 'HIDDEN') return hiddenCargoIds.has(load.cargoId)
        if (filter === 'ACCEPTED') return acceptedCargoIds.has(load.cargoId)
        return !hiddenCargoIds.has(load.cargoId) && !acceptedCargoIds.has(load.cargoId)
      })
      .sort((a, b) => (b.matchScore ?? 0) - (a.matchScore ?? 0))
      .map((load) => ({ ...load, rankingMode }))

    const cursor = Number(query.get('cursor') ?? 0)
    const size = 10
    const content = data.slice(cursor, cursor + size)
    const nextCursor = cursor + size < data.length ? String(cursor + size) : undefined

    return HttpResponse.json({
      success: true,
      data: { content, nextCursor, hasNext: nextCursor !== undefined, size: content.length },
    })
  }),

  http.post('/api/v1/loads/{cargoId}/hide', ({ params, query }) => {
    const cargoId = Number(params.cargoId)
    const driverId = Number(query.get('driverId'))
    if (!driverId) {
      return HttpResponse.json(
        { success: false, message: '기사를 찾을 수 없습니다.' },
        { status: 404 },
      )
    }

    hiddenCargoIds.add(cargoId)
    return HttpResponse.json({ success: true })
  }),

  http.post('/api/v1/loads/{cargoId}/accept', ({ params, query }) => {
    const cargoId = Number(params.cargoId)
    const driverId = Number(query.get('driverId'))

    const cargo = getCargoDetail(cargoId)
    const driver = findDriverByDriverId(driverId)
    if (!cargo || !driver) {
      return HttpResponse.json(
        { success: false, message: '화물 또는 기사를 찾을 수 없습니다.' },
        { status: 404 },
      )
    }

    updateCargoDetail(cargoId, { status: 'MATCHED' })
    acceptedCargoIds.add(cargoId)

    return HttpResponse.json({
      success: true,
      data: {
        assignmentId: cargoId * 100 + driverId,
        cargoId,
        status: 'MATCHED',
      },
    })
  }),
]
