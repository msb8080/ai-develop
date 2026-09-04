package dev.rainbow.aidevelop.chat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
@Import(ChatProperties.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatService service;

    @Test
    void streamsMetadataTokensAndCompletion() throws Exception {
        when(service.stream(any(), eq("hello")))
                .thenReturn(Flux.just("你", "好").delayElements(Duration.ofMillis(10)));

        MvcResult result = mockMvc.perform(post("/api/chat/stream")
                        .contentType("application/json")
                        .content("""
                                {"message":"hello"}
                                """))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        MvcResult completed = mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/event-stream"))
                .andReturn();

        assertThat(completed.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .contains("event:metadata", "event:token", "\"content\":\"你\"", "event:done");
    }

    @Test
    void rejectsBlankMessage() throws Exception {
        mockMvc.perform(post("/api/chat/stream")
                        .contentType("application/json")
                        .content("""
                                {"message":" "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.violations[0].field").value("message"));
    }

    @Test
    void rejectsLegacyClientSideCredentials() throws Exception {
        mockMvc.perform(post("/api/chat/stream")
                        .contentType("application/json")
                        .content("""
                                {"message":"hello","apiKey":"must-not-be-accepted"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void streamsSafeErrorWhenModelIsNotConfigured() throws Exception {
        when(service.stream(any(), eq("hello")))
                .thenReturn(Flux.error(new ChatUnavailableException("sensitive internal detail")));

        MvcResult result = mockMvc.perform(post("/api/chat/stream")
                        .contentType("application/json")
                        .content("""
                                {"message":"hello"}
                                """))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        MvcResult completed = mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(completed.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .contains("event:error", "\"code\":\"AI_NOT_CONFIGURED\"")
                .doesNotContain("sensitive internal detail");
    }
}
