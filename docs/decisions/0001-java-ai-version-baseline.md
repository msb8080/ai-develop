# ADR-0001：Java AI 技术版本基线

日期：2026-07-02
状态：已接受

## 背景

项目需要同时使用 Spring AI 与 Spring AI Alibaba。当前 Spring AI 2.0 已对应 Spring Boot 4.x，但 Spring AI Alibaba 的稳定版本仍基于 Spring AI 1.1.x；其 2.0 版本尚处于里程碑阶段。

## 候选方案

1. Spring Boot 4.1 + Spring AI 2.0，不引入稳定版 Spring AI Alibaba。
2. Spring Boot 3.5 + Spring AI 1.1 + Spring AI Alibaba 1.1.2.2。
3. 使用 Spring AI Alibaba 2.0 里程碑版本。

## 决定

阶段 1 使用：

- JDK 17。
- Maven 3.9.x。
- Spring Boot 3.5.16。
- Spring AI 1.1.2。
- Spring AI Alibaba 1.1.2.2。

依赖通过 Spring Boot Parent 和 Spring AI Alibaba BOM 管理，不混用快照或里程碑仓库。

## 理由

- 本机已有 JDK 17，满足当前框架基线。
- Spring AI Alibaba 1.1.2.2 是稳定发布，并与 Spring AI 1.1.2 对齐。
- 避免在学习项目起步阶段承担 Boot 4 与 Agent 框架预发布版本的双重迁移风险。
- Spring Boot 3.5.16 虽是 3.5.x 最后一个 OSS 版本，但适合作为兼容起点。

## 后果与升级

- Agent 阶段开始前重新检查 Spring AI Alibaba 2.x 是否已 GA。
- 如果 2.x 已稳定，单独创建升级分支验证 Boot 4、Spring AI 2 和 Jakarta 兼容性。
- 如果仍未稳定，主项目保持当前基线并记录安全更新策略。
