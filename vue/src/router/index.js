import store from '@/store'
import request from '@/utils/request'
import Layout from '../layout/Layout.vue'
import {createRouter, createWebHistory} from "vue-router";


export const constantRoutes = [
    {path: '/Login', name: 'Login', component: () => import("@/views/Login")},
    {
        path: '/Layout', name: 'Layout', component: Layout, children: [
            //
            {path: '/home', name: 'Home', component: () => import("@/views/Home")},
            {path: '/stuInfo', name: 'StuInfo', component: () => import("@/views/StuInfo")},
            {path: '/dormManagerInfo', name: 'DormManagerInfo', component: () => import("@/views/DormManagerInfo")},
            {path: '/buildingInfo', name: 'BuildingInfo', component: () => import("@/views/BuildingInfo")},
            {path: '/roomInfo', name: 'RoomInfo', component: () => import("@/views/RoomInfo")},
            {path: '/noticeInfo', name: 'NoticeInfo', component: () => import("@/views/NoticeInfo")},
            {path: '/adjustRoomInfo', name: 'AdjustRoomInfo', component: () => import("@/views/AdjustRoomInfo")},
            {path: '/repairInfo', name: 'RepairInfo', component: () => import("@/views/RepairInfo")},
            {path: '/visitorInfo', name: 'VisitorInfo', component: () => import("@/views/VisitorInfo")},
            //
            {path: '/myRoomInfo', name: 'MyRoomInfo', component: () => import("@/views/MyRoomInfo")},
            {path: '/applyRepairInfo', name: 'ApplyRepairInfo', component: () => import("@/views/ApplyRepairInfo")},
            {path: '/applyChangeRoom', name: 'ApplyChangeRoom', component: () => import("@/views/ApplyChangeRoom")},

            {path: '/selfInfo', name: 'SelfInfo', component: () => import("@/views/SelfInfo")},
        ]
    },

]
const management = ['admin', 'dormManager'];
const access = {StuInfo: management, DormManagerInfo: ['admin'], BuildingInfo: management, RoomInfo: management,
 NoticeInfo: ['admin'], AdjustRoomInfo: management, RepairInfo: management, VisitorInfo: management,
 MyRoomInfo: ['stu'], ApplyRepairInfo: ['stu'], ApplyChangeRoom: ['stu']};
constantRoutes.forEach(route => (route.children || []).forEach(child => { child.meta = {roles: access[child.name]}; }));
const router = createRouter({
    routes: constantRoutes,
    history: createWebHistory(process.env.BASE_URL)
})
router.beforeEach(async (to, from, next) => {
 if(to.path.toLowerCase()==='/login')return next();
 if(!store.state.initialized) {
  try {const response=await request.get('/auth/me',{skipAuthRedirect:true});store.commit('setUser',response.data);}
  catch (_) {store.commit('clearUser');}
 }
 if(!store.state.user)return next('/Login');
 if(to.meta.roles && !to.meta.roles.includes(store.state.identity))return next('/home');
 return next(to.path==='/'?'/home':undefined);
});
export default router;
