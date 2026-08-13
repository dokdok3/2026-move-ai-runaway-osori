import { HttpResponse } from 'msw'
import { createOpenApiHttp } from 'openapi-msw'
import type { components, paths } from '@/api/schema.gen'

const http = createOpenApiHttp<paths>()

let profile: components['schemas']['ShipperProfileResponse'] = {
  shipperId: 1,
  companyName: undefined,
  contactName: undefined,
  phoneNumber: undefined,
  businessNumber: undefined,
  address: undefined,
}

export const shipperHandlers = [
  http.get('/api/v1/shippers/me', () => HttpResponse.json({ success: true, data: profile })),

  http.put('/api/v1/shippers/me', async ({ request }) => {
    const body = (await request.json()) as components['schemas']['ShipperProfileRequest']
    profile = { ...profile, ...body }
    return HttpResponse.json({ success: true, data: profile })
  }),
]
