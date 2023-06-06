<template>
  <div>
    <el-breadcrumb separator-icon="ArrowRight" style="margin: 16px">
      <el-breadcrumb-item :to="{ path: '/home' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item>申请管理</el-breadcrumb-item>
      <el-breadcrumb-item>调宿申请</el-breadcrumb-item>
    </el-breadcrumb>
    <el-card style="margin: 15px; min-height: calc(100vh - 111px)">
      <div>
        <!--    功能区-->
        <div style="margin: 10px 0">
          <!--    搜索区-->
          <div style="margin: 10px 0">
            <el-input v-model="search" clearable placeholder="请输入学号" prefix-icon="Search" style="width: 20%"/>
            <el-button icon="Search" style="margin-left: 5px" type="primary" @click="load"></el-button>
            <div style="float: right">
              <el-tooltip content="添加" placement="top">
                <el-button icon="plus" style="width: 50px" type="primary" @click="add"></el-button>
              </el-tooltip>
            </div>
          </div>
        </div>
        <!--    表格-->
        <el-table v-loading="loading" :data="tableData" border max-height="705" style="width: 100%">
          <el-table-column label="#" type="index"/>
          <el-table-column label="学号" prop="username" sortable width="100px"/>
          <el-table-column label="姓名" prop="name" width="100px"/>
          <el-table-column label="当前房间号" prop="currentRoomId" sortable/>
          <el-table-column label="当前床位号" prop="currentBedId" sortable/>
          <el-table-column label="目标房间号" prop="towardsRoomId" sortable/>
          <el-table-column label="目标床位号" prop="towardsBedId" sortable/>
          <el-table-column
              :filter-method="filterTag"
              :filters="[
              { text: '未处理', value: '未处理' },
              { text: '通过', value: '通过' },
              { text: '驳回', value: '驳回' },
            ]"
              filter-placement="bottom-end"
              label="申请状态"
              prop="state"
              sortable
              width="130px"
          >
            <template #default="scope">
              <el-tag :type="scope.row.state === '通过' ? 'success' : (scope.row.state === '驳回' ? 'danger' : 'info')"
                      disable-transitions
              >{{ scope.row.state }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="申请时间" prop="applyTime" sortable/>
          <el-table-column label="处理时间" prop="finishTime" sortable/>
          <!--      操作栏-->
          <el-table-column label="操作" width="130px">
            <template #default="scope">
              <el-button icon="more-filled" type="default" @click="showDetail(scope.row)"></el-button>
              <el-button v-if="scope.row.state!=='通过' " icon="Edit" type="primary"
                         @click="handleEdit(scope.row)"></el-button>
            </template>
          </el-table-column>
        </el-table>
        <!--分页-->
        <div style="margin: 10px 0">
          <el-pagination
              v-model:currentPage="currentPage"
              :page-size="pageSize"
              :page-sizes="[10, 20]"
              :total="total"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
          >
          </el-pagination>
        </div>
        <div>
          <!--      弹窗-->
          <el-dialog v-model="dialogVisible" title="操作" width="30%" @close="cancel">
            <el-form ref="form" :model="form" :rules="rules" label-width="120px">
              <el-form-item label="学号" prop="username">
                <el-input v-model="form.username" disabled style="width: 80%"></el-input>
              </el-form-item>
              <el-form-item label="姓名" prop="name">
                <el-input v-model="form.name" disabled style="width: 80%"></el-input>
              </el-form-item>
              <el-form-item disabled label="当前房间号" prop="currentRoomId">
                <el-input v-model="form.currentRoomId" disabled style="width: 80%"></el-input>
              </el-form-item>
              <el-form-item label="当前床位号" prop="currentBedId">
                <el-input v-model="form.currentBedId" disabled style="width: 80%"></el-input>
              </el-form-item>
              <el-form-item label="目标房间号" prop="towardsRoomId">
                <el-input v-model.number="form.towardsRoomId" style="width: 80%"></el-input>
              </el-form-item>
              <el-form-item label="目标床位号" prop="towardsBedId">
                <el-input v-model.number="form.towardsBedId" style="width: 80%"></el-input>
              </el-form-item>
              <el-form-item label="申请时间" prop="applyTime" style="margin-top: 27px">
                <el-date-picker
                    v-model="form.applyTime"
                    :disabled="!judgeOption"
                    clearable
                    placeholder="选择时间"
                    style="width: 50%"
                    type="datetime"
                    value-format="YYYY-MM-DD HH:mm:ss"
                ></el-date-picker>
              </el-form-item>
            </el-form>
            <template #footer>
              <span class="dialog-footer">
                <el-button @click="cancel">取 消</el-button>
                <el-button type="primary" @click="save">确 定</el-button>
              </span>
            </template>
          </el-dialog>
          <!--详情信息弹窗-->
          <el-dialog v-model="detailDialog" title="学生信息" width="30%" @close="cancel">
            <el-form ref="form" :model="form" label-width="220px">
              <el-form-item label="学号：" prop="username">
                <template #default="scope">
                  <span>{{ form.username }}</span>
                </template>
              </el-form-item>
              <el-form-item label="姓名：" prop="name">
                <template #default="scope">
                  <span>{{ form.name }}</span>
                </template>
              </el-form-item>
              <el-form-item label="当前房间号：" prop="currentRoomId">
                <template #default="scope">
                  <span>{{ form.currentRoomId }}</span>
                </template>
              </el-form-item>
              <el-form-item label="当前床位号：" prop="currentBedId">
                <template #default="scope">
                  <span>{{ form.currentBedId }}</span>
                </template>
              </el-form-item>
              <el-form-item label="目标房间号：" prop="towardsRoomId">
                <template #default="scope">
                  <span>{{ form.towardsRoomId }}</span>
                </template>
              </el-form-item>
              <el-form-item label="目标床位号：" prop="towardsBedId">
                <template #default="scope">
                  <span>{{ form.towardsBedId }}</span>
                </template>
              </el-form-item>
              <el-form-item label="申请时间：" prop="applyTime">
                <template #default="scope">
                  <span>{{ form.applyTime }}</span>
                </template>
              </el-form-item>
              <el-form-item label="申请状态：" prop="state">
                <template #default="scope">
                  <span>{{ form.state }}</span>
                </template>
              </el-form-item>
              <el-form-item label="处理时间：" prop="finishTime">
                <template #default="scope">
                  <span>{{ form.finishTime }}</span>
                </template>
              </el-form-item>
            </el-form>
          </el-dialog>
        </div>
      </div>
    </el-card>
  </div>
</template>
<script src="@/assets/js/ApplyChangeRoom.js"></script>