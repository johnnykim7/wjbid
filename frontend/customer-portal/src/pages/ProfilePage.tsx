import { useState, useEffect } from 'react'
import { Button } from '../components/ui/button'
import { LoadingSpinner } from '../components/ui/loading-spinner'
import { getMyProfile, updateMyProfile } from '../api/client'
import type { MemberProfile } from '../types'

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
    <div className="p-6">
      <div className="max-w-4xl mx-auto">
        <div className="flex justify-between items-center mb-6">
          <div>
            <h2 className="text-xl font-bold text-gray-800">기업 프로필 설정</h2>
            <p className="text-sm text-gray-500 mt-1">
              이 정보를 바탕으로 귀사에 최적화된 제안서를 작성합니다.
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
          <div className="mb-4 px-4 py-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-600">
            {error}
          </div>
        )}

        <div className="bg-white border border-gray-200 rounded-xl shadow-sm p-8 space-y-8">
          {/* 계정 정보 (읽기 전용) */}
          <section>
            <h3 className="text-lg font-bold text-gray-800 border-b border-gray-200 pb-2 mb-4">
              계정 정보
            </h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">이메일</label>
                <input
                  type="email"
                  value={profile?.email ?? ''}
                  disabled
                  className="w-full px-4 py-2 bg-gray-100 border border-gray-200 rounded-lg text-sm text-gray-500 cursor-not-allowed"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">역할</label>
                <input
                  type="text"
                  value={profile?.role ?? ''}
                  disabled
                  className="w-full px-4 py-2 bg-gray-100 border border-gray-200 rounded-lg text-sm text-gray-500 cursor-not-allowed"
                />
              </div>
            </div>
          </section>

          {/* 기업 정보 */}
          <section>
            <h3 className="text-lg font-bold text-gray-800 border-b border-gray-200 pb-2 mb-4">
              기업 정보
            </h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  회사명 <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  value={form.companyName}
                  onChange={(e) => handleChange('companyName', e.target.value)}
                  className="w-full px-4 py-2 bg-gray-50 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-secondary focus:bg-white transition text-sm"
                  placeholder="예: VOOMERANG Co., Ltd."
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  사업자 등록번호
                </label>
                <input
                  type="text"
                  value={form.businessRegistrationNumber}
                  onChange={(e) => handleChange('businessRegistrationNumber', e.target.value)}
                  className="w-full px-4 py-2 bg-gray-50 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-secondary focus:bg-white transition text-sm font-mono"
                  placeholder="000-00-00000"
                />
              </div>
              <div className="md:col-span-2">
                <label className="block text-sm font-medium text-gray-700 mb-1">주소</label>
                <input
                  type="text"
                  value={form.address}
                  onChange={(e) => handleChange('address', e.target.value)}
                  className="w-full px-4 py-2 bg-gray-50 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-secondary focus:bg-white transition text-sm"
                  placeholder="예: 서울특별시 강남구 ..."
                />
              </div>
            </div>
          </section>

          {/* 담당자 정보 */}
          <section>
            <h3 className="text-lg font-bold text-gray-800 border-b border-gray-200 pb-2 mb-4">
              담당자 정보
            </h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">담당자명</label>
                <input
                  type="text"
                  value={form.contactPerson}
                  onChange={(e) => handleChange('contactPerson', e.target.value)}
                  className="w-full px-4 py-2 bg-gray-50 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-secondary focus:bg-white transition text-sm"
                  placeholder="예: 김철수"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">전화번호</label>
                <input
                  type="tel"
                  value={form.phone}
                  onChange={(e) => handleChange('phone', e.target.value)}
                  className="w-full px-4 py-2 bg-gray-50 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-secondary focus:bg-white transition text-sm"
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
