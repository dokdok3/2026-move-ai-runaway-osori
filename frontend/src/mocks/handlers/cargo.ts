import { HttpResponse } from 'msw'
import { createOpenApiHttp } from 'openapi-msw'
import type { paths } from '@/api/schema.gen'
import { getCargoDetail, createCargoDetail, updateCargoDetail } from '../data/store'
import { parseFreeText, toFareQuoteResponse } from '../data/transform'

const http = createOpenApiHttp<paths>()

export const cargoHandlers = [
  http.get('/api/v1/cargos/{cargoId}', ({ params }) => {
    const cargoId = Number(params.cargoId)
    const cargo = getCargoDetail(cargoId)
    if (!cargo) {
      return HttpResponse.json(
        { success: false, message: '화물을 찾을 수 없습니다.' },
        { status: 404 },
      )
    }

    return HttpResponse.json({ success: true, data: cargo })
  }),

  http.patch('/api/v1/cargos/{cargoId}', async ({ params, request }) => {
    const cargoId = Number(params.cargoId)
    const body = await request.json()
    const updated = updateCargoDetail(cargoId, body)
    if (!updated) {
      return HttpResponse.json(
        { success: false, message: '화물을 찾을 수 없습니다.' },
        { status: 404 },
      )
    }

    return HttpResponse.json({ success: true, data: updated })
  }),

  http.post('/api/v1/cargos', async ({ request }) => {
    const body = await request.json()

    const created = createCargoDetail({
      status: 'PENDING',
      origin: body.origin,
      destination: body.destination,
      cargoType: body.cargoType,
      cargoDescription: body.cargoDescription,
      weightTon: body.weightTon,
      vehicleType: body.vehicleType,
      bodyType: body.bodyType,
      desiredFare: body.desiredFare,
      loadingAt: body.loadingAt,
      unloadingAt: body.unloadingAt,
      distanceKm: body.distanceKm,
      fare: toFareQuoteResponse(
        body.origin?.sido,
        body.destination?.sido,
        body.cargoType,
        body.desiredFare,
      ),
    })

    return HttpResponse.json({ success: true, data: created })
  }),

  http.post('/api/v1/cargos/parse', async ({ request }) => {
    const body = await request.json()
    const parsed = parseFreeText(body.rawText)

    return HttpResponse.json({
      success: true,
      data: {
        origin: parsed.originSido ? { sido: parsed.originSido } : undefined,
        destination: parsed.destinationSido ? { sido: parsed.destinationSido } : undefined,
        cargoType: parsed.cargoType,
        cargoDescription: body.rawText,
        weightTon: parsed.weightTon,
        desiredFare: parsed.desiredFare,
      },
    })
  }),
]
