<template>
  <div class="analysis-container">
    <el-page-header @back="goHome" title="返回首页" :content="`分析中：${store.stockCode}`" />

    <div class="main-content">
      <!-- 左侧：进度 -->
      <div class="left-panel">
        <PipelineProgress v-if="store.isPipelineMode" />
        <DebateProgress v-if="store.isDebateMode" />

        <!-- 实时日志 -->
        <el-card header="实时日志">
          <div class="log-area" ref="logRef">
            <div v-for="(e, i) in store.events" :key="i" class="log-line">
              <el-tag :type="logTagType(e.stage)" size="small">{{ formatStage(e.stage) }}</el-tag>
              <span class="log-msg">{{ e.message }}</span>
            </div>
            <div v-if="store.running" class="log-line">
              <el-icon class="is-loading"><Loading /></el-icon>
              <span class="log-msg">分析中...</span>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 右侧：状态 -->
      <div class="right-panel">
        <el-card header="分析状态">
          <el-descriptions :column="1" size="small">
            <el-descriptions-item label="股票代码">{{ store.stockCode }}</el-descriptions-item>
            <el-descriptions-item label="分析模式">{{ modeLabel }}</el-descriptions-item>
            <el-descriptions-item label="时间范围">{{ store.dateRange }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag v-if="store.running" type="warning">进行中</el-tag>
              <el-tag v-else-if="store.error" type="danger">失败</el-tag>
              <el-tag v-else type="success">完成</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card v-if="store.isCompleted && !store.error" class="action-card">
          <el-button type="primary" size="large" style="width:100%" @click="goReport">
            查看完整报告 →
          </el-button>
        </el-card>

        <el-alert v-if="store.error" type="error" :title="store.error" show-icon />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Loading } from '@element-plus/icons-vue'
import { connectSSE } from '../api/analysis.js'
import { useAnalysisStore } from '../stores/analysisStore.js'
import PipelineProgress from '../components/PipelineProgress.vue'
import DebateProgress from '../components/DebateProgress.vue'

const route = useRoute()
const router = useRouter()
const store = useAnalysisStore()
const logRef = ref(null)
let es = null

const modeLabel = computed(() => ({
  pipeline: '流水线', debate: '辩论', combined: '串联'
}[store.mode] || store.mode))

function formatStage(stage) {
  const map = {
    RESEARCHER_STARTED: '研究员', RESEARCHER_COMPLETED: '研究员✓',
    TECHNICAL_STARTED: '技术分析', TECHNICAL_COMPLETED: '技术分析✓',
    SENTIMENT_STARTED: '舆情分析', SENTIMENT_COMPLETED: '舆情分析✓',
    INVESTMENT_MANAGER_STARTED: '投资经理', INVESTMENT_MANAGER_COMPLETED: '投资经理✓',
    BULL_STARTED: '多方', BULL_COMPLETED: '多方✓',
    BEAR_STARTED: '空方', BEAR_COMPLETED: '空方✓',
    NEUTRAL_STARTED: '中立', NEUTRAL_COMPLETED: '中立✓',
    ARBITRATOR_STARTED: '仲裁官', ARBITRATOR_COMPLETED: '仲裁官✓',
    ANALYSIS_COMPLETED: '完成', ANALYSIS_FAILED: '失败'
  }
  return map[stage] || stage
}

function logTagType(stage) {
  if (stage.endsWith('COMPLETED') || stage === 'ANALYSIS_COMPLETED') return 'success'
  if (stage === 'ANALYSIS_FAILED') return 'danger'
  return 'warning'
}

function goHome() { router.push('/') }
function goReport() { router.push(`/report/${store.sessionId}`) }

onMounted(() => {
  const sessionId = route.params.sessionId
  if (!store.sessionId || store.sessionId !== sessionId) {
    // 页面刷新后 store 丢失，重定向回首页
    router.push('/')
    return
  }
  es = connectSSE(sessionId, (event) => {
    store.addEvent(event)
  }, () => {
    if (!store.isCompleted) store.addEvent({ stage: 'ANALYSIS_FAILED', message: 'SSE连接断开' })
  })
})

onUnmounted(() => { if (es) es.close() })

// 日志自动滚动到底部
watch(() => store.events.length, async () => {
  await nextTick()
  if (logRef.value) logRef.value.scrollTop = logRef.value.scrollHeight
})
</script>

<style scoped>
.analysis-container { padding: 20px; max-width: 1200px; margin: 0 auto; }
.main-content { display: grid; grid-template-columns: 1fr 300px; gap: 20px; margin-top: 20px; }
.log-area { max-height: 300px; overflow-y: auto; padding: 8px; background: #fafafa; border-radius: 4px; }
.log-line { display: flex; align-items: center; gap: 8px; padding: 4px 0; font-size: 13px; }
.log-msg { color: #333; }
.action-card { margin-top: 16px; }
</style>
