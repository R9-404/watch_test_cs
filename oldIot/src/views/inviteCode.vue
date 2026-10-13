<template>
  <div class="invite">
    <!-- 工具栏 -->
    <div class="sys-research">
      <el-row class="row-search" :gutter="10">
        <el-col :span="6">
          <el-select v-model="state" placeholder="全部状态" @change="searchInviteCodes">
            <el-option label="全部状态" value="all"></el-option>
            <el-option label="可使用" value="unused"></el-option>
            <el-option label="已使用" value="used"></el-option>
            <el-option label="已过期" value="expired"></el-option>
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-button type="primary" plain @click="openCreateDialog">生成邀请码</el-button>
        </el-col>
        <el-col :span="10" class="tip">
          邀请码由管理员生成后发给家属，家属在登录页点“注册账号”填写邀请码即可自助注册，
          注册成功后自动绑定所选老人。
        </el-col>
      </el-row>
    </div>

    <!-- 生成邀请码弹窗 -->
    <el-dialog title="生成邀请码" :visible.sync="createDialogVisible" width="420px" @close="closeCreateDialog">
      <el-form label-width="100px" :model="createForm" :rules="rules" ref="createFormRef">
        <el-form-item label="绑定老人" prop="oldId">
          <el-select v-model="createForm.oldId" placeholder="请选择老人" style="width: 100%;">
            <el-option
              v-for="item in oldPeople"
              :key="item.oldId"
              :label="item.name + '（编号 ' + item.oldId + '）'"
              :value="item.oldId">
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="生成数量" prop="count">
          <el-input-number v-model="createForm.count" :min="1" :max="20"></el-input-number>
        </el-form-item>
        <el-form-item label="有效天数" prop="expireDays">
          <el-input v-model="createForm.expireDays" placeholder="留空表示长期有效"></el-input>
          <div class="tip-small">例如填 7 表示 7 天内有效</div>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="createDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="createInviteCode('createFormRef')">生 成</el-button>
      </span>
    </el-dialog>

    <!-- 生成结果弹窗 -->
    <el-dialog title="邀请码生成成功" :visible.sync="resultDialogVisible" width="420px">
      <div class="tip-small">请把下面的邀请码发给家属（每个邀请码只能使用一次）：</div>
      <div v-for="(item, index) in createdCodes" :key="index" class="code-line">
        <span class="code-text">{{ item }}</span>
        <el-button size="mini" plain type="primary" @click="copyCode(item)">复制</el-button>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button type="primary" @click="resultDialogVisible = false">关 闭</el-button>
      </span>
    </el-dialog>

    <!-- 邀请码列表 -->
    <el-table :data="inviteCodeList.slice((currentPage-1)*pageSize, currentPage*pageSize)"
              style="width: 100%" border>
      <el-table-column type="index" width="60" align="center"></el-table-column>
      <el-table-column prop="code" label="邀请码" width="140" align="center"></el-table-column>
      <el-table-column prop="oldPersonName" label="绑定老人" width="120" align="center"></el-table-column>
      <el-table-column prop="createUser" label="生成人" width="100" align="center"></el-table-column>
      <el-table-column prop="createTime" label="生成时间" width="160" align="center"></el-table-column>
      <el-table-column label="有效期" width="180" align="center">
        <template slot-scope="scope">
          <span v-if="scope.row.expireTime === ''">长期有效</span>
          <span v-else>{{ scope.row.expireTime }}<br><span class="tip-small">剩余 {{ scope.row.remainDays }} 天</span></span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.status === 'unused'" type="success">可使用</el-tag>
          <el-tag v-else-if="scope.row.status === 'used'" type="info">已使用</el-tag>
          <el-tag v-else type="danger">已过期</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="usedTime" label="使用时间" width="160" align="center"></el-table-column>
      <el-table-column label="操作" align="center">
        <template slot-scope="scope">
          <el-button size="mini" plain type="primary" @click="copyCode(scope.row.code)">复制</el-button>
          <el-button size="mini" plain type="danger"
                     :disabled="scope.row.status !== 'unused'"
                     @click="deleteInviteCode(scope.row.id)">作废</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div style="margin-top: 10px;text-align: center">
      <el-pagination
        @size-change="handleSizeChange"
        @current-change="changePage"
        :current-page.sync="currentPage"
        :page-size="pageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total">
      </el-pagination>
    </div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  data () {
    return {
      state: 'all',
      createDialogVisible: false,
      resultDialogVisible: false,
      createdCodes: [],
      oldPeople: [],
      inviteCodeList: [],
      total: 0,
      pageSize: 10,
      currentPage: 1,
      createForm: {
        oldId: '',
        count: 1,
        expireDays: ''
      },
      rules: {
        oldId: [
          { required: true, message: '请选择要绑定的老人', trigger: 'change' }
        ]
      }
    }
  },
  created () {
    this.searchInviteCodes()
    this.loadOldPeople()
  },
  methods: {
    // 邀请码列表
    searchInviteCodes () {
      var _this = this
      axios.post('restful/inviteCode/list',
        { 'state': _this.state },
        {
          headers: { 'content-type': 'application/json' },
          withCredentials: true
        }).then(function (response) {
        _this.inviteCodeList = response.data.inviteCodes || []
        _this.total = _this.inviteCodeList.length
      }).catch(function (error) {
        console.log(error)
        _this.$message({ type: 'error', message: '邀请码列表加载失败' })
      })
    },
    // 老人下拉列表
    loadOldPeople () {
      var _this = this
      axios.post('restful/inviteCode/oldPersonList', {},
        {
          headers: { 'content-type': 'application/json' },
          withCredentials: true
        }).then(function (response) {
        _this.oldPeople = response.data.oldPeople || []
      }).catch(function (error) {
        console.log(error)
      })
    },
    openCreateDialog () {
      this.createDialogVisible = true
      // 每次打开都重新拉取，避免刚添加的老人选不到
      this.loadOldPeople()
    },
    closeCreateDialog () {
      if (this.$refs.createFormRef) {
        this.$refs.createFormRef.resetFields()
      }
      this.createForm.count = 1
      this.createForm.expireDays = ''
    },
    // 生成邀请码
    createInviteCode (formName) {
      var _this = this
      _this.$refs[formName].validate(function (valid) {
        if (!valid) {
          return false
        }
        var expireDays = (_this.createForm.expireDays || '').toString().trim()
        if (expireDays !== '' && !/^\d+$/.test(expireDays)) {
          _this.$message({ type: 'warning', message: '有效天数只能填数字，留空表示长期有效' })
          return false
        }
        axios.post('restful/inviteCode/create',
          {
            oldId: _this.createForm.oldId,
            count: _this.createForm.count,
            expireDays: expireDays,
            createUser: localStorage.getItem('user_name') || '管理员'
          },
          {
            headers: { 'content-type': 'application/json' },
            withCredentials: true
          }).then(function (response) {
          if (response.data.returnCode === '1111') {
            _this.createdCodes = response.data.codes || []
            _this.createDialogVisible = false
            _this.resultDialogVisible = true
            _this.searchInviteCodes()
          } else {
            _this.$message({
              type: 'error',
              message: response.data.message ? response.data.message : '生成邀请码失败'
            })
          }
        }).catch(function (error) {
          console.log(error)
          _this.$message({ type: 'error', message: '生成邀请码失败，请稍后重试' })
        })
      })
    },
    // 作废邀请码
    async deleteInviteCode (id) {
      var _this = this
      try {
        await _this.$confirm('作废后该邀请码将无法再用于注册，是否继续？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        axios.post('restful/inviteCode/delete', { 'id': id },
          {
            headers: { 'content-type': 'application/json' },
            withCredentials: true
          }).then(function (response) {
          if (response.data.returnCode === '1111') {
            _this.$message({ type: 'success', message: '已作废' })
          } else {
            _this.$message({
              type: 'error',
              message: response.data.message ? response.data.message : '作废失败'
            })
          }
          _this.searchInviteCodes()
        }).catch(function (error) {
          console.log(error)
        })
      } catch (err) {
        _this.$message({ type: 'info', message: '已取消操作' })
      }
    },
    // 复制邀请码
    copyCode (code) {
      var _this = this
      var input = document.createElement('input')
      input.value = code
      document.body.appendChild(input)
      input.select()
      try {
        document.execCommand('copy')
        _this.$message({ type: 'success', message: '已复制邀请码：' + code })
      } catch (e) {
        _this.$message({ type: 'warning', message: '复制失败，请手动记录：' + code })
      }
      document.body.removeChild(input)
    },
    handleSizeChange (val) {
      this.pageSize = val
    },
    changePage (val) {
      this.currentPage = val
    }
  }
}
</script>

<style lang="less" scoped>
.invite {
  .sys-research {
    padding: 20px 20px 0;
  }
  .tip {
    color: #909399;
    font-size: 12px;
    line-height: 32px;
  }
  .tip-small {
    color: #909399;
    font-size: 12px;
  }
  .code-line {
    padding: 8px 0;
    font-size: 18px;
    letter-spacing: 2px;
    color: #52BAB5;
  }
  .code-text {
    display: inline-block;
    min-width: 140px;
    font-weight: bold;
  }
}
</style>
