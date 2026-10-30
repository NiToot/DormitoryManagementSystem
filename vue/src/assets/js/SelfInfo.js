import request from '@/utils/request';
import {ElMessage} from 'element-plus';

export default {
    name: 'SelfInfo',
    data() {
        return {
            username: '', name: '', gender: '', age: '', phoneNum: '', email: '', avatar: '',
            image: '', imgDisplay: {display: 'none'}, form: {}, dialogVisible: false,
            passwordDialog: false, passwordForm: {currentPassword: '', newPassword: '', confirmPassword: ''},
            rules: {name: [{required: true, message: '请输入姓名', trigger: 'blur'}]}
        };
    },
    created() { this.find().catch(() => {}); },
    methods: {
        load() {
            const profile = this.$store.state.user || {};
            ['username', 'name', 'gender', 'age', 'phoneNum', 'email', 'avatar'].forEach(key => { this[key] = profile[key] });
        },
        async find() {
            const result = await request.get('/auth/me');
            this.$store.commit('setUser', result.data); this.load(); await this.init(this.avatar);
        },
        Edit() { this.form = {...this.$store.state.user}; this.dialogVisible = true; },
        cancel() { this.dialogVisible = false; },
        save() {
            this.$refs.form.validate(async valid => {
                if (!valid) return;
                const body = {};
                ['name', 'age', 'gender', 'phoneNum', 'email'].forEach(key => { body[key] = this.form[key] });
                try {
                    const result = await request.put('/auth/profile', body);
                    this.$store.commit('setUser', result.data); this.load(); this.dialogVisible = false;
                    ElMessage.success('个人资料已更新');
                } catch (_) { /* Unified request error handler displays the failure. */ }
            });
        },
        openPassword() { this.passwordForm = {currentPassword: '', newPassword: '', confirmPassword: ''}; this.passwordDialog = true; },
        async changePassword() {
            const {currentPassword, newPassword, confirmPassword} = this.passwordForm;
            if (!currentPassword || newPassword.length < 8 || newPassword !== confirmPassword) {
                ElMessage.error('请输入原密码，新密码至少8位且两次输入一致'); return;
            }
            try {
                await request.post('/auth/password', {currentPassword, newPassword});
                this.passwordForm = {currentPassword: '', newPassword: '', confirmPassword: ''};
                this.$store.commit('clearUser'); ElMessage.success('密码已更新，请重新登录'); this.$router.replace('/Login');
            } catch (_) { /* Keep the form available for correction. */ }
        },
        async init(filename) {
            this.imgDisplay = {display: filename ? 'block' : 'none'};
            if (filename) {
                try { const result = await request.get('/files/initAvatar/' + filename); this.image = result.data.data; }
                catch (_) { this.imgDisplay = {display: 'none'}; }
            }
        },
        async uploadAvatar({file, onSuccess, onError}) {
            const body = new FormData(); body.append('file', file);
            try { const result = await request.post('/files/avatar', body); await this.find(); onSuccess(result); ElMessage.success('头像已更新'); }
            catch (error) { onError(error); }
        }
    }
};
