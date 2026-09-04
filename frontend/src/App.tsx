import { FormEvent, useEffect, useMemo, useRef, useState } from 'react'
import {
  Conversation,
  createProject,
  getJson,
  Message,
  ModelStatus,
  Project,
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
  const [projectForm, setProjectForm] = useState({ name: 'AI Develop', relativePath: '.' })
  const endRef = useRef<HTMLDivElement>(null)

  const selectedProject = useMemo(
    () => projects.find((project) => project.id === projectId),
    [projects, projectId],
  )

  async function refresh() {
    const [nextStatus, nextProjects, nextConversations, nextSkills] = await Promise.all([
      getJson<ModelStatus>('/api/chat/status'),
      getJson<Project[]>('/api/projects'),
      getJson<Conversation[]>('/api/conversations'),
      getJson<Skill[]>('/api/skills'),
    ])
    setStatus(nextStatus)
    setProjects(nextProjects)
    setConversations(nextConversations)
    setSkills(nextSkills)
    if (!projectId && nextProjects.length) setProjectId(nextProjects[0].id)
  }

  useEffect(() => {
    refresh().catch((error: Error) => setNotice(error.message))
  }, [])

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  async function openConversation(id: string) {
    setConversationId(id)
    const conversation = conversations.find((item) => item.id === id)
    setProjectId(conversation?.projectId ?? '')
    setMessages(await getJson<Message[]>(`/api/conversations/${id}/messages`))
    setContext(null)
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
    if (!message || running) return
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
      )
      setConversationId(nextConversationId)
      await refresh()
    } catch (error) {
      setNotice(error instanceof Error ? error.message : '模型响应失败')
      setPhase('error')
    } finally {
      setRunning(false)
    }
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <a className="brand" href="#top" aria-label="Rainbow AI Dev Copilot">
          <span className="brand-mark">R</span>
          <span><strong>Rainbow</strong><small>AI DEV COPILOT</small></span>
        </a>

        <button className="new-chat" onClick={() => { setConversationId(''); setMessages([]); setContext(null) }}>
          <span>＋</span> 新建分析
        </button>

        <div className="side-section">
          <p className="eyebrow">RECENT RUNS</p>
          <div className="conversation-list">
            {conversations.map((conversation) => (
              <button key={conversation.id} className={conversation.id === conversationId ? 'active' : ''}
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
            <button title="接入项目">↗</button>
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
          <a href="https://github.com/msb8080/ai-develop" target="_blank" rel="noreferrer">GitHub ↗</a>
        </header>

        <section className="hero">
          <p className="eyebrow violet">JAVA × AI ENGINEERING WORKBENCH</p>
          <h1>从代码证据出发，<br /><em>把问题说清楚。</em></h1>
          <p className="hero-copy">选择本地项目与分析 Skill。系统只读取受控上下文，保留每轮会话和来源，不执行写操作。</p>
        </section>

        <section className="control-deck">
          <label>项目上下文
            <select value={projectId} disabled={Boolean(conversationId)} onChange={(event) => setProjectId(event.target.value)}>
              <option value="">不加载项目</option>
              {projects.map((project) => <option key={project.id} value={project.id}>{project.name} · {project.sourceReference}</option>)}
            </select>
          </label>
          <label>按需 Skill
            <select value={skillId} onChange={(event) => setSkillId(event.target.value)}>
              <option value="">自动识别</option>
              {skills.map((skill) => <option key={skill.id} value={skill.id}>{skill.name}</option>)}
            </select>
          </label>
          <div className="run-state"><span>{phase}</span><small>{selectedProject?.name ?? '通用对话'}</small></div>
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
          {notice && <div className="notice">{notice}</div>}

          <form className="composer" onSubmit={submit}>
            <textarea aria-label="问题" value={input} maxLength={16000}
              onChange={(event) => setInput(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === 'Enter' && !event.shiftKey) { event.preventDefault(); submit() }
              }}
              placeholder="描述一个 Java / Spring / AI 工程问题…" />
            <button disabled={running || !input.trim()}>{running ? '分析中' : '发送 ↑'}</button>
            <small>Enter 发送 · Shift + Enter 换行 · 服务端持有模型凭据</small>
          </form>
        </section>
      </main>
    </div>
  )
}
