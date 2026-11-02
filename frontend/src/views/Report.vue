<template>
  <div class="report-container">
    <el-card class="report-card">
      <template #header>
        <div class="card-header">
          <el-button type="info" @click="goBack" :icon="ArrowLeft">返回首页</el-button>
          <span class="title">基础报表</span>
        </div>
      </template>

      <!-- 报表类型选择 -->
      <el-tabs v-model="activeTab" class="report-tabs">
        <el-tab-pane label="部门资产统计" name="department">
          <div class="department-stats">
            <el-row :gutter="20">
              <el-col :span="24">
                <el-card shadow="hover">
                  <template #header>
                    <div class="card-title">部门资产分布</div>
                  </template>
                  <div class="chart-container">
                    <div ref="departmentChartRef" class="chart"></div>
                  </div>
                </el-card>
              </el-col>
            </el-row>
            
            <el-row :gutter="20" style="margin-top: 20px;">
              <el-col :span="24">
                <el-card shadow="hover">
                  <template #header>
                    <div class="card-title">部门资产详细统计</div>
                  </template>
                  <el-table :data="departmentStats" style="width: 100%">
                    <el-table-column prop="departmentName" label="部门名称" min-width="120" />
                    <el-table-column prop="assetCount" label="资产数量" width="100" />
                    <el-table-column prop="totalValue" label="资产总价值" width="120">
                      <template #default="scope">
                        {{ formatPrice(scope.row.totalValue) }}
                      </template>
                    </el-table-column>
                    <el-table-column prop="averageValue" label="平均价值" width="120">
                      <template #default="scope">
                        {{ formatPrice(scope.row.averageValue) }}
                      </template>
                    </el-table-column>
                    <el-table-column prop="inUseCount" label="使用中" width="80" />
                    <el-table-column prop="idleCount" label="闲置" width="80" />
                    <el-table-column prop="maintenanceCount" label="维修中" width="80" />
                    <el-table-column prop="scrappedCount" label="已报废" width="80" />
                  </el-table>
                </el-card>
              </el-col>
            </el-row>
          </div>
        </el-tab-pane>
        
        <el-tab-pane label="资产状态分布" name="status">
          <div class="status-distribution">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-card shadow="hover">
                  <template #header>
                    <div class="card-title">状态数量分布</div>
                  </template>
                  <div class="chart-container">
                    <div ref="statusChartRef" class="chart"></div>
                  </div>
                </el-card>
              </el-col>
              <el-col :span="12">
                <el-card shadow="hover">
                  <template #header>
                    <div class="card-title">状态价值分布</div>
                  </template>
                  <div class="chart-container">
                    <div ref="statusValueChartRef" class="chart"></div>
                  </div>
                </el-card>
              </el-col>
            </el-row>
            
            <el-row :gutter="20" style="margin-top: 20px;">
              <el-col :span="24">
                <el-card shadow="hover">
                  <template #header>
                    <div class="card-title">状态详细统计</div>
                  </template>
                  <el-table :data="statusDistribution" style="width: 100%">
                    <el-table-column prop="statusName" label="状态" min-width="100" />
                    <el-table-column prop="count" label="数量" width="80" />
                    <el-table-column prop="percentage" label="占比" width="100">
                      <template #default="scope">
                        {{ scope.row.percentage.toFixed(2) }}%
                      </template>
                    </el-table-column>
                    <el-table-column prop="totalValue" label="总价值" width="120">
                      <template #default="scope">
                        {{ formatPrice(scope.row.totalValue) }}
                      </template>
                    </el-table-column>
                  </el-table>
                </el-card>
              </el-col>
            </el-row>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script>
