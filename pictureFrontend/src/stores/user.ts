import { defineStore } from "pinia";
import { idText } from "typescript";
import { ref } from "vue";

export const userLoginUserStore = defineStore("loginUser", () => {
    const loginUser = ref<any>({
        userName: "未登录"
    });

    async function getLoginUser() {
        // TODO: 这里应该调用后端接口获取用户信息
        // const res = await get();
        // if (res.data.code === 0 && res.data.data) {
        //    loginUser.value = res.data.data;
        // }
        setTimeout(() => {
            loginUser.value = {
                userName: "testUser",
                id: 1
            }
        }, 1000);
    }

    function setLpginUser(user: any) {
        loginUser.value = user;
    }

    return { loginUser, getLoginUser, setLpginUser }
})