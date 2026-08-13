import { HttpResponse } from 'msw'
import { createOpenApiHttp } from 'openapi-msw'
import type { paths } from '@/api/schema.gen'
import { regionsBySido } from '../data/dataset'

const http = createOpenApiHttp<paths>()

export const regionHandlers = [
  http.get('/api/v1/regions', () => {
    const data = Array.from(regionsBySido().entries()).map(([sido, sigungus]) => ({
      sido,
      sigungus: Array.from(sigungus),
    }))

    return HttpResponse.json({ success: true, data })
  }),
]
