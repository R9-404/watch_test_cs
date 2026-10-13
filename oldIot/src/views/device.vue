<template>
  <div class="device">
    <!-- 工具栏 -->
    <div class="sys-research">
      <el-row :gutter="10" type="flex" align="middle">
        <el-col :span="4">
          <el-button type="primary" plain @click="createDialogVisible = true">登记新设备</el-button>
        </el-col>
        <el-col :span="4">
          <el-button plain @click="loadDevices">刷新</el-button>
        </el-col>
        <el-col :span="4">
          <el-switch v-model="autoRefresh" active-text="自动刷新"></el-switch>
        </el-col>
        <el-col :span="12" class="stat">
          共 {{ deviceList.length }} 台设备，在线
          <span class="online">{{ onlineCount }}</span> 台
        </el-col>
      </el-row>
      <el-row :gutter="10" class="alert-bar" type="flex" align="middle">
        <el-col :span="16">
          待处理异常：
          <el-badge :value="alertTotals.personTotal" :hidden="alertTotals.personTotal === 0" class="dot">
            <span class="dot-label">人员不合格</span>
          </el-badge>
          <el-badge :value="alertTotals.rateTotal" :hidden="alertTotals.rateTotal === 0" class="dot">
            <span class="dot-label">心率不合格</span>
          </el-badge>
          <el-badge :value="alertTotals.preTotal" :hidden="alertTotals.preTotal === 0" class="dot">
            <span class="dot-label">血压不合格</span>
          </el-badge>
        </el-col>
        <el-col :span="8" style="text-align: right;">
          <el-button size="mini" plain type="primary" :disabled="alertTotals.total === 0"
                     @click="openWorkbench">查看全部并处理</el-button>
        </el-col>
      </el-row>
      <div class="tip">
        把设备的「设备号 + 密钥」填进手表端（现在可先用模拟手表 watch-sim/watch-sim.js），
        手表就能上报数据；未绑定时先在下面生成一个绑定码，在手表上输入即可绑定老人。
        <b>点表格里的红色数字，可以直接看到问题原因和处理办法。</b>
      </div>
    </div>

    <!-- 设备表格 -->
    <el-table :data="deviceList" style="width: 100%" border>
      <el-table-column prop="deviceId" label="ID" width="60" align="center"></el-table-column>
      <el-table-column prop="deviceNo" label="设备号" width="130" align="center"></el-table-column>
      <el-table-column prop="alias" label="别名" width="120" align="center"></el-table-column>
      <el-table-column prop="oldPersonName" label="绑定老人" width="110" align="center"></el-table-column>
      <el-table-column label="在线" width="90" align="center">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.online === 1" type="success" size="mini">在线</el-tag>
          <el-tag v-else type="info" size="mini">离线</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="待处理异常（点击查看原因）" width="240" align="center">
        <template slot-scope="scope">
          <el-badge :value="alertOf(scope.row.deviceId).personCount" :hidden="alertOf(scope.row.deviceId).personCount === 0">
            <el-button size="mini" :type="alertOf(scope.row.deviceId).personCount ? 'danger' : 'default'"
                       plain @click="showAlert(scope.row, 'person')">人员不合格</el-button>
          </el-badge>
          <el-badge :value="alertOf(scope.row.deviceId).rateCount" :hidden="alertOf(scope.row.deviceId).rateCount === 0">
            <el-button size="mini" :type="alertOf(scope.row.deviceId).rateCount ? 'danger' : 'default'"
                       plain @click="showAlert(scope.row, 'rate')">心率不合格</el-button>
          </el-badge>
          <el-badge :value="alertOf(scope.row.deviceId).preCount" :hidden="alertOf(scope.row.deviceId).preCount === 0">
            <el-button size="mini" :type="alertOf(scope.row.deviceId).preCount ? 'danger' : 'default'"
                       plain @click="showAlert(scope.row, 'pre')">血压不合格</el-button>
          </el-badge>
        </template>
      </el-table-column>
      <el-table-column label="电量" width="80" align="center">
        <template slot-scope="scope">
          <span v-if="scope.row.battery === null || scope.row.battery === undefined">-</span>
          <span v-else>{{ scope.row.battery }}%</span>
        </template>
      </el-table-column>
      <el-table-column prop="lastTime" label="最后心跳" width="150" align="center">
        <template slot-scope="scope">
          <span>{{ scope.row.lastTime || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="最后位置" width="170" align="center">
        <template slot-scope="scope">
          <span v-if="scope.row.lastLng">{{ scope.row.lastLng }}, {{ scope.row.lastLat }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center">
        <template slot-scope="scope">
          <el-button size="mini" plain type="primary" @click="openBindCodeDialog(scope.row)">绑定码</el-button>
          <el-button size="mini" plain @click="resetSecret(scope.row)">重置密钥</el-button>
          <el-button size="mini" plain type="warning"
                     :disabled="!scope.row.oldId" @click="unbind(scope.row)">解绑</el-button>
          <el-button size="mini" plain type="danger" @click="deleteDevice(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 红点详情：说明原因 + 处理办法 -->
    <el-dialog :title="alertDialog.title" :visible.sync="alertDialog.visible" width="620px">
      <div v-if="alertDialog.loading" style="text-align:center;padding:20px;">正在读取 ...</div>
      <div v-else-if="alertDialog.hasAlert === 0" class="no-alert">
        <i class="el-icon-success" style="color:#52BAB5;font-size:32px;"></i>
        <div style="margin-top:8px;">{{ alertDialog.message }}</div>
      </div>
      <div v-else>
        <el-alert :title="'问题：' + alertDialog.reason" type="error" :closable="false" show-icon></el-alert>

        <table class="alert-table">
          <tbody>
            <tr><th>设备</th><td>{{ alertDialog.deviceNo }}（{{ alertDialog.alias }}）</td></tr>
            <tr><th>绑定老人</th><td>{{ alertDialog.oldPersonName }}</td></tr>
            <tr v-if="alertDialog.value !== ''"><th>本次测量</th><td>{{ alertDialog.value }}</td></tr>
            <tr v-if="alertDialog.threshold"><th>正常范围</th><td>{{ alertDialog.threshold }}</td></tr>
            <tr><th>发生时间</th><td>{{ alertDialog.occurTime }}</td></tr>
            <tr><th>判断依据</th><td>{{ alertDialog.detail }}</td></tr>
            <tr><th>怎么处理</th><td>{{ alertDialog.advice }}</td></tr>
          </tbody>
        </table>

        <div class="alert-actions">
          <el-button type="primary" :loading="alertDialog.resolving" @click="resolveAlert">标记已处理（消除红点）</el-button>
          <el-button plain @click="gotoPage(alertDialog.kind)">去相关页面看看</el-button>
        </div>
        <div class="tip">
          「标记已处理」表示这个问题已经人工确认/跟进过了：设备类问题会记录为已处理，
          心率血压会同时把这条记录从"异常"改为"已处理"，红点随之消失，历史记录仍然保留。
        </div>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="alertDialog.visible = false">关 闭</el-button>
      </span>
    </el-dialog>

    <!-- 全部待处理异常工作台 -->
    <el-dialog title="待处理异常" :visible.sync="workbench.visible" width="760px">
      <div class="tip">共 {{ workbench.list.length }} 条未处理异常。点「查看原因」看具体问题和处理办法，处理完点「标记已处理」消除红点。</div>
      <el-table :data="workbench.list" size="mini" border style="width:100%;margin-top:8px;">
        <el-table-column prop="deviceNo" label="设备号" width="120" align="center"></el-table-column>
        <el-table-column prop="alias" label="别名" width="110" align="center"></el-table-column>
        <el-table-column label="问题" width="120" align="center">
          <template slot-scope="scope">
            <el-tag type="danger" size="mini">{{ scope.row.kindText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" align="left"></el-table-column>
        <el-table-column label="操作" width="170" align="center">
          <template slot-scope="scope">
            <el-button size="mini" plain type="primary" @click="showAlert(scope.row, scope.row.kind)">查看原因</el-button>
            <el-button size="mini" plain @click="resolveAlertRow(scope.row)">已处理</el-button>
          </template>
        </el-table-column>
      </el-table>
      <span slot="footer" class="dialog-footer">
        <el-button @click="workbench.visible = false">关 闭</el-button>
      </span>
    </el-dialog>

    <!-- 登记新设备 -->
    <el-dialog title="登记新设备" :visible.sync="createDialogVisible" width="460px">
      <el-form label-width="100px" :model="createForm" ref="createFormRef">
        <el-form-item label="别名">
          <el-input v-model="createForm.alias" placeholder="如：爷爷的手表" maxlength="16"></el-input>
        </el-form-item>
        <el-form-item label="型号">
          <el-input v-model="createForm.model" placeholder="如：HarmonyOS-Wear"></el-input>
        </el-form-item>
        <el-form-item label="直接绑定老人">
          <el-select v-model="createForm.oldId" placeholder="可不选，之后用绑定码绑定" clearable style="width: 100%;">
            <el-option v-for="item in oldPeople" :key="item.oldId"
                       :label="item.name + '（编号 ' + item.oldId + '）'" :value="item.oldId"></el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <div v-if="createdDevice" class="created">
        <div class="created-title">设备已登记，请把下面的信息填到手表端：</div>
        <div class="created-line">设备号：<b>{{ createdDevice.deviceNo }}</b>
          <el-button size="mini" plain type="primary" @click="copy(createdDevice.deviceNo)">复制</el-button>
        </div>
        <div class="created-line">密　钥：<b class="secret">{{ createdDevice.secret }}</b>
          <el-button size="mini" plain type="primary" @click="copy(createdDevice.secret)">复制</el-button>
        </div>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="closeCreateDialog">关 闭</el-button>
        <el-button type="primary" @click="createDevice">登 记</el-button>
      </span>
    </el-dialog>

    <!-- 绑定码 -->
    <el-dialog title="设备绑定码" :visible.sync="bindCodeDialogVisible" width="560px">
      <div v-if="currentDevice" class="tip">
        设备：{{ currentDevice.deviceNo }}（{{ currentDevice.alias }}）；
        当前绑定老人：{{ currentDevice.oldPersonName }}
      </div>
      <el-form :inline="true" label-width="80px">
        <el-form-item label="绑定老人">
          <el-select v-model="bindOldId" placeholder="请选择老人" style="width: 160px;">
            <el-option v-for="item in oldPeople" :key="item.oldId"
                       :label="item.name + '（编号 ' + item.oldId + '）'" :value="item.oldId"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="有效天数">
          <el-input v-model="bindExpireDays" style="width: 90px;"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="createBindCode">生成绑定码</el-button>
        </el-form-item>
      </el-form>

      <div v-if="newBindCode" class="new-code">
        <div>生成成功，请在手表上输入：</div>
        <div class="code-text">{{ newBindCode.code }}
          <el-button size="mini" plain type="primary" @click="copy(newBindCode.code)">复制</el-button>
        </div>
        <div class="tip">有效期至 {{ newBindCode.expireTime }}，一个绑定码只能绑定一台手表</div>
      </div>

      <el-table :data="bindCodeList" size="mini" border style="width: 100%;margin-top: 10px;">
        <el-table-column prop="code" label="绑定码" width="110" align="center"></el-table-column>
        <el-table-column prop="expireTime" label="过期时间" width="160" align="center"></el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.used === 1" type="info" size="mini">已使用</el-tag>
            <el-tag v-else type="success" size="mini">未使用</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center">
          <template slot-scope="scope">
            <el-button size="mini" plain type="danger" :disabled="scope.row.used === 1"
                       @click="deleteBindCode(scope.row)">作废</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  data () {
    return {
      deviceList: [],
      oldPeople: [],
      autoRefresh: true,
      timer: null,
      createDialogVisible: false,
      bindCodeDialogVisible: false,
      createdDevice: null,
      currentDevice: null,
      bindCodeList: [],
      newBindCode: null,
      bindOldId: '',
      bindExpireDays: '1',
      createForm: {
        alias: '',
        model: 'HarmonyOS-Wear',
        oldId: ''
      },
      // 红点：各设备未处理异常的条数
      alertCounts: {},
      alertTotals: { personTotal: 0, rateTotal: 0, preTotal: 0, total: 0 },
      alertDialog: {
        visible: false,
        loading: false,
        resolving: false,
        hasAlert: 1,
        title: '异常原因',
        kind: 'person',
        deviceId: null,
        deviceNo: '',
        alias: '',
        oldPersonName: '',
        reason: '',
        detail: '',
        value: '',
        threshold: '',
        occurTime: '',
        advice: '',
        recordId: null,
        message: ''
      },
      workbench: {
        visible: false,
        list: []
      }
    }
  },
  computed: {
    onlineCount () {
      var count = 0
      this.deviceList.forEach(function (item) {
        if (item.online === 1) {
          count++
        }
      })
      return count
    }
  },
  created () {
    this.loadDevices()
    this.loadOldPeople()
  },
  mounted () {
    this.startTimer()
  },
  beforeDestroy () {
    this.stopTimer()
  },
  methods: {
    post (path, params) {
      return axios.post('restful/device/' + path, params || {}, {
        headers: { 'content-type': 'application/json' },
        withCredentials: true
      })
    },
    loadDevices () {
      var _this = this
      this.post('list').then(function (response) {
        _this.deviceList = response.data.devices || []
      }).catch(function (error) {
        console.log(error)
      })
      this.loadAlertCounts()
    },
    // 未处理异常条数（红点小数字）
    loadAlertCounts () {
      var _this = this
      this.post('alertCounts').then(function (response) {
        var map = {}
        ;(response.data.items || []).forEach(function (item) {
          map[item.deviceId] = item
        })
        _this.alertCounts = map
        _this.alertTotals = {
          personTotal: response.data.personTotal || 0,
          rateTotal: response.data.rateTotal || 0,
          preTotal: response.data.preTotal || 0,
          total: response.data.total || 0
        }
      }).catch(function (error) {
        console.log(error)
      })
    },
    alertOf (deviceId) {
      return this.alertCounts[deviceId] || { personCount: 0, rateCount: 0, preCount: 0 }
    },
    // 点红点：查看问题原因与处理办法
    showAlert (row, kind) {
      var _this = this
      this.alertDialog = {
        visible: true, loading: true, resolving: false, hasAlert: 1,
        title: kind === 'rate' ? '心率不合格 · 原因与处理' : (kind === 'pre' ? '血压不合格 · 原因与处理' : '人员不合格 · 原因与处理'),
        kind: kind, deviceId: row.deviceId, deviceNo: row.deviceNo, alias: row.alias || '',
        oldPersonName: '', reason: '', detail: '', value: '', threshold: '',
        occurTime: '', advice: '', recordId: null, message: ''
      }
      this.post('alertDetail', { deviceId: row.deviceId, kind: kind }).then(function (response) {
        var d = response.data
        _this.alertDialog.loading = false
        _this.alertDialog.hasAlert = d.hasAlert === undefined ? 1 : d.hasAlert
        _this.alertDialog.message = d.message || ''
        _this.alertDialog.oldPersonName = d.oldPersonName || ''
        _this.alertDialog.reason = d.reason || ''
        _this.alertDialog.detail = d.detail || ''
        _this.alertDialog.value = d.value === undefined || d.value === null ? '' : d.value
        _this.alertDialog.threshold = d.threshold || ''
        _this.alertDialog.occurTime = d.occurTime || ''
        _this.alertDialog.advice = d.advice || ''
        _this.alertDialog.recordId = d.recordId === undefined ? null : d.recordId
      }).catch(function (error) {
        console.log(error)
        _this.alertDialog.loading = false
        _this.$message({ type: 'error', message: '读取异常详情失败' })
      })
    },
    // 消除红点
    resolveAlert () {
      var _this = this
      this.alertDialog.resolving = true
      this.post('resolveAlert', {
        deviceId: this.alertDialog.deviceId,
        kind: this.alertDialog.kind,
        recordId: this.alertDialog.recordId === null ? '' : this.alertDialog.recordId
      }).then(function (response) {
        _this.alertDialog.resolving = false
        if (response.data.returnCode === '1111') {
          _this.$message({ type: 'success', message: '已处理：' + (_this.alertDialog.reason || '异常') + '，红点已消除' })
          _this.alertDialog.visible = false
          _this.loadDevices()
        } else {
          _this.$message({ type: 'error', message: response.data.message || '处理失败' })
        }
      }).catch(function (error) {
        console.log(error)
        _this.alertDialog.resolving = false
        _this.$message({ type: 'error', message: '处理失败' })
      })
    },
    // 工作台里的"已处理"
    resolveAlertRow (row) {
      var _this = this
      this.post('resolveAlert', { deviceId: row.deviceId, kind: row.kind, recordId: row.recordId || '' })
        .then(function (response) {
          if (response.data.returnCode === '1111') {
            _this.$message({ type: 'success', message: '已处理，红点已消除' })
            _this.loadDevices()
            _this.openWorkbench()
          } else {
            _this.$message({ type: 'error', message: response.data.message || '处理失败' })
          }
        }).catch(function (error) {
          console.log(error)
        })
    },
    // 打开"全部待处理异常"
    openWorkbench () {
      var _this = this
      this.workbench.visible = true
      var list = []
      var kinds = ['person', 'rate', 'pre']
      var devices = this.deviceList.slice()
      var tasks = []
      devices.forEach(function (device) {
        kinds.forEach(function (kind) {
          var counts = _this.alertOf(device.deviceId)
          var count = kind === 'person' ? counts.personCount : (kind === 'rate' ? counts.rateCount : counts.preCount)
          if (count > 0) {
            tasks.push(_this.post('alertDetail', { deviceId: device.deviceId, kind: kind }).then(function (response) {
              var d = response.data
              if (d.hasAlert === 1) {
                list.push({
                  deviceId: device.deviceId,
                  deviceNo: d.deviceNo,
                  alias: d.alias,
                  oldPersonName: d.oldPersonName,
                  kind: kind,
                  kindText: d.kindText,
                  reason: d.reason,
                  recordId: d.recordId
                })
              }
            }))
          }
        })
      })
      Promise.all(tasks).then(function () {
        _this.workbench.list = list
      })
    },
    // 跳到相关页面看原始数据
    gotoPage (kind) {
      if (kind === 'person') {
        this.$router.push({ path: '/watch' })
      } else {
        this.$router.push({ path: '/ratepre' })
      }
      this.alertDialog.visible = false
    },
    loadOldPeople () {
      var _this = this
      axios.post('restful/inviteCode/oldPersonList', {}, {
        headers: { 'content-type': 'application/json' },
        withCredentials: true
      }).then(function (response) {
        _this.oldPeople = response.data.oldPeople || []
      }).catch(function (error) {
        console.log(error)
      })
    },
    startTimer () {
      var _this = this
      this.stopTimer()
      // 每 5 秒刷新一次，便于观察设备上下线
      this.timer = setInterval(function () {
        if (_this.autoRefresh) {
          _this.loadDevices()
        }
      }, 5000)
    },
    stopTimer () {
      if (this.timer) {
        clearInterval(this.timer)
        this.timer = null
      }
    },
    createDevice () {
      var _this = this
      var params = {
        alias: this.createForm.alias || '新手表',
        model: this.createForm.model || 'HarmonyOS-Wear',
        oldId: this.createForm.oldId === '' ? '' : this.createForm.oldId
      }
      this.post('create', params).then(function (response) {
        if (response.data.returnCode === '1111') {
          _this.createdDevice = response.data
          _this.$message({ type: 'success', message: '设备登记成功' })
          _this.loadDevices()
        } else {
          _this.$message({ type: 'error', message: response.data.message || '登记失败' })
        }
      }).catch(function (error) {
        console.log(error)
        _this.$message({ type: 'error', message: '登记失败' })
      })
    },
    closeCreateDialog () {
      this.createDialogVisible = false
      this.createdDevice = null
      this.createForm = { alias: '', model: 'HarmonyOS-Wear', oldId: '' }
    },
    openBindCodeDialog (row) {
      this.currentDevice = row
      this.bindOldId = row.oldId || ''
      this.bindExpireDays = '1'
      this.newBindCode = null
      this.bindCodeDialogVisible = true
      this.loadBindCodes(row.oldId)
    },
    loadBindCodes (oldId) {
      var _this = this
      if (!oldId) {
        this.bindCodeList = []
        return
      }
      this.post('bindCodeList', { oldId: oldId }).then(function (response) {
        _this.bindCodeList = response.data.bindCodes || []
      }).catch(function (error) {
        console.log(error)
      })
    },
    createBindCode () {
      var _this = this
      if (!this.bindOldId) {
        this.$message({ type: 'warning', message: '请先选择要绑定的老人' })
        return
      }
      if (!/^\d+$/.test(this.bindExpireDays)) {
        this.$message({ type: 'warning', message: '有效天数只能填数字' })
        return
      }
      this.post('createBindCode', {
        oldId: this.bindOldId,
        expireDays: this.bindExpireDays,
        createUser: localStorage.getItem('user_name') || '管理员'
      }).then(function (response) {
        if (response.data.returnCode === '1111') {
          _this.newBindCode = response.data
          _this.loadBindCodes(_this.bindOldId)
        } else {
          _this.$message({ type: 'error', message: response.data.message || '生成失败' })
        }
      }).catch(function (error) {
        console.log(error)
      })
    },
    deleteBindCode (row) {
      var _this = this
      this.post('deleteBindCode', { id: row.id }).then(function (response) {
        if (response.data.returnCode === '1111') {
          _this.$message({ type: 'success', message: '已作废' })
          _this.loadBindCodes(_this.bindOldId)
        } else {
          _this.$message({ type: 'error', message: response.data.message || '作废失败' })
        }
      }).catch(function (error) {
        console.log(error)
      })
    },
    resetSecret (row) {
      var _this = this
      this.$confirm('重置后手表端需要用新密钥重新登录，是否继续？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(function () {
        _this.post('resetSecret', { deviceId: row.deviceId }).then(function (response) {
          if (response.data.returnCode === '1111') {
            _this.$alert('新密钥：' + response.data.secret, '密钥已重置', { confirmButtonText: '知道了' })
            _this.loadDevices()
          } else {
            _this.$message({ type: 'error', message: response.data.message || '重置失败' })
          }
        })
      }).catch(function () {})
    },
    unbind (row) {
      var _this = this
      this.$confirm('解绑后该手表将不再上报到这位老人，是否继续？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(function () {
        _this.post('unbind', { deviceId: row.deviceId }).then(function (response) {
          if (response.data.returnCode === '1111') {
            _this.$message({ type: 'success', message: '已解绑' })
            _this.loadDevices()
          } else {
            _this.$message({ type: 'error', message: response.data.message || '解绑失败' })
          }
        })
      }).catch(function () {})
    },
    deleteDevice (row) {
      var _this = this
      this.$confirm('删除后需要重新登记设备，是否继续？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(function () {
        _this.post('delete', { deviceId: row.deviceId }).then(function (response) {
          if (response.data.returnCode === '1111') {
            _this.$message({ type: 'success', message: '已删除' })
            _this.loadDevices()
          } else {
            _this.$message({ type: 'error', message: response.data.message || '删除失败' })
          }
        })
      }).catch(function () {})
    },
    copy (text) {
      var input = document.createElement('input')
      input.value = text
      document.body.appendChild(input)
      input.select()
      try {
        document.execCommand('copy')
        this.$message({ type: 'success', message: '已复制：' + text })
      } catch (e) {
        this.$message({ type: 'warning', message: '复制失败，请手动记录：' + text })
      }
      document.body.removeChild(input)
    }
  }
}
</script>

<style lang="less" scoped>
.device {
  .sys-research {
    padding: 20px 20px 0;
  }
  .stat {
    color: #606266;
    line-height: 32px;
  }
  .online {
    color: #52BAB5;
    font-weight: bold;
  }
  .tip {
    color: #909399;
    font-size: 12px;
    line-height: 24px;
    padding-top: 6px;
  }
  .created {
    margin-top: 10px;
    padding: 10px;
    background: #f5f7fa;
    border-radius: 6px;
  }
  .created-title {
    font-size: 13px;
    color: #606266;
    padding-bottom: 6px;
  }
  .created-line {
    font-size: 14px;
    line-height: 28px;
  }
  .secret {
    color: #52BAB5;
    word-break: break-all;
  }
  .new-code {
    padding: 10px;
    background: #f0f9f8;
    border-radius: 6px;
  }
  .alert-bar {
    padding-top: 12px;
  }
  .alert-bar .dot {
    margin-right: 26px;
  }
  .alert-bar .dot-label {
    font-size: 13px;
    color: #606266;
    padding: 0 4px;
  }
  .alert-table {
    width: 100%;
    margin-top: 12px;
    border-collapse: collapse;
    font-size: 13px;
  }
  .alert-table th {
    width: 90px;
    text-align: left;
    color: #909399;
    font-weight: normal;
    padding: 6px 8px;
    vertical-align: top;
    background: #fafafa;
    border: 1px solid #ebeef5;
  }
  .alert-table td {
    padding: 6px 8px;
    color: #303133;
    border: 1px solid #ebeef5;
    line-height: 20px;
  }
  .alert-actions {
    margin-top: 14px;
  }
  .no-alert {
    text-align: center;
    padding: 20px;
    color: #606266;
  }
  .code-text {
    font-size: 26px;
    letter-spacing: 4px;
    color: #52BAB5;
    font-weight: bold;
    padding: 4px 0;
  }
}
</style>
