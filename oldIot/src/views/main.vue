<template>
  <div class="home_main">

			<el-container>


			  <el-header style="height: 400px;">
			  	<el-carousel :interval="4000" type="card" height="350px">
					<el-carousel-item >
					  <img src="https://th.bing.com/th/id/R.a97509d23345be92421bc014bce32e41?rik=nvz8cTRmRopddw&riu=http%3a%2f%2f5b0988e595225.cdn.sohucs.com%2fimages%2f20190815%2f67822f40aa7a41bd8c53b20ea084b705.jpeg&ehk=PmzwK9MZ7sSsqtPHgm6BVR58cJdqqEs9nnuM9H8A1Qs%3d&risl=&pid=ImgRaw&r=0">
					</el-carousel-item>
            <el-carousel-item >
              <img src="https://th.bing.com/th/id/R.41b3abd72a934d4bd6e183b45ae00d60?rik=Aijdi6m1aPTDKg&riu=http%3a%2f%2fwww.cnjdny.com%2fuploads%2fyanglao-01.jpg&ehk=MjoKHkAJPIWyK0Zak2g1Wx9KNCw5yN%2fXRp2XG%2bV0xpc%3d&risl=&pid=ImgRaw&r=0">
            </el-carousel-item>
            <el-carousel-item >
              <img src="https://th.bing.com/th/id/R.cc2cfda4efaf1121e47174024034fe3c?rik=NUrCkCMn2ee3gw&riu=http%3a%2f%2fwww.bjtzky.com%2fupload%2fportal%2f20220623%2f28c852b75d2e0081c828658a61b674d9.png&ehk=voipm%2fTQLaezKgBRKmHhIYAUbJhk1QDa%2b2UM2qLlHms%3d&risl=&pid=ImgRaw&r=0">
            </el-carousel-item>
				  </el-carousel>
			  </el-header>
	  <el-main>
		  <el-row>
			  <el-col :span="3"><span style="color: #C0C0C0;font-size: 20px;margin-left: 18px;">设备监测情况:</span></el-col>

		  <el-col :span="2" class="logout-desc" >
		  	<el-badge :value="this.allCount" class="item">
		  	<el-button size="small" @click="open">人员不合格</el-button>
		  	</el-badge>
		  </el-col>
		  <el-col :span="2" class="logout-desc" >
		  	 <el-badge :value="this.num" class="item">
		  	  <el-button size="small">摔倒</el-button>
		  	</el-badge>
		  </el-col>

		  </el-row>
		  <hr />
		  <el-row type="flex" >
		  	<span style="margin-left: 30px;font-weight: bold;padding-top: 8px;margin-right: 50px;">公告动态</span>

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


		  </el-row>

		  <!--表格-->
		  <el-table
		  :data="tableData.slice((currentPage-1)*pageSize,currentPage*pageSize)"
		  @row-click="getDetail"
		  style="width: 100%">
		  <el-table-column
		  label=" "
		  	type="index">
		  </el-table-column>
		  <el-table-column
		  	label=" "
		  	width="180"
		  	prop="title">
		  </el-table-column>
		  <el-table-column
		  	label=""
		  	width="180"
		  	prop="time">
		  </el-table-column>

		  <el-table-column label="">

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
	  </el-main>
	</el-container>
  </div>
</template>
<style scoped>
	.time {
    font-size: 13px;
    color: #999;
  }

  .bottom {
    margin-top: 13px;
    line-height: 12px;
  }

  .button {
    padding: 0;
    float: right;
  }

  .image {
    width: 100%;
    display: block;
  }

  .clearfix:before,
  .clearfix:after {
      display: table;
      content: "";
  }

  .clearfix:after {
      clear: both
  }

  /* 轮播图 */
  .el-carousel__item h3 {
    color: #475669;
    font-size: 14px;
    opacity: 0.75;
    line-height: 200px;
    margin: 0;
  }

  .el-carousel__item:nth-child(2n) {
    /* background-color: #99a9bf; */
  }

  .el-carousel__item:nth-child(2n+1) {
    /* background-color: #d3dce6; */
  }
</style>
<script>

	import axios from 'axios'

