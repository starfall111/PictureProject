// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** addUser POST /api/user/add */
export async function addUserUsingPost(
  body: API.UserAddDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseString_>("/api/user/add", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** updateUser POST /api/user/admin/update */
export async function updateUserUsingPost(
  body: API.AdminUpdateDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>("/api/user/admin/update", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** uploadAvatar POST /api/user/avatar/upload */
export async function uploadAvatarUsingPost(
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

  return request<API.BaseResponseString_>("/api/user/avatar/upload", {
    method: "POST",
    data: formData,
    requestType: "form",
    ...(options || {}),
  });
}

/** bindAccount POST /api/user/bind/account */
export async function bindAccountUsingPost(
  body: API.UserBindAccountDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>("/api/user/bind/account", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** getUserInfoCache GET /api/user/cache/get/info */
export async function getUserInfoCacheUsingGet(options?: {
  [key: string]: any;
}) {
  return request<API.BaseResponseUserVO_>("/api/user/cache/get/info", {
    method: "GET",
    ...(options || {}),
  });
}

/** getLoginUserCache GET /api/user/cache/get/login */
export async function getLoginUserCacheUsingGet(options?: {
  [key: string]: any;
}) {
  return request<API.BaseResponseLoginUserVO_>("/api/user/cache/get/login", {
    method: "GET",
    ...(options || {}),
  });
}

/** getUserProfileCache GET /api/user/cache/profile/${param0} */
export async function getUserProfileCacheUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getUserProfileCacheUsingGETParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseUserProfileVO_>(
    `/api/user/cache/profile/${param0}`,
    {
      method: "GET",
      params: { ...queryParams },
      ...(options || {}),
    }
  );
}

/** deleteUser DELETE /api/user/delete */
export async function deleteUserUsingDelete(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>("/api/user/delete", {
    method: "DELETE",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** getUserInfo GET /api/user/get/${param0} */
export async function getUserInfoUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getUserInfoUsingGETParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseUser_>(`/api/user/get/${param0}`, {
    method: "GET",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** getUserInfo GET /api/user/get/info */
export async function getUserInfoUsingGet1(options?: { [key: string]: any }) {
  return request<API.BaseResponseUserVO_>("/api/user/get/info", {
    method: "GET",
    ...(options || {}),
  });
}

/** getLoginUser GET /api/user/get/login */
export async function getLoginUserUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseLoginUserVO_>("/api/user/get/login", {
    method: "GET",
    ...(options || {}),
  });
}

/** login POST /api/user/login */
export async function loginUsingPost(
  body: API.UserLoginDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLoginUserVO_>("/api/user/login", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** logOut POST /api/user/logout */
export async function logOutUsingPost(options?: { [key: string]: any }) {
  return request<API.BaseResponseString_>("/api/user/logout", {
    method: "POST",
    ...(options || {}),
  });
}

/** listUserVOByQuery POST /api/user/page/query */
export async function listUserVoByQueryUsingPost(
  body: API.UserQueryDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageUserVO_>("/api/user/page/query", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** updatePassword POST /api/user/password/update */
export async function updatePasswordUsingPost(
  body: API.UserPasswordUpdateDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>("/api/user/password/update", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** getUserProfile GET /api/user/profile/${param0} */
export async function getUserProfileUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getUserProfileUsingGETParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseUserProfileVO_>(
    `/api/user/profile/${param0}`,
    {
      method: "GET",
      params: { ...queryParams },
      ...(options || {}),
    }
  );
}

/** register POST /api/user/register */
export async function registerUsingPost(
  body: API.UserRegisterDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong_>("/api/user/register", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** updateUser POST /api/user/update */
export async function updateUserUsingPost1(
  body: API.UserUpdateDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>("/api/user/update", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
