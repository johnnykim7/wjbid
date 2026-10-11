import { useMemo, useState, type FormEvent } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { Button, Input } from '@therecommerce-develop/ds-ui'
import { Icon } from '../components/layout/Icon'
import { findMenuItem } from '../components/layout/menu'
import { PageHead, SubPageFrame } from '../components/subpage/SubPageFrame'
import { GLOSSARY_TEXT, INDEX_LETTERS, TERMS } from '../data/academyContent'

const PLACE = findMenuItem('support', 'glossary')
const HAVE = new Set(TERMS.map((t) => t.abbr[0]))

/** 용어 사전 (CR-133, T3-3 「용어 사전」, 화면 정본 glossary). 찾는 말과 색인 글자는 함께 걸지 않는다(T3-4 시나리오 4) */
export default function AcademyGlossaryPage() {
  const [draft, setDraft] = useState('')
  const [query, setQuery] = useState('')
  const [letter, setLetter] = useState<string | null>(null)

  const list = useMemo(() => {
    const q = query.trim().toLowerCase()
    return TERMS.filter((t) => {
      if (letter) return t.abbr.startsWith(letter)
      if (!q) return true
      return [t.abbr, t.en, t.ko, t.desc].some((v) => v.toLowerCase().includes(q))
    })
  }, [query, letter])

  if (!PLACE) return <Navigate to="/" replace />
  const { group, item } = PLACE
  const t = GLOSSARY_TEXT

  const search = (e: FormEvent) => {
    e.preventDefault()
    setLetter(null)
    setQuery(draft)
  }
  const pick = (l: string | null) => {
    setDraft('')
    setQuery('')
    setLetter(l)
  }

  return (
    <SubPageFrame group={group} item={item}>
      <PageHead title={t.title} desc={t.desc} />
      <section className="cbx pad">
        <form className="gsch" onSubmit={search} role="search">
          <Input className="wj-reset in" value={draft} onChange={(e) => setDraft(e.target.value)} placeholder={t.placeholder} aria-label={t.title} />
          <Button type="submit" className="wj-reset bigb"><Icon name="search" />{t.search}</Button>
        </form>
        <div className="idx">
          <Button className={`wj-reset all${letter === null ? ' on' : ''}`} onClick={() => pick(null)}>{t.all}</Button>
          {INDEX_LETTERS.map((l) => (
            <Button
              key={l}
              className={`wj-reset${letter === l ? ' on' : HAVE.has(l) ? '' : ' nn'}`}
              disabled={!HAVE.has(l)}
              onClick={() => pick(l)}
            >
              {l}
            </Button>
          ))}
        </div>
        <div className="gcnt"><span>{t.countLabel} <b>{t.count(list.length)}</b></span><span>{t.basis}</span></div>
        <dl className="gls">
          {list.length === 0 && (
            <div className="none">{t.none}<Link to="/support">{t.noneAction}</Link></div>
          )}
          {list.map((term) => (
            <div key={term.abbr}>
              <dt><b className="mono">{term.abbr}</b><span>{term.en}</span><em>{term.ko}</em></dt>
              <dd>
                {term.desc}
                {term.related && <><br /><Link to={term.related.to}>{term.related.label}<Icon name="cr" /></Link></>}
              </dd>
            </div>
          ))}
        </dl>
      </section>
    </SubPageFrame>
  )
}
