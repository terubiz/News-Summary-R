import axiosClient from './axiosClient'

export interface KeywordResponse {
  id: number
  userId: number
  word: string
  isActive: boolean
}

export interface CreateKeywordRequest {
  word: string
}

export interface UpdateKeywordRequest {
  isActive: boolean
}

export const getKeywords = (): Promise<KeywordResponse[]> =>
  axiosClient.get<KeywordResponse[]>('/api/keywords').then((r) => r.data)

export const createKeyword = (request: CreateKeywordRequest): Promise<KeywordResponse> =>
  axiosClient.post<KeywordResponse>('/api/keywords', request).then((r) => r.data)

export const updateKeyword = (id: number, request: UpdateKeywordRequest): Promise<KeywordResponse> =>
  axiosClient.patch<KeywordResponse>(`/api/keywords/${id}`, request).then((r) => r.data)

export const deleteKeyword = (id: number): Promise<void> =>
  axiosClient.delete(`/api/keywords/${id}`).then(() => undefined)
