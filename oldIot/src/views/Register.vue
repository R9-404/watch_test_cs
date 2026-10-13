<template>
  <el-row class="register" type="flex" justify="center" align="middle">
    <el-col :xs="18" :sm="16" :md="12" :lg="10" :xl="8">
      <el-card class="register-card" shadow="always">
        <div class="register-title">
          <span style="font-size: 24px;">智慧养老手表管理系统</span>
          <div class="register-sub">家属账号注册（需管理员发放邀请码）</div>
        </div>

        <el-form
          label-position="left"
          :model="registerForm"
          :rules="rules"
          ref="registerForm"
          label-width="90px"
          class="register-form"
        >
          <el-form-item label="邀请码" prop="code">
            <el-input
              v-model="registerForm.code"
              placeholder="请输入管理员提供的邀请码"
              maxlength="20"
              clearable
              @blur="checkCode">
              <el-button slot="append" @click="checkCode">验证</el-button>
            </el-input>
            <div v-if="codeMessage" :class="codeOk ? 'tip-ok' : 'tip-error'">{{ codeMessage }}</div>
          </el-form-item>

          <el-form-item label="姓名" prop="familyName">
            <el-input v-model="registerForm.familyName" maxlength="10" placeholder="请输入您的姓名"></el-input>
          </el-form-item>

          <el-form-item label="与老人关系" prop="familyRelation">
            <el-input v-model="registerForm.familyRelation" maxlength="10" placeholder="如：儿子、女儿"></el-input>
          </el-form-item>

          <el-form-item label="性别" prop="familySex">
            <el-radio-group v-model="registerForm.familySex">
              <el-radio label="男">男</el-radio>
              <el-radio label="女">女</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="手机号" prop="familyPhone">
            <el-input v-model="registerForm.familyPhone" maxlength="11" placeholder="用于登录的手机号"></el-input>
          </el-form-item>

          <el-form-item label="密码" prop="familyPassword">
            <el-input type="password" v-model="registerForm.familyPassword" placeholder="6-20个字符"></el-input>
          </el-form-item>

          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input type="password" v-model="registerForm.confirmPassword" placeholder="请再次输入密码"></el-input>
          </el-form-item>

          <el-form-item label="联系地址" prop="familyAddress">
            <el-input v-model="registerForm.familyAddress" maxlength="20" placeholder="选填"></el-input>
          </el-form-item>

          <el-form-item>
            <el-button class="green" :loading="submitting" @click="submitForm">提交注册</el-button>
            <el-button @click="goLogin">返回登录</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </el-col>
  </el-row>
</template>

<script>
import axios from 'axios'

