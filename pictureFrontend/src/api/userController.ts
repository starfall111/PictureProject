// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 POST /user/add */
export async function userControllerAddUser(
  body: API.UserAddDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseString>("/user/add", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /user/admin/update */
export async function userControllerUpdateUser1(
  body: API.AdminUpdateDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/user/admin/update", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /user/avatar/upload */
export async function userControllerUploadAvatar(
  body: {},
  file?: File,
  options?: { [key: string]: any }
) {
  const formData = new FormData();

  if (file) {
    formData.append("file", file);
  }

  Object.keys(body).forEach((ele) => {
    const item = (body as any)[ele];

    if (item !== undefined && item !== null) {
      if (typeof item === "object" && !(item instanceof File)) {
        if (item instanceof Array) {
          item.forEach((f) => formData.append(ele, f || ""));
        } else {
          formData.append(
            ele,
            new Blob([JSON.stringify(item)], { type: "application/json" })
          );
        }
      } else {
        formData.append(ele, item);
      }
    }
  });

  return request<API.BaseResponseString>("/user/avatar/upload", {
    method: "POST",
    data: formData,
    requestType: "form",
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /user/bind/account */
export async function userControllerBindAccount(
  body: API.UserBindAccountDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/user/bind/account", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /user/cache/get/info */
export async function userControllerGetUserInfoCache(options?: {
  [key: string]: any;
}) {
  return request<API.BaseResponseUserVO>("/user/cache/get/info", {
    method: "GET",
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /user/cache/get/login */
export async function userControllerGetLoginUserCache(options?: {
  [key: string]: any;
}) {
  return request<API.BaseResponseLoginUserVO>("/user/cache/get/login", {
    method: "GET",
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /user/cache/profile/${param0} */
export async function userControllerGetUserProfileCache(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.UserControllerGetUserProfileCacheParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseUserProfileVO>(
    `/user/cache/profile/${param0}`,
    {
      method: "GET",
      params: { ...queryParams },
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 DELETE /user/delete */
export async function userControllerDeleteUser(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/user/delete", {
    method: "DELETE",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /user/get/${param0} */
export async function userControllerGetUserInfo(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.UserControllerGetUserInfoParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseUser>(`/user/get/${param0}`, {
    method: "GET",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /user/get/info */
export async function userControllerGetUserInfo1(options?: {
  [key: string]: any;
}) {
  return request<API.BaseResponseUserVO>("/user/get/info", {
    method: "GET",
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /user/get/login */
export async function userControllerGetLoginUser(options?: {
  [key: string]: any;
}) {
  return request<API.BaseResponseLoginUserVO>("/user/get/login", {
    method: "GET",
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /user/login */
export async function userControllerLogin(
  body: API.UserLoginDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLoginUserVO>("/user/login", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /user/logout */
export async function userControllerLogOut(options?: { [key: string]: any }) {
  return request<API.BaseResponseString>("/user/logout", {
    method: "POST",
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /user/page/query */
export async function userControllerListUserVoByQuery(
  body: API.UserQueryDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageUserVO>("/user/page/query", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /user/password/update */
export async function userControllerUpdatePassword(
  body: API.UserPasswordUpdateDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/user/password/update", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /user/profile/${param0} */
export async function userControllerGetUserProfile(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.UserControllerGetUserProfileParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseUserProfileVO>(`/user/profile/${param0}`, {
    method: "GET",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /user/register */
export async function userControllerRegister(
  body: API.UserRegisterDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong>("/user/register", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /user/update */
export async function userControllerUpdateUser(
  body: API.UserUpdateDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/user/update", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
