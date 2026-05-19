// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** getPictureByIdAdmin GET /api/picture/admin/${param0} */
export async function getPictureByIdAdminUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getPictureByIdAdminUsingGETParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponsePicture_>(`/api/picture/admin/${param0}`, {
    method: "GET",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** queryPictureAdmin POST /api/picture/admin/query */
export async function queryPictureAdminUsingPost(
  body: API.PictureQueryDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePagePictureEntityVO_>(
    "/api/picture/admin/query",
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      data: body,
      ...(options || {}),
    }
  );
}

/** deletePicture DELETE /api/picture/delete */
export async function deletePictureUsingDelete(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>("/api/picture/delete", {
    method: "DELETE",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** download GET /api/picture/download */
export async function downloadUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.downloadUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<any>("/api/picture/download", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** editPicture POST /api/picture/edit */
export async function editPictureUsingPost(
  body: API.PictureEditDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>("/api/picture/edit", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** reviewPicture POST /api/picture/review */
export async function reviewPictureUsingPost(
  body: API.PictureReviewDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>("/api/picture/review", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** updatePicture POST /api/picture/update */
export async function updatePictureUsingPost(
  body: API.PictureUpdateDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>("/api/picture/update", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** upload POST /api/picture/upload */
export async function uploadUsingPost1(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.uploadUsingPOST1Params,
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

  return request<API.BaseResponsePictureVO_>("/api/picture/upload", {
    method: "POST",
    params: {
      ...params,
    },
    data: formData,
    requestType: "form",
    ...(options || {}),
  });
}

/** pictureUploadByBatch POST /api/picture/upload/batch */
export async function pictureUploadByBatchUsingPost(
  body: API.PictureUploadByBatchDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseInt_>("/api/picture/upload/batch", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** upload POST /api/picture/upload/url */
export async function uploadUsingPost(
  body: API.FileDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePictureVO_>("/api/picture/upload/url", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** getPictureByIdUser GET /api/picture/user/${param0} */
export async function getPictureByIdUserUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getPictureByIdUserUsingGETParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponsePictureVO_>(`/api/picture/user/${param0}`, {
    method: "GET",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** queryPictureUser POST /api/picture/user/query */
export async function queryPictureUserUsingPost(
  body: API.PictureQueryDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePagePictureVO_>("/api/picture/user/query", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** queryPictureUserCache POST /api/picture/user/query/cache */
export async function queryPictureUserCacheUsingPost(
  body: API.PictureQueryDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePagePictureVO_>(
    "/api/picture/user/query/cache",
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      data: body,
      ...(options || {}),
    }
  );
}
