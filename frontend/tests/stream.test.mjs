import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { stripTypeScriptTypes } from 'node:module'

async function moduleUrl(file, demoUrl) {
  const source = await readFile(new URL(`../src/${file}`, import.meta.url), 'utf8')
  let code = stripTypeScriptTypes(source)
  code = code.replace('import.meta.env.VITE_DEMO_MODE', '"false"')
  if (demoUrl) code = code.replace("'./demo'", JSON.stringify(demoUrl))
  return `data:text/javascript;base64,${Buffer.from(code).toString('base64')}`
}
const demoUrl = await moduleUrl('demo.ts')
const { demoStream, demoGet } = await import(demoUrl)
const { streamChat } = await import(await moduleUrl('api.ts', demoUrl))

test('demo cancellation stops tokens and preserves partial history', async () => {
  const controller = new AbortController()
  const events = []
  let conversationId
  await assert.rejects(demoStream({ message: 'cancel regression', skillId: 'test' }, (name, event) => {
    events.push(name)
    conversationId = event.conversationId
    if (name === 'token') controller.abort()
  }, controller.signal), { name: 'AbortError' })
  assert.equal(events.filter(name => name === 'token').length, 1)
  assert.ok(!events.includes('done'))
  const history = demoGet(`/api/conversations/${conversationId}/messages`)
  assert.equal(history.length, 2)
  assert.ok(history[1].content.length > 0)
})

test('SSE handles split UTF-8, CRLF and trailing terminal frame', async () => {
  const originalFetch = globalThis.fetch
  const encoder = new TextEncoder()
  const bytes = encoder.encode('event: token\r\ndata: {"content":"你好"}\r\n\r\nevent: done\ndata: {}')
  const controller = new AbortController()
  globalThis.fetch = async (_url, options) => {
    assert.equal(options.signal, controller.signal)
    return new Response(new ReadableStream({ start(stream) {
      for (const byte of bytes) stream.enqueue(Uint8Array.of(byte))
      stream.close()
    } }))
  }
  try {
    const events = []
    await streamChat({message:'hello'}, (name, event) => events.push([name,event]), controller.signal)
    assert.deepEqual(events, [['token',{content:'你好'}],['done',{}]])
  } finally { globalThis.fetch = originalFetch }
})

test('SSE rejects a disconnected response without terminal event', async () => {
  const originalFetch = globalThis.fetch
  globalThis.fetch = async () => new Response('event: token\ndata: {"content":"partial"}\n\n')
  try {
    await assert.rejects(streamChat({message:'hello'}, () => {}), /响应提前中断/)
  } finally { globalThis.fetch = originalFetch }
})
