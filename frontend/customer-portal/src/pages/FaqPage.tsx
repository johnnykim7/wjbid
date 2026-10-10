import { useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { Button } from '@therecommerce-develop/ds-ui'
import { Icon } from '../components/layout/Icon'
import { findMenuItem } from '../components/layout/menu'
import { PageHead, SubPageFrame } from '../components/subpage/SubPageFrame'
import { FAQS, FAQ_CATEGORIES, FAQ_TEXT, type FaqCategory } from '../data/supportContent'

const PLACE = findMenuItem('support', 'faq')
const CATEGORY_LABEL = Object.fromEntries(FAQ_CATEGORIES.map((c) => [c.key, c.label]))

/** 자주 묻는 질문 (CR-133, T3-3 「자주 묻는 질문」, 화면 정본 faq). 진입 때 첫 질문만 펼치고 분류를 바꾸면 모두 접는다 */
export default function FaqPage() {
  const [category, setCategory] = useState<FaqCategory | 'all'>('all')
  const [opened, setOpened] = useState<Set<string>>(() => new Set(FAQS.slice(0, 1).map((f) => f.id)))

  if (!PLACE) return <Navigate to="/" replace />
  const { group, item } = PLACE
  const items = category === 'all' ? FAQS : FAQS.filter((f) => f.category === category)

  const choose = (key: FaqCategory | 'all') => {
    setCategory(key)
    setOpened(new Set())
  }
  const toggle = (id: string) => {
    setOpened((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  return (
    <SubPageFrame group={group} item={item}>
      <PageHead title={FAQ_TEXT.title} desc={FAQ_TEXT.desc} />
      <section className="cbx pad">
        <div className="chips">
          {FAQ_CATEGORIES.map((c) => (
            <Button key={c.key} className={`wj-reset${category === c.key ? ' on' : ''}`} onClick={() => choose(c.key)}>{c.label}</Button>
          ))}
        </div>
        <div className="faqs">
          {items.map((f) => {
            const open = opened.has(f.id)
            return (
              <div key={f.id} className={open ? 'open' : undefined}>
                <Button className="wj-reset fq" aria-expanded={open} onClick={() => toggle(f.id)}>
                  <i>Q</i>{f.q}<small>{CATEGORY_LABEL[f.category]}</small><Icon name={open ? 'cu' : 'cd'} />
                </Button>
                {open && (
                  <div className="a">
                    <i>A</i>
                    <p>
                      {f.a.map((part, i) => typeof part === 'string'
                        ? part
                        : <Link key={i} to={part.to}>{part.label}</Link>)}
                    </p>
                  </div>
                )}
              </div>
            )
          })}
        </div>
      </section>
    </SubPageFrame>
  )
}
