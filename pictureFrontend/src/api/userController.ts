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
  body: API.AdminUpdateDTO,
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
