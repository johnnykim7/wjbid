import { useState, useEffect } from 'react'
import { Button, Input, Label } from '@therecommerce-develop/ds-ui'
import { LoadingSpinner } from '../components/ui/loading-spinner'
import { getMyProfile, updateMyProfile } from '../api/client'
import { Link } from 'react-router-dom'
import type { MemberProfile } from '../types'
import { MEMBERSHIP_TEXT, PRICING_PATH } from '../data/membershipContent'

/** 회원 구분 (CR-157). 관리자는 기간 없이 회원과 같다 */
function membershipLabel(profile: MemberProfile | null) {
  if (!profile) return ''
  if (profile.role === 'ADMIN') return MEMBERSHIP_TEXT.profile.admin
  const endsOn = profile.membership?.endsOn
  return profile.membership?.member && endsOn ? MEMBERSHIP_TEXT.profile.member(endsOn) : MEMBERSHIP_TEXT.profile.free
}

type FormState = {
  companyName: string
  businessRegistrationNumber: string
  contactPerson: string
  phone: string
  address: string
}

export default function ProfilePage() {
  const [profile, setProfile] = useState<MemberProfile | null>(null)
  const [form, setForm] = useState<FormState>({
    companyName: '',
    businessRegistrationNumber: '',
    contactPerson: '',
    phone: '',
    address: '',
  })
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [saved, setSaved] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const load = async () => {
      try {
        const { data } = await getMyProfile()
        setProfile(data)
        setForm({
          companyName: data.companyName ?? '',
          businessRegistrationNumber: data.businessRegistrationNumber ?? '',
          contactPerson: data.contactPerson ?? '',
          phone: data.phone ?? '',
          address: data.address ?? '',
        })
      } catch {
        setError('프로필을 불러오는 데 실패했습니다.')
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [])

  const handleChange = (field: keyof FormState, value: string) => {
    setForm((f) => ({ ...f, [field]: value }))
    setSaved(false)
  }

  const handleSave = async () => {
    setSaving(true)
    try {
      await updateMyProfile({
        companyName: form.companyName,
        contactPerson: form.contactPerson || undefined,
        phone: form.phone || undefined,
        address: form.address || undefined,
      })
      setSaved(true)
      setTimeout(() => setSaved(false), 2000)
    } catch {
      setError('저장에 실패했습니다. 다시 시도해 주세요.')
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <LoadingSpinner fullPage />

  return (
    <div className="space-y-6">
      <div className="max-w-4xl mx-auto">
        <div className="flex justify-between items-center mb-6">
          <div>
            <h2 className="text-xl font-bold text-ink">회사 정보</h2>
            <p className="text-sm text-ink-mute mt-1">
              이 정보는 제안서 작성과 입찰 자격 진단에 쓰입니다.
            </p>
          </div>
          <Button variant="primary" onClick={handleSave} disabled={saving}>
            {saved ? (
              <><i className="fa-solid fa-check mr-1" /> 저장됨</>
            ) : saving ? (
              <><i className="fa-solid fa-spinner fa-spin mr-1" /> 저장 중...</>
            ) : (
              '저장하기'
            )}
          </Button>
        </div>

        {error && (
          <div className="mb-4 px-4 py-3 bg-danger-soft border border-danger/30 rounded-lg text-sm text-danger">
            {error}
          </div>
        )}

        <div className="bg-white border border-line rounded-xl shadow-sm p-8 space-y-8">
          {/* 계정 정보 (읽기 전용) */}
          <section>
            <h3 className="text-lg font-bold text-ink border-b border-line pb-2 mb-4">
              계정 정보
            </h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <Label className="block text-sm font-medium text-ink-sub mb-1">이메일</Label>
                <Input
                  type="email"
                  value={profile?.email ?? ''}
                  disabled
                />
              </div>
              <div>
                <Label className="block text-sm font-medium text-ink-sub mb-1">{MEMBERSHIP_TEXT.profile.label}</Label>
                <Input type="text" value={membershipLabel(profile)} disabled />
                {profile && profile.role !== 'ADMIN' && !profile.membership?.member && (
                  <Link to={PRICING_PATH} className="inline-block mt-1 text-sm text-brand hover:underline">
                    {MEMBERSHIP_TEXT.pricing}
                  </Link>
                )}
              </div>
            </div>
          </section>

          {/* 회사 정보 */}
          <section>
            <h3 className="text-lg font-bold text-ink border-b border-line pb-2 mb-4">
              회사 정보
            </h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <Label className="block text-sm font-medium text-ink-sub mb-1">
                  회사명 <span className="text-danger">*</span>
                </Label>
                <Input
                  type="text"
                  value={form.companyName}
                  onChange={(e) => handleChange('companyName', e.target.value)}
                  placeholder="예: VOOMERANG Co., Ltd."
                />
              </div>
              <div>
                <Label className="block text-sm font-medium text-ink-sub mb-1">
                  사업자 등록번호
                </Label>
                <Input
                  type="text"
                  value={form.businessRegistrationNumber}
                  onChange={(e) => handleChange('businessRegistrationNumber', e.target.value)}
                  className="font-mono"
                  placeholder="000-00-00000"
                />
              </div>
              <div className="md:col-span-2">
                <Label className="block text-sm font-medium text-ink-sub mb-1">주소</Label>
                <Input
                  type="text"
                  value={form.address}
                  onChange={(e) => handleChange('address', e.target.value)}
                  placeholder="예: 서울특별시 강남구 ..."
                />
              </div>
            </div>
          </section>

          {/* 담당자 정보 */}
          <section>
            <h3 className="text-lg font-bold text-ink border-b border-line pb-2 mb-4">
              담당자 정보
            </h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <Label className="block text-sm font-medium text-ink-sub mb-1">담당자명</Label>
                <Input
                  type="text"
                  value={form.contactPerson}
                  onChange={(e) => handleChange('contactPerson', e.target.value)}
                  placeholder="예: 김철수"
                />
              </div>
              <div>
                <Label className="block text-sm font-medium text-ink-sub mb-1">전화번호</Label>
                <Input
                  type="tel"
                  value={form.phone}
                  onChange={(e) => handleChange('phone', e.target.value)}
                  placeholder="예: 010-1234-5678"
                />
              </div>
            </div>
          </section>
        </div>
      </div>
    </div>
  )
}
