<template>
  <div class="report-container">
    <el-page-header @back="() => router.push('/')" title="返回首页"
      :content="`分析报告：${store.stockCode}`" />

    <div v-if="!store.finalReport" class="no-report">
      <el-empty description="暂无报告，请先完成分析">
        <el-button type="primary" @click="router.push('/')">开始分析</el-button>
      </el-empty>
    </div>

    <template v-else>
      <!-- 综合评级卡片 -->
      <el-card class="rating-card" shadow="always">
        <div class="rating-content">
          <div class="rating-main">
            <el-tag :type="ratingColor" size="large" effect="dark" class="rating-tag">
              {{ finalReport.rating || finalReport.finalRating }}
            </el-tag>
            <span class="stock-name">{{ store.stockCode }}</span>
          </div>
          <div class="confidence">
            <span>置信度</span>
            <el-progress
              :percentage="finalReport.confidencePercent || 0"
              :color="progressColor"
              style="width: 200px; margin-left: 12px"
            />
          </div>
        </div>
      </el-card>

      <!-- 流水线报告：各 Agent 折叠 -->
      <template v-if="isPipelineReport">
        <el-card header="核心投资逻辑" class="section-card">
          <div class="text-block">{{ finalReport.coreLogic }}</div>
        </el-card>

        <el-card header="主要风险" class="section-card">
          <div class="text-block risk">{{ finalReport.mainRisks }}</div>
        </el-card>

        <el-card header="综合建议" class="section-card">
          <div class="text-block">{{ finalReport.summaryText }}</div>
        </el-card>

        <el-card header="目标价区间" class="section-card" v-if="finalReport.targetPriceLow">
          <el-statistic title="目标价下限" :value="finalReport.targetPriceLow" :precision="2" prefix="¥" style="display:inline-block;margin-right:40px" />
          <el-statistic title="目标价上限" :value="finalReport.targetPriceHigh" :precision="2" prefix="¥" style="display:inline-block" />
        </el-card>
      </template>

      <!-- 辩论报告：三方论点对比 -->
      <template v-if="isDebateReport">
        <el-card header="三方辩论论点" class="section-card">
          <div class="debate-grid">
            <div v-for="arg in finalReport.debateArguments" :key="arg.stance" class="debate-col">
              <el-card shadow="never" :class="['stance-card', stanceClass(arg.stance)]">
                <template #header>
                  <el-tag :type="stanceTagType(arg.stance)" effect="dark">
                    {{ stanceLabel(arg.stance) }}（{{ arg.model }}）
                  </el-tag>
                </template>
                <p><strong>核心观点：</strong>{{ arg.mainPoints }}</p>
                <p style="margin-top:8px"><strong>结论：</strong>{{ arg.conclusion }}</p>
              </el-card>
            </div>
          </div>
        </el-card>

        <el-card header="仲裁官裁决" class="section-card">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="最强论点">
              <el-tag>{{ finalReport.strongestArgument }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="最弱论点">
              <el-tag type="info">{{ finalReport.weakestArgument }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
          <div class="text-block" style="margin-top:12px">{{ finalReport.synthesisText }}</div>
        </el-card>
      </template>

      <div class="actions">
        <el-button @click="router.push('/')">重新分析</el-button>
        <el-button type="primary" @click="printReport">打印 / 导出 PDF</el-button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAnalysisStore } from '../stores/analysisStore.js'

const router = useRouter()
const store = useAnalysisStore()

const finalReport = computed(() => store.finalReport)

const isPipelineReport = computed(() => finalReport.value && 'rating' in finalReport.value)
const isDebateReport = computed(() => finalReport.value && 'finalRating' in finalReport.value)

const ratingColor = computed(() => {
  const r = finalReport.value?.rating || finalReport.value?.finalRating || ''
  if (r.includes('强烈买入')) return 'success'
  if (r.includes('买入')) return 'success'
  if (r.includes('持有')) return 'warning'
  if (r.includes('卖出')) return 'danger'
  return 'info'
})

const progressColor = computed(() => {
  const p = finalReport.value?.confidencePercent || 0
  if (p >= 70) return '#67c23a'
  if (p >= 50) return '#e6a23c'
  return '#f56c6c'
})

function stanceLabel(stance) {
  return { BULL: '多方', BEAR: '空方', NEUTRAL: '中立' }[stance] || stance
}
function stanceClass(stance) {
  return { BULL: 'bull', BEAR: 'bear', NEUTRAL: 'neutral' }[stance] || ''
}
function stanceTagType(stance) {
  return { BULL: 'success', BEAR: 'danger', NEUTRAL: 'info' }[stance] || 'info'
}

function printReport() {
  window.print()
}
</script>

<style scoped>
.report-container { padding: 20px; max-width: 1000px; margin: 0 auto; }
.no-report { padding: 80px 0; text-align: center; }
.rating-card { margin: 20px 0; }
.rating-content { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 16px; }
.rating-main { display: flex; align-items: center; gap: 16px; }
.rating-tag { font-size: 18px; padding: 12px 24px; }
.stock-name { font-size: 20px; font-weight: 600; color: #1a1a2e; }
.confidence { display: flex; align-items: center; }
.section-card { margin: 12px 0; }
.text-block { line-height: 1.8; color: #333; white-space: pre-wrap; }
.risk { color: #c0392b; }
.debate-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.stance-card.bull { border-color: #67c23a; }
.stance-card.bear { border-color: #f56c6c; }
.stance-card.neutral { border-color: #909399; }
.actions { margin-top: 24px; display: flex; gap: 12px; justify-content: flex-end; }
</style>
