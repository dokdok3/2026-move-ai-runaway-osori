import { HttpResponse } from 'msw'
import { createOpenApiHttp } from 'openapi-msw'
import type { paths } from '@/api/schema.gen'
import { toFareQuoteResponse } from '../data/transform'

const http = createOpenApiHttp<paths>()

export const fareHandlers = [
  http.get('/api/v1/fares/quote', ({ query }) => {
    const originSido = query.get('originSido')
    const destSido = query.get('destSido')
    const cargoType = query.get('cargoType')
    const desiredFareParam = query.get('desiredFare')
    const desiredFare = desiredFareParam ? Number(desiredFareParam) : undefined

    const data = toFareQuoteResponse(originSido, destSido, cargoType, desiredFare)

    return HttpResponse.json({ success: true, data })
  }),
]
