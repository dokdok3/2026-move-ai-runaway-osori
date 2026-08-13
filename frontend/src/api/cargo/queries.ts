import { mutationOptions, queryOptions } from '@tanstack/react-query'
import { createCargo, getCargoDetail, parseCargo, updateCargo } from './api'
import type {
  CreateCargoParams,
  CreateCargoRequest,
  ParseCargoRequest,
  UpdateCargoRequest,
} from './model'

export const cargoQueryKeys = {
  all: ['cargo'] as const,
  details: () => [...cargoQueryKeys.all, 'detail'] as const,
  detail: (cargoId: number) => [...cargoQueryKeys.details(), cargoId] as const,
}

export const cargoQueries = {
  detail: (cargoId: number) =>
    queryOptions({
      queryKey: cargoQueryKeys.detail(cargoId),
      queryFn: () => getCargoDetail(cargoId),
    }),
}

export const cargoMutations = {
  create: () =>
    mutationOptions({
      mutationKey: [...cargoQueryKeys.all, 'create'],
      mutationFn: ({ params, body }: { params: CreateCargoParams; body: CreateCargoRequest }) =>
        createCargo(params, body),
    }),
  update: () =>
    mutationOptions({
      mutationKey: [...cargoQueryKeys.all, 'update'],
      mutationFn: ({ cargoId, body }: { cargoId: number; body: UpdateCargoRequest }) =>
        updateCargo(cargoId, body),
    }),
  parse: () =>
    mutationOptions({
      mutationKey: [...cargoQueryKeys.all, 'parse'],
      mutationFn: (body: ParseCargoRequest) => parseCargo(body),
    }),
}
