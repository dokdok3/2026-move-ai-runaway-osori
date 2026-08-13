import { HttpResponse } from 'msw'
import { createOpenApiHttp } from 'openapi-msw'
import type { paths } from '@/api/schema.gen'
import {
  getCargoDetail,
  getCreatedCargoDetails,
  createCargoDetail,
  updateCargoDetail,
} from '../data/store'
import { parseFreeText, toFareQuoteResponse } from '../data/transform'

const http = createOpenApiHttp<paths>()

export const cargoHandlers = [
  http.get('/api/v1/cargos/me', ({ query, request }) => {
    const shipperId = Number(query.get('shipperId'))
    if (!shipperId) {
      return HttpResponse.json(
        { success: false, message: '화주를 찾을 수 없습니다.' },
        { status: 404 },
      )
    }

    const searchParams = new URL(request.url).searchParams
    const page = Number(searchParams.get('pageable[page]') ?? searchParams.get('page') ?? 0)
    const size = Number(searchParams.get('pageable[size]') ?? searchParams.get('size') ?? 10)
    const cargos = getCreatedCargoDetails()
    const content = cargos.slice(page * size, (page + 1) * size)
    const totalPages = Math.ceil(cargos.length / size)

    return HttpResponse.json({
      success: true,
      data: {
        content,
        page,
        size,
        totalElements: cargos.length,
        totalPages,
        first: page === 0,
        last: totalPages === 0 || page >= totalPages - 1,
      },
    })
  }),

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
    const parsed = parseFreeText(body.requestText)

    const missingFields = [
      !parsed.originSido && 'origin',
      !parsed.destinationSido && 'destination',
      parsed.weightTon === undefined && 'weightTon',
      parsed.desiredFare === undefined && 'offeredFareKrw',
    ].filter((field): field is string => Boolean(field))

    return HttpResponse.json({
      success: true,
      data: {
        origin: parsed.originSido ? { sido: parsed.originSido, sigungu: '전체' } : undefined,
        destination: parsed.destinationSido
          ? { sido: parsed.destinationSido, sigungu: '전체' }
          : undefined,
        cargoType: parsed.cargoType,
        cargoDescription: body.requestText,
        weightTon: parsed.weightTon,
        offeredFareKrw: parsed.desiredFare,
        loadingDate: body.requestText.includes('내일') ? '2026-08-14' : undefined,
        unloadingDate: body.requestText.includes('내일') ? '2026-08-14' : undefined,
        missingFields,
        confidence: missingFields.length === 0 ? 'HIGH' : 'MEDIUM',
        warnings: [],
      },
    })
  }),
]
