<template>
  <div class="home-container">
    <div class="hero">
      <h1>多Agent股票分析系统</h1>
      <p class="subtitle">GPT-4o × Claude × DeepSeek 三模型协同分析</p>
    </div>

    <el-card class="form-card" shadow="always">
      <el-form :model="form" label-width="100px" size="large">

        <el-form-item label="股票代码">
          <el-input
            v-model="form.stockCode"
            placeholder="例：600519.SH（茅台）、000858.SZ（五粮液）"
            clearable
            @keyup.enter="handleStart"
          />
        </el-form-item>

        <el-form-item label="分析模式">
          <el-radio-group v-model="form.mode">
            <el-radio-button value="pipeline">
              流水线模式
              <el-tooltip content="研究员→技术分析师→舆情分析师→投资经理，顺序分析" placement="top">
                <el-icon style="margin-left:4px"><InfoFilled /></el-icon>
              </el-tooltip>
            </el-radio-button>
            <el-radio-button value="debate">
              辩论模式
              <el-tooltip content="多方/空方/中立三方并行对抗，仲裁官综合裁决" placement="top">
                <el-icon style="margin-left:4px"><InfoFilled /></el-icon>
              </el-tooltip>
            </el-radio-button>
            <el-radio-button value="combined">
              串联模式
              <el-tooltip content="先流水线，再辩论，双阶段完整分析" placement="top">
                <el-icon style="margin-left:4px"><InfoFilled /></el-icon>
              </el-tooltip>
            </el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="时间范围">
          <el-segmented v-model="form.dateRange" :options="rangeOptions" />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            :disabled="!form.stockCode.trim()"
            style="width: 200px"
            @click="handleStart"
          >
            开始分析
          </el-button>
        </el-form-item>

      </el-form>
    </el-card>

    <div class="tips">
      <el-tag type="info" effect="plain">A股代码格式：沪市 600xxx.SH，深市 000xxx.SZ / 300xxx.SZ</el-tag>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { InfoFilled } from '@element-plus/icons-vue'
import { startAnalysis } from '../api/analysis.js'
import { useAnalysisStore } from '../stores/analysisStore.js'

const router = useRouter()
const store = useAnalysisStore()

const form = ref({ stockCode: '', mode: 'pipeline', dateRange: '3m' })
const loading = ref(false)

const rangeOptions = [
  { label: '1个月', value: '1m' },
  { label: '3个月', value: '3m' },
  { label: '6个月', value: '6m' },
  { label: '1年', value: '1y' }
]

async function handleStart() {
  const code = form.value.stockCode.trim()
  if (!code) return
  loading.value = true
  try {
    const result = await startAnalysis(code, form.value.mode, form.value.dateRange)
    store.startAnalysis(result.sessionId, code, form.value.mode, form.value.dateRange)
    router.push(`/analysis/${result.sessionId}`)
  } catch (e) {
    ElMessage.error('启动分析失败：' + (e.response?.data?.message || e.message))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.home-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}
.hero { text-align: center; margin-bottom: 40px; }
.hero h1 { font-size: 32px; font-weight: 700; color: #1a1a2e; margin-bottom: 8px; }
.subtitle { font-size: 16px; color: #666; }
.form-card { width: 100%; max-width: 680px; }
.tips { margin-top: 20px; }
</style>
