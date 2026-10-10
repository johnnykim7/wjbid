import { Link, useSearchParams } from 'react-router-dom'
import { Button } from '@therecommerce-develop/ds-ui'

/** 메뉴에는 있으나 아직 화면이 없는 항목을 한 곳에서 안내한다 (CR-127, T3-3 「준비 중」) */
export default function ComingSoonPage() {
  const [params] = useSearchParams()
  const name = params.get('name')

  return (
    <div className="bg-white border border-line rounded-[14px] px-10 py-16 text-center">
      <span className="inline-flex items-center h-7 px-3 rounded bg-brand-deep text-white text-[13px] font-semibold">준비 중</span>
      <h1 className="mt-4 text-[28px] text-ink">{name ? `${name}, 준비 중입니다` : '준비 중입니다'}</h1>
      <p className="mt-2 text-[16px] text-ink-sub">
        화면을 만들고 있습니다. 그동안 아래에서 필요한 것을 찾아보세요.
      </p>
      <div className="mt-8 flex justify-center gap-2.5">
        <Button asChild variant="outline" size="lg"><Link to="/">홈</Link></Button>
        <Button asChild variant="outline" size="lg"><Link to="/search">마감 전 공고</Link></Button>
        <Button asChild variant="primary" size="lg" className="bg-brand hover:bg-brand-strong"><Link to="/support">1:1 문의</Link></Button>
      </div>
    </div>
  )
}
