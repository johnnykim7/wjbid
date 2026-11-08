/**
 * 서버 실패 응답 흉내.
 *
 * 서버·DB·밤 작업이 실패하는 상황은 공용 개발기에 장애를 넣지 않고는 만들 수 없다. 화면에 뜨는
 * 실패 안내는 그 요청 하나만 서버로 보내지 않고 실패 응답을 돌려줘 판정한다. 나머지 요청은 그대로
 * 서버로 간다. 서버가 되돌리고 기록을 남기는지는 여기서 보지 않는다. 그것은 단위시험 몫이다.
 *
 *   afterEach(() => fake.reset());
 *
 *   fake.fail('POST', /\/api\/v1\/payments\/prepare(\?|$)/, 500,
 *     { success: false, data: null, error: { code: 50000, message: '일시적인 오류가 발생했습니다' } },
 *     '결제 건 저장이 실패하면 공통 예외 처리가 50000 으로 답한다(GlobalExceptionHandler)');
 *   ... 화면 조작 ...
 *   judge(id, 1, verdict, `${fake.said()}. 결제창이 열리지 않는지`, `가로챈 요청 ${fake.hits()}회 ...`);
 *
 * 흉내 낸 응답은 서버가 그 실패에서 실제로 돌려주는 것과 같아야 한다. 서버 소스에서 그 실패가 어느
 * 상태 코드와 결과 코드로 나가는지 찾아 body 에 옮기고 why 에 근거를 적는다. 판정 근거(asked)에
 * said() 를 붙여 무엇을 흉내 냈는지 남긴다.
 *
 * fetch 와 XMLHttpRequest 를 둘 다 가로챈다. axios 는 화면 시험 환경(jsdom, happy-dom)에서
 * XMLHttpRequest 로 부른다. 화면 시험이 MSW 같은 가짜 응답기를 이미 깔아 두었으면 그 응답기가 요청을
 * 먼저 받아 여기까지 오지 않는다. 그런 레포는 fail() 이 그 응답기에 실패 처리를 얹도록 바꾼다.
 *
 * 시험 파일의 다른 fetch 가로채기보다 나중에 부르면 그것보다 먼저 받는다. setup 의 쿠키 처리는
 * 테스트 시작 전에 걸리므로 fail() 은 테스트 안이나 beforeEach 에서 부른다.
 */

interface Rule {
  method: string;
  url: RegExp;
  status: number;
  body: unknown;
  why: string;
  hits: number;
}

const rules: Rule[] = [];
let installed = false;

/** 나중에 건 규칙이 먼저 받는다 */
function match(method: string, url: string): Rule | undefined {
  for (let i = rules.length - 1; i >= 0; i -= 1) {
    const r = rules[i];
    if (r.method === method && r.url.test(url)) return r;
  }
  return undefined;
}

/** 보내지 않은 요청에 응답이 온 것처럼 상태를 채우고 끝났다는 알림을 낸다. axios 는 loadend 로 받는다 */
function answer(xhr: XMLHttpRequest, url: string, r: Rule) {
  const text = JSON.stringify(r.body);
  const own: Record<string, unknown> = {
    readyState: 4, status: r.status, statusText: '', responseURL: url, responseText: text,
    response: xhr.responseType === 'json' ? r.body : text,
    getAllResponseHeaders: () => 'content-type: application/json\r\n',
    getResponseHeader: (name: string) => (name.toLowerCase() === 'content-type' ? 'application/json' : null),
  };
  for (const [key, value] of Object.entries(own)) Object.defineProperty(xhr, key, { value });
  setTimeout(() => {
    for (const type of ['readystatechange', 'load', 'loadend']) xhr.dispatchEvent(new ProgressEvent(type));
  });
}

function install() {
  if (installed) return;
  const passThrough = globalThis.fetch;
  globalThis.fetch = (async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = input instanceof Request ? input.url : String(input);
    const method = (init?.method ?? (input instanceof Request ? input.method : 'GET')).toUpperCase();
    const r = match(method, url);
    if (!r) return passThrough(input, init);
    r.hits += 1;
    return new Response(JSON.stringify(r.body), {
      status: r.status,
      headers: { 'Content-Type': 'application/json' },
    });
  }) as typeof fetch;

  const Base = globalThis.XMLHttpRequest;
  if (Base) {
    globalThis.XMLHttpRequest = class extends Base {
      private asked = { method: 'GET', url: '' };

      open(method: string, url: string | URL, async = true, username?: string | null, password?: string | null) {
        this.asked = { method: method.toUpperCase(), url: String(url) };
        super.open(method, url, async, username, password);
      }

      send(body?: Document | XMLHttpRequestBodyInit | null) {
        const r = match(this.asked.method, this.asked.url);
        if (!r) return super.send(body);
        r.hits += 1;
        answer(this, this.asked.url, r);
      }
    };
  }
  installed = true;
}

/** 판정 근거에 남길 주소. 정규식 기호를 걷어 사람이 읽는 경로로 쓴다 */
function readable(url: RegExp): string {
  return url.source.replace(/\\\//g, '/').replace(/\(\\\?\|\$\)|[\^$]/g, '');
}

export const fake = {
  /** 그 요청을 서버로 보내지 않고 status 와 body 로 답한다. why 는 서버가 그렇게 답하는 근거다 */
  fail(method: string, url: RegExp, status: number, body: unknown, why: string) {
    install();
    rules.push({ method: method.toUpperCase(), url, status, body, why, hits: 0 });
  },

  /** 흉내 낸 요청이 화면에서 몇 번 나갔나 */
  hits(): number {
    return rules.reduce((n, r) => n + r.hits, 0);
  },

  /** 판정 근거에 붙일 한 줄. 무엇을 어떤 응답으로 흉내 냈는지 */
  said(): string {
    return rules
      .map((r) => `${r.method} ${readable(r.url)} 요청을 서버로 보내지 않고 ${r.status} 응답으로 흉내(${r.why})`)
      .join(' / ');
  },

  reset() {
    rules.length = 0;
  },
};
