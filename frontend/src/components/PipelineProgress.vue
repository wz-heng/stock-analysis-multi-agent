<template>
  <el-card class="progress-card" header="流水线分析进度">
    <el-steps :active="activeStep" finish-status="success" direction="vertical">

      <el-step title="研究员" :description="stepDesc('RESEARCHER')">
        <template #icon>
          <el-icon><Search /></el-icon>
        </template>
      </el-step>

      <el-step title="技术分析师" :description="stepDesc('TECHNICAL')">
        <template #icon>
          <el-icon><TrendCharts /></el-icon>
        </template>
      </el-step>

      <el-step title="舆情分析师" :description="stepDesc('SENTIMENT')">
        <template #icon>
          <el-icon><ChatDotRound /></el-icon>
        </template>
      </el-step>

      <el-step title="投资经理" :description="stepDesc('INVESTMENT_MANAGER')">
        <template #icon>
          <el-icon><Briefcase /></el-icon>
        </template>
      </el-step>

    </el-steps>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'
import { Search, TrendCharts, ChatDotRound, Briefcase } from '@element-plus/icons-vue'
import { useAnalysisStore } from '../stores/analysisStore.js'

const store = useAnalysisStore()

const stageOrder = ['RESEARCHER', 'TECHNICAL', 'SENTIMENT', 'INVESTMENT_MANAGER']

const activeStep = computed(() => {
  const map = store.stageMap
  for (let i = stageOrder.length - 1; i >= 0; i--) {
    if (map[stageOrder[i] + '_COMPLETED']) return i + 1
    if (map[stageOrder[i] + '_STARTED']) return i
  }
  return 0
})

function stepDesc(stage) {
  const map = store.stageMap
  const completed = map[stage + '_COMPLETED']
  const started = map[stage + '_STARTED']
  if (completed) return completed.message
  if (started) return started.message + ' ...'
  return '等待中'
}
</script>

<style scoped>
.progress-card { margin-bottom: 20px; }
</style>
