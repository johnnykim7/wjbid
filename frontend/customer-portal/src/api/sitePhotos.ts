import { api } from './client'

/** 자리 키마다 보이는 사진 주소 (CR-153, GET /site-photos). 사진이 없는 자리는 빈 배열 */
export type SitePhotoSlots = Record<string, string[]>

export async function fetchSitePhotos(): Promise<SitePhotoSlots> {
  const { data } = await api.get<{ slots: SitePhotoSlots }>('/site-photos')
  return data.slots ?? {}
}
