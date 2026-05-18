// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** download GET /api/file/download */
export async function downloadUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.downloadUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<any>("/api/file/download", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** getPictureByIdAdmin GET /api/file/picture/admin/${param0} */
export async function getPictureByIdAdminUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getPictureByIdAdminUsingGETParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponsePicture_>(
    `/api/file/picture/admin/${param0}`,
    {
      method: "GET",
      params: { ...queryParams },
      ...(options || {}),
    }
  );
}

/** queryPictureAdmin POST /api/file/picture/admin/query */
export async function queryPictureAdminUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.queryPictureAdminUsingPOSTParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePagePictureEntityVO_>(
    "/api/file/picture/admin/query",
    {
      method: "POST",
      params: {
        ...params,
      },
      ...(options || {}),
    }
  );
}

/** deletePicture DELETE /api/file/picture/delete */
export async function deletePictureUsingDelete(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deletePictureUsingDELETEParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>("/api/file/picture/delete", {
    method: "DELETE",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** editPicture POST /api/file/picture/edit */
export async function editPictureUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.editPictureUsingPOSTParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>("/api/file/picture/edit", {
    method: "POST",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** updatePicture POST /api/file/picture/update */
export async function updatePictureUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.updatePictureUsingPOSTParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>("/api/file/picture/update", {
    method: "POST",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** getPictureByIdUser GET /api/file/picture/user/${param0} */
export async function getPictureByIdUserUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getPictureByIdUserUsingGETParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponsePictureVO_>(
    `/api/file/picture/user/${param0}`,
    {
      method: "GET",
      params: { ...queryParams },
      ...(options || {}),
    }
  );
}

/** queryPictureUser POST /api/file/picture/user/query */
export async function queryPictureUserUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.queryPictureUserUsingPOSTParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePagePictureVO_>(
    "/api/file/picture/user/query",
    {
      method: "POST",
      params: {
        ...params,
      },
      ...(options || {}),
    }
  );
}

/** upload POST /api/file/upload */
export async function uploadUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.uploadUsingPOSTParams,
  body: {},
  files?: File,
  options?: { [key: string]: any }
) {
  const formData = new FormData();

  if (files) {
    formData.append("files", files);
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

  return request<API.BaseResponsePictureVO_>("/api/file/upload", {
    method: "POST",
    params: {
      ...params,
    },
    data: formData,
    requestType: "form",
    ...(options || {}),
  });
}
