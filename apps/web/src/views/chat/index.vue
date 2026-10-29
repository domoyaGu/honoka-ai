<script setup lang="ts">
/**
 * AI 对话页 —— Mock 流式接线（阶段 A · R1）
 *
 * 数据流：输入 → AbortController → streamMockReply → onChunk 追加同一条 assistant → 停止 abort
 * 说明：docs/R1练习-自己写.md §5～§6
 */
import { nextTick, ref } from "vue";
import { ElMessage } from "element-plus";
import { streamMockReply } from "@/composables/useMockChatStream";

interface ChatMessage {
  id: string;
  role: "user" | "assistant";
  content: string;
}

const input = ref("");
const streaming = ref(false);
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
  };
  messages.value.push(assistant);
  scrollBottom();

  abort = new AbortController();
  streaming.value = true;

  try {
    await streamMockReply(prompt, {
      signal: abort.signal,
      onChunk: (piece) => {
        assistant.content += piece;
        scrollBottom();
      },
    });
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
        <span>AI 对话 · Mock 流式</span>
        <el-tag size="small" type="success">SSE 已接线</el-tag>
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
        <div class="bubble">{{ m.content || "…" }}</div>
      </div>
      <el-empty
        v-if="!messages.length"
        description="输入问题后看逐字流式输出；可点停止中断"
      />
    </div>

    <div class="composer">
      <el-input
        v-model="input"
        type="textarea"
        :rows="3"
        placeholder="输入问题，Enter 发送（Shift+Enter 换行）；可试 samples/fullstack-kb.md §7"
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

.bubble {
  max-width: 70%;
  padding: 10px 12px;
  border-radius: 10px;
  white-space: pre-wrap;
  line-height: 1.5;
  font-size: 14px;
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
