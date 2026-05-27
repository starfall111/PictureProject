<template>
    <div id="loginPage">
        <div class="login-brand">
            <div class="brand-title">
                <h2 class="brand-heading">智能云图库</h2>
                <span class="brand-subtitle">属于你自己的图片素材库</span>
            </div>
        </div>

        <div class="login-container">
            <div class="menu">
                <div class="form">
                    <div class="form-password">
                        <a-form :model="registerDTO" name="basic" autocomplete="off" @finish="register(registerDTO)"
                            layout="vertical" @finishFailed="onFinishFailed" class="login-form">
                            <a-form-item name="account" label="账号"
                                :rules="[{ required: true, message: '请输入账号' }, { min: 4, message: '账号长度不能小于4位' }]">
                                <a-input v-model:value="registerDTO.account" placeholder="请输入账号" :size="'large'" />
                            </a-form-item>

                            <a-form-item name="password" label="密码"
                                :rules="[{ required: true, message: '请输入密码' }, { min: 6, message: '密码长度不能小于6位' }]">
                                <a-input-password v-model:value="registerDTO.password" placeholder="请输入密码"
                                    :size="'large'" />
                            </a-form-item>

                            <!-- 密码复杂度指示器 -->
                            <div v-if="registerDTO.password" style="margin-top: -16px; margin-bottom: 16px;">
                                <a-progress :percent="passwordStrength.percent" :stroke-color="passwordStrength.color"
                                    :show-info="false" size="small" />
                                <span :style="{ color: passwordStrength.color, fontSize: '12px' }">
                                    密码强度：{{ passwordStrength.label }}
                                </span>
                            </div>

                            <a-form-item name="checkPassword" label="确认密码"
                                :rules="[{ required: true, message: '请输入确认密码' }, { validator: validateCheckPassword }]">
                                <a-input-password v-model:value="registerDTO.checkPassword" placeholder="请输入确认密码"
                                    :size="'large'" />
                            </a-form-item>

                            <div class="form-footer">
                                <div class="tips">
                                    已有账号?
                                    <RouterLink to="/user/login">去登录</RouterLink>
                                </div>
                            </div>

                            <a-form-item :wrapper-col="{ span: 24 }">
                                <a-button type="primary" html-type="submit" class="submit-btn"
                                    :size="'large'" :loading="registerLoading">注册</a-button>
                            </a-form-item>
                        </a-form>
                    </div>
                </div>

            </div>

        </div>
    </div>

</template>


<script setup lang="ts">
import { reactive, ref, computed, onMounted } from 'vue';
import { message } from 'ant-design-vue';
import { registerUsingPost } from '@/api/userController';
import { sendVerificationCodeUsingPost } from '@/api/noticeController';
import { useAliyunCaptcha } from '@/access/useAliyunCaptcha';
import router from '@/router';

/* 是否已发送验证码 */
const codeSent = ref(false);

/* 验证码已发送校验 */
const validateCodeSent = (_rule: any, value: string) => {
    if (value && !codeSent.value) {
        return Promise.reject('请先获取验证码');
    }
    return Promise.resolve();
};

/* 确认密码校验 */
const validateCheckPassword = (_rule: any, value: string) => {
    if (value && value !== registerDTO.password) {
        return Promise.reject('两次输入的密码不一致');
    }
    return Promise.resolve();
};

/* 注册请求体  */
const registerDTO = reactive<API.UserRegisterDTO>({
    account: '',
    checkPassword: '',
    password: '',
    type: 0,
    verityCode: ''
});

/* 密码复杂度计算 */
const passwordStrength = computed(() => {
    const pwd = registerDTO.password;
    if (!pwd) return { percent: 0, color: '#d9d9d9', label: '' };

    let score = 0;
    if (/[a-z]/.test(pwd)) score++;
    if (/[A-Z]/.test(pwd)) score++;
    if (/[0-9]/.test(pwd)) score++;
    if (/[^a-zA-Z0-9]/.test(pwd)) score++;

    if (score < 2) return { percent: 33, color: '#ff4d4f', label: '弱' };
    if (score === 2) return { percent: 66, color: '#faad14', label: '中' };
    return { percent: 100, color: '#52c41a', label: '强' };
});

/* 判断account类型 */
const isPhone = (account: string) => {
    return /^1[3-9]\d{9}$/.test(account);
};

const onFinish = (values: any) => {
    console.log('Success:', values);
};

const onFinishFailed = (errorInfo: any) => {
    console.log('Failed:', errorInfo);
};

/* 注册loading */
const registerLoading = ref(false);

/* 注册方法 */
const register = async (DTO: any) => {
    DTO.account = DTO.account?.trim();
    registerLoading.value = true;
    try {
        const res = await registerUsingPost(DTO);
        if (res.data.code === 0 && res.data.data) {
            message.success('注册成功');
            router.push({
                path: '/user/login',
                replace: true
            })
        } else {
            message.error(res.data.message || '注册失败');
        }
    } finally {
        registerLoading.value = false;
    }
}

</script>

<style>
#loginPage {
    display: flex;
    width: 100%;
    height: 100vh;
}

/* 左侧品牌区 */
.login-brand {
    display: flex;
    justify-content: center;
    align-items: center;
    width: 50%;
    height: 100%;
    background-color: #2563EB;
}

.brand-title {
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
}

.brand-heading {
    font-size: 48px;
    padding: 16px;
    color: aliceblue;
}

.brand-subtitle {
    font-size: 16px;
    padding: 16px;
    color: aliceblue;
}

/* 右侧表单区 */
.login-container {
    width: 50%;
    display: flex;
    justify-content: center;
    align-items: center;
}

.menu {
    width: 60vh;
    height: 80vh;
    border: 4px solid #efefef;
    border-radius: 4px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
    display: flex;
    justify-content: center;
    align-items: center;
}

.form {
    width: 80%;
    height: 80%;
    display: flex;
    flex-direction: column;
}

.form-menu {
    width: 100%;
    display: flex;
    justify-content: center;
    top: 0;
    margin: 16px 0;
}

.form-form {
    display: flex;
}

.login-form {
    margin: 24px;
    width: 90%;
}

.form-footer {
    display: flex;
    justify-content: space-between;
    margin-bottom: 24px;
}

.submit-btn {
    width: 100%;
}

/* 移动端适配 */
@media (max-width: 768px) {
    #loginPage {
        flex-direction: column;
    }

    .login-brand {
        display: none;
    }

    .login-container {
        width: 100%;
        height: 100vh;
        padding: 16px;
        box-sizing: border-box;
    }

    .menu {
        width: 100%;
        height: auto;
        min-height: auto;
        max-width: 500px;
        border: none;
        box-shadow: none;
    }

    .form {
        width: 100%;
        height: auto;
    }

    .login-form {
        margin: 16px 0;
        width: 100%;
    }

    .brand-heading {
        font-size: 28px;
    }

    .brand-subtitle {
        font-size: 14px;
    }
}
</style>
