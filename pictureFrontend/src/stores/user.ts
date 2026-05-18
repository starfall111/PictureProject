import { getLoginUserUsingGet } from "@/api/userController";
import { message } from "ant-design-vue";
import { defineStore } from "pinia";
import { ref } from "vue";

export const userLoginUserStore = defineStore("loginUser", () => {
    const loginUser = ref<API.LoginUserVO>({});

    async function getLoginUser(silent = false) {
        const res = await getLoginUserUsingGet();
        if (res.data.code === 0 && res.data.data) {
            if (!silent) {
                message.success('欢迎回来 ' + res.data.data.userName);
            }
            loginUser.value = res.data.data;
        }
    }

    function setLoginUser(user: API.LoginUserVO) {
        loginUser.value = user;
    }

    return { loginUser, getLoginUser, setLoginUser }
})