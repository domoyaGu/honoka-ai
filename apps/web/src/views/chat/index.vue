<script setup lang="ts">
/**
 * AI 对话页 —— Mock / RAG-Mock 流式
 *
 * 数据流：输入 → AbortController → streamMockReply(rag|mock)
 *   → meta.citations 展示引用 → onChunk 追加同一条 assistant → 停止 abort
 */
import { nextTick, ref } from "vue";
import { ElMessage } from "element-plus";
import {
  streamMockReply,
  type Citation,
  type StreamMode,
} from "@/composables/useMockChatStream";

interface ChatMessage {
  id: string;
  role: "user" | "assistant";
  content: string;
  citations?: Citation[];
}

const input = ref("");
const streaming = ref(false);
const mode = ref<StreamMode>("rag");
const messages = ref<ChatMessage[]>([]);
const listRef = ref<HTMLElement | null>(null);
let abort: AbortController | null = null;

function scrollBottom() {
  void nextTick(() => {
    const el = listRef.value;
    if (el) el.scrollTop = el.scrollHeight;
  });
}

function newId() {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
}

async function onSend() {
  const prompt = input.value.trim();
  if (!prompt || streaming.value) return;

  messages.value.push({ id: newId(), role: "user", content: prompt });
  input.value = "";

  // 先放一条空助手气泡，后续 chunk 只改这一条（不要每个字 push 新消息）
  const assistant: ChatMessage = {
    id: newId(),
    role: "assistant",
    content: "",
    citations: [],
  };
  messages.value.push(assistant);
  scrollBottom();

  abort = new AbortController();
  streaming.value = true;

  try {
    await streamMockReply(
      prompt,
      {
        signal: abort.signal,
        onChunk: (piece) => {
          assistant.content += piece;
          scrollBottom();
        },
        onCitations: (citations) => {
          assistant.citations = citations;
          scrollBottom();
        },
      },
      mode.value,
    );
  } catch (e) {
    // 用户点停止会 abort，不算错误提示
    if (e instanceof DOMException && e.name === "AbortError") {
      if (!assistant.content) {
        assistant.content = "（已停止）";
      }
      return;
    }
    const msg = e instanceof Error ? e.message : "流式请求失败";
    ElMessage.error(msg);
    if (!assistant.content) {
      assistant.content = `错误：${msg}`;
    }
  } finally {
    streaming.value = false;
    abort = null;
  }
}

function onStop() {
  abort?.abort();
}
</script>

<template>
  <el-card shadow="never" class="chat-card">
    <template #header>
      <div class="head">
        <span>AI 对话 · {{ mode === "rag" ? "RAG Mock" : "纯 Mock" }}</span>
        <el-tag size="small" type="success">SSE</el-tag>
        <el-radio-group v-model="mode" size="small" :disabled="streaming">
          <el-radio-button value="rag">RAG</el-radio-button>
          <el-radio-button value="mock">Mock</el-radio-button>
        </el-radio-group>
      </div>
    </template>

    <div ref="listRef" class="msg-list">
      <div
        v-for="m in messages"
        :key="m.id"
        class="msg"
        :class="m.role"
      >
        <div class="role">{{ m.role === "user" ? "我" : "助手" }}</div>
        <div class="bubble-wrap">
          <div class="bubble">{{ m.content || "…" }}</div>
          <div
            v-if="m.role === 'assistant' && m.citations?.length"
            class="citations"
          >
            <div class="citations-title">引用片段</div>
            <div
              v-for="(c, idx) in m.citations"
              :key="c.chunkId"
              class="citation"
            >
              <span class="cite-idx">[{{ idx + 1 }}]</span>
              <span class="cite-doc">{{ c.docName }}</span>
              <span class="cite-src">{{ c.source }}</span>
              <p class="cite-snip">{{ c.snippet }}</p>
            </div>
          </div>
        </div>
      </div>
      <el-empty
        v-if="!messages.length"
        description="默认走 RAG：检索 samples/uploads 后流式回答并展示引用；可切回纯 Mock"
      />
    </div>

    <div class="composer">
      <el-input
        v-model="input"
        type="textarea"
        :rows="3"
        placeholder="试问：Honoka 的后端推荐用哪个？端口是多少？"
        :disabled="streaming"
        @keydown.enter.exact.prevent="onSend"
      />
      <div class="actions">
        <el-button v-if="streaming" type="warning" @click="onStop">停止</el-button>
        <el-button type="primary" :loading="streaming" @click="onSend">
          发送
        </el-button>
      </div>
    </div>
  </el-card>
</template>

<style scoped lang="scss">
.chat-card {
  display: flex;
  flex-direction: column;
  min-height: calc(100vh - 160px);

  :deep(.el-card__body) {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 12px;
  }
}

.head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.msg-list {
  flex: 1;
  overflow: auto;
  padding: 8px;
  background: #fafafa;
  border-radius: 8px;
  min-height: 280px;
}

.msg {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;

  &.user {
    flex-direction: row-reverse;

    .bubble {
      background: #2563eb;
      color: #fff;
    }
  }

  &.assistant .bubble {
    background: #fff;
    border: 1px solid #e5e7eb;
  }
}

.role {
  font-size: 12px;
  color: #6b7280;
  width: 32px;
  flex-shrink: 0;
  padding-top: 8px;
}

.bubble-wrap {
  max-width: 70%;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.bubble {
  padding: 10px 12px;
  border-radius: 10px;
  white-space: pre-wrap;
  line-height: 1.5;
  font-size: 14px;
}

.citations {
  padding: 8px 10px;
  background: #f3f4f6;
  border-radius: 8px;
  border: 1px dashed #d1d5db;
  font-size: 12px;
  color: #374151;
}

.citations-title {
  font-weight: 600;
  margin-bottom: 6px;
  color: #4b5563;
}

.citation {
  margin-bottom: 8px;

  &:last-child {
    margin-bottom: 0;
  }
}

.cite-idx {
  color: #2563eb;
  margin-right: 4px;
}

.cite-doc {
  font-weight: 500;
  margin-right: 6px;
}

.cite-src {
  color: #9ca3af;
}

.cite-snip {
  margin: 4px 0 0;
  line-height: 1.4;
  color: #6b7280;
  white-space: pre-wrap;
}

.composer {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
