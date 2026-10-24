<script setup lang="ts">
/**
 * AI 对话 —— UI 样板已搭；【onSend / onStop 请自己写】
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

async function onSend() {
  const prompt = input.value.trim();
  if (!prompt || streaming.value) return;

  // 【你来写】
  // 1. push 用户消息；清空 input
  // 2. push 空的 assistant 消息
  // 3. abort = new AbortController(); streaming = true
  // 4. await streamMockReply(prompt, { signal, onChunk: 追加到 assistant.content })
  // 5. finally 里 streaming = false
  pushUserMessage(prompt)
  pushAssistantMessage()
  abort = new AbortController();
  streaming.value = true;
  await streamMockReply(prompt, {
    signal: abort.signal, onChunk: (chunk) => {
      pushAssistantMessage(chunk)
    }
  })
    .finally(() => {
      streaming.value = false;
    })
}

function onStop() {
  if (abort) abort.abort();
}

function pushUserMessage(prompt: string) {
  messages.value.push({ id: Date.now().toString(), role: "user", content: prompt })
}
function pushAssistantMessage(content: string) {
  messages.value.push({ id: Date.now().toString(), role: "assistant", content })
}

</script>

<template>
  <el-card shadow="never" class="chat-card">
    <template #header>
      <div class="head">
        <span>AI 对话 · Mock 流式（自己接线）</span>
        <el-tag size="small" type="info">先完成后端 SSE</el-tag>
      </div>
    </template>

    <div ref="listRef" class="msg-list">
      <div v-for="m in messages" :key="m.id" class="msg" :class="m.role">
        <div class="role">{{ m.role === "user" ? "我" : "助手" }}</div>
        <div class="bubble">{{ m.content || "…" }}</div>
      </div>
      <el-empty v-if="!messages.length" description="实现 onSend + SSE 后在这里看流式输出" />
    </div>

    <div class="composer">
      <el-input v-model="input" type="textarea" :rows="3" placeholder="输入问题，Enter 发送（Shift+Enter 换行）"
        :disabled="streaming" @keydown.enter.exact.prevent="onSend" />
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