import { ref, onMounted, watch, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from '../utils/request'
import * as echarts from 'echarts'
import { ArrowLeft } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/userStore'

export default {
  name: 'Report',
  setup() {
    const router = useRouter()
    const userStore = useUserStore()
    const activeTab = ref('department')
    const departmentStats = ref([])
    const statusDistribution = ref([])
    const departmentChartRef = ref(null)
    const statusChartRef = ref(null)
    const statusValueChartRef = ref(null)
    const departmentChart = ref(null)
    const statusChart = ref(null)
    const statusValueChart = ref(null)

    const goBack = () => {
      router.push('/home')
    }

    const formatPrice = (price) => {
      if (price === null || price === undefined) return '-'
      return '¥' + parseFloat(price).toFixed(2)
    }

    const fetchDepartmentStats = async () => {
      try {
        const user = userStore.user || {}
        const userRole = userStore.currentUserRole

        let response
        if ((userRole === 'manager' || userRole === 'leader') && user.departmentId) {
          // 获取部门资产统计
          response = await axios.get(`/reports/department-stats/${user.departmentId}`)
        } else {
          // 系统管理员可以查看所有部门数据
          response = await axios.get('/reports/department-stats')
        }

        if (response.data.code === 200) {
          departmentStats.value = response.data.data
          renderDepartmentChart()
        }
      } catch (error) {
        console.error('获取部门资产统计失败', error)
      }
    }

    // 获取资产状态分布
    const fetchStatusDistribution = async () => {
      try {
        // 获取资产状态分布
        const response = await axios.get('/reports/status-distribution')
        if (response.data.code === 200) {
          statusDistribution.value = response.data.data
          renderStatusCharts()
        }
      } catch (error) {
        console.error('获取资产状态分布失败', error)
      }
    }

    const renderDepartmentChart = () => {
      if (!departmentChartRef.value) return
      
      if (departmentChart.value) {
        departmentChart.value.dispose()
      }
      
      departmentChart.value = echarts.init(departmentChartRef.value)
      
      const departments = departmentStats.value.map(item => item.departmentName)
      const assetCounts = departmentStats.value.map(item => item.assetCount)
      
      const option = {
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          }
        },
        legend: {
          data: ['资产数量']
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: departments,
          axisLabel: {
            rotate: 45
          }
        },
        yAxis: {
          type: 'value'
        },
        series: [
          {
            name: '资产数量',
            type: 'bar',
            data: assetCounts,
            itemStyle: {
              color: '#409EFF'
            }
          }
        ]
      }
      
      departmentChart.value.setOption(option)
    }

    const renderStatusCharts = () => {
      // 状态数量分布
      if (statusChartRef.value) {
        if (statusChart.value) {
          statusChart.value.dispose()
        }
        
        statusChart.value = echarts.init(statusChartRef.value)
        
        const statusNames = statusDistribution.value.map(item => item.statusName)
        const counts = statusDistribution.value.map(item => item.count)
        
        const option = {
          tooltip: {
            trigger: 'item'
          },
          legend: {
            orient: 'vertical',
            left: 'left'
          },
          series: [
            {
              name: '资产状态',
              type: 'pie',
              radius: '60%',
              data: statusDistribution.value.map(item => ({
                name: item.statusName,
                value: item.count
              })),
              emphasis: {
                itemStyle: {
                  shadowBlur: 10,
                  shadowOffsetX: 0,
                  shadowColor: 'rgba(0, 0, 0, 0.5)'
                }
              }
            }
          ]
        }
        
        statusChart.value.setOption(option)
      }
      
      // 状态价值分布
      if (statusValueChartRef.value) {
        if (statusValueChart.value) {
          statusValueChart.value.dispose()
        }
        
        statusValueChart.value = echarts.init(statusValueChartRef.value)
        
        const statusNames = statusDistribution.value.map(item => item.statusName)
        const values = statusDistribution.value.map(item => item.totalValue)
        
        const option = {
          tooltip: {
            trigger: 'axis',
            axisPointer: {
              type: 'shadow'
            }
          },
          legend: {
            data: ['资产价值'],
            top: '5%',
            left: 'center'
          },
          grid: {
            left: '3%',
            right: '4%',
            bottom: '15%',
            top: '15%',
            containLabel: true
          },
          xAxis: {
            type: 'category',
            data: statusNames,
            axisLabel: {
              rotate: 0
            }
          },
          yAxis: {
            type: 'value'
          },
          series: [
            {
              name: '资产价值',
              type: 'bar',
              data: values,
              itemStyle: {
                color: '#67C23A'
              },
              barWidth: '50%'
            }
          ]
        }
        
        statusValueChart.value.setOption(option)
      }
    }

    const handleResize = () => {
      if (departmentChart.value) {
        try {
          departmentChart.value.resize()
        } catch (e) {
          console.warn('部门图表 resize 失败:', e.message)
        }
      }
      if (statusChart.value) {
        try {
          statusChart.value.resize()
        } catch (e) {
          console.warn('状态图表 resize 失败:', e.message)
        }
      }
      if (statusValueChart.value) {
        try {
          statusValueChart.value.resize()
        } catch (e) {
          console.warn('状态价值图表 resize 失败:', e.message)
        }
      }
    }

    watch(activeTab, (newTab) => {
      if (newTab === 'department' && departmentStats.value.length === 0) {
        fetchDepartmentStats()
      } else if (newTab === 'status') {
        if (statusDistribution.value.length === 0) {
          fetchStatusDistribution()
        } else {
          // 如果数据已存在，重新渲染图表确保尺寸正确
          setTimeout(() => {
            renderStatusCharts()
          }, 100)
        }
      }
    })

    onMounted(() => {
      fetchDepartmentStats()
      fetchStatusDistribution()
      window.addEventListener('resize', handleResize)
    })

    onUnmounted(() => {
      window.removeEventListener('resize', handleResize)
      departmentChart.value?.dispose()
      statusChart.value?.dispose()
      statusValueChart.value?.dispose()
    })

    return {
      activeTab,
      departmentStats,
      statusDistribution,
      departmentChartRef,
      statusChartRef,
      statusValueChartRef,
      goBack,
      formatPrice
    }
  }
}
</script>

<style scoped>
.report-container {
  padding: 20px;
}

.report-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  font-size: 18px;
  font-weight: bold;
}

.report-tabs {
  margin-top: 20px;
}

.card-title {
  font-size: 16px;
  font-weight: bold;
}

.chart-container {
  height: 400px;
  width: 100%;
}

.chart {
  width: 100%;
  height: 100%;
}

.department-stats,
.status-distribution {
  margin-top: 20px;
}
</style>
