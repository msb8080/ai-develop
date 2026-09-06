import type { Conversation, Message, StreamMetadata } from './api'
export const demoMode = import.meta.env.VITE_DEMO_MODE === 'true'
const names: Record<string, string> = {general:'通用助手',prd:'需求分析',design:'交互设计',code:'编码与重构',test:'测试设计',deploy:'部署规划',chef:'烹饪助手',stock:'金融知识',gamer:'游戏助手','code-review':'Java 代码审查','spring-diagnosis':'Spring 故障诊断'}
const conversations: Conversation[] = []
const messages = new Map<string, Message[]>()
export function demoGet(url: string): unknown {
  if (url === '/api/chat/status') return {enabled:true,provider:'交互演示',model:'预设示例',streamMode:'不调用模型'}
  if (url === '/api/projects') return [{id:'demo-project',name:'示例 Java 项目',sourceReference:'示例材料（非真实工作区）'}]
  if (url === '/api/conversations') return [...conversations]
  if (url === '/api/skills') return Object.entries(names).map(([id,name]) => ({id,name,description:name,version:'1.0.0'}))
  if (url === '/api/sandbox/status') return {enabled:false,approval:'required',network:'none',memory:'—',cpus:'—',timeout:'—'}
  if (url === '/api/sandbox/actions' || url === '/api/sandbox/jobs') return []
  const match = url.match(/^\/api\/conversations\/([^/]+)\/messages$/)
  if (match) return [...(messages.get(match[1]) ?? [])]
  throw new Error('演示不支持此操作')
}
export async function demoStream(input: {message:string;conversationId?:string;skillId?:string}, emit:(name:string,event:StreamMetadata)=>void, signal?:AbortSignal) {
  const conversationId = input.conversationId || crypto.randomUUID()
  const metadata = {requestId:crypto.randomUUID(),conversationId,contextSources:['demo:示例材料'],skillId:input.skillId}
  if (!messages.has(conversationId)) {
    messages.set(conversationId, [])
    conversations.unshift({id:conversationId,title:input.message.slice(0,40),updatedAt:new Date().toISOString(),projectId:'demo-project'})
  }
  const history = messages.get(conversationId)!
  history.push({role:'user',content:input.message})
  emit('metadata',metadata)
  emit('phase',{...metadata,phase:'respond'})
  const answer = `【预设交互示例，非模型生成】\n\n当前角色：${names[input.skillId ?? 'general'] ?? '通用助手'}\n\n1. 确认目标与可用证据。\n2. 按所选 Skill 组织分析，并标注不确定项。\n3. 列出验证步骤；构建和测试须进入审批沙盒。\n\n本页不会读取你的项目、调用模型或执行命令。完整功能由 ai-develop 本地服务提供，刷新页面会清空演示对话。`
  let content = ''
  try {
    for (const chunk of answer.match(/.{1,12}|\n/g) ?? []) {
      signal?.throwIfAborted()
      await new Promise(resolve=>setTimeout(resolve,35))
      signal?.throwIfAborted()
      content += chunk
      emit('token',{...metadata,content:chunk})
    }
    emit('done',metadata)
  } finally { history.push({role:'assistant',content}) }
}
