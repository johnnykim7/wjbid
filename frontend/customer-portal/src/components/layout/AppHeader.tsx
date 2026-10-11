import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { Button, Input, Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@therecommerce-develop/ds-ui'
import { useAuth } from '../../context/AuthContext'
import { CiLogo, Icon } from './Icon'
import { MENU, menuHref } from './menu'
import {
  LOGO_LABEL,
  RECOMMENDED_KEYWORDS,
  SEARCH_FIELDS,
  SEARCH_PLACEHOLDER,
  SERVICE_LINE,
  searchHref,
} from './siteContent'
import type { MemberSummary } from './useMemberSummary'

/**
 * 공통 틀 위쪽: 유틸 띠, 머리말(로고·검색·아이콘), 메뉴 띠, 전체메뉴 (CR-127).
 * 화면 정본과 클래스 이름을 맞춰 wjbid-aida.css 규칙이 그대로 걸리게 한다.
 */
export function AppHeader({ member }: { member: MemberSummary }) {
  const navigate = useNavigate()
  const { pathname, search } = useLocation()
  const { isAuthenticated, logout } = useAuth()
  const [menuOpen, setMenuOpen] = useState(false)
  const [field, setField] = useState(SEARCH_FIELDS[0].value)
  const [keyword, setKeyword] = useState('')
  const rootRef = useRef<HTMLDivElement>(null)

  useEffect(() => setMenuOpen(false), [pathname, search])

  useEffect(() => {
    if (!menuOpen) return
    const onDown = (e: MouseEvent) => {
      if (!rootRef.current?.contains(e.target as Node)) setMenuOpen(false)
    }
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setMenuOpen(false)
    }
    document.addEventListener('mousedown', onDown)
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('mousedown', onDown)
      document.removeEventListener('keydown', onKey)
    }
  }, [menuOpen])

  const onSearch = (e: FormEvent) => {
    e.preventDefault()
    navigate(searchHref(keyword.trim()))
  }

  const count = (n: number | null) => (isAuthenticated && n ? <span className="n">{n}</span> : null)

  return (
    <div ref={rootRef} className={`wj top${menuOpen ? ' menu-open' : ''}`}>
      <div className="util">
        <div className="wrap">
          <span>{SERVICE_LINE}</span>
          {isAuthenticated ? (
            <nav>
              <Link className="b" to="/profile">{member.companyName ?? ''} 님</Link>
              <Button className="wj-reset" onClick={logout}>로그아웃</Button>
              <Link to="/support">고객센터</Link>
            </nav>
          ) : (
            <nav>
              <Link className="b" to="/register">회원가입</Link>
              <Link to="/login">로그인</Link>
              <Link to="/guide">이용 안내</Link>
              <Link to="/support">고객센터</Link>
            </nav>
          )}
        </div>
      </div>
      <header className="hd">
        <div className="wrap">
          <Link className="logo" to="/" aria-label={LOGO_LABEL}>
            <CiLogo className="ci" />
          </Link>
          <div className="sch">
            <form className="box" onSubmit={onSearch} role="search">
              <Select value={field} onValueChange={setField}>
                <SelectTrigger className="wj-reset sel" aria-label="검색 구분">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {SEARCH_FIELDS.map((f) => (
                    <SelectItem key={f.value} value={f.value}>{f.label}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <Input
                className="wj-reset phd"
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                placeholder={SEARCH_PLACEHOLDER}
                aria-label="검색어"
              />
              <Button type="submit" className="wj-reset go" aria-label="검색"><Icon name="search" /></Button>
            </form>
            <div className="kw">
              <span>추천 검색어</span>
              {RECOMMENDED_KEYWORDS.map((k) => (
                <Link key={k} to={searchHref(k)}>{k}</Link>
              ))}
              <Link className="adv" to={searchHref()}>상세검색</Link>
            </div>
          </div>
          <div className="hico">
            <Link to="/bookmarks"><Icon name="star" />{count(member.bookmarkCount)}관심 공고</Link>
            <Link to="/notifications"><Icon name="bell" />{count(member.unreadCount)}알림</Link>
            <Link to="/documents"><Icon name="folder" />서류함</Link>
          </div>
        </div>
        <div className="gnb">
          <div className="wrap">
            <Button className="wj-reset all" aria-expanded={menuOpen} onClick={() => setMenuOpen((o) => !o)}>
              <Icon name="menu" />전체메뉴
            </Button>
            <nav>
              {MENU.map((g) => (
                <Link key={g.label} to={menuHref(g)}>{g.label}</Link>
              ))}
            </nav>
            <Link className="price" to="/pricing">요금 안내</Link>
          </div>
        </div>
        <div className="mega">
          <div className="wrap">
            {MENU.map((g) => (
              <div className="col" key={g.label}>
                <h5>{g.label}</h5>
                <ul>
                  {g.items.map((i) => (
                    <li key={i.label}>
                      <Link to={menuHref(i)} onClick={() => setMenuOpen(false)}>{i.label}</Link>
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </div>
        </div>
      </header>
    </div>
  )
}
