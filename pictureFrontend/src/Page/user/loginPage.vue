<template>
    <div id="loginPage">
        <div class="login-brand">
            <div class="brand-title">
                <h2 class="brand-heading">图境</h2>
                <span class="brand-subtitle">属于你自己的图片素材库</span>
            </div>
        </div>

        <div class="login-container">
            <div class="menu">
                <div class="form">
                    <div class="form-menu">
                        <a-menu v-model:selectedKeys="current" mode="horizontal" :items="items"
                            style="font-size: 24px; width: 564px;" />
                    </div>
                    <div class="form-form" v-if="current[0] === 'login'">
                        <a-form :model="verificationCodeDTO" name="basic" autocomplete="off"
                            @finish="login(verificationCodeDTO)" layout="vertical" @finishFailed="onFinishFailed"
                            class="login-form">
                            <a-form-item name="account" label="账号"
                                :rules="[{ required: true, message: '请输入手机号/邮箱' }, { validator: validateAccount }]">
                                <a-input v-model:value="verificationCodeDTO.account" placeholder="请输入手机号/邮箱"
                                    :size="'large'" />
                            </a-form-item>

                            <a-form-item name="verityCode" label="验证码"
                                :rules="[{ required: true, message: '请输入验证码' }, { len: 6, message: '验证码必须为6位' }, { validator: validateCodeSent }]">
                                <a-input v-model:value="verificationCodeDTO.verityCode" placeholder="请输入验证码"
                                    :size="'large'" class="code-input" />
                                <a-button type="primary" class="code-btn" :size="'large'"
                                    :disabled="countdown > 0 || captchaVerifying" :loading="captchaVerifying"
                                    @click="sendCode">
                                    {{ countdown > 0 ? `${countdown}s 后重新获取` : '获取验证码' }}
                                </a-button>
                            </a-form-item>

                            <div class="form-footer">
                                <a-checkbox v-model:checked="remember">记住密码</a-checkbox>

                                <div class="tips">
                                    没有账号?
                                    <RouterLink to="/user/register">去注册</RouterLink>
                                </div>
                            </div>

                            <a-form-item :wrapper-col="{ span: 24 }">
                                <a-button type="primary" html-type="submit" class="submit-btn" :size="'large'"
                                    :loading="loginLoading">登录/注册</a-button>
                            </a-form-item>
                        </a-form>
                    </div>

                    <div class="form-password" v-if="current[0] === 'password'">
                        <a-form :model="passwordDTO" name="basic" autocomplete="off" @finish="login(passwordDTO)"
                            layout="vertical" @finishFailed="onFinishFailed" class="login-form">
                            <a-form-item name="account" label="账号"
                                :rules="[{ required: true, message: '请输入账号/手机号/邮箱' }, { min: 4, message: '账号长度不能小于4位' }]">
                                <a-input v-model:value="passwordDTO.account" placeholder="请输入账号/手机号/邮箱"
                                    :size="'large'" />
                            </a-form-item>

                            <a-form-item name="password" label="密码"
                                :rules="[{ required: true, message: '请输入密码' }, { min: 6, message: '密码长度不能小于6位' }]">
                                <a-input-password v-model:value="passwordDTO.password" placeholder="请输入密码"
                                    :size="'large'" />
                                <!-- <a-button type="primary" style="width: 40%;" :size="'large'">获取验证码</a-button> -->
                            </a-form-item>

                            <div class="form-footer">
                                <a-checkbox v-model:checked="remember">记住密码</a-checkbox>

                                <div class="tips">
                                    没有账号?
                                    <RouterLink to="/user/register">去注册</RouterLink>
                                </div>
                            </div>

                            <a-form-item :wrapper-col="{ span: 24 }">
                                <a-button type="primary" html-type="submit" class="submit-btn" :size="'large'"
                                    :loading="loginLoading">登录</a-button>
                            </a-form-item>
                        </a-form>
                    </div>
                </div>

            </div>

        </div>
    </div>

</template>


<script setup lang="ts">
import { h, ref, onMounted } from 'vue';
import { UserOutlined, LockOutlined } from '@ant-design/icons-vue';
import { MenuProps, message } from 'ant-design-vue';
import { reactive } from 'vue';
import { userControllerLogin } from '@/api/userController';
import { useAliyunCaptcha } from '@/access/useAliyunCaptcha';
import { noticeControllerSendVerificationCode } from '@/api/noticeController';
import router from '@/router';
import { userLoginUserStore } from '@/stores/user';
const current = ref<string[]>(['login']);
const items = ref<MenuProps['items']>([
    {
        key: 'login',
        icon: () => h(UserOutlined),
        label: '验证码',
        title: '验证码',
        style: { fontSize: '16px', left: '16px', padding: '0 48px' }
    },
    {
        key: 'password',
        icon: () => h(LockOutlined),
        label: '账号密码',
        title: '账号密码',
        style: { fontSize: '16px', marginLeft: 'auto', marginRight: '16px', padding: '0 48px' }
    },
]);

