# 阶段 3：Agent 与上下文工程

状态：未开始

计划：2026-08-08 ～ 2026-08-16

最后更新：2026-09-04

## 目标

从单 Agent 工具调用演进到高信噪比、有边界、有验证的 Java 项目诊断流程。

## 任务

- [ ] 实现 `ContextAssembler`，组装目标、规则、项目元数据、文件片段、工具和历史摘要。
- [ ] 建立只读的目录、文件、Maven 依赖和日志分析工具，约束路径、大小与超时。
- [ ] 建立 Skills 注册、触发、按需加载与版本记录；少量 Skills 使用规则路由。
- [ ] 使用 Spring AI Alibaba 实现单 Agent 项目诊断。
- [ ] 建立工具白名单、权限等级和人工审批。
- [ ] 拆分 Context、Code Analyst 与 Diagnostic Agent。
- [ ] 加入 Coordinator、Verifier 和 Report Agent。
- [ ] 用 Graph 显式管理 State 更新策略、条件边、checkpoint、最大循环次数、超时与成本限制。

## 验收

- [ ] 同一故障案例可以运行单 Agent 与多 Agent 版本。
- [ ] 诊断结论包含代码或文档证据。
- [ ] 每轮上下文可追踪来源、裁剪原因和预算，长内容可以按需回读。
- [ ] Skill、Prompt、Function Calling 与 MCP 的职责在实现和事件中可区分。
- [ ] 证据冲突时 Verifier 阻止确定性结论。
- [ ] 未经确认不能执行命令或写文件。

## 实施记录

- 2026-09-04：依据 JavaGuide AI 内容完成上下文工程、Skills、Graph/Loop、Memory 与 MCP 工程任务拆分。
