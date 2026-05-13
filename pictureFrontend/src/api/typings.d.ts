declare namespace API {
  type AdminUpdateDTO = {
    id?: number;
    userAvatar?: string;
    userEmail?: string;
    userName?: string;
    userPhone?: string;
    userProfile?: string;
    userRole?: string;
  };

  type BaseResponseBoolean_ = {
    code?: number;
    data?: boolean;
    message?: string;
  };

  type BaseResponseLoginUserVO_ = {
    code?: number;
    data?: LoginUserVO;
    message?: string;
  };

  type BaseResponseLong_ = {
    code?: number;
    data?: number;
    message?: string;
  };

  type BaseResponsePageUserVO_ = {
    code?: number;
    data?: PageUserVO_;
    message?: string;
  };

  type BaseResponseString_ = {
    code?: number;
    data?: string;
    message?: string;
  };

  type BaseResponseUser_ = {
    code?: number;
    data?: User;
    message?: string;
  };

  type BaseResponseUserVO_ = {
    code?: number;
    data?: UserVO;
    message?: string;
  };

  type DeleteRequest = {
    id?: number;
  };

  type getUserInfoUsingGETParams = {
    /** id */
    id: number;
  };

  type LoginUserVO = {
    createTime?: string;
    editTime?: string;
    id?: number;
    updateTime?: string;
    userAccount?: string;
    userAvatar?: string;
    userEmail?: string;
    userName?: string;
    userPhone?: string;
    userProfile?: string;
    userRole?: string;
  };

  type PageUserVO_ = {
    current?: number;
    pages?: number;
    records?: UserVO[];
    size?: number;
    total?: number;
  };

  type SendVerificationCodeDTO = {
    account?: string;
    captchaVerifyParam?: string;
    type?: number;
  };

  type User = {
    createTime?: string;
    editTime?: string;
    id?: number;
    isDelete?: number;
    updateTime?: string;
    userAccount?: string;
    userAvatar?: string;
    userEmail?: string;
    userName?: string;
    userPassword?: string;
    userPhone?: string;
    userProfile?: string;
    userRole?: string;
  };

  type UserAddDTO = {
    userAccount?: string;
    userAvatar?: string;
    userName?: string;
    userProfile?: string;
    userRole?: string;
  };

  type UserLoginDTO = {
    account?: string;
    isVerityCode?: number;
    password?: string;
    type?: number;
    verityCode?: string;
  };

  type UserQueryDTO = {
    current?: number;
    id?: number;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
    userAccount?: string;
    userEmail?: string;
    userName?: string;
    userPhone?: string;
    userRole?: string;
  };

  type UserRegisterDTO = {
    account?: string;
    checkPassword?: string;
    password?: string;
    type?: number;
    verityCode?: string;
  };

  type UserVO = {
    createTime?: string;
    id?: number;
    userAccount?: string;
    userAvatar?: string;
    userEmail?: string;
    userName?: string;
    userPhone?: string;
    userProfile?: string;
    userRole?: string;
  };
}
