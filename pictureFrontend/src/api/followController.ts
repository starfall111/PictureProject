// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 POST /follow/action */
export async function followControllerToggleFollow(
  body: API.FollowActionDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/follow/action", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /follow/count/${param0} */
export async function followControllerGetFollowCount(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.FollowControllerGetFollowCountParams,
  options?: { [key: string]: any }
) {
  const { userId: param0, ...queryParams } = params;
  return request<API.BaseResponseFollowCountVO>(`/follow/count/${param0}`, {
    method: "GET",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /follow/list/followers */
export async function followControllerListFollowers(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.FollowControllerListFollowersParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageFollowUserVO>("/follow/list/followers", {
    method: "GET",
    params: {
      // current has a default value: 1
      current: "1",
      // pageSize has a default value: 10
      pageSize: "10",
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /follow/list/following */
export async function followControllerListFollowing(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.FollowControllerListFollowingParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageFollowUserVO>("/follow/list/following", {
    method: "GET",
    params: {
      // current has a default value: 1
      current: "1",
      // pageSize has a default value: 10
      pageSize: "10",
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /follow/status/${param0} */
export async function followControllerIsFollowing(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.FollowControllerIsFollowingParams,
  options?: { [key: string]: any }
) {
  const { targetUserId: param0, ...queryParams } = params;
  return request<API.BaseResponseBoolean>(`/follow/status/${param0}`, {
    method: "GET",
    params: { ...queryParams },
    ...(options || {}),
  });
}
