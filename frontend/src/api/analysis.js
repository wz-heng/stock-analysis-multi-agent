import axios from 'axios'

const http = axios.create({ baseURL: '/api' })

/**
 * 启动分析，返回 { sessionId, status, stockCode, mode }
 */
export async function startAnalysis(stockCode, mode, dateRange) {
  const { data } = await http.post('/analysis/start', { stockCode, mode, dateRange })
  return data
}

/**
 * 建立 SSE 连接，onEvent(parsedEvent) 回调每个事件，onError 回调错误
 * 返回 EventSource 实例，调用方负责 close()
 */
export function connectSSE(sessionId, onEvent, onError) {
  const es = new EventSource(`/api/analysis/${sessionId}/stream`)
  es.onmessage = (e) => {
    try {
      const event = JSON.parse(e.data)
      onEvent(event)
      if (event.stage === 'ANALYSIS_COMPLETED' || event.stage === 'ANALYSIS_FAILED') {
        es.close()
      }
    } catch (err) {
      console.warn('SSE parse error:', err)
    }
  }
  es.onerror = (e) => {
    es.close()
    if (onError) onError(e)
  }
  return es
}
