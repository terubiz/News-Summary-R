import axiosClient from './axiosClient'

export interface SummaryConfigResponse {
  userId: number
  executionTime: string
  lookbackDays: number
  fetchCount: number
  aiProviderName: string
}

export interface SummaryConfigRequest {
  executionTime: string
  lookbackDays: number
  fetchCount: number
  aiProviderName: string
}

export const getSummaryConfig = (): Promise<SummaryConfigResponse> =>
  axiosClient.get<SummaryConfigResponse>('/api/summary-config').then((r) => r.data)

export const updateSummaryConfig = (request: SummaryConfigRequest): Promise<SummaryConfigResponse> =>
  axiosClient.put<SummaryConfigResponse>('/api/summary-config', request).then((r) => r.data)
