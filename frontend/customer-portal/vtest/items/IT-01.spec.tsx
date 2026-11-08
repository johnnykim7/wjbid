import { afterEach, test } from 'vitest'
import { judge, noMaterial, reportTo } from '../report'
import { fake } from '../fake'
import {
  apiPost, buttonNamed, click, dbRows, here, openScreen, screen, screenText, settled, text, traffic, typeInto, user,
} from '../harness'

reportTo('IT-01')

afterEach(() => {
  fake.reset()
  localStorage.clear()
})

const EC = /\/api\/auth\/email-codes(\?|$)/
const EC_VERIFY = /\/api\/auth\/email-codes\/verify(\?|$)/
const PW_RESET = /\/api\/auth\/password-reset(\?|$)/
// 재설정 화면 하나에 쓰는 시험 이메일. 흉내 낸 응답만 받으므로 메일이 나가지 않는다
const FAKE_EMAIL = 'qa-cr143@example.com'

const input = (id: string) => document.getElementById(id) as HTMLInputElement | null
const hintOf = (id: string) => document.getElementById(`${id}-hint`)
// 빨간 테두리는 wjbid-auth.css 의 .in[aria-invalid="true"] 규칙이 칠한다. 가상 화면은 색을 계산하지 않아 그 규칙이 걸리는 표시로 본다
const redBorder = (el: HTMLInputElement | null) =>
  !!el && el.classList.contains('in') && el.getAttribute('aria-invalid') === 'true' && !!el.closest('.au')?.parentElement?.closest('.wj')
const partOf = (title: string) =>
  Array.from(document.querySelectorAll('.part')).find((p) => text(p.querySelector('h2')).includes(title)) ?? null
// 칸 이름 뒤에 붙는 필수 표시(*)는 떼고 본다
const labelText = (el: Element | null | undefined) => text(el).replace(/\s*\*$/, '')
const resetCalls = () => traffic.calls.filter((c) => c.includes('/auth/password-reset')).length

/** 서버 소스의 응답 모양 그대로다(AuthFlowResponses.EmailCodeSent, EmailCodeVerified) */
function fakeVerified() {
  fake.fail('POST', EC, 200, { expiresInSeconds: 600, resendAfterSeconds: 60 },
    '앞 조건(이메일 확인을 마친 화면)을 만드는 보내기 성공 응답. 실제 보내기는 IT-01-073 이 본다')
  fake.fail('POST', EC_VERIFY, 200, { verified: true },
    '앞 조건을 만드는 확인 성공 응답. 실제 확인은 IT-01-073 이 본다')
}

/** 재설정 화면에서 이메일 확인까지 마친다. 흉내 응답을 건 뒤에 부른다 */
async function verifyOnScreen(email: string, code: string) {
  await typeInto(input('reset-email')!, email)
  await click(buttonNamed('인증번호 받기')!)
  await typeInto(input('reset-email-code')!, code)
  await click(buttonNamed('확인')!)
}

function latestResetCode(email: string): string | null | undefined {
  const rows = dbRows(
    "SELECT REGEXP_SUBSTR(content, '(?<![0-9])[0-9][0-9][0-9][0-9][0-9][0-9](?![0-9])') AS code " +
    `FROM notification_db.message_logs WHERE recipient = '${email}' AND template_code = 'BID_PASSWORD_RESET_CODE' ORDER BY id DESC LIMIT 1`)
  if (rows === null) return undefined
  return rows[0]?.code ?? null
}

const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms))

