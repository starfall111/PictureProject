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

/** recordDownloadCountCache POST /api/picture/cache/download/count/${param0} */
export async function recordDownloadCountCacheUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.recordDownloadCountCacheUsingPOSTParams,
  options?: { [key: string]: any }
) {
  const { pictureId: param0, ...queryParams } = params;
  return request<API.BaseResponseBoolean_>(
    `/api/picture/cache/download/count/${param0}`,
    {
      method: "POST",
      params: { ...queryParams },
      ...(options || {}),
    }
  );
}

/** toggleFavoriteCache POST /api/picture/cache/favorite/${param0} */
export async function toggleFavoriteCacheUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.toggleFavoriteCacheUsingPOSTParams,
  options?: { [key: string]: any }
) {
  const { pictureId: param0, ...queryParams } = params;
  return request<API.BaseResponseToggleFavoriteVO_>(
    `/api/picture/cache/favorite/${param0}`,
    {
      method: "POST",
      params: { ...queryParams },
      ...(options || {}),
    }
  );
}

/** batchFavoriteStatusCache POST /api/picture/cache/favorite/status */
export async function batchFavoriteStatusCacheUsingPost(
  body: API.BatchStatusQueryDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseMapLongBoolean_>(
    "/api/picture/cache/favorite/status",
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

/** getUserFavoritedPicturesCache POST /api/picture/cache/favorited/user/${param0}/query */
export async function getUserFavoritedPicturesCacheUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getUserFavoritedPicturesCacheUsingPOSTParams,
  body: API.UserPictureQueryDTO,
  options?: { [key: string]: any }
) {
  const { userId: param0, ...queryParams } = params;
  return request<API.BaseResponsePagePictureBriefVO_>(
    `/api/picture/cache/favorited/user/${param0}/query`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      params: { ...queryParams },
      data: body,
      ...(options || {}),
    }
  );
}

/** toggleLikeCache POST /api/picture/cache/like/${param0} */
export async function toggleLikeCacheUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.toggleLikeCacheUsingPOSTParams,
  options?: { [key: string]: any }
) {
  const { pictureId: param0, ...queryParams } = params;
  return request<API.BaseResponseToggleLikeVO_>(
    `/api/picture/cache/like/${param0}`,
    {
      method: "POST",
      params: { ...queryParams },
      ...(options || {}),
    }
  );
}

/** batchLikeStatusCache POST /api/picture/cache/like/status */
export async function batchLikeStatusCacheUsingPost(
  body: API.BatchStatusQueryDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseMapLongBoolean_>(
    "/api/picture/cache/like/status",
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

/** getUserLikedPicturesCache POST /api/picture/cache/liked/user/${param0}/query */
export async function getUserLikedPicturesCacheUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getUserLikedPicturesCacheUsingPOSTParams,
  body: API.UserPictureQueryDTO,
  options?: { [key: string]: any }
) {
  const { userId: param0, ...queryParams } = params;
  return request<API.BaseResponsePagePictureBriefVO_>(
    `/api/picture/cache/liked/user/${param0}/query`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      params: { ...queryParams },
      data: body,
      ...(options || {}),
    }
  );
}

/** recordShareCache POST /api/picture/cache/share/${param0} */
export async function recordShareCacheUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.recordShareCacheUsingPOSTParams,
  options?: { [key: string]: any }
) {
  const { pictureId: param0, ...queryParams } = params;
  return request<API.BaseResponseBoolean_>(
    `/api/picture/cache/share/${param0}`,
    {
      method: "POST",
      params: { ...queryParams },
      ...(options || {}),
    }
  );
}

/** getUserUploadedPicturesCache POST /api/picture/cache/uploaded/user/${param0}/query */
export async function getUserUploadedPicturesCacheUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getUserUploadedPicturesCacheUsingPOSTParams,
  body: API.UserPictureQueryDTO,
  options?: { [key: string]: any }
) {
  const { userId: param0, ...queryParams } = params;
  return request<API.BaseResponsePagePictureBriefVO_>(
    `/api/picture/cache/uploaded/user/${param0}/query`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      params: { ...queryParams },
      data: body,
      ...(options || {}),
    }
  );
}

/** getPictureByIdUserCache GET /api/picture/cache/user/${param0} */
export async function getPictureByIdUserCacheUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getPictureByIdUserCacheUsingGETParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponsePictureVO_>(
    `/api/picture/cache/user/${param0}`,
    {
      method: "GET",
      params: { ...queryParams },
      ...(options || {}),
    }
  );
}

