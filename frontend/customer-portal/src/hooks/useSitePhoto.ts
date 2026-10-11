import { useSyncExternalStore } from 'react'
import { fetchSitePhotos, type SitePhotoSlots } from '../api/sitePhotos'

/**
 * 고객 화면 사진 자리 (CR-153). 자리 이름과 사진은 관리자 화면에서 정하고, 여기는 화면 칸이 쓰는 자리 키만 둔다.
 */
export type SitePhotoSlot =
  | 'HOME_INTRO'
  | 'HOME_EMPTY'
  | 'HOME_PROMO_REGISTRATION'
  | 'HOME_PROMO_SITE_VISIT'
  | 'LOGIN'
  | 'REGISTER'
  | 'SERVICE_DESK'
  | 'SERVICE_ONLINE'
  | 'SERVICE_MEETING'
  | 'SERVICE_SEMINAR'

const CACHE_KEY = 'wjbid.sitePhotos'

let slots: SitePhotoSlots | null = null
const picks = new Map<SitePhotoSlot, string | null>()
const listeners = new Set<() => void>()

function readCache(): SitePhotoSlots | null {
  try {
    const raw = localStorage.getItem(CACHE_KEY)
    const parsed: unknown = raw ? JSON.parse(raw) : null
    return parsed && typeof parsed === 'object' ? (parsed as SitePhotoSlots) : null
  } catch {
    return null
  }
}

function writeCache(value: SitePhotoSlots) {
  try {
    localStorage.setItem(CACHE_KEY, JSON.stringify(value))
  } catch {
    // 남기지 못하면 다음 방문에 목록이 올 때까지 사진 없이 보일 뿐이다
  }
}

/**
 * 앱이 열릴 때 한 번 부른다. 화면은 목록을 기다리지 않고 먼저 그린다.
 * 남겨 둔 목록이 있으면 그것으로 바로 고르고, 새 목록은 다음 방문을 위해 남겨 두기만 한다.
 * 보던 중에 사진이 바뀌지 않게 하려는 것이다(CR-153 4.5).
 */
export function startSitePhotos() {
  slots = readCache()
  fetchSitePhotos()
    .then((fresh) => {
      writeCache(fresh)
      if (!slots) {
        slots = fresh
        listeners.forEach((notify) => notify())
      }
    })
    .catch(() => {
      // 목록을 못 받으면 사진 없이 그린다(BIZ-067)
    })
}

/** 자리마다 한 번만 골라 방문 동안 화면을 옮겨도 같은 사진이다 */
function pick(slot: SitePhotoSlot): string | null {
  if (!slots) return null
  if (!picks.has(slot)) {
    const candidates = slots[slot]
    const urls = Array.isArray(candidates) ? candidates.filter((u): u is string => typeof u === 'string' && u !== '') : []
    picks.set(slot, urls.length ? urls[Math.floor(Math.random() * urls.length)] : null)
  }
  return picks.get(slot) ?? null
}

function subscribe(listener: () => void) {
  listeners.add(listener)
  return () => {
    listeners.delete(listener)
  }
}

/** 이 자리에 보일 사진 주소. 목록이 아직 없거나 보일 사진이 없으면 null */
export function useSitePhoto(slot: SitePhotoSlot): string | null {
  return useSyncExternalStore(subscribe, () => pick(slot))
}