test('IT-01-073 가입한 이메일로 인증번호를 확인하고 비밀번호를 바꾼다', async () => {
  const ID = 'IT-01-073'
  const lines = [4, 5, 6, 7, 8]
  // 서버 판정과 메일 하나의 하루 한도를 나눠 쓰지 않게 다른 시험 고객으로 돈다
  const email = (process.env.VTEST_RESET_EMAIL ?? '').toLowerCase()
  const p1 = process.env.VTEST_RESET_PASSWORD ?? ''
  if (!/^[a-z0-9._-]+@[a-z0-9.-]+$/.test(email) || !p1 || !process.env.TESTBOOK_DB_CMD) {
    for (const n of lines) noMaterial(ID, n, '재설정 시험 고객(VTEST_RESET_EMAIL, VTEST_RESET_PASSWORD)이나 DB 조회 명령(TESTBOOK_DB_CMD)이 없다')
    return
  }
  const p2 = `${p1}v2`
  let sentAt = 0
  let changed = false
  try {
    await openScreen('/login')
    await click(screen.getByRole('link', { name: '비밀번호 재설정' }))
    await typeInto(input('reset-email')!, email)
    await click(buttonNamed('인증번호 받기')!)
    sentAt = Date.now()
    const code = latestResetCode(email)
    if (!code || !input('reset-email-code')) {
      for (const n of lines) noMaterial(ID, n, `인증번호를 받지 못했다. 화면 ${screenText().slice(0, 300)}`)
      return
    }
    await typeInto(input('reset-email-code')!, code)
    await click(buttonNamed('확인')!)

    const good = hintOf('reset-email')
    const pw = input('reset-password')
    const pwc = input('reset-password-confirm')
    judge(ID, 4, good?.classList.contains('good') && text(good).includes('이메일이 확인되었습니다') && pw && !pw.disabled && pwc && !pwc.disabled ? 'pass' : 'fail',
      '[확인] 뒤 이메일 칸 아래 초록 줄(hint good)의 글자와 새 비밀번호 두 칸이 열렸는지',
      `줄 "${text(good)}" 칸 ${good?.className ?? '없음'}, 새 비밀번호 ${pw ? (pw.disabled ? '닫힘' : '열림') : '없음'}, 확인 ${pwc ? (pwc.disabled ? '닫힘' : '열림') : '없음'}`)

    await typeInto(pw!, p2)
    await typeInto(pwc!, p2)
    const submit = buttonNamed('비밀번호 바꾸기')!
    await user.click(submit)
    const during = buttonNamed(/바꾸는 중|비밀번호 바꾸기/)
    const waiting = traffic.pending
    judge(ID, 5, text(during) === '바꾸는 중' && !!during?.disabled && waiting > 0 ? 'pass' : 'fail',
      '[비밀번호 바꾸기] 를 누르고 응답을 기다리는 동안 단추 글자와 눌림',
      `단추 "${text(during)}" ${during?.disabled ? '눌리지 않음' : '눌림'}, 기다리는 요청 ${waiting}개`)
    await settled()
    changed = here() === '/login'

    const box = document.querySelector('form.card')
    const msg = box?.querySelector('.msg.good')
    judge(ID, 6, here() === '/login' && text(msg) === '비밀번호를 바꿨습니다. 새 비밀번호로 로그인해 주세요.' ? 'pass' : 'fail',
      '바꾼 뒤 열린 화면 주소와 로그인 상자 안 초록 줄(msg good)의 글자', `주소 ${here()}, 줄 "${text(msg)}"`)
    const loginEmail = input('login-email')
    judge(ID, 7, loginEmail?.value === email ? 'pass' : 'fail', '로그인 화면 이메일 칸에 채워진 값', `"${loginEmail?.value ?? '칸 없음'}"`)

    await typeInto(input('login-password')!, p2)
    await click(buttonNamed('로그인')!)
    judge(ID, 8, here() === '/search' ? 'pass' : 'fail', '바꾼 비밀번호로 [로그인] 을 누른 뒤 열린 화면 주소', `주소 ${here()}`)
  } finally {
    if (sentAt) {
      // 바꾼 비밀번호를 처음 값으로 되돌린다. 같은 메일로 다시 받으려면 60초를 기다려야 한다(POL-037)
      await sleep(Math.max(0, sentAt + 62000 - Date.now()))
      const sent = await apiPost('/auth/email-codes', { email, purpose: 'PASSWORD_RESET' })
      const code = sent.status === 200 ? latestResetCode(email) : null
      const ok = code
        && (await apiPost('/auth/email-codes/verify', { email, code, purpose: 'PASSWORD_RESET' })).status === 200
        && (await apiPost('/auth/password-reset', { email, newPassword: p1 })).status === 200
      if (!ok && changed) throw new Error(`시험 고객 ${email} 의 비밀번호를 되돌리지 못했다. 보내기 ${sent.status}`)
    }
  }
})