/** queryPictureUserCache POST /api/picture/cache/user/query */
export async function queryPictureUserCacheUsingPost(
  body: API.PictureQueryDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePagePictureVO_>(
    "/api/picture/cache/user/query",
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

/** recordViewCache POST /api/picture/cache/view/${param0} */
export async function recordViewCacheUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.recordViewCacheUsingPOSTParams,
  options?: { [key: string]: any }
) {
  const { pictureId: param0, ...queryParams } = params;
  return request<API.BaseResponseBoolean_>(
    `/api/picture/cache/view/${param0}`,
    {
      method: "POST",
      params: { ...queryParams },
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

/** recordDownloadCount POST /api/picture/download/count/${param0} */
export async function recordDownloadCountUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.recordDownloadCountUsingPOSTParams,
  options?: { [key: string]: any }
) {
  const { pictureId: param0, ...queryParams } = params;
  return request<API.BaseResponseBoolean_>(
    `/api/picture/download/count/${param0}`,
    {
      method: "POST",
      params: { ...queryParams },
      ...(options || {}),
    }
  );
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

/** toggleFavorite POST /api/picture/favorite/${param0} */
export async function toggleFavoriteUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.toggleFavoriteUsingPOSTParams,
  options?: { [key: string]: any }
) {
  const { pictureId: param0, ...queryParams } = params;
  return request<API.BaseResponseToggleFavoriteVO_>(
    `/api/picture/favorite/${param0}`,
    {
      method: "POST",
      params: { ...queryParams },
      ...(options || {}),
    }
  );
}

/** batchFavoriteStatus POST /api/picture/favorite/status */
export async function batchFavoriteStatusUsingPost(
  body: API.BatchStatusQueryDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseMapLongBoolean_>(
    "/api/picture/favorite/status",
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

/** getUserFavoritedPictures POST /api/picture/favorited/user/${param0}/query */
export async function getUserFavoritedPicturesUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getUserFavoritedPicturesUsingPOSTParams,
  body: API.UserPictureQueryDTO,
  options?: { [key: string]: any }
) {
  const { userId: param0, ...queryParams } = params;
  return request<API.BaseResponsePagePictureBriefVO_>(
    `/api/picture/favorited/user/${param0}/query`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      params: { ...queryParams },
      data: body,
      ...(options || {}),
    }
  );
}

/** toggleLike POST /api/picture/like/${param0} */
export async function toggleLikeUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.toggleLikeUsingPOSTParams,
  options?: { [key: string]: any }
) {
  const { pictureId: param0, ...queryParams } = params;
  return request<API.BaseResponseToggleLikeVO_>(`/api/picture/like/${param0}`, {
    method: "POST",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** batchLikeStatus POST /api/picture/like/status */
export async function batchLikeStatusUsingPost(
  body: API.BatchStatusQueryDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseMapLongBoolean_>("/api/picture/like/status", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** getUserLikedPictures POST /api/picture/liked/user/${param0}/query */
export async function getUserLikedPicturesUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getUserLikedPicturesUsingPOSTParams,
  body: API.UserPictureQueryDTO,
  options?: { [key: string]: any }
) {
  const { userId: param0, ...queryParams } = params;
  return request<API.BaseResponsePagePictureBriefVO_>(
    `/api/picture/liked/user/${param0}/query`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      params: { ...queryParams },
      data: body,
      ...(options || {}),
    }
  );
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

/** searchPictureByPicture POST /api/picture/search/picture */
export async function searchPictureByPictureUsingPost(
  body: API.SearchPictureByPictureDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListImageSearchResult_>(
    "/api/picture/search/picture",
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

/** recordShare POST /api/picture/share/${param0} */
export async function recordShareUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.recordShareUsingPOSTParams,
  options?: { [key: string]: any }
) {
  const { pictureId: param0, ...queryParams } = params;
  return request<API.BaseResponseBoolean_>(`/api/picture/share/${param0}`, {
    method: "POST",
    params: { ...queryParams },
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

/** queryPendingPictures POST /api/picture/user/pending/query */
export async function queryPendingPicturesUsingPost(
  body: API.PictureQueryDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePagePictureVO_>(
    "/api/picture/user/pending/query",
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

/** recordView POST /api/picture/view/${param0} */
export async function recordViewUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.recordViewUsingPOSTParams,
  options?: { [key: string]: any }
) {
  const { pictureId: param0, ...queryParams } = params;
  return request<API.BaseResponseBoolean_>(`/api/picture/view/${param0}`, {
    method: "POST",
    params: { ...queryParams },
    ...(options || {}),
  });
}
