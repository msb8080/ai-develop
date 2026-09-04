# Rainbow AI Dev Copilot

面向个人学习和面试展示的 Java + AI 研发助手。项目围绕模型流式对话、上下文工程、Java 项目诊断、Agent Skills、安全工具调用与多编码 Agent 协议兼容逐步建设。

GitHub：<https://github.com/msb8080/ai-develop>（当前为私有仓库）

## 当前状态

- 当前阶段：阶段 2～4 的本地可运行纵向切片
- 总体状态：真实模型、会话记忆、受控项目上下文、Skills、只读 MCP、React 工作台与评测基线已落地
- 最近更新：2026-09-04
- 下一项任务：补充工作流 checkpoint、预算限流和本地沙盒；公开部署仍需人工确认

## 资源分工

- Mac M2 16G：开发、本地模型、构建测试、Python 评测、Agent 与沙盒实验。
- 阿里云 2 核 2G：Java 在线服务、PostgreSQL、静态前端、HTTPS 和轻量监控。
- 在线模型：使用受预算限制的模型 API；云主机不承载大模型推理。

## 文档导航

- [总体规划](docs/MASTER_PLAN.md)
- [当前进度](docs/PROGRESS.md)
- [系统架构](docs/ARCHITECTURE.md)
- [技术矩阵](docs/TECHNOLOGY_MATRIX.md)
- [路线图](docs/ROADMAP.md)
- [变更日志](docs/CHANGELOG.md)
- [阶段 1：Java 工程基础](docs/stages/01-java-foundation.md)
- [阶段 2：模型与对话](docs/stages/02-model-chat.md)
- [CodeLens 迁移说明](docs/migrations/CODELENS_MIGRATION.md)
- [JavaGuide AI 开发规划摘录](docs/research/JAVAGUIDE_AI_PLAN.md)
- [决策记录说明](docs/decisions/README.md)
- [协作与文档同步规则](AGENTS.md)

## 目标成果

最终形成一个可在线演示的 Rainbow AI Dev Copilot：接入 Java 项目后，系统能够按需组装项目上下文，完成只读代码分析、故障诊断、受控工具调用、人工审批后的沙盒验证，并比较 Codex、Claude Code、OpenCode 的执行效果。

## 一键本地预览

本机已配置 xtoken 时，脚本会从现有 OpenCode 配置中临时读取 `xtoken` 的地址和密钥；密钥不会写入项目或日志。当前验证可用模型为 `gpt-5.4`。由于该入口的原生流偶发空响应/超时，脚本默认使用 `buffered` 兼容模式，再由服务端稳定切片为 SSE；其他提供商默认保留 `native` 模式。

```bash
./scripts/dev-preview.sh
```

打开 <http://127.0.0.1:5173/>。脚本会启动 PostgreSQL、Spring Boot 和 Vite，退出时停止应用进程但保留数据库容器。

DeepSeek 或其他 OpenAI-compatible 服务可通过服务端环境变量接入：`AI_ENABLED`、`AI_PROVIDER`、`AI_BASE_URL`、`AI_API_KEY`、`AI_MODEL`。浏览器请求不接受任何模型凭据。

## 构建验证

```bash
docker compose up -d postgres
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ./mvnw -s /Users/shuaibomin/tools/maven/hexin-settings.xml test
cd ../frontend && npm install --registry=https://registry.npmjs.org && npm run build
```

健康检查：`GET http://localhost:8080/actuator/health`。

主要接口：`POST /api/chat/stream`、`GET/POST /api/projects`、`GET /api/conversations`、`GET /api/skills`。流式请求可提交 `message`、服务端项目/会话 ID 与 Skill ID；未知字段会被拒绝。设置 `MCP_ENABLED=true` 后，仅建议在可信本机网络启用只读 Streamable HTTP MCP。

评测基线位于 `evaluation/cases.jsonl`，共 20 个 Java/Spring/安全/上下文案例。服务运行后可先执行一条低成本冒烟：

```bash
python3 evaluation/run.py --limit 1
```
