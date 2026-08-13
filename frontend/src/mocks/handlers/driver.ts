import { HttpResponse } from 'msw'
import { createOpenApiHttp } from 'openapi-msw'
import type { paths } from '@/api/schema.gen'
import { findDriverByDriverId } from '../data/dataset'
import { toDriverResponse } from '../data/transform'

const http = createOpenApiHttp<paths>()

export const driverHandlers = [
  http.get('/api/v1/drivers/me', ({ query }) => {
    const driverId = Number(query.get('driverId'))
    const driver = findDriverByDriverId(driverId)
    if (!driver) {
      return HttpResponse.json(
        { success: false, message: '기사를 찾을 수 없습니다.' },
        { status: 404 },
      )
    }

    return HttpResponse.json({ success: true, data: toDriverResponse(driver) })
  }),

  http.put('/api/v1/drivers/me', async ({ query, request }) => {
    const driverId = Number(query.get('driverId'))
    const driver = findDriverByDriverId(driverId)
    if (!driver) {
      return HttpResponse.json(
        { success: false, message: '기사를 찾을 수 없습니다.' },
        { status: 404 },
      )
    }

    const body = await request.json()
    return HttpResponse.json({
      success: true,
      data: { ...toDriverResponse(driver), ...body, driverId },
    })
  }),

  http.put('/api/v1/drivers/me/route-preferences', async ({ query, request }) => {
    const driverId = Number(query.get('driverId'))
    const driver = findDriverByDriverId(driverId)
    if (!driver) {
      return HttpResponse.json(
        { success: false, message: '기사를 찾을 수 없습니다.' },
        { status: 404 },
      )
    }

    const body = await request.json()

    return HttpResponse.json({
      success: true,
      data: { ...toDriverResponse(driver), routePreferences: body },
    })
  }),
]
