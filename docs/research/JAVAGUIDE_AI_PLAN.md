# JavaGuide AI 开发内容纳入规划

调研日期：2026-09-04

来源范围：JavaGuide 官方 GitHub 仓库 `docs/ai` 目录

结论：保留模型、上下文、Agent、Skills、MCP、安全和评测；不纳入 RAG、Embedding 与向量数据库。

## 采用项

| JavaGuide 主题 | 对本项目的价值 | 纳入的可交付任务 | 验收证据 |
|---|---|---|---|
| [上下文工程](https://github.com/Snailclimb/JavaGuide/blob/main/docs/ai/agent/context-engineering.md) | 长窗口不等于高质量；需要统一控制规则、项目材料、工具、历史和预算 | `ContextAssembler`、来源记录、上下文裁剪、按需读取、Token 预算 | 同一任务可回放每轮上下文来源和裁剪原因 |
| [Agent Skills](https://github.com/Snailclimb/JavaGuide/blob/main/docs/ai/agent/skills.md) | 将任务流程和约束从临时 Prompt 中分离，支持发现与延迟加载 | Skill 元数据、触发规则、版本、正文按需加载；明确 Skill/Prompt/Function Calling/MCP 边界 | 至少实现代码审查与故障诊断两个 Skill，并记录命中原因 |
| [Workflow、Graph 与 Loop](https://github.com/Snailclimb/JavaGuide/blob/main/docs/ai/agent/workflow-graph-loop.md) | 让诊断、验证和修正成为显式状态机，防止无界循环 | State 更新策略、条件边、checkpoint、轮次/超时/Token 上限和人工中断 | 故障案例可暂停、恢复、达到上限退出并输出完整事件轨迹 |
| [Agent Memory](https://github.com/Snailclimb/JavaGuide/blob/main/docs/ai/agent/agent-memory.md) | 会话历史需要缩减、卸载、隔离和过期治理，不应无限追加 | PostgreSQL 会话消息、滑动窗口、摘要、原始工具结果引用、过期策略 | 重启可恢复会话；摘要保留版本号、错误码、文件位置等关键字段 |
| [MCP](https://github.com/Snailclimb/JavaGuide/blob/main/docs/ai/agent/mcp.md) | 跨客户端复用工具，同时补齐 Schema、权限、审计、成本和依赖治理 | 先实现窄粒度只读 Tools，再评估 Resources/Prompts；增加版本、Trace、超时、限流与人工确认 | 三种客户端复用同一工具契约，危险参数和越界路径被拒绝 |

## 评测基线

从真实 Java 开发场景整理 20～50 条任务，至少覆盖：项目结构说明、Maven 依赖冲突、Spring 配置错误、异常栈定位、代码审查、测试失败分析和受控修改建议。每次只改变一个策略，记录：

- 任务成功率与人工补救次数。
- 工具选择、参数、重复调用和危险操作拦截率。
- 输入/输出 Token、首 Token、端到端和工具等待耗时。
- 上下文裁剪后的关键信息保留率。
- 结论与文件、行号、日志或工具结果的一致性。

## 暂不采用

- RAG、Embedding、pgvector 及知识库页面已经移出当前范围。
- 少量 Skills 不使用向量召回，先以名称、description、典型触发词和风险阈值进行规则路由。
- 长期记忆不先引入专用向量库；优先使用 PostgreSQL 结构化字段、摘要和可回查的文件引用。
- MCP 首版只做少量窄粒度只读 Tools，不实现 `execute_sql`、任意文件操作或任意 URL 请求等万能工具。

## 纳入顺序

1. 完成真实模型、会话持久化、调用指标和预算降级。
2. 完成 `ContextAssembler` 与只读项目文件工具。
3. 完成 Skills 注册和两个示范 Skill。
4. 用 Spring AI Alibaba Graph 建立诊断 → 验证 → 报告的有界工作流。
5. 建立评测集后再拆分多 Agent，避免在单 Agent 基线未知时增加复杂度。
6. 最后暴露稳定的 MCP 工具契约并验证三种客户端兼容性。
