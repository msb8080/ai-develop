export type Project = {
  id: string
  name: string
  sourceReference: string
}

export type Conversation = {
  id: string
  title: string
  projectId?: string
  updatedAt: string
}

export type Message = {
  id?: string
  role: 'user' | 'assistant'
  content: string
}

export type Skill = {
  id: string
  name: string
  description: string
  version: string
}

export type ModelStatus = {
  enabled: boolean
  provider: string
  model: string
  streamMode: string
}

export type SandboxStatus = {
  enabled: boolean
  approval: string
  network: string
  memory: string
  cpus: string
  timeout: string
}

export type SandboxAction = {
  id: string
  name: string
  description: string
}

export type SandboxJob = {
  id: string
  projectId: string
  action: string
  status: string
  requestedAt: string
  approvedAt?: string
  completedAt?: string
  exitCode?: number
  output?: string
  error?: string
}

export type StreamMetadata = {
  requestId: string
  runId?: string
  conversationId: string
  provider?: string
  model?: string
  skillId?: string
  contextSources?: string[]
  contextTruncated?: boolean
  phase?: string
  content?: string
  code?: string
  message?: string
}

export async function getJson<T>(url: string): Promise<T> {
  const response = await fetch(url)
  if (!response.ok) throw new Error(`请求失败：${response.status}`)
  return response.json() as Promise<T>
}

export async function createProject(name: string, relativePath: string): Promise<Project> {
  const response = await fetch('/api/projects', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, relativePath }),
  })
  if (!response.ok) {
    const detail = await response.json().catch(() => null)
    throw new Error(detail?.message ?? `项目创建失败：${response.status}`)
  }
  return response.json() as Promise<Project>
}

async function postJson<T>(url: string, body?: unknown): Promise<T> {
  const response = await fetch(url, {
    method: 'POST',
    headers: body === undefined ? undefined : { 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  if (!response.ok) {
    const detail = await response.json().catch(() => null)
    throw new Error(detail?.message ?? `请求失败：${response.status}`)
  }
  return response.json() as Promise<T>
}

export function planSandbox(projectId: string, action: string): Promise<SandboxJob> {
  return postJson('/api/sandbox/jobs', { projectId, action })
}

export function approveSandbox(id: string): Promise<SandboxJob> {
  return postJson(`/api/sandbox/jobs/${id}/approve`)
}

export function rejectSandbox(id: string): Promise<SandboxJob> {
  return postJson(`/api/sandbox/jobs/${id}/reject`)
}

export async function streamChat(
  input: { message: string; conversationId?: string; projectId?: string; skillId?: string },
  onEvent: (name: string, event: StreamMetadata) => void,
): Promise<void> {
  const response = await fetch('/api/chat/stream', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })
  if (!response.ok || !response.body) throw new Error(`对话请求失败：${response.status}`)

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  while (true) {
    const { value, done } = await reader.read()
    buffer += decoder.decode(value ?? new Uint8Array(), { stream: !done })
    const blocks = buffer.split(/\r?\n\r?\n/)
    buffer = blocks.pop() ?? ''
    for (const block of blocks) {
      let eventName = 'message'
      const data: string[] = []
      for (const line of block.split(/\r?\n/)) {
        if (line.startsWith('event:')) eventName = line.slice(6).trim()
        if (line.startsWith('data:')) data.push(line.slice(5).trim())
      }
      if (data.length) onEvent(eventName, JSON.parse(data.join('\n')) as StreamMetadata)
    }
    if (done) break
  }
}
