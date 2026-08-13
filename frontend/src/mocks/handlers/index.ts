import { driverHandlers } from './driver'
import { loadHandlers } from './load'
import { cargoHandlers } from './cargo'
import { regionHandlers } from './region'
import { fareHandlers } from './fare'
import { shipperHandlers } from './shipper'

export const handlers = [
  ...driverHandlers,
  ...loadHandlers,
  ...cargoHandlers,
  ...regionHandlers,
  ...fareHandlers,
  ...shipperHandlers,
]
