package dev.rainbow.aidevelop.chat;

import reactor.core.publisher.Flux;

/**
 * 模型调用边界。具体模型提供商只能通过服务端配置接入，禁止由请求动态指定地址或密钥。
 */
public interface ChatGateway {

    Flux<String> stream(String systemPrompt, String message);
}