export default {
	created () {
		// let myChart = this.$echarts.init(document.getElementById('myChart'))
		console.log('---')
		this.searchOlds()
		this.getCount()
		this.seachPicShow()
		this.searchposition()

		console.log('---')
	// this.drawLine()
	},
	methods: {
		searchposition () {
		  var _this = this

		  axios.post('restful/position/select',
		    {
		      'name': ''
		    },
		    {
		      headers: {
		        'content-type': 'application/json'
		      },
		      withCredentials: true
		    }).then(function (response) {
		    console.log(response)
				var count=0;
				var len=response.data.positionDtos.length
				_this.positionList=response.data.positionDtos
				for(var i=0;i<len;i++){
					if(_this.positionList[i].status=='1'){
						console.log('摔倒')
						count++;
					}
				}
				_this.num=count
		  })
		    .catch(function (error) {
		      console.log(error)
		    })
		},
      handleEdit(index, row) {

        console.log(index, row);
				 // this.$router.push({path: '/text'})
				var nametwo = row.title
				var textMain = row.textmain
				this.$router.push({
					path: '/text',
					query: {
						nametwo: nametwo,
						textMain: textMain
					}
				})
      },
	  open(){

	  },
	  getCount(){
		  console.log('kaishe')
		  var _this=this
		  axios.post('restful/step/getAllCount',
		  	{

		  	},
		  	{
		  		headers: {
		  			'content-type': 'application/json'
		  		},
		  		withCredentials: true
		  	}).then(function (response) {
		  	_this.allCount=response.data.allCount
		  })
		  	.catch(function (error) {
		  		console.log(error)
		  	})
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
	  getDetail(row, column, event){
		  console.log(row, column, event)
		  var textId = row.textId
		  var picId = row.picId
		  console.log('-------------------------------')
		  this.$router.push({
		  	path: '/textMainUser',
		  	query: {
		  		textId: textId,
		  		picId: picId
		  	}
		  })
	  },
      handleDelete(index, row) {
        console.log(index, row);
      },
	  // seachPicShow(){
		//   var _this=this
    //
		//   axios.post('restful/picshow/select',{},
		//   {
		//   	headers: {
		//   		'content-type': 'application/json'
		//   	},
		//   	withCredentials: true
		//   }).then(function (response) {
		// 	  console.log(response)
		//   _this.imgList = response.data.picShows
		//   console.log(_this.imgList)
		//   // for(var i=0;i<_this.imgList.length;i++){
		//   // 	_this.imgList[i].url ='http://47.106.37.197:9110/' +_this.imgList[i].picName
		//   // 	console.log('图片地址')
		//   // 	console.log(_this.imgList[i].url)
		//   // }
    //
		//   })
		//   .catch(function (error) {
		//   	console.log(error)
		//   })
	  // },

			addtext(){
				this.$router.push('/text')
			},
			searchOlds(){
				var _this=this
				var params = {
					'title': _this.searchText
				}
				console.log(params)
				axios.post('restful/text/select',params,
				{
					headers: {
						'content-type': 'application/json'
					},
					withCredentials: true
				}).then(function (response) {
				console.log(response)
				_this.tableData=response.data.texts
				_this.total = response.data.texts.length
				})
				.catch(function (error) {
					console.log(error)
				})
			}


    },
  data() {
    return {
	  tableData: [{
	  	time: '2016-05-02',
	  	title: '王小虎',
	  	textmain: '上海市普陀区金沙江路 1518 弄'
	  }, {
	  	time: '2016-05-04',
	  	title: '王小高',
	  	textmain: '上海市普陀区金沙江路 1517 弄'
	  }],
		searchText: '', // 查询内容
		total: 0,
		pageSize: 10,
		currentPage: 1,
		allCount:'',
		imgList:[
      "https://th.bing.com/th/id/R.a97509d23345be92421bc014bce32e41?rik=nvz8cTRmRopddw&riu=http%3a%2f%2f5b0988e595225.cdn.sohucs.com%2fimages%2f20190815%2f67822f40aa7a41bd8c53b20ea084b705.jpeg&ehk=PmzwK9MZ7sSsqtPHgm6BVR58cJdqqEs9nnuM9H8A1Qs%3d&risl=&pid=ImgRaw&r=0",
      "https://tse1.mm.bing.net/th?id=OIP.QbOr1yqTTUvW4YO0WuANYAHaDy&rs=1&pid=ImgDetMain"
    ],
		num:'',
		positionList:[],
		urls:[]
    };
  }
}
</script>
