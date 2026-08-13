import { HttpResponse } from 'msw'
import { createOpenApiHttp } from 'openapi-msw'
import type { paths } from '@/api/schema.gen'
import { findDriverByDriverId, freights } from '../data/dataset'
import { loadsForDriver, toLoadResponse } from '../data/transform'
import { getCargoDetail, updateCargoDetail } from '../data/store'

const http = createOpenApiHttp<paths>()

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

    const data = loadsForDriver(driver, freights)
      .map(toLoadResponse)
      .sort((a, b) => (b.matchScore ?? 0) - (a.matchScore ?? 0))

    return HttpResponse.json({ success: true, data })
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
