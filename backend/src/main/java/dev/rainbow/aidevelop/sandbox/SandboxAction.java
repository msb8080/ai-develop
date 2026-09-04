package dev.rainbow.aidevelop.sandbox;

import java.util.List;

enum SandboxAction {
    BACKEND_TEST(
            "backend-test",
            "后端测试",
            "在隔离容器中离线运行 Maven 测试",
            List.of("mvn", "-o", "-B", "-s", "/workspace/backend/src/main/resources/sandbox/maven-settings.xml",
                    "-Dmaven.repo.local=/m2", "-Dproject.build.directory=/tmp/rainbow-target",
                    "-f", "/workspace/backend/pom.xml", "test")
    ),
    BACKEND_PACKAGE(
            "backend-package",
            "后端打包",
            "在隔离容器中离线测试并生成临时 JAR",
            List.of("mvn", "-o", "-B", "-s", "/workspace/backend/src/main/resources/sandbox/maven-settings.xml",
                    "-Dmaven.repo.local=/m2", "-Dproject.build.directory=/tmp/rainbow-target",
                    "-f", "/workspace/backend/pom.xml", "package")
    );

    private final String id;
    private final String name;
    private final String description;
    private final List<String> command;

    SandboxAction(String id, String name, String description, List<String> command) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.command = command;
    }

    String id() { return id; }
    String displayName() { return name; }
    String description() { return description; }
    List<String> command() { return command; }

    static SandboxAction fromId(String id) {
        for (SandboxAction action : values()) {
            if (action.id.equals(id)) return action;
        }
        throw new IllegalArgumentException("Sandbox action is not allowed");
    }
}
