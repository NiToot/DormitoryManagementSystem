import {createStore} from 'vuex'

// Discard pre-upgrade cached credentials; the router restores a safe profile from /auth/me.
['user', 'identity', 'access-user'].forEach(key => sessionStorage.removeItem(key));

export default createStore({
    state: {user: null, identity: '', isLogin: false, initialized: false},
    mutations: {
        setUser(state, profile) {
            const {password, ...user} = profile;
            state.user = user;
            state.identity = user.identity;
            state.isLogin = true;
            state.initialized = true;
            // Temporary compatibility for existing pages; authorization comes from the server.
            sessionStorage.setItem('user', JSON.stringify(user));
            sessionStorage.setItem('identity', JSON.stringify(user.identity));
            sessionStorage.removeItem('access-user');
        },
        clearUser(state) {
            state.user = null; state.identity = ''; state.isLogin = false; state.initialized = true;
            ['user', 'identity', 'access-user'].forEach(key => sessionStorage.removeItem(key));
        }
    }
})
