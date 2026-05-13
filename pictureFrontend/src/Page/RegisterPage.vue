<template>
    <div id="loginPage">
        <div class="login-brand" style="display: flex; width: 50%; height: 100%; background-color: #2563EB;">
            <div class="brand-title">
                <h2 style="font-size: 48px; padding: 16px; color: aliceblue;">智能云图库</h2>
                <span style="font-size: 16px; padding: 16px; color: aliceblue;">属于你自己的图片素材库</span>
            </div>
        </div>

        <div class="login-container">
            <div class="menu">
                <div class="form">
                    <div class="form-password">
                        <a-form :model="registerDTO" name="basic" autocomplete="off" @finish="register(registerDTO)"
                            layout="vertical" @finishFailed="onFinishFailed" style="margin: 24px; width: 90%;">
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

                            <div style="display: flex; justify-content: space-between; margin-bottom: 24px;">
                                <div class="tips">
                                    已有账号?
                                    <RouterLink to="/user/login">去登录</RouterLink>
                                </div>
                            </div>

                            <a-form-item :wrapper-col="{ span: 24 }">
                                <a-button type="primary" html-type="submit" style="width: 100%;"
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
import { sendVerificationCodeUsingPost } from '@/api/verificationCodeController';
import { useAliyunCaptcha } from '@/api/useAliyunCaptcha';
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

/* 阿里云验证码 */
const { init: initCaptcha, triggerCaptcha, captchaVerifying } = useAliyunCaptcha({
    onCaptchaVerify: async (captchaVerifyParam: string) => {
        const account = registerDTO.account ?? '';
        const type = isPhone(account) ? 1 : 2;
        const res = await sendVerificationCodeUsingPost({
            account: registerDTO.account,
            captchaVerifyParam,
            type: type
        });

        if (res.data.code === 0 && res.data.data) {
            codeSent.value = true;
            startCountdown();
            message.success('验证码已发送');
            return true;
        } else {
            message.error(res.data.message || '验证码发送失败');
            return false;
        }
    }
});

onMounted(() => {
    initCaptcha();
});

/* 验证码计时器 */
const countdown = ref(0);
let timer: ReturnType<typeof setInterval> | null = null;

const startCountdown = () => {
    countdown.value = 60;
    timer = setInterval(() => {
        countdown.value--;
        if (countdown.value <= 0) {
            clearInterval(timer!);
            timer = null;
        }
    }, 1000);
};

const sendCode = () => {
    if (!registerDTO.account) {
        message.warning('请先输入账号');
        return;
    }
    triggerCaptcha();
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

.login-brand {
    display: flex;
    justify-content: center;
    align-items: center;
}

.brand-title {
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
}

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
    border-radius: 4;
    /* 投影效果 */
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
    display: flex;
    /* flex-direction: column; */
    justify-content: center;
    align-items: center;
}

.form {
    width: 80%;
    height: 80%;
    display: flex;
    flex-direction: column;
    /* justify-content: center; */
    /* align-items: center; */
}

.form-menu {
    width: 100%;
    display: flex;
    /* flex-direction: column; */
    justify-content: center;
    /* align-items: center; */
    top: 0;
    margin: 16px 0;

}

.form-form {
    display: flex;
}

.tips {}
</style>