test('IT-01-074 비밀번호 재설정 화면의 구역과 칸, 들어오고 나가는 길을 본다', async () => {
  const ID = 'IT-01-074'
  await openScreen('/password-reset')

  const h1 = document.querySelector('form.card h1')
  const lead = document.querySelector('form.card p.lead')
  judge(ID, 1, text(h1) === '비밀번호 재설정' && text(lead) === '가입한 이메일로 인증번호를 받아 새 비밀번호를 정합니다' ? 'pass' : 'fail',
    '상자 머리의 제목과 그 아래 줄', `제목 "${text(h1)}", 줄 "${text(lead)}"`)

  const part1 = partOf('이메일 확인')
  const row = input('reset-email')?.closest('.inrow')
  const send = row ? Array.from(row.querySelectorAll('button')).find((b) => text(b) === '인증번호 받기') : undefined
  judge(ID, 2, !!part1 && text(part1.querySelector('h2')) === '1이메일 확인' && !!part1.querySelector('#reset-email')
    && !!send && send.className.includes('border') ? 'pass' : 'fail',
    '1 이메일 확인 구역의 이메일 칸과 같은 줄의 단추 글자, 단추 테두리(outline 단추의 border 줄)',
    `구역 "${text(part1?.querySelector('h2'))}", 이메일 칸 ${part1?.querySelector('#reset-email') ? '있음' : '없음'}, 단추 "${text(send)}" ${send?.className.includes('border') ? '테두리 있음' : '테두리 없음'}`)

  const part2 = partOf('새 비밀번호')
  const pwLabel = part2?.querySelector('label[for="reset-password"]')
  const pwcLabel = part2?.querySelector('label[for="reset-password-confirm"]')
  judge(ID, 3, text(part2?.querySelector('h2')) === '2새 비밀번호' && labelText(pwLabel) === '새 비밀번호' && text(hintOf('reset-password')) === '8자 이상'
    && labelText(pwcLabel) === '새 비밀번호 확인' && !!part2?.querySelector('#reset-password-confirm') ? 'pass' : 'fail',
    '2 새 비밀번호 구역의 칸 이름과 칸 아래 글자',
    `구역 "${text(part2?.querySelector('h2'))}", "${labelText(pwLabel)}" 아래 "${text(hintOf('reset-password'))}", "${labelText(pwcLabel)}"`)

  const pw = input('reset-password')!
  await user.type(pw, 'abcd1234')
  judge(ID, 4, pw.value === '' ? 'pass' : 'fail', '이메일 확인 전 새 비밀번호 칸에 글자를 넣어 본 뒤 칸의 값', `"${pw.value}" ${pw.disabled ? '닫힘' : '열림'}`)

  const submit = document.querySelector('form.card button[type="submit"]')
  judge(ID, 5, text(submit) === '비밀번호 바꾸기' ? 'pass' : 'fail', '보내기 단추 글자', `"${text(submit)}"`)

  await click(screen.getByRole('link', { name: '로그인으로 돌아가기' }))
  judge(ID, 6, here() === '/login' ? 'pass' : 'fail', '[로그인으로 돌아가기] 를 누른 뒤 화면 주소', `주소 ${here()}`)
})

test('IT-01-075 가입되지 않은 이메일에는 재설정 인증번호를 보내지 않는다', async () => {
  const ID = 'IT-01-075'
  // 서버 판정과 같은 가입되지 않은 주소. 서버가 404 로 막아 메일이 나가지 않는다
  const email = 'qa-not-registered-186@example.com'
  await openScreen('/password-reset')
  await typeInto(input('reset-email')!, email)
  await click(buttonNamed('인증번호 받기')!)

  const el = input('reset-email')
  const hint = hintOf('reset-email')
  const link = hint?.querySelector('a')
  judge(ID, 1, redBorder(el) && hint?.classList.contains('bad') && text(hint).startsWith('가입되지 않은 이메일입니다.') && text(link) === '회원가입' ? 'pass' : 'fail',
    '이메일 칸 테두리 표시와 칸 아래 빨간 줄(hint bad)의 글자와 링크',
    `칸 ${redBorder(el) ? '빨간 테두리 규칙 걸림' : '규칙 안 걸림'} aria-invalid=${el?.getAttribute('aria-invalid')}, 줄 "${text(hint)}", 링크 "${text(link)}"`)
  judge(ID, 2, input('reset-email-code') === null ? 'pass' : 'fail', '인증번호 칸이 있는지', input('reset-email-code') ? '있음' : '없음')

  if (!link) noMaterial(ID, 5, '[회원가입] 링크가 없다')
  else {
    await click(link)
    judge(ID, 5, here() === '/register' ? 'pass' : 'fail', '[회원가입] 을 누른 뒤 화면 주소', `주소 ${here()}`)
  }
})

