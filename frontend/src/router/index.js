import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import AnalysisView from '../views/AnalysisView.vue'
import ReportView from '../views/ReportView.vue'

export default createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: HomeView },
    { path: '/analysis/:sessionId', component: AnalysisView },
    { path: '/report/:sessionId', component: ReportView }
  ]
})
