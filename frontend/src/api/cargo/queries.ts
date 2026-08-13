import { mutationOptions, queryOptions } from '@tanstack/react-query'
import { createCargo, getCargoDetail, getMyCargos, parseCargo, updateCargo } from './api'
import type {
  CreateCargoParams,
  CreateCargoRequest,
  GetMyCargosParams,
  ParseCargoRequest,
  UpdateCargoParams,
  UpdateCargoRequest,
} from './model'

export const cargoQueryKeys = {
  all: ['cargo'] as const,
  details: () => [...cargoQueryKeys.all, 'detail'] as const,
  detail: (cargoId: number) => [...cargoQueryKeys.details(), cargoId] as const,
  myCargos: (params: GetMyCargosParams) =>
    [
      ...cargoQueryKeys.all,
      'my-cargos',
      params.shipperId,
      params.pageable.page ?? 0,
      params.pageable.size ?? 10,
    ] as const,
}

export const cargoQueries = {
  detail: (cargoId: number) =>
    queryOptions({
      queryKey: cargoQueryKeys.detail(cargoId),
      queryFn: () => getCargoDetail(cargoId),
    }),
  myCargos: (params: GetMyCargosParams) =>
    queryOptions({
      queryKey: cargoQueryKeys.myCargos(params),
      queryFn: () => getMyCargos(params),
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
      mutationFn: ({ params, body }: { params: UpdateCargoParams; body: UpdateCargoRequest }) =>
        updateCargo(params, body),
    }),
  parse: () =>
    mutationOptions({
      mutationKey: [...cargoQueryKeys.all, 'parse'],
      mutationFn: (body: ParseCargoRequest) => parseCargo(body),
    }),
}