test('IT-01-076 새 비밀번호가 8자 미만이거나 확인과 다르면 바꾸지 않는다', async () => {
  const ID = 'IT-01-076'
  fakeVerified()
  await openScreen('/password-reset')
  await verifyOnScreen(FAKE_EMAIL, '123456')
  const before = resetCalls()

  await typeInto(input('reset-password')!, 'abc1234')
  await typeInto(input('reset-password-confirm')!, 'abc1234')
  await click(buttonNamed('비밀번호 바꾸기')!)
  const pw = input('reset-password')
  judge(ID, 1, redBorder(pw) && text(hintOf('reset-password')) === '8자 이상 넣어 주세요' ? 'pass' : 'fail',
    `${fake.said()}. 같은 7자를 넣고 누른 뒤 새 비밀번호 칸 테두리 표시와 칸 아래 글자`,
    `칸 aria-invalid=${pw?.getAttribute('aria-invalid')}, 줄 "${text(hintOf('reset-password'))}"`)
  const afterShort = resetCalls()

  await typeInto(input('reset-password')!, 'abcd12345')
  await typeInto(input('reset-password-confirm')!, 'abcd12346')
  await click(buttonNamed('비밀번호 바꾸기')!)
  const pwc = input('reset-password-confirm')
  judge(ID, 2, redBorder(pwc) && text(hintOf('reset-password-confirm')) === '비밀번호가 같지 않습니다' ? 'pass' : 'fail',
    '8자와 다른 8자를 넣고 누른 뒤 새 비밀번호 확인 칸 테두리 표시와 칸 아래 글자',
    `칸 aria-invalid=${pwc?.getAttribute('aria-invalid')}, 줄 "${text(hintOf('reset-password-confirm'))}"`)

  const sent = resetCalls() - before
  judge(ID, 3, sent === 0 ? 'pass' : 'fail', '1번과 2번에 화면이 보낸 POST /auth/password-reset 수',
    `7자 때 ${afterShort - before}건, 둘 합쳐 ${sent}건`)
})

