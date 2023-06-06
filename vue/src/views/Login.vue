<template>
    <div class="login-container">
        <!--        <div style="margin-right: 150px; min-width: 300px">
                    <img alt="" src="../../public/login_Team.png" style="width: 600px"/>
                </div>-->
        <div style="width: 400px; margin: 150px auto">
            <div style="color: black; font-size: 30px; text-align: left; padding: 30px 0">登陆</div>
            <el-form ref="form" :model="form" :rules="rules" size="large">
                <el-form-item prop="username">
                    <el-input v-model="form.username" clearable prefix-icon="avatar"></el-input>
                </el-form-item>
                <el-form-item prop="password">
                    <el-input v-model="form.password" prefix-icon="lock" show-password></el-input>
                </el-form-item>
                <el-form-item :model="form" prop="identity">
                    <el-radio v-model="form.identity" label="stu" style="color: grey">学生</el-radio>
                    <el-radio v-model="form.identity" label="dormManager" style="color: grey">宿舍管理员</el-radio>
                    <el-radio v-model="form.identity" label="admin" style="color: grey">后勤管理中心</el-radio>
                </el-form-item>
                <el-form-item>
                    <el-button :disabled="!disabled" style="width: 20%" type="primary" @click="login">登 录</el-button>
                </el-form-item>
            </el-form>
        </div>
    </div>
    <div class="footer">
        ©Copyright 2021-2023 NiToot
    </div>
</template>
<script src="@/assets/js/Login.js"></script>
<style scoped>
.login-container {
    position: fixed;
    height: 100%;
    width: 100%;
    top: 0;
    left: 0;
    background: linear-gradient(
            135deg,
            hsl(170deg, 80%, 70%),
            hsl(190deg, 80%, 70%),
            hsl(250deg, 80%, 70%),
            hsl(320deg, 80%, 70%),
            hsl(320deg, 80%, 70%),
            hsl(250deg, 80%, 70%),
            hsl(190deg, 80%, 70%),
            hsl(190deg, 80%, 70%),
            hsl(170deg, 80%, 70%)
    );
    background-size: 600%;
    animation: myanimation 15s linear infinite;
}

@keyframes myanimation {
    0% {
        background-position: 0 0;
    }
    100% {
        background-position: 100% 100%;
    }
}

.footer {
    left: 0;
    bottom: 0;
    color: #fff;
    width: 100%;
    position: absolute;
    text-align: center;
    line-height: 30px;
    padding-bottom: 10px;
    text-shadow: #000 0.1em 0.1em 0.1em;
    font-size: 14px;
}

.footer a,
.footer span {
    color: #fff;
}
</style>