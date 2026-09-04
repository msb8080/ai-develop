# Rainbow AI Dev Copilot

面向个人学习和面试展示的 Java + AI 研发助手。项目围绕模型流式对话、上下文工程、Java 项目诊断、Agent Skills、安全工具调用与多编码 Agent 协议兼容逐步建设。

GitHub：<https://github.com/msb8080/ai-develop>（当前为私有仓库）

## 当前状态

- 当前阶段：阶段 2——模型与对话
- 总体状态：阶段 1 已完成；安全的 SSE 对话边界已落地，真实模型和会话状态待接入
- 最近更新：2026-09-04
- 下一项任务：确定模型 API，完成服务端模型网关

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

## 本地验证

```bash
docker compose up -d postgres
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ./mvnw -s /Users/shuaibomin/tools/maven/hexin-settings.xml test
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ./mvnw -s /Users/shuaibomin/tools/maven/hexin-settings.xml spring-boot:run
```

健康检查：`GET http://localhost:8080/actuator/health`。

流式接口：`POST http://localhost:8080/api/chat/stream`，请求体仅接受 `message`。当前尚未配置模型网关时，接口会返回结构化的 `AI_NOT_CONFIGURED` 事件，不会接收或转发客户端提供的 API Key、模型地址与系统提示词。
