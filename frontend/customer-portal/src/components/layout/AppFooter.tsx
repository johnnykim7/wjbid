import { useNavigate } from 'react-router-dom'
import { Footer } from '@therecommerce-develop/ds-ui'

/**
 * CR-124 — DS 기반 공통 푸터
 * 법적 4링크(약관/개인정보/환불/면책) + 사업자정보(전상법 표기) + 고객센터
 * ⚠️ 사업자정보 실값·고객센터 번호는 운영 확정 필요 (현재 플레이스홀더)
 */
export function AppFooter() {
  const navigate = useNavigate()

  return (
    <Footer
      companyInfo={{
        name: '우진USbid',
        ceo: '(대표자명)',
        businessNumber: '(사업자등록번호)',
        address: '(주소)',
        email: 'support@wjbid.com',
      }}
      linkGroups={[
        {
          title: '약관·정책',
          links: [
            { label: '이용약관', onClick: () => navigate('/terms') },
            { label: '개인정보처리방침', onClick: () => navigate('/privacy') },
            { label: '환불 정책', onClick: () => navigate('/refund') },
            { label: '면책 고지', onClick: () => navigate('/disclaimer') },
          ],
        },
        {
          title: '서비스',
          links: [
            { label: '이용 안내', onClick: () => navigate('/guide') },
            { label: '요금 안내', onClick: () => navigate('/pricing') },
            { label: '공지사항', onClick: () => navigate('/notices') },
            { label: 'FAQ', onClick: () => navigate('/faq') },
          ],
        },
      ]}
      customerService={{
        phone: '(고객센터 번호)',
        hours: '평일 09:00 ~ 18:00',
        email: 'support@wjbid.com',
      }}
      bottomText="© 2026 WJbid · 우진USbid · 미군·연방 조달 입찰 대행 서비스. 본 서비스는 낙찰을 보장하지 않으며 입찰서 최종 제출·서명은 고객 책임입니다."
    />
  )
}
