import { defineStore } from 'pinia'

export const useAnalysisStore = defineStore('analysis', {
  state: () => ({
    sessionId: null,
    stockCode: '',
    mode: 'pipeline',
    dateRange: '3m',
    // 进度事件列表：[{ stage, message, data, ts }]
    events: [],
    // 最终报告
    finalReport: null,
    // 是否分析中
    running: false,
    error: null
  }),

  getters: {
    stageMap: (state) => {
      const map = {}
      state.events.forEach(e => { map[e.stage] = e })
      return map
    },
    isPipelineMode: (state) => state.mode === 'pipeline' || state.mode === 'combined',
    isDebateMode: (state) => state.mode === 'debate' || state.mode === 'combined',
    isCompleted: (state) => state.events.some(e =>
      e.stage === 'ANALYSIS_COMPLETED' || e.stage === 'ANALYSIS_FAILED')
  },

  actions: {
    startAnalysis(sessionId, stockCode, mode, dateRange) {
      this.sessionId = sessionId
      this.stockCode = stockCode
      this.mode = mode
      this.dateRange = dateRange
      this.events = []
      this.finalReport = null
      this.running = true
      this.error = null
    },
    addEvent(event) {
      this.events.push({ ...event, ts: Date.now() })
      if (event.stage === 'ANALYSIS_COMPLETED') {
        this.finalReport = event.data
        this.running = false
      }
      if (event.stage === 'ANALYSIS_FAILED') {
        this.error = event.message
        this.running = false
      }
    },
    reset() {
      this.$reset()
    }
  }
})