export default {
  data () {
    var checkPhone = (rule, value, callback) => {
      if (!value) {
        return callback(new Error('手机号不能为空'))
      }
      if (!/^1\d{10}$/.test(value)) {
        return callback(new Error('手机号格式不正确'))
      }
      callback()
    }
    var checkConfirm = (rule, value, callback) => {
      if (!value) {
        return callback(new Error('请再次输入密码'))
      }
      if (value !== this.registerForm.familyPassword) {
        return callback(new Error('两次输入的密码不一致'))
      }
      callback()
    }
    return {
      submitting: false,
      codeMessage: '',
      codeOk: false,
      oldPersonName: '',
      registerForm: {
        code: '',
        familyName: '',
        familyRelation: '',
        familySex: '男',
        familyPhone: '',
        familyPassword: '',
        confirmPassword: '',
        familyAddress: ''
      },
      rules: {
        code: [
          { required: true, message: '请输入邀请码', trigger: 'blur' }
        ],
        familyName: [
          { required: true, message: '请输入姓名', trigger: 'blur' }
        ],
        familyRelation: [
          { required: true, message: '请输入与老人的关系', trigger: 'blur' }
        ],
        familySex: [
          { required: true, message: '请选择性别', trigger: 'change' }
        ],
        familyPhone: [
          { required: true, message: '请输入手机号', trigger: 'blur' },
          { validator: checkPhone, trigger: 'blur' }
        ],
        familyPassword: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 6, max: 20, message: '密码长度为6-20个字符', trigger: 'blur' }
        ],
        confirmPassword: [
          { validator: checkConfirm, trigger: 'blur' }
        ]
      }
    }
  },
  methods: {
    // 验证邀请码，并显示该邀请码绑定的老人
    checkCode () {
      var _this = this
      var code = (_this.registerForm.code || '').trim()
      _this.codeMessage = ''
      _this.codeOk = false
      if (code === '') {
        return
      }
      axios.post('restful/inviteCode/checkCode', { 'code': code }, {
        headers: { 'content-type': 'application/json' },
        withCredentials: true
      }).then(function (response) {
        var data = response.data
        if (data.returnCode === '1111') {
          _this.codeOk = true
          _this.oldPersonName = data.oldPersonName
          if (data.remainDays === -1) {
            _this.codeMessage = '邀请码有效，绑定老人：' + data.oldPersonName + '（长期有效）'
          } else {
            _this.codeMessage = '邀请码有效，绑定老人：' + data.oldPersonName + '（剩余 ' + data.remainDays + ' 天）'
          }
        } else {
          _this.codeOk = false
          _this.oldPersonName = ''
          _this.codeMessage = data.message ? data.message : '邀请码无效'
        }
      }).catch(function (error) {
        console.log(error)
        _this.codeOk = false
        _this.codeMessage = '邀请码验证失败，请稍后重试'
      })
    },
    submitForm () {
      var _this = this
      _this.$refs.registerForm.validate(function (valid) {
        if (!valid) {
          return false
        }
        if (!_this.codeOk) {
          _this.$message({
            type: 'warning',
            message: '请先验证邀请码是否可用'
          })
          return false
        }
        _this.submitting = true
        var params = {
          code: _this.registerForm.code.trim(),
          familyName: _this.registerForm.familyName,
          familyRelation: _this.registerForm.familyRelation,
          familySex: _this.registerForm.familySex,
          familyPhone: _this.registerForm.familyPhone,
          familyPassword: _this.registerForm.familyPassword,
          familyAddress: _this.registerForm.familyAddress
        }
        axios.post('restful/inviteCode/join', params, {
          headers: { 'content-type': 'application/json' },
          withCredentials: true
        }).then(function (response) {
          _this.submitting = false
          var data = response.data
          if (data.returnCode === '1111') {
            _this.$message({
              type: 'success',
              message: '注册成功！已绑定老人：' + data.oldPersonName + '，请使用手机号登录'
            })
            localStorage.setItem('login_phone', _this.registerForm.familyPhone)
            setTimeout(function () {
              _this.$router.push({ path: '/login' })
            }, 1500)
          } else {
            _this.$message({
              type: 'error',
              message: data.message ? data.message : '注册失败，请稍后重试'
            })
            // 邀请码被占用或过期时重新校验一次，避免用户重复提交
            _this.checkCode()
          }
        }).catch(function (error) {
          console.log(error)
          _this.submitting = false
          _this.$message({
            type: 'error',
            message: '注册失败，请检查后端服务是否已启动'
          })
        })
      })
    },
    goLogin () {
      this.$router.push({ path: '/login' })
    }
  }
}
</script>

<style scoped>
.register {
  height: 100vh;
  background-color: #52BAB5;
  overflow: auto;
}
.register-card {
  padding: 10px 20px 0;
  border-radius: 15px;
}
.register-title {
  text-align: left;
  padding-bottom: 15px;
}
.register-sub {
  font-size: 13px;
  color: #909399;
  margin-top: 6px;
}
.register-form {
  padding-bottom: 10px;
}
.green {
  background-color: #52BAB5;
  color: #fff;
}
.tip-ok {
  font-size: 12px;
  color: #52BAB5;
  line-height: 20px;
}
.tip-error {
  font-size: 12px;
  color: #F56C6C;
  line-height: 20px;
}
</style>
