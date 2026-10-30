import axios from 'axios'
import {ElMessage} from 'element-plus'
import store from '@/store'

const request = axios.create({baseURL: '/api', timeout: 10000, withCredentials: true});
let csrfRequest;
request.interceptors.request.use(async config => {
    if (!['get', 'head', 'options'].includes((config.method || 'get').toLowerCase())) {
        if (!csrfRequest) csrfRequest = axios.get('/api/auth/csrf', {withCredentials: true})
            .then(response => response.data.data).finally(() => { csrfRequest = null });
        const csrf = await csrfRequest;
        config.headers[csrf.headerName] = csrf.token;
    }
    return config;
});
request.interceptors.response.use(response => {
    if (response.config.responseType === 'blob') return response.data;
    return typeof response.data === 'string' ? JSON.parse(response.data) : response.data;
}, error => {
    if (error.response && error.response.status === 401) {
        store.commit('clearUser');
        if (window.location.pathname.toLowerCase() === '/login' && !error.config.skipAuthRedirect) ElMessage.error(error.response.data.msg || '用户名或密码错误');
        if (!error.config.skipAuthRedirect && window.location.pathname.toLowerCase() !== '/login') window.location.replace('/Login');
    } else {
        ElMessage.error(error.response && error.response.data.msg || '请求失败，请稍后重试');
    }
    return Promise.reject(error);
});
export default request;
