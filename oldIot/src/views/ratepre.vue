<template>
  <div class="roles">
    <!-- 查询和添加 -->
    <div class="sys-research">
      <el-row class="row-search">
        <el-col :span="8">
          <!-- 查询 -->
          <el-input
            placeholder="请输入搜索内容"
            v-model="searchText"
            class="input-with-select">
            <el-button
              slot="append"
              icon="el-icon-search"
              @click="searchOlds"></el-button>
          </el-input>
        </el-col>
        <el-col :span="8">
        <div class="block">
        <el-date-picker
        v-model="value6"
		value-format="yyyy-MM-dd"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
				style="display: none;">
        </el-date-picker>
        </div>
        </el-col>
		<el-badge :value="this.rateCount" :hidden="this.rateCount === 0" class="item">
		  <el-button size="small" type="danger" plain @click="showAlertByKind('rate')">心率不合格（点我看原因）</el-button>
		</el-badge>
		<el-badge :value="this.preCount" :hidden="this.preCount === 0" class="item" style="margin-left: 20px;">
			<el-button size="small" type="danger" plain @click="showAlertByKind('pre')">血压不合格（点我看原因）</el-button>
		</el-badge>
		<el-button size="small" plain type="primary" style="margin-left: 20px;" @click="openWorkbench">只看待处理异常</el-button>
      </el-row>
    </div>
    <!-- start 添加角色弹窗 -->
    <el-dialog
      title="角色添加"
      :visible.sync="roleDialogVisible"
      @close="closeRoleAddDialog"
      width="30%">
      <el-form label-width="80px" :model="roleForm"  ref="addRoleFormRef">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="roleForm.roleName"></el-input>
        </el-form-item>
        <el-form-item label="角色描述" prop="roleDesc">
          <el-input v-model="roleForm.roleDesc"></el-input>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
      <el-button @click="roleDialogVisible = false">取 消</el-button>
      <el-button type="primary" @click="addRoles('addRoleFormRef')">确 定</el-button>
    </span>
    </el-dialog>
    <!-- end 添加角色弹窗 -->
    
    <!-- 表格 -->
    <el-table :data="oldsList.slice((currentPage-1)*pageSize,currentPage*pageSize)"
    style="width: 100%" border>
    <!-- 可展开行 -->
      
      <el-table-column type="index" width="150" align="center"></el-table-column>
      <el-table-column prop="name" label="姓名" width="180" align="center"></el-table-column>
			<!-- <el-table-column prop="oldId" style="display: none;" label="id" width="180" align="center"></el-table-column> -->
      <el-table-column prop="sex" label="性别" width="150" align="center"></el-table-column>
			<el-table-column prop="preState" label="血压状态" width="150" align="center">
				<template slot-scope="scope">
                  <span v-if="scope.row.preState==1" style="color:red">异常</span>
                  <span v-else-if="scope.row.preHandled==1" style="color:#E6A23C">已处理</span>
                  <span v-else style="color: #37B328">正常</span>
				</template>
			</el-table-column>
			<el-table-column prop="rateState" label="心率状态" width="150" align="center">
				<template slot-scope="scope">
					<span v-if="scope.row.rateState==1" style="color:red">异常</span>
					<span v-else-if="scope.row.rateHandled==1" style="color:#E6A23C">已处理</span>
					<span v-else style="color: #37B328">正常</span>
				</template>
			</el-table-column>
			<el-table-column prop="createTime" label="时间" width="170" align="center"></el-table-column>
      <el-table-column label="操作" align="center">
        <template slot-scope="scope">
          <el-button v-if="scope.row.rateState==1 || scope.row.preState==1"
                     size="mini" plain type="danger" icon="el-icon-warning"
                     @click="showAlertRow(scope.row)">查看原因</el-button>
          <el-button v-if="scope.row.rateState==1 || scope.row.preState==1"
                     size="mini" plain type="primary" icon="el-icon-check"
                     @click="resolveRow(scope.row)">标记已处理</el-button>
          <el-button size="mini" plain type="success" icon="el-icon-check" @click="showRoleDialog(scope.row)">
            详细情况
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <!-- start 分页 -->
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
    <!-- end 分页 -->
    <!-- 异常原因与处理（红点点开看这个） -->
    <el-dialog :title="alertDialog.title" :visible.sync="alertDialog.visible" width="620px">
      <div v-if="alertDialog.loading" style="text-align:center;padding:20px;">正在读取 ...</div>
      <div v-else-if="alertDialog.hasAlert === 0" style="text-align:center;padding:20px;color:#606266;">
        <i class="el-icon-success" style="color:#52BAB5;font-size:32px;"></i>
        <div style="margin-top:8px;">{{ alertDialog.message }}</div>
      </div>
      <div v-else>
        <el-alert :title="'问题：' + alertDialog.reason" type="error" :closable="false" show-icon></el-alert>
        <table class="alert-table">
          <tbody>
            <tr><th>老人</th><td>{{ alertDialog.oldPersonName }}</td></tr>
            <tr><th>本次测量</th><td>心率 {{ alertDialog.hr }} 次/分　血压 {{ alertDialog.bp }} mmHg</td></tr>
            <tr><th>正常范围</th><td>心率 55-110 次/分；血压 90-140 mmHg</td></tr>
            <tr><th>发生时间</th><td>{{ alertDialog.occurTime }}</td></tr>
            <tr><th>判断依据</th><td>{{ alertDialog.detail }}</td></tr>
            <tr><th>怎么处理</th><td>{{ alertDialog.advice }}</td></tr>
          </tbody>
        </table>
        <div style="margin-top:14px;">
          <el-button v-if="alertDialog.rateBad === 1" type="primary" :loading="alertDialog.resolving"
                     @click="resolveAlert('rate')">标记心率已处理</el-button>
          <el-button v-if="alertDialog.preBad === 1" type="primary" :loading="alertDialog.resolving"
                     @click="resolveAlert('pre')">标记血压已处理</el-button>
          <el-button plain @click="$router.push({ path: '/device' })">去设备管理看该手表</el-button>
        </div>
        <div class="tip-small">
          处理后这条记录会从「异常」变成「已处理」，红点消失，历史记录与曲线仍然保留。
          如果老人基础心率/血压本身偏离默认范围，请到「基础管理-手表信息」调整阈值，避免反复报警。
        </div>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="alertDialog.visible = false">关 闭</el-button>
      </span>
    </el-dialog>

    <!-- 分配权限对话框 -->
    <el-dialog title="心率血压详细信息" :visible.sync="isShowRoleDialog" >
      <!--
        Tree 树形控件
          :data="data2" 数据源，数据中通过 label 属性表示树形节点的名称；通过 children 属性来指定该节点的子节点
          show-checkbox 添加该属性后，节点会带有复选框
          :default-expanded-keys="[2, 3]" 设置默认展开的节点key（key对应数据中的id）
          :default-checked-keys="[5]" 设置默认选中的节点key（id）
          node-key="id" 使用数据中的哪个属性来表示每个节点的key值，一般就是唯一的id值
          :props="defaultProps"
          :default-expand-all="true" 是否默认展开所有节点
      -->
      
	  
	  <!--start echarts表格-->
	  <div id="myChart" ref="mychart" style="width: 500px;height:200px;display: inline-block;"></div>
		<!--end echarts表格-->
		
		<!--start echarts表格-->
		<div id="myChartTwo" ref="mychartTwo" style="width: 500px;height:200px;display: inline-block;"></div>
		<!--end echarts表格-->
		
      <div slot="footer" class="dialog-footer">
        <el-button @click="isShowRoleDialog = false">取 消</el-button>
        <el-button type="primary" @click="assignRights">确 定</el-button>
      </div>
    </el-dialog>
  </div>

