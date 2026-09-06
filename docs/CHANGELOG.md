# 变更日志

## 2026-09-06

- 整合 OmniAgent 9 个内置角色为服务端 Skills，并支持 7 个快捷命令。
- 增加停止生成、流式尾帧处理、异常断流提示、会话切换竞态保护及中文输入法保护。
- 增加对话导出、白名单角色偏好导入导出、本地离线演示。
- 保持源码和工作台私有；旧任意模型地址、客户端密钥与任意 MCP 命令不迁入。

记录用户可见能力和重要工程行为变化。纯格式调整不记录。

## 2026-09-04

### Added

- 新增 `POST /api/chat/stream` 流式对话接口及 `metadata`、`token`、`done`、`error` 事件协议。
- 新增服务端模型接入边界 `ChatGateway`、请求超时与取消处理、请求计数和耗时指标。
- 新增流式控制器与服务层测试，覆盖正常流、参数校验、遗留高风险字段拒绝和未配置模型降级。
- 新增 `codelens-ai` 到 `ai-develop` 的迁移计划与归档验收门槛。
- 新增 Spring AI OpenAI-compatible 模型网关、xtoken 本地安全启动脚本和 DeepSeek 环境变量模板。
- 新增项目、会话、消息持久化与恢复 API、上下文来源元数据和首 Token/字符指标。
- 新增受控项目文件读取、代码审查与 Spring 故障诊断 Skills。
- 新增默认关闭的只读 Streamable HTTP MCP Server。
- 新增 React + TypeScript 工作台、本地预览脚本、20 条 JSONL 评测集与 Python runner。
- 新增可查询的 Agent Run 与事件轨迹，记录上下文、模型启动、完成、失败和客户端取消。
- 新增模型 `native`/`buffered` 传输模式；xtoken 预览默认使用已实测稳定的 buffered SSE 兼容链路。
- 新增需要二次批准的本地 Docker 沙盒、白名单 Maven 动作、持久化任务审计和前端执行面板。

### Changed

- JSON 请求启用未知字段拒绝，避免旧客户端继续提交模型地址、API Key 或系统提示词。
- 统一异常处理新增错误 JSON 与模型未配置场景。

### Security

- 模型凭据、模型地址和系统提示词改为仅允许服务端配置。
- 沙盒默认无网络、非 root、只读根和项目挂载，不暴露 Docker Socket、主目录、SSH 或云凭据，并限制 CPU、内存、进程、时间与输出。
- 旧仓库在历史密钥撤销、功能迁移和 Git 历史保全全部验收前保持独立，不删除、不归档。

### Removed

- 从当前产品范围删除 RAG、Embedding、pgvector 和知识库 CRUD，避免与 Java 开发助手主目标无关的复杂度。
- PostgreSQL 镜像改为标准 `postgres:16-alpine`，初始 schema 只保留项目、会话和消息表。

### Planning

- 根据 JavaGuide AI 应用开发内容，将上下文工程、Agent Skills、Graph/Loop、短期记忆、MCP 治理和任务评测纳入路线图。

## 2026-07-02

### Added

- 建立 Rainbow AI Dev Copilot 产品规划。
- 建立总体进度、路线图、架构、技术矩阵和阶段计划。
- 增加多 Agent、执行沙盒和多编码 Agent 协议兼容设计。
- 增加改动后强制同步维护文档的工作规则。
- 初始化 Git、编辑器和忽略文件基线。
- 锁定 Java 17、Spring Boot 3.5.16、Spring AI 1.1.2 与 Spring AI Alibaba 1.1.2.2。
- 新增 Spring Boot 后端、Maven Wrapper、Dockerfile 和 PostgreSQL/pgvector Compose。
- 新增知识库查询、创建、更新和删除 API。
- 新增 Flyway 初始领域表、统一异常、参数校验和健康检查。
- 新增 Web 层与 Testcontainers 数据库集成测试。
