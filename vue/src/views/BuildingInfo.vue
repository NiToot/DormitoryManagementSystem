<template>
  <div>
    <el-breadcrumb replace="true" separator-icon="ArrowRight" style="margin: 16px">
      <el-breadcrumb-item :to="{ path: 'home' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item>宿舍管理</el-breadcrumb-item>
      <el-breadcrumb-item>楼宇信息</el-breadcrumb-item>
    </el-breadcrumb>
    <el-card style="margin: 15px; min-height: calc(100vh - 111px)">
      <div>
        <!--    功能区-->
        <div style="margin: 10px 0">
          <!--    搜索区-->
          <div style="margin: 10px 0">
            <el-input v-model="search" clearable placeholder="请输入编号" prefix-icon="Search" style="width: 20%"/>
            <el-button icon="Search" style="margin-left: 5px" type="primary" @click="load"></el-button>
            <el-button icon="refresh-left" style="margin-left: 10px" type="default" @click="reset"></el-button>
            <div style="float: right">
              <el-tooltip content="添加" placement="top">
                <el-button icon="plus" style="width: 50px" type="primary" @click="add"></el-button>
              </el-tooltip>
            </div>
          </div>
        </div>
        <!--    表格-->
        <el-table v-loading="loading" :data="tableData" border max-height="705" show-overflow-tooltip
                  style="width: 100%">
          <el-table-column label="#" type="index"/>
          <el-table-column label="编号" prop="dormBuildId" sortable/>
          <el-table-column label="名称" prop="dormBuildName"/>
          <el-table-column
              :filter-method="filterTag"
              :filters="[
              { text: '男宿舍', value: '男宿舍' },
              { text: '女宿舍', value: '女宿舍' },
            ]"
              filter-placement="bottom-end"
              label="备注"
              prop="dormBuildDetail"
          />
          <!--      操作栏-->
          <el-table-column label="操作" width="130px">
            <template #default="scope">
              <el-button icon="Edit" type="primary" @click="handleEdit(scope.row)"
              ></el-button>
              <el-popconfirm title="确认删除？" @confirm="handleDelete(scope.row.id)">
                <template #reference>
                  <el-button icon="Delete" type="danger"></el-button>
                </template>
              </el-popconfirm>
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
        <!--      弹窗-->
        <div>
          <el-dialog v-model="dialogVisible" title="操作" width="30%" @close="cancel">
            <el-form ref="form" :model="form" :rules="rules" label-width="120px">
              <el-form-item label="编号" prop="dormBuildId">
                <el-input v-model.number="form.dormBuildId" :disabled="disabled" style="width: 80%"></el-input>
              </el-form-item>
              <el-form-item label="名称" prop="dormBuildName">
                <el-input v-model="form.dormBuildName" style="width: 80%"></el-input>
              </el-form-item>
              <el-form-item label="备注" prop="dormBuildDetail">
                <el-input
                    v-model="form.dormBuildDetail"
                    :autosize="{ minRows: 2, maxRows: 4 }"
                    autosize
                    style="width: 80%"
                    type="textarea"
                ></el-input>
              </el-form-item>
            </el-form>
            <template #footer>
              <span class="dialog-footer">
                <el-button @click="cancel">取 消</el-button>
                <el-button type="primary" @click="save">确 定</el-button>
              </span>
            </template>
          </el-dialog>
        </div>
      </div>
    </el-card>
  </div>
</template>
<script src="@/assets/js/BuildingInfo.js"></script>