</template>

<script>
// 引入
import axios from 'axios'

import echarts from 'echarts'

export default {
  data () {
    return {
      searchText: '', // 角色查询内容
      roleDialogVisible: false,
      roleEditDialogVisible: false,
      roleForm: {
        roleName: '',
        roleDesc: ''
      },
      roleEditForm: {
        roleId: '',
        roleName: '',
        roleDesc: ''
      },
	  value6: '',
      popoverRole: true,
      total: 0,
      pageSize: 10,
      currentPage: 1,
      oldsList: [{
		  oldId:'1',
			name:'小黄',
		  sex:'男',
		  preState:'正常',
			rateState:'正常',
			createTime:'2019-02-20 15:34:56'
	  }],
      // 控制分配权限对话框的展示和隐藏
      isShowRoleDialog: false,

      // 树形控件的数据
      rightsTree: [],

      // Tree树形控件，默认使用数据中的 label 属性作为节点名称，使用 children 属性指定该接待你的子节点
      // 但是，如果我们自己的数据结构与默认的结构不同，可以通过 defaultProps 来修改默认的两个属性
      defaultProps: {
        // children: 'children',
        label: 'authName'
      },
      // 当前被分配权限的角色id
      roleId: -1,
			hrs: [],
			bps: [],
			createTimes: [],
			time: '',
			name: '',
			rateCount: '',
			preCount: '',
			// 异常（红点）处置
			alertDialog: {
				visible: false,
				loading: false,
				resolving: false,
				hasAlert: 1,
				title: '异常原因与处理',
				recordId: null,
				oldPersonName: '',
				hr: '',
				bp: '',
				rateBad: 0,
				preBad: 0,
				reason: '',
				detail: '',
				occurTime: '',
				advice: '',
				message: ''
			}
    }
  },
  created () {
	  // let myChart = this.$echarts.init(document.getElementById('myChart'))
		console.log('---')
    this.searchOlds()
		this.getCount()
		console.log('---')
	// this.drawLine()
  },
  updated() {
	  // this.drawLine()
     // this.getAttendanceData();
  },
  mounted(){
    // this.drawLine()
  },
  methods: {
    // 查询
    searchOlds () {
			
      var _this = this
      var name = _this.searchText
			if(this.value6.length==0){
				console.log('null')
				this.value6=['','']
			}
      axios.post('restful/ratepre/selectRatepre',
        {
          'name': name === '' ? '' : _this.searchText,
					'timeFirst':_this.value6[0] =='' ? '' : _this.value6[0],
					'timeLast': _this.value6[1] =='' ? '' : _this.value6[1]
        },
        {
          headers: {
            'content-type': 'application/json'
          },
          withCredentials: true
        }).then(function (response) {
        _this.oldsList = response.data.ratepreDtoList
        _this.total = response.data.ratepreDtoList.length
      })
        .catch(function (error) {
          console.log(error)
        })
    },
		getCount(){
			var _this=this
			axios.post('restful/ratepre/selectRatepreCount',
				{
					
				},
				{
					headers: {
						'content-type': 'application/json'
					},
					withCredentials: true
				}).then(function (response) {
				_this.rateCount=response.data.rates.length
				_this.preCount=response.data.pres.length
			})
				.catch(function (error) {
					console.log(error)
				})
			// 红点数字用"未处理的异常条数"，而不是历史异常设备数
			this.loadAlertCount()
		},
		loadAlertCount () {
			var _this = this
			axios.post('restful/ratepre/alertCount', {}, {
				headers: { 'content-type': 'application/json' },
				withCredentials: true
			}).then(function (response) {
				_this.rateCount = response.data.rateTotal
				_this.preCount = response.data.preTotal
			}).catch(function (error) {
				console.log(error)
			})
		},
		// 点表格里的"查看原因"
		showAlertRow (row) {
			this.loadAlertDetail(null, row.name, row.oldId)
		},
		// 点顶部的"心率不合格/血压不合格"：找该类型最新一条未处理异常
		showAlertByKind (kind) {
			var _this = this
			var target = null
			this.oldsList.forEach(function (row) {
				var bad = kind === 'rate' ? row.rateState == 1 : row.preState == 1
				if (bad && !target) {
					target = row
				}
			})
			if (!target) {
				this.$message({ type: 'success', message: '当前没有未处理的' + (kind === 'rate' ? '心率' : '血压') + '异常' })
				return
			}
			this.loadAlertDetail(null, target.name, target.oldId, kind)
		},
		// 读取异常详情：优先用 recordId，其次按老人找最新未处理异常
		loadAlertDetail (recordId, name, oldId, kind) {
			var _this = this
			this.alertDialog = {
				visible: true, loading: true, resolving: false, hasAlert: 1,
				title: '异常原因与处理', recordId: recordId, oldPersonName: name || '',
				hr: '', bp: '', rateBad: 0, preBad: 0, reason: '', detail: '',
				occurTime: '', advice: '', message: ''
			}
			var params = recordId ? { recordId: recordId } : { oldId: oldId, kind: kind || '' }
			axios.post('restful/ratepre/alertDetail', params, {
				headers: { 'content-type': 'application/json' },
				withCredentials: true
			}).then(function (response) {
				var d = response.data
				_this.alertDialog.loading = false
				if (d.returnCode !== '1111') {
					_this.alertDialog.hasAlert = 0
					_this.alertDialog.message = d.message || '没有找到待处理的异常'
					return
				}
				_this.alertDialog.hasAlert = d.hasAlert === undefined ? 1 : d.hasAlert
				_this.alertDialog.message = d.message || ''
				_this.alertDialog.recordId = d.recordId === undefined ? null : d.recordId
				_this.alertDialog.oldPersonName = d.oldPersonName || _this.alertDialog.oldPersonName
				_this.alertDialog.hr = d.hr
				_this.alertDialog.bp = d.bp
				_this.alertDialog.rateBad = d.rateBad || 0
				_this.alertDialog.preBad = d.preBad || 0
				_this.alertDialog.reason = d.reason || ''
				_this.alertDialog.detail = d.detail || ''
				_this.alertDialog.occurTime = d.occurTime || ''
				_this.alertDialog.advice = d.advice || ''
			}).catch(function (error) {
				console.log(error)
				_this.alertDialog.loading = false
				_this.alertDialog.hasAlert = 0
				_this.alertDialog.message = '读取失败，请稍后重试'
			})
		},
		// 处置：消除红点
		resolveAlert (kind) {
			var _this = this
			this.alertDialog.resolving = true
			axios.post('restful/ratepre/resolveAlert',
				{ recordId: this.alertDialog.recordId, kind: kind },
				{
					headers: { 'content-type': 'application/json' },
					withCredentials: true
				}).then(function (response) {
					_this.alertDialog.resolving = false
					if (response.data.returnCode === '1111') {
						_this.$message({ type: 'success', message: '已处理，红点已消除' })
						_this.alertDialog.visible = false
						_this.searchOlds()
						_this.getCount()
					} else {
						_this.$message({ type: 'error', message: response.data.message || '处理失败' })
					}
				}).catch(function (error) {
					console.log(error)
					_this.alertDialog.resolving = false
					_this.$message({ type: 'error', message: '处理失败' })
				})
		},
		// 表格里直接点"标记已处理"
		resolveRow (row) {
			var _this = this
			var tasks = []
			if (row.rateState == 1) {
				tasks.push(axios.post('restful/ratepre/resolveAlert', { recordId: row.recordId, kind: 'rate' }, _this.postConfig()))
			}
			if (row.preState == 1) {
				tasks.push(axios.post('restful/ratepre/resolveAlert', { recordId: row.recordId, kind: 'pre' }, _this.postConfig()))
			}
			Promise.all(tasks).then(function () {
				_this.$message({ type: 'success', message: '已处理，红点已消除' })
				_this.searchOlds()
				_this.getCount()
			}).catch(function (error) {
				console.log(error)
				_this.$message({ type: 'error', message: '处理失败' })
			})
		},
		postConfig () {
			return {
				headers: { 'content-type': 'application/json' },
				withCredentials: true
			}
		},
		// 只看待处理异常（把有异常的老人筛出来）
		openWorkbench () {
			var list = []
			this.oldsList.forEach(function (row) {
				if (row.rateState == 1 || row.preState == 1) {
					list.push(row)
				}
			})
			if (list.length === 0) {
				this.$message({ type: 'success', message: '当前没有待处理的异常' })
				return
			}
			this.oldsList = list
			this.total = list.length
			this.$message({ type: 'info', message: '已筛选出 ' + list.length + ' 条待处理异常，点「查看原因」了解处理办法' })
		},
    // 添加角色
    addRoles () {
      var _this = this
      axios.post('restful/roles/save',
        {
          'roleName': _this.roleForm.roleName
        },
        {
          headers: {
            'content-type': 'application/json'
          },
          withCredentials: true
        }).then(function (response) {
        console.log(response)
        if (response.data.returnCode === '1111') {
          _this.roleDialogVisible = false
          _this.searchOlds()
          _this.$message({
            type: 'success',
            message: '添加角色成功'
          })
        } else {
          _this.$message({
            type: 'error',
            message: '添加角色失败'
          })
        }
      })
        .catch(function (error) {
          console.log(error)
        })
    },
    // 关闭添加角色对话框
    closeRoleAddDialog () {
      // 重置表单
      this.$refs.addRoleFormRef.resetFields()
    },
    
    // 删除
    async deleteRole (roleName) {
      console.log(roleName)

      var _this = this
      try {
        await _this.$confirm('此操作将永久删除该文件, 是否继续?', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        console.log(roleName)
        axios.post('restful/roles/delete', {'roleName': roleName},
          {
            headers: {
              'content-type': 'application/json'
            },
            withCredentials: true
          }).then(function (response) {
          console.log(response)
          if (response.data.returnCode === '1111') {
            _this.$message({
              type: 'success',
              message: '删除信息成功'
            })
          }
          _this.searchOlds()
        })
          .catch(function (error) {
            console.log(error)
          })
      } catch (err) {
        // 取消删除
        this.$message({
          type: 'info',
          message: '已取消删除'
        })
      }
    },
    // 编辑信息
    handleEditRole (index, row) {
      this.roleEditDialogVisible = true
      this.roleEditForm = Object.assign({}, row) // 解决修改弹出框数据时，表格数据会发生同步改变
      // console.log(row)
    },
    handleSizeChange (val) {
      console.log(`每页 ${val} 条`)
      this.pageSize = val
    },
    handleCurrentChange (val) {
      console.log(`当前页: ${val}`)
      this.currentPage = val
    },
    changePage (val) {
      console.log(`当前页: ${val}`)
      this.currentPage = val
    },
    // 展示分配权限对话框
    showRoleDialog (curOld) {
			var _this=this
      // curRole 表示当前角色的数据，children属性是当前角色拥有的权限
      console.log('showRoleDialog', curOld.oldId)
			_this.oldId=curOld.oldId
      // 展示对话框
      _this.isShowRoleDialog = true
			axios.post('restful/ratepre/selectRatepreDetail',{
          'oldId': curOld.oldId
        },
				{
					headers: {
						'content-type': 'application/json'
					},
					withCredentials: true
				}).then(function (response) {
					
				console.log(response)
				console.log('this',_this)
				_this.bps=response.data.bps
				_this.hrs=response.data.hrs
				_this.time=response.data.time
				_this.createTimes=response.data.createTimes
				_this.drawLine()
				_this.drawLineTwo()
				console.log('bps',_this.bps)
			})
				.catch(function (error) {
					console.log(error)
				})
	  
      // 暂存角色id
      this.roleId = curOld.id
    },

    // 给角色分配权限
    async assignRights () {
    },
	
	//  echarts 
	drawLine(){
		// var echarts = require('echarts');
        // 基于准备好的dom，初始化echarts实例
				var _this=this
		this.$nextTick(()=>{
			let myChart = _this.$echarts.init(document.getElementById('myChart'));
					// let myChart =this.$echarts.init(this.$refs.myChart)
			//         let dom = this.$refs.mychart;
			// 		this.myChart = echarts.init(dom);
			
					// 绘制图表
			        myChart.setOption({
			            title: { text: _this.name+"  心率  "+_this.time+"时" },
			            tooltip: {},
			            xAxis: {
			                data: _this.createTimes
			            },
			            yAxis:[{
										type: 'value',
												show: true,    //显示纵轴false-不显示，true-显示
												name:'次/每分钟'   //这里是纵轴标题
									}],
			            series: [{
			                name: '销量',
			                type: 'line',
			                data: _this.hrs
			            }]
			        });
		});
        
    },
		
		//  echartsTwo 
		drawLineTwo(){
			// var echarts = require('echarts');
			var _this=this
					// 基于准备好的dom，初始化echarts实例
			this.$nextTick(()=>{
				let myChart = this.$echarts.init(document.getElementById('myChartTwo'));
						// let myChart =this.$echarts.init(this.$refs.myChart)
				//         let dom = this.$refs.mychart;
				// 		this.myChart = echarts.init(dom);
						// 绘制图表
								myChart.setOption({
										title: { text: _this.name+"  血压  "+_this.time+"时" },
										tooltip: {},
										xAxis: {
												data: _this.createTimes
										},
										yAxis: [{
										type: 'value',
												show: true,    //显示纵轴false-不显示，true-显示
												name:'mmHg'   //这里是纵轴标题
									}],
										series: [{
												name: '销量',
												type: 'line',
												data: _this.bps
										}]
								});
			});
					
			}
  }
}

</script>

<style lang="less" scoped>
  .sys-research {
    height: 60px;
    padding: 20px 20px 0;
  }

  .level1 {
    border-bottom: 1px dotted #ccc;
    padding: 10px 0;

    &:last-child {
      border: 0;
    }
  }

  .level2 {
    padding: 5px 0;
  }

  .level3 {
    margin: 3px;
  }

</style>
