# 阶段 3：Agent 与上下文工程

状态：进行中

计划：2026-08-08 ～ 2026-08-16

最后更新：2026-09-04

## 目标

从单 Agent 工具调用演进到高信噪比、有边界、有验证的 Java 项目诊断流程。

## 任务

- [x] 实现 `ContextAssembler`，组装目标、规则、项目元数据、文件片段、工具和历史窗口。
- [x] 建立只读项目快照工具，约束路径、深度、类型、文件数和字符预算。
- [x] 建立 Skills 注册、触发、按需加载与版本记录；少量 Skills 使用规则路由。
- [ ] 使用 Spring AI Alibaba 实现单 Agent 项目诊断。
- [x] 建立第一版只读工具白名单；当前不存在写入或命令工具。
- [ ] 拆分 Context、Code Analyst 与 Diagnostic Agent。
- [ ] 加入 Coordinator、Verifier 和 Report Agent。
- [ ] 用 Graph 显式管理 State 更新策略、条件边、checkpoint、最大循环次数、超时与成本限制。

## 验收

- [ ] 同一故障案例可以运行单 Agent 与多 Agent 版本。
- [ ] 诊断结论包含代码或文档证据。
- [x] 每轮上下文可追踪来源、裁剪状态和字符预算。
- [x] Skill、Prompt、Function Calling 与 MCP 的职责在实现和事件中可区分。
- [ ] 证据冲突时 Verifier 阻止确定性结论。
- [x] 当前 Agent 链路没有命令和写文件能力。

## 实施记录

- 2026-09-04：依据 JavaGuide AI 内容完成上下文工程、Skills、Graph/Loop、Memory 与 MCP 工程任务拆分。
- 2026-09-04：实现 `ContextAssembler`、安全 `WorkspaceReader`、代码审查与 Spring 故障诊断两个 Skill。
- 2026-09-04：实现默认关闭的 Streamable HTTP MCP，只暴露 Skill 列表与受控项目快照。
- 2026-09-04：持久化单轮工作流与事件 checkpoint，覆盖成功、失败和客户端取消；多轮恢复与 Spring AI Alibaba Graph 仍待实现。