test('IT-01-078 확인 뒤 비밀번호를 바꾸기까지 남는 시간이 지나면 다시 인증하게 한다', async () => {
  const ID = 'IT-01-078'
  fakeVerified()
  // 확인 뒤 30분이 지나면 consumeVerified 가 EMAIL_NOT_VERIFIED 를 던지고 MemberExceptionHandler 가 400 으로 답한다(AuthError)
  fake.fail('POST', PW_RESET, 400, { error: 'EMAIL_NOT_VERIFIED', message: '이메일 인증을 마치지 않았습니다' },
    '확인 뒤 30분이 지나 서버가 EMAIL_NOT_VERIFIED 400 으로 답한다(EmailCodeService.consumeVerified, MemberExceptionHandler)')
  await openScreen('/password-reset')
  await verifyOnScreen(FAKE_EMAIL, '123456')

  const scrolled: Element[] = []
  const original = HTMLElement.prototype.scrollIntoView
  HTMLElement.prototype.scrollIntoView = function (this: HTMLElement) { scrolled.push(this) }
  try {
    await typeInto(input('reset-password')!, 'abcd12345')
    await typeInto(input('reset-password-confirm')!, 'abcd12345')
    await click(buttonNamed('비밀번호 바꾸기')!)
  } finally {
    HTMLElement.prototype.scrollIntoView = original
  }

  const hint = hintOf('reset-email')
  const last = scrolled[scrolled.length - 1]
  judge(ID, 1, text(hint) === '이메일 인증을 다시 해 주세요' && last?.id === 'reset-email' ? 'pass' : 'fail',
    `${fake.said()}. 이메일 칸 아래 글자와 화면을 옮긴 곳(scrollIntoView 를 부른 칸)`,
    `줄 "${text(hint)}", 옮긴 곳 ${last ? `#${last.id}` : '없음'}`)
  const el = input('reset-email')
  judge(ID, 2, !screenText().includes('이메일이 확인되었습니다') && !!el && !el.disabled ? 'pass' : 'fail',
    '초록 줄 "이메일이 확인되었습니다" 가 남았는지와 이메일 칸이 열렸는지',
    `초록 줄 ${screenText().includes('이메일이 확인되었습니다') ? '남음' : '없음'}, 이메일 칸 ${el ? (el.disabled ? '닫힘' : '열림') : '없음'}`)
})

test('IT-01-079 비밀번호 바꾸기 중 그 밖의 서버 오류는 정해진 문구로 알린다', async () => {
  const ID = 'IT-01-079'
  const serverText = '일시적인 서버 오류입니다. 잠시 후 다시 시도해 주세요.'
  fakeVerified()
  fake.fail('POST', PW_RESET, 500,
    { status: 500, error: 'Internal Server Error', message: serverText, path: '/api/auth/password-reset' },
    '비밀번호 저장 중 DB 오류면 DataAccessExceptionHandler 가 500 으로 답한다')
  await openScreen('/password-reset')
  await verifyOnScreen(FAKE_EMAIL, '123456')
  await typeInto(input('reset-password')!, 'abcd12345')
  await typeInto(input('reset-password-confirm')!, 'abcd12345')
  await click(buttonNamed('비밀번호 바꾸기')!)

  const alert = document.querySelector('form.card [role="alert"]')
  const submit = document.querySelector('form.card button[type="submit"]')
  const above = !!alert && !!submit && !!(alert.compareDocumentPosition(submit) & Node.DOCUMENT_POSITION_FOLLOWING)
  judge(ID, 1, alert?.classList.contains('err') && text(alert) === '비밀번호를 바꾸지 못했습니다. 잠시 뒤 다시 시도해 주세요.' && above ? 'pass' : 'fail',
    `${fake.said()}. 빨간 줄(msg err)의 글자와 단추보다 위에 있는지`,
    `줄 "${text(alert)}" ${alert?.className ?? ''}, ${above ? '단추 위' : '단추 위 아님'}`)
  const page = screenText()
  judge(ID, 2, !page.includes(serverText) && !page.includes('Internal Server Error') ? 'pass' : 'fail',
    '서버가 보낸 오류 문장이 화면 글자에 있는지', page.includes(serverText) || page.includes('Internal Server Error') ? '있음' : '없음')
  judge(ID, 3, here() === '/password-reset' ? 'pass' : 'fail', '오류 뒤 화면 주소', `주소 ${here()}`)
})

test('IT-01-081 재요청 간격은 가입 인증번호와 재설정 인증번호를 가리지 않고 메일 하나로 센다', async () => {
  const ID = 'IT-01-081'
  // 가입 인증번호를 받은 지 60초 안이면 EmailCodeService.send 가 EMAIL_CODE_TOO_SOON 429 와 남은 초를 준다
  fake.fail('POST', EC, 429, { error: 'EMAIL_CODE_TOO_SOON', message: '잠시 뒤 다시 받을 수 있습니다', retryAfterSeconds: 40 },
    '가입 인증번호를 받은 지 60초가 안 된 메일이라 서버가 EMAIL_CODE_TOO_SOON 429 로 답한다(EmailCodeService.send)')
  await openScreen('/password-reset')
  await typeInto(input('reset-email')!, FAKE_EMAIL)
  await click(buttonNamed('인증번호 받기')!)
  const hint = hintOf('reset-email')
  judge(ID, 1, hint?.classList.contains('bad') && text(hint) === '잠시 뒤 다시 받을 수 있습니다' ? 'pass' : 'fail',
    `${fake.said()}. 이메일 칸 아래 줄의 글자`, `줄 "${text(hint)}" ${hint?.className ?? ''}`)
})
