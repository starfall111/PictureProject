// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** toggleFollow POST /api/follow/action */
export async function toggleFollowUsingPost(
  body: API.FollowActionDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>("/api/follow/action", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** getFollowCount GET /api/follow/count/${param0} */
export async function getFollowCountUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getFollowCountUsingGETParams,
  options?: { [key: string]: any }
) {
  const { userId: param0, ...queryParams } = params;
  return request<API.BaseResponseFollowCountVO_>(
    `/api/follow/count/${param0}`,
    {
      method: "GET",
      params: { ...queryParams },
      ...(options || {}),
    }
  );
}

/** listFollowers GET /api/follow/list/followers */
export async function listFollowersUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listFollowersUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageFollowUserVO_>(
    "/api/follow/list/followers",
    {
      method: "GET",
      params: {
        // current has a default value: 1
        current: "1",
        // pageSize has a default value: 10
        pageSize: "10",
        ...params,
      },
      ...(options || {}),
    }
  );
}

/** listFollowing GET /api/follow/list/following */
export async function listFollowingUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listFollowingUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageFollowUserVO_>(
    "/api/follow/list/following",
    {
      method: "GET",
      params: {
        // current has a default value: 1
        current: "1",
        // pageSize has a default value: 10
        pageSize: "10",
        ...params,
      },
      ...(options || {}),
    }
  );
}

/** isFollowing GET /api/follow/status/${param0} */
export async function isFollowingUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.isFollowingUsingGETParams,
  options?: { [key: string]: any }
) {
  const { targetUserId: param0, ...queryParams } = params;
  return request<API.BaseResponseBoolean_>(`/api/follow/status/${param0}`, {
    method: "GET",
    params: { ...queryParams },
    ...(options || {}),
  });
}
