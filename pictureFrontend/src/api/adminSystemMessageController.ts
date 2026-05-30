// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** updateSystemMessage PUT /api/admin/system-message */
export async function updateSystemMessageUsingPut(
  body: API.SystemMessageCreateDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>("/api/admin/system-message", {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** createSystemMessage POST /api/admin/system-message */
export async function createSystemMessageUsingPost(
  body: API.SystemMessageCreateDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong_>("/api/admin/system-message", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** deleteSystemMessage DELETE /api/admin/system-message/${param0} */
export async function deleteSystemMessageUsingDelete(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deleteSystemMessageUsingDELETEParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseBoolean_>(
    `/api/admin/system-message/${param0}`,
    {
      method: "DELETE",
      params: { ...queryParams },
      ...(options || {}),
    }
  );
}

/** listSystemMessages GET /api/admin/system-message/list */
export async function listSystemMessagesUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listSystemMessagesUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageSystemMessageVO_>(
    "/api/admin/system-message/list",
    {
      method: "GET",
      params: {
        ...params,
      },
      ...(options || {}),
    }
  );
}

/** publishSystemMessage POST /api/admin/system-message/publish/${param0} */
export async function publishSystemMessageUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.publishSystemMessageUsingPOSTParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseBoolean_>(
    `/api/admin/system-message/publish/${param0}`,
    {
      method: "POST",
      params: { ...queryParams },
      ...(options || {}),
    }
  );
}

/** revokeSystemMessage POST /api/admin/system-message/revoke/${param0} */
export async function revokeSystemMessageUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.revokeSystemMessageUsingPOSTParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseBoolean_>(
    `/api/admin/system-message/revoke/${param0}`,
    {
      method: "POST",
      params: { ...queryParams },
      ...(options || {}),
    }
  );
}
