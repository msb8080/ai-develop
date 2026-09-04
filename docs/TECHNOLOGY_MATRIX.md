# 技术矩阵

最后更新：2026-09-04

| 技术 | 定位 | 运行位置 | 是否主链路 |
|---|---|---|---|
| Spring Boot | 业务与 API 基座 | Mac、阿里云 | 是 |
| Spring AI | 模型流式调用、结构化输出与 Tool Calling 抽象 | Mac、阿里云 | 是 |
| Spring AI Alibaba | Agent、Graph、上下文工程与多 Agent 编排 | Mac、阿里云 | 是 |
| AgentScope Java | 权限、事件、沙盒和多 Agent 对比实验 | Mac | 否 |
| PostgreSQL | 项目、会话、消息、任务状态和审计记录 | Mac、阿里云 | 是 |
| Agent Skills | 按需加载任务流程、约束与参考资料 | Mac、阿里云 | 是 |
| React + TypeScript | 产品界面与流式交互 | 构建在 Mac，静态部署 | 是 |
| Python | 评测、数据准备、生态实验 | Mac | 否 |
| Docker Compose | 本地依赖与云端轻量编排 | Mac、阿里云 | 是 |
| Caddy | HTTPS、静态文件、反向代理 | 阿里云 | 是 |
| MCP | 向编码 Agent 暴露统一工具 | Mac | 是 |
| ACP | OpenCode/IDE 协议对比 | Mac | 实验 |

## 当前版本基线

| 组件 | 版本 | 说明 |
|---|---|---|
| JDK | 17.0.19 | Homebrew `openjdk@17`，项目通过 `JAVA_HOME` 使用 |
| Maven | 3.9.x | 本机为 3.9.14 |
| Spring Boot | 3.5.16 | 与稳定版 Java AI 技术栈兼容 |
| Spring AI | 1.1.2 | 与 Spring AI Alibaba 稳定版对齐 |
| Spring AI Alibaba | 1.1.2.2 | 当前稳定 Agent 框架基线 |

## 选型原则

- 业务能力优先使用 Java 实现，保持面试主线清晰。
- Python 不成为第二套在线后端。
- TypeScript 不在云端运行常驻 Node 服务，除非后续有明确收益。
- AgentScope Java 不与 Spring AI Alibaba 同时进入主业务运行时。
- 新技术必须解决明确问题，并补充决策记录和可验证收益。

## 待决策

- 模型 API、额度和降级策略。
- React 构建工具及组件库。
- 云主机 Linux 发行版和备份目标。

## 明确移出主线

- RAG、Embedding 和向量数据库不进入当前版本。
- 不为少量 Skills 引入向量召回；先使用明确触发条件、结构化元数据和规则路由。
