import { FormEvent, useEffect, useMemo, useRef, useState } from 'react'
import { demoMode } from './demo'
import {
  Conversation,
  approveSandbox,
  createProject,
  getJson,
  Message,
  ModelStatus,
  Project,
  planSandbox,
  rejectSandbox,
  SandboxAction,
  SandboxJob,
  SandboxStatus,
  Skill,
  streamChat,
  StreamMetadata,
} from './api'

const starters = [
  '梳理这个 Java 项目的架构与主要风险',
  '审查当前项目，给出按优先级排序的问题',
  '分析 Spring Boot 启动失败最常见的证据链',
]

export default function App() {
  const [status, setStatus] = useState<ModelStatus>({ enabled: false, provider: 'none', model: 'none', streamMode: 'native' })
  const [projects, setProjects] = useState<Project[]>([])
  const [conversations, setConversations] = useState<Conversation[]>([])
  const [skills, setSkills] = useState<Skill[]>([])
  const [projectId, setProjectId] = useState('')
  const [conversationId, setConversationId] = useState('')
  const [skillId, setSkillId] = useState('')
  const [messages, setMessages] = useState<Message[]>([])
  const [input, setInput] = useState('')
  const [running, setRunning] = useState(false)
  const [phase, setPhase] = useState('ready')
  const [context, setContext] = useState<StreamMetadata | null>(null)
  const [notice, setNotice] = useState('')
  const [sandboxStatus, setSandboxStatus] = useState<SandboxStatus | null>(null)
  const [sandboxActions, setSandboxActions] = useState<SandboxAction[]>([])
  const [sandboxJobs, setSandboxJobs] = useState<SandboxJob[]>([])
  const [sandboxAction, setSandboxAction] = useState('backend-test')
  const [sandboxBusy, setSandboxBusy] = useState(false)
  const [projectForm, setProjectForm] = useState({ name: 'AI Develop', relativePath: '.' })
  const endRef = useRef<HTMLDivElement>(null)
  const abortRef = useRef<AbortController | null>(null)
  const conversationLoad = useRef(0)
  useEffect(() => () => abortRef.current?.abort(), [])

  function download(name: string, text: string, type: string) {
    const url = URL.createObjectURL(new Blob([text], {type}))
    const link = document.createElement('a'); link.href = url; link.download = name; link.click()
    setTimeout(() => URL.revokeObjectURL(url), 1000)
  }
  async function importSettings(file?: File) {
    if (!file) return
    try {
      if (file.size > 100_000) throw new Error('配置文件不能超过 100KB')
      const data: unknown = JSON.parse(await file.text())
      if (!data || typeof data !== 'object' || Array.isArray(data)) throw new Error('配置格式不正确')
      const value = data as Record<string, unknown>
      const requested = value.skillId ?? value.activeAgentId ?? 'general'
      if (typeof requested !== 'string' || !skills.some(skill => skill.id === requested)) throw new Error('角色尚未注册，请从列表选择。自定义角色请添加为服务端 Skill。')
      setSkillId(requested)
      setNotice('已导入角色偏好。旧模型密钥、服务地址、MCP 命令和自定义系统提示词不会导入；由服务端统一管理。')
    } catch (error) { setNotice(error instanceof Error ? error.message : '导入失败') }
  }

  const selectedProject = useMemo(
    () => projects.find((project) => project.id === projectId),
    [projects, projectId],
  )

  async function refresh() {
    const [nextStatus, nextProjects, nextConversations, nextSkills, nextSandboxStatus, nextSandboxActions, nextSandboxJobs] = await Promise.all([
      getJson<ModelStatus>('/api/chat/status'),
      getJson<Project[]>('/api/projects'),
      getJson<Conversation[]>('/api/conversations'),
      getJson<Skill[]>('/api/skills'),
      getJson<SandboxStatus>('/api/sandbox/status'),
      getJson<SandboxAction[]>('/api/sandbox/actions'),
      getJson<SandboxJob[]>('/api/sandbox/jobs'),
    ])
    setStatus(nextStatus)
    setProjects(nextProjects)
    setConversations(nextConversations)
    setSkills(nextSkills)
    setSandboxStatus(nextSandboxStatus)
    setSandboxActions(nextSandboxActions)
    setSandboxJobs(nextSandboxJobs)
    if (!projectId && nextProjects.length) setProjectId(nextProjects[0].id)
  }

  useEffect(() => {
    refresh().catch((error: Error) => setNotice(error.message))
  }, [])

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  async function openConversation(id: string) {
    if (running) return
    const loadId = ++conversationLoad.current
    try {
    const loaded = await getJson<Message[]>(`/api/conversations/${id}/messages`)
    if (loadId !== conversationLoad.current) return
    setConversationId(id)
    const conversation = conversations.find((item) => item.id === id)
    setProjectId(conversation?.projectId ?? '')
    setMessages(loaded)
    setContext(null)
    } catch (error) { setNotice(error instanceof Error ? error.message : '会话读取失败') }
  }

  async function addProject(event: FormEvent) {
    event.preventDefault()
    try {
      const project = await createProject(projectForm.name, projectForm.relativePath)
      setProjects((items) => [project, ...items])
      setProjectId(project.id)
      setNotice(`已接入 ${project.name}`)
    } catch (error) {
      setNotice(error instanceof Error ? error.message : '项目接入失败')
    }
  }

  async function submit(event?: FormEvent) {
    event?.preventDefault()
    const message = input.trim()
    if (!message || running || !status.enabled) return
    const controller = new AbortController()
    abortRef.current = controller
    conversationLoad.current++
    setInput('')
    setRunning(true)
    setNotice('')
    setPhase('context')
    setMessages((items) => [...items, { role: 'user', content: message }, { role: 'assistant', content: '' }])
    let nextConversationId = conversationId
    try {
      await streamChat(
        {
          message,
          conversationId: conversationId || undefined,
          projectId: conversationId ? undefined : projectId || undefined,
          skillId: skillId || undefined,
        },
        (name, data) => {
          if (name === 'metadata') {
            setContext(data)
            nextConversationId = data.conversationId
            setConversationId(data.conversationId)
          }
          if (name === 'phase') setPhase(data.phase ?? 'respond')
          if (name === 'token') {
            setMessages((items) => items.map((item, index) =>
              index === items.length - 1 ? { ...item, content: item.content + (data.content ?? '') } : item,
            ))
          }
          if (name === 'error') throw new Error(data.message ?? data.code ?? '模型响应失败')
          if (name === 'done') setPhase('done')
        },
        controller.signal,
      )
      setConversationId(nextConversationId)
      await refresh()
    } catch (error) {
      setNotice(controller.signal.aborted ? '已停止生成，已收到的内容已保留。' : error instanceof Error ? error.message : '模型响应失败')
      setPhase(controller.signal.aborted ? 'cancelled' : 'error')
      // A cancelled run still owns a persisted conversation; keep it reachable.
      try { setConversations(await getJson<Conversation[]>('/api/conversations')) } catch { /* Keep the original run error visible. */ }
    } finally {
      setRunning(false)
      abortRef.current = null
    }
  }

  async function createSandboxPlan() {
    if (!projectId) return setNotice('请先选择项目')
    try {
      const job = await planSandbox(projectId, sandboxAction)
      setSandboxJobs((items) => [job, ...items])
      setNotice('沙盒任务已生成，尚未执行；请核对后批准。')
    } catch (error) {
      setNotice(error instanceof Error ? error.message : '沙盒任务创建失败')
    }
  }

  async function decideSandbox(job: SandboxJob, approved: boolean) {
    if (approved && !window.confirm('批准后将启动一次无网络、资源受限的临时 Docker 容器。确认执行？')) return
    setSandboxBusy(true)
    setNotice(approved ? '沙盒正在执行…' : '')
    try {
      const updated = approved ? await approveSandbox(job.id) : await rejectSandbox(job.id)
      setSandboxJobs((items) => items.map((item) => item.id === updated.id ? updated : item))
      setNotice(approved ? `沙盒执行${updated.status === 'SUCCEEDED' ? '成功' : '结束'}：${updated.status}` : '沙盒任务已拒绝')
    } catch (error) {
      setNotice(error instanceof Error ? error.message : '沙盒操作失败')
    } finally {
      setSandboxBusy(false)
    }
  }

  const latestSandboxJob = sandboxJobs[0]

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <a className="brand" href="#top" aria-label="Rainbow AI Dev Copilot">
          <span className="brand-mark">R</span>
          <span><strong>Rainbow</strong><small>AI DEVELOP · OMNIAGENT</small></span>
        </a>

        <button className="new-chat" disabled={running} onClick={() => { conversationLoad.current++; setConversationId(''); setMessages([]); setContext(null); setNotice(''); setPhase('ready') }}>
          <span>＋</span> 新建分析
        </button>

        <div className="side-section">
          <p className="eyebrow">RECENT RUNS</p>
          <div className="conversation-list">
            {conversations.map((conversation) => (
              <button key={conversation.id} className={conversation.id === conversationId ? 'active' : ''}
                disabled={running}
                onClick={() => openConversation(conversation.id)}>
                <span>{conversation.title}</span><small>{new Date(conversation.updatedAt).toLocaleDateString()}</small>
              </button>
            ))}
            {!conversations.length && <p className="empty-copy">第一条对话会保存在这里。</p>}
          </div>
        </div>

        <form className="project-form" onSubmit={addProject}>
          <p className="eyebrow">LOCAL PROJECT</p>
          <input aria-label="项目名称" value={projectForm.name}
            onChange={(event) => setProjectForm({ ...projectForm, name: event.target.value })} />
          <div className="path-row">
            <input aria-label="相对路径" value={projectForm.relativePath}
              onChange={(event) => setProjectForm({ ...projectForm, relativePath: event.target.value })} />
            <button title="接入项目" disabled={demoMode || running}>↗</button>
          </div>
          <small>路径只能位于服务端允许的工作区内</small>
        </form>
      </aside>

      <main id="top" className="workspace">
        <header className="topbar">
          <div>
            <span className={`status-dot ${status.enabled ? 'online' : ''}`} />
            {status.enabled ? `${status.provider} / ${status.model} · ${status.streamMode}` : '模型未启用'}
          </div>
          <a href="https://msb8080.github.io/">个人主页 ↗</a>
        </header>
        {demoMode && <div className="notice" role="status">本地离线演示：回答为预设示例，不调用模型、不读取项目、不执行沙盒。刷新后清空数据；请勿输入敏感信息。</div>}

        <section className="hero">
          <p className="eyebrow violet">JAVA × AI ENGINEERING WORKBENCH</p>
          <h1>从代码证据出发，<br /><em>把问题说清楚。</em></h1>
          <p className="hero-copy">选择本地项目与分析 Skill。分析默认只读；构建与测试只能通过需要二次批准的隔离沙盒执行。</p>
        </section>

        <section className="control-deck">
          <label>项目上下文
            <select value={projectId} disabled={Boolean(conversationId)} onChange={(event) => setProjectId(event.target.value)}>
              <option value="">不加载项目</option>
              {projects.map((project) => <option key={project.id} value={project.id}>{project.name} · {project.sourceReference}</option>)}
            </select>
          </label>
          <label>按需 Skill
            <select value={skillId} disabled={running} onChange={(event) => setSkillId(event.target.value)}>
              <option value="">自动识别</option>
              {skills.map((skill) => <option key={skill.id} value={skill.id}>{skill.name}</option>)}
            </select>
          </label>
          <div className="run-state"><span>{phase}</span><small>{selectedProject?.name ?? '通用对话'}</small></div>
        </section>

        <section className="migration-tools" aria-label="OmniAgent 功能">
          <p>OmniAgent 已整合进 AI Develop。角色作为服务端 Skill 维护，模型配置与工具权限由服务端管理。</p>
          <div className="migration-actions">
            <button type="button" onClick={() => download('ai-develop-settings.json',JSON.stringify({version:1,skillId:skillId || 'general'},null,2),'application/json')}>导出角色偏好</button>
            <label>导入角色偏好<input type="file" accept=".json,application/json" disabled={running} onChange={event => {void importSettings(event.target.files?.[0]); event.target.value = ''}} /></label>
            <button type="button" disabled={!messages.length || running} onClick={() => download('ai-develop-conversation.md',messages.map(message=>`## ${message.role === 'user' ? '用户' : '助手'}\n\n${message.content}`).join('\n\n'),'text/markdown')}>导出对话</button>
          </div>
          <details><summary>快捷命令与安全规则</summary><p>/explain 解释 · /optimize 优化 · /bug 诊断 · /test 测试 · /refactor 重构 · /doc 文档 · /review 审查。显式选择角色时优先使用所选角色。</p><p>默认中文、结构化输出。只读分析；构建必须单独审批。自定义角色在服务端 skills 目录注册，浏览器不能覆盖系统规则。RAG 不在本项目范围。</p></details>
        </section>

        <section className="sandbox-deck">
          <div className="sandbox-heading">
            <div><p className="eyebrow violet">APPROVAL SANDBOX</p><strong>受控验证</strong></div>
            <span className={sandboxStatus?.enabled ? 'sandbox-on' : ''}>{sandboxStatus?.enabled ? 'READY' : 'DISABLED'}</span>
          </div>
          <p>无网络 · 非 root · 只读项目 · {sandboxStatus?.memory ?? '—'} 内存 · {sandboxStatus?.timeout ?? '—'} 超时</p>
          <div className="sandbox-controls">
            <select aria-label="沙盒动作" value={sandboxAction} onChange={(event) => setSandboxAction(event.target.value)}>
              {sandboxActions.map((action) => <option key={action.id} value={action.id}>{action.name} · {action.description}</option>)}
            </select>
            <button disabled={!projectId || sandboxBusy || !sandboxStatus?.enabled} onClick={createSandboxPlan}>生成待批准任务</button>
          </div>
          {latestSandboxJob && (
            <div className="sandbox-job">
              <div><code>{latestSandboxJob.action}</code><strong>{latestSandboxJob.status}</strong></div>
              {latestSandboxJob.status === 'PENDING_APPROVAL' && (
                <div className="sandbox-actions">
                  <button disabled={sandboxBusy} onClick={() => decideSandbox(latestSandboxJob, true)}>批准并执行</button>
                  <button disabled={sandboxBusy} onClick={() => decideSandbox(latestSandboxJob, false)}>拒绝</button>
                </div>
              )}
              {(latestSandboxJob.output || latestSandboxJob.error) && (
                <details className="sandbox-output">
                  <summary>查看执行输出</summary>
                  <pre>{latestSandboxJob.output || latestSandboxJob.error}</pre>
                </details>
              )}
            </div>
          )}
        </section>

        <section className="chat-stage">
          {!messages.length && (
            <div className="starter-grid">
              {starters.map((starter, index) => (
                <button key={starter} onClick={() => setInput(starter)}>
                  <span>0{index + 1}</span><strong>{starter}</strong><small>填入输入框 →</small>
                </button>
              ))}
            </div>
          )}
          <div className="messages">
            {messages.map((message, index) => (
              <article key={`${message.role}-${index}`} className={`message ${message.role}`}>
                <span>{message.role === 'user' ? 'YOU' : 'RAINBOW'}</span>
                <p>{message.content || (running ? '正在组织证据…' : '暂无内容')}</p>
              </article>
            ))}
            <div ref={endRef} />
          </div>

          {context && (
            <details className="context-trace">
              <summary>本轮上下文 · {context.contextSources?.length ?? 0} 个来源 {context.contextTruncated ? '· 已裁剪' : ''}</summary>
              <div>
                {context.runId && <code>run:{context.runId}</code>}
                {context.contextSources?.map((source) => <code key={source}>{source}</code>)}
              </div>
            </details>
          )}
          {notice && <div className="notice" role="status">{notice}</div>}

          <form className="composer" onSubmit={submit}>
            <textarea aria-label="问题" value={input} maxLength={16000}
              onChange={(event) => setInput(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === 'Enter' && !event.shiftKey && !event.nativeEvent.isComposing) { event.preventDefault(); submit() }
              }}
              placeholder="描述一个 Java / Spring / AI 工程问题…" />
            {running ? <button type="button" onClick={() => abortRef.current?.abort()}>停止生成</button> : <button disabled={!status.enabled || !input.trim()}>发送 ↑</button>}
            <small>Enter 发送 · Shift + Enter 换行 · 服务端持有模型凭据</small>
          </form>
        </section>
      </main>
    </div>
  )
}
