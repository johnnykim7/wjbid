import { useState } from 'react'
import { useSitePhoto, type SitePhotoSlot } from '../hooks/useSitePhoto'

/**
 * 고객 화면 사진 한 장 (CR-153). 고를 사진이 없거나 파일을 못 받으면 그리지 않아 칸 바탕색만 남는다(BIZ-067).
 */
export function SitePhoto({ slot }: { slot: SitePhotoSlot }) {
  const src = useSitePhoto(slot)
  const [failed, setFailed] = useState<string | null>(null)
  if (!src || failed === src) return null
  return <img src={src} alt="" onError={() => setFailed(src)} />
}
