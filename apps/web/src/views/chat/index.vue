<script setup lang="ts">
/**
 * AI 对话页 —— Mock / RAG / Agent 流式
 *
 * 数据流：输入 → AbortController → streamMockReply
 *   → tool 步骤（Agent）→ meta.citations → onChunk 追加 → 停止 abort
 */
import { computed, nextTick, ref } from "vue";
import { ElMessage } from "element-plus";
import {
  streamMockReply,
  type AgentToolStep,
  type Citation,
  type StreamMode,
} from "@/composables/useMockChatStream";

interface ChatMessage {
  id: string;
  role: "user" | "assistant";
  content: string;
  citations?: Citation[];
  toolSteps?: AgentToolStep[];
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

  // 先放空助手气泡；后续必须改 messages 里的响应式项
  // （勿改 push 前的裸对象，否则 Vue 不刷新，看起来像「一次性全出来」）
  messages.value.push({
    id: newId(),
    role: "assistant",
    content: "",
    citations: [],
    toolSteps: [],
  });
  const assistantIdx = messages.value.length - 1;
  scrollBottom();

  abort = new AbortController();
  streaming.value = true;

  try {
    await streamMockReply(
      prompt,
      {
        signal: abort.signal,
        onChunk: (piece) => {
          const msg = messages.value[assistantIdx];
          if (!msg) return;
          msg.content += piece;
          scrollBottom();
        },
        onCitations: (citations) => {
          const msg = messages.value[assistantIdx];
          if (!msg) return;
          msg.citations = citations;
          scrollBottom();
        },
        onToolStep: (step) => {
          const msg = messages.value[assistantIdx];
          if (!msg) return;
          const list = msg.toolSteps ? [...msg.toolSteps] : [];
          // 避免 done 里重复 steps 时刷两遍
          if (list.some((s) => s.tool === step.tool && JSON.stringify(s.args) === JSON.stringify(step.args))) {
            return;
          }
          list.push(step);
          msg.toolSteps = list;
          scrollBottom();
        },
      },
      mode.value,
    );
  } catch (e) {
    const msg = messages.value[assistantIdx];
    // 用户点停止会 abort，不算错误提示
    if (e instanceof DOMException && e.name === "AbortError") {
      if (msg && !msg.content) {
        msg.content = "（已停止）";
      }
      return;
    }
    const errText = e instanceof Error ? e.message : "流式请求失败";
    ElMessage.error(errText);
    if (msg && !msg.content) {
      msg.content = `错误：${errText}`;
    }
  } finally {
    streaming.value = false;
    abort = null;
  }
}

function onStop() {
  abort?.abort();
}

const modeLabel = computed(() =>
  mode.value === "agent" ? "Agent Mock" : mode.value === "rag" ? "RAG Mock" : "纯 Mock",
);
</script>

<template>
  <el-card shadow="never" class="chat-card">
    <template #header>
      <div class="head">
        <span>AI 对话 · {{ modeLabel }}</span>
        <el-tag size="small" type="success">SSE</el-tag>
        <el-radio-group v-model="mode" size="small" :disabled="streaming">
          <el-radio-button value="rag">RAG</el-radio-button>
          <el-radio-button value="agent">Agent</el-radio-button>
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
          <div
            v-if="m.role === 'assistant' && m.toolSteps?.length"
            class="tool-steps"
          >
            <div class="citations-title">工具调用</div>
            <div
              v-for="(s, si) in m.toolSteps"
              :key="`${s.tool}-${si}`"
              class="tool-step"
            >
              <code>{{ s.tool }}</code>
              <span class="tool-args">{{ JSON.stringify(s.args) }}</span>
            </div>
          </div>
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

.tool-steps {
  padding: 8px 10px;
  background: #eef2ff;
  border-radius: 8px;
  border: 1px solid #c7d2fe;
  font-size: 12px;
  color: #3730a3;
}

.tool-step {
  margin-bottom: 4px;

  code {
    font-weight: 600;
    margin-right: 6px;
  }
}

.tool-args {
  color: #6366f1;
  word-break: break-all;
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
