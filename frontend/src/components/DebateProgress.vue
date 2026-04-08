<template>
  <el-card class="progress-card" header="辩论分析进度">
    <div class="debate-grid">

      <div v-for="agent in agents" :key="agent.key" class="agent-col">
        <el-card shadow="never" :class="['agent-card', statusClass(agent.key)]">
          <template #header>
            <span>{{ agent.label }}</span>
            <el-tag :type="tagType(agent.key)" size="small" style="float:right">
              {{ statusLabel(agent.key) }}
            </el-tag>
          </template>
          <p class="agent-message">{{ agentMessage(agent.key) }}</p>
        </el-card>
      </div>

    </div>

    <el-divider v-if="arbitratorStarted">仲裁官综合裁决</el-divider>
    <el-card v-if="arbitratorStarted" shadow="never" class="arbitrator-card">
      <el-tag :type="arbitratorCompleted ? 'success' : 'warning'">
        {{ arbitratorCompleted ? '裁决完成' : '裁决中...' }}
      </el-tag>
      <p v-if="arbitratorCompleted" class="agent-message">{{ arbitratorMessage }}</p>
    </el-card>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'
import { useAnalysisStore } from '../stores/analysisStore.js'

const store = useAnalysisStore()

const agents = [
  { key: 'BULL', label: '多方（GPT-4o）' },
  { key: 'BEAR', label: '空方（Claude）' },
  { key: 'NEUTRAL', label: '中立（DeepSeek）' }
]

function statusClass(key) {
  const map = store.stageMap
  if (map[key + '_COMPLETED']) return 'status-done'
  if (map[key + '_STARTED']) return 'status-running'
  return ''
}

function tagType(key) {
  const map = store.stageMap
  if (map[key + '_COMPLETED']) return 'success'
  if (map[key + '_STARTED']) return 'warning'
  return 'info'
}

function statusLabel(key) {
  const map = store.stageMap
  if (map[key + '_COMPLETED']) return '完成'
  if (map[key + '_STARTED']) return '立论中'
  return '等待'
}

function agentMessage(key) {
  const map = store.stageMap
  const e = map[key + '_COMPLETED'] || map[key + '_STARTED']
  return e ? e.message : '等待中...'
}

const arbitratorStarted = computed(() => !!store.stageMap['ARBITRATOR_STARTED'])
const arbitratorCompleted = computed(() => !!store.stageMap['ARBITRATOR_COMPLETED'])
const arbitratorMessage = computed(() => store.stageMap['ARBITRATOR_COMPLETED']?.message || '')
</script>

<style scoped>
.progress-card { margin-bottom: 20px; }
.debate-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.agent-card { min-height: 80px; }
.status-done { border-color: #67c23a !important; }
.status-running { border-color: #e6a23c !important; }
.agent-message { font-size: 13px; color: #666; margin-top: 8px; }
.arbitrator-card { margin-top: 8px; }
</style>