/* 记住密码开关 */
const remember = ref(false);

/* 记住密码 - localStorage 持久化（base64 编码，非真正加密，仅防肉眼读取） */
const REMEMBER_KEY = 'login_remember';

const encode = (data: unknown) => btoa(encodeURIComponent(JSON.stringify(data)));
const decode = (encoded: string): { remember: boolean; account: string; password: string } | null => {
    try {
        return JSON.parse(decodeURIComponent(atob(encoded)));
    } catch {
        return null;
    }
};

const loadRemembered = () => decode(localStorage.getItem(REMEMBER_KEY) || '');
const saveRemembered = (account: string, password: string) => {
    localStorage.setItem(REMEMBER_KEY, encode({ remember: true, account, password }));
};
const clearRemembered = () => localStorage.removeItem(REMEMBER_KEY);

/* 验证码登录 DTO */
const verificationCodeDTO = reactive<API.UserLoginDTO>({
    account: '',
    isVerityCode: 1,
    password: '',
    verityCode: '',
    type: 1
});

/* 密码登录 DTO */
const passwordDTO = reactive<API.UserLoginDTO>({
    account: '',
    password: '',
    verityCode: '',
    isVerityCode: 0,
    type: 1
});

/* 阿里云验证码 */
const { init: initCaptcha, triggerCaptcha, captchaVerifying } = useAliyunCaptcha({
    onCaptchaVerify: async (captchaVerifyParam: string) => {
        const account = verificationCodeDTO.account ?? '';
        const type = isPhone(account) ? 1 : 2;
        const res = await noticeControllerSendVerificationCode({
            account: verificationCodeDTO.account,
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
    /* 回填记住的账号密码 */
    const saved = loadRemembered();
    if (saved && saved.remember) {
        remember.value = true;
        verificationCodeDTO.account = saved.account || '';
        passwordDTO.account = saved.account || '';
        passwordDTO.password = saved.password || '';
    }
});

/* 是否已发送验证码 */
const codeSent = ref(false);

/* 账号格式校验 */
const validateAccount = (_rule: any, value: string) => {
    if (!value) return Promise.resolve();
    return (isPhone(value) || isEmail(value)) ? Promise.resolve() : Promise.reject('请输入有效的手机号/邮箱');
};

/* 验证码已发送校验 */
const validateCodeSent = (_rule: any, value: string) => {
    if (value && !codeSent.value) {
        return Promise.reject('请先获取验证码');
    }
    return Promise.resolve();
};

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
    if (!verificationCodeDTO.account) {
        message.warning('请先输入账号');
        return;
    }
    if (!isPhone(verificationCodeDTO.account) && !isEmail(verificationCodeDTO.account)) {
        message.warning('请输入有效的手机号/邮箱');
        return;
    }
    triggerCaptcha();
};

/* 判断account类型 */
const isPhone = (account: string) => {
    return /^1[3-9]\d{9}$/.test(account);
};

const isEmail = (account: string) => {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(account);
};

const onFinishFailed = (errorInfo: any) => {
    console.log('Failed:', errorInfo);
};

/* 登录loading状态 */
const loginLoading = ref(false);
const loginUserStore = userLoginUserStore();

/* 登录方法 */
const login = async (DTO: any) => {
    DTO.account = DTO.account?.trim();
    if (isPhone(DTO.account)) {
        DTO.type = 1;
    } else if (isEmail(DTO.account)) {
        DTO.type = 2;
    } else {
        DTO.type = 0;
    }
    loginLoading.value = true;
    try {
        const res = await userControllerLogin(DTO);
        if (res.data.code === 0 && res.data.data) {
            /* 根据勾选状态保存/清除记住的账号密码 */
            if (remember.value) {
                saveRemembered(DTO.account, DTO.password || '');
            } else {
                clearRemembered();
            }
            await loginUserStore.getLoginUser();
            message.success('登录成功');
            router.push({
                path: '/',
                replace: true
            })
        } else {
            message.error(res.data.message || '登录失败');
        }
    } finally {
        loginLoading.value = false;
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
    background-color: #33A1C9;
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

.code-input {
    width: 60%;
}

.code-btn {
    width: 40%;
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
