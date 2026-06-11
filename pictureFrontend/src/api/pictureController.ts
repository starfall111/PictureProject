// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 GET /picture/admin/${param0} */
export async function pictureControllerGetPictureByIdAdmin(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.PictureControllerGetPictureByIdAdminParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponsePicture>(`/picture/admin/${param0}`, {
    method: "GET",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /picture/admin/query */
export async function pictureControllerQueryPictureAdmin(
  body: API.PictureQueryDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePagePictureEntityVO>("/picture/admin/query", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /picture/cache/download/count/${param0} */
export async function pictureControllerRecordDownloadCountCache(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.PictureControllerRecordDownloadCountCacheParams,
  options?: { [key: string]: any }
) {
  const { pictureId: param0, ...queryParams } = params;
  return request<API.BaseResponseBoolean>(
    `/picture/cache/download/count/${param0}`,
    {
      method: "POST",
      params: { ...queryParams },
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 POST /picture/cache/favorite/${param0} */
export async function pictureControllerToggleFavoriteCache(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.PictureControllerToggleFavoriteCacheParams,
  options?: { [key: string]: any }
) {
  const { pictureId: param0, ...queryParams } = params;
  return request<API.BaseResponseToggleFavoriteVO>(
    `/picture/cache/favorite/${param0}`,
    {
      method: "POST",
      params: { ...queryParams },
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 POST /picture/cache/favorite/status */
export async function pictureControllerBatchFavoriteStatusCache(
  body: API.BatchStatusQueryDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseMapLongBoolean>(
    "/picture/cache/favorite/status",
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

/** 此处后端没有提供注释 POST /picture/cache/favorited/user/${param0}/query */
export async function pictureControllerGetUserFavoritedPicturesCache(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.PictureControllerGetUserFavoritedPicturesCacheParams,
  body: API.UserPictureQueryDTO,
  options?: { [key: string]: any }
) {
  const { userId: param0, ...queryParams } = params;
  return request<API.BaseResponsePagePictureBriefVO>(
    `/picture/cache/favorited/user/${param0}/query`,
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

/** 此处后端没有提供注释 POST /picture/cache/following/user/query */
export async function pictureControllerGetFollowingPicturesCache(
  body: API.UserPictureQueryDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePagePictureBriefVO>(
    "/picture/cache/following/user/query",
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

/** 此处后端没有提供注释 POST /picture/cache/like/${param0} */
export async function pictureControllerToggleLikeCache(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.PictureControllerToggleLikeCacheParams,
  options?: { [key: string]: any }
) {
  const { pictureId: param0, ...queryParams } = params;
  return request<API.BaseResponseToggleLikeVO>(
    `/picture/cache/like/${param0}`,
    {
      method: "POST",
      params: { ...queryParams },
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 POST /picture/cache/like/status */
export async function pictureControllerBatchLikeStatusCache(
  body: API.BatchStatusQueryDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseMapLongBoolean>("/picture/cache/like/status", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /picture/cache/liked/user/${param0}/query */
export async function pictureControllerGetUserLikedPicturesCache(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.PictureControllerGetUserLikedPicturesCacheParams,
  body: API.UserPictureQueryDTO,
  options?: { [key: string]: any }
) {
  const { userId: param0, ...queryParams } = params;
  return request<API.BaseResponsePagePictureBriefVO>(
    `/picture/cache/liked/user/${param0}/query`,
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

/** 此处后端没有提供注释 POST /picture/cache/share/${param0} */
export async function pictureControllerRecordShareCache(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.PictureControllerRecordShareCacheParams,
  options?: { [key: string]: any }
) {
  const { pictureId: param0, ...queryParams } = params;
  return request<API.BaseResponseBoolean>(`/picture/cache/share/${param0}`, {
    method: "POST",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /picture/cache/uploaded/user/${param0}/query */
export async function pictureControllerGetUserUploadedPicturesCache(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.PictureControllerGetUserUploadedPicturesCacheParams,
  body: API.UserPictureQueryDTO,
  options?: { [key: string]: any }
) {
  const { userId: param0, ...queryParams } = params;
  return request<API.BaseResponsePagePictureBriefVO>(
    `/picture/cache/uploaded/user/${param0}/query`,
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

/** 此处后端没有提供注释 GET /picture/cache/user/${param0} */
export async function pictureControllerGetPictureByIdUserCache(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.PictureControllerGetPictureByIdUserCacheParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponsePictureVO>(`/picture/cache/user/${param0}`, {
    method: "GET",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /picture/cache/user/query */
export async function pictureControllerQueryPictureUserCache(
  body: API.PictureQueryDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePagePictureVO>("/picture/cache/user/query", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /picture/cache/view/${param0} */
export async function pictureControllerRecordViewCache(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.PictureControllerRecordViewCacheParams,
  options?: { [key: string]: any }
) {
  const { pictureId: param0, ...queryParams } = params;
  return request<API.BaseResponseBoolean>(`/picture/cache/view/${param0}`, {
    method: "POST",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 DELETE /picture/delete */
export async function pictureControllerDeletePicture(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/picture/delete", {
    method: "DELETE",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /picture/download */
export async function pictureControllerDownload(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.PictureControllerDownloadParams,
  options?: { [key: string]: any }
) {
  return request<any>("/picture/download", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /picture/download/count/${param0} */
export async function pictureControllerRecordDownloadCount(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.PictureControllerRecordDownloadCountParams,
  options?: { [key: string]: any }
) {
  const { pictureId: param0, ...queryParams } = params;
  return request<API.BaseResponseBoolean>(`/picture/download/count/${param0}`, {
    method: "POST",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /picture/edit */
export async function pictureControllerEditPicture(
  body: API.PictureEditDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/picture/edit", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /picture/favorite/${param0} */
export async function pictureControllerToggleFavorite(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.PictureControllerToggleFavoriteParams,
  options?: { [key: string]: any }
) {
  const { pictureId: param0, ...queryParams } = params;
  return request<API.BaseResponseToggleFavoriteVO>(
    `/picture/favorite/${param0}`,
    {
      method: "POST",
      params: { ...queryParams },
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 POST /picture/favorite/status */
export async function pictureControllerBatchFavoriteStatus(
  body: API.BatchStatusQueryDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseMapLongBoolean>("/picture/favorite/status", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /picture/favorited/user/${param0}/query */
export async function pictureControllerGetUserFavoritedPictures(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.PictureControllerGetUserFavoritedPicturesParams,
  body: API.UserPictureQueryDTO,
  options?: { [key: string]: any }
) {
  const { userId: param0, ...queryParams } = params;
  return request<API.BaseResponsePagePictureBriefVO>(
    `/picture/favorited/user/${param0}/query`,
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

/** 此处后端没有提供注释 POST /picture/following/user/query */
export async function pictureControllerGetFollowingPictures(
  body: API.UserPictureQueryDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePagePictureBriefVO>(
    "/picture/following/user/query",
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

/** 此处后端没有提供注释 POST /picture/like/${param0} */
export async function pictureControllerToggleLike(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.PictureControllerToggleLikeParams,
  options?: { [key: string]: any }
) {
  const { pictureId: param0, ...queryParams } = params;
  return request<API.BaseResponseToggleLikeVO>(`/picture/like/${param0}`, {
    method: "POST",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /picture/like/status */
export async function pictureControllerBatchLikeStatus(
  body: API.BatchStatusQueryDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseMapLongBoolean>("/picture/like/status", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /picture/liked/user/${param0}/query */
export async function pictureControllerGetUserLikedPictures(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.PictureControllerGetUserLikedPicturesParams,
  body: API.UserPictureQueryDTO,
  options?: { [key: string]: any }
) {
  const { userId: param0, ...queryParams } = params;
  return request<API.BaseResponsePagePictureBriefVO>(
    `/picture/liked/user/${param0}/query`,
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

/** 此处后端没有提供注释 POST /picture/review */
export async function pictureControllerReviewPicture(
  body: API.PictureReviewDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/picture/review", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /picture/search/picture */
export async function pictureControllerSearchPictureByPicture(
  body: API.SearchPictureByPictureDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListImageSearchResult>(
    "/picture/search/picture",
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

/** 此处后端没有提供注释 POST /picture/share/${param0} */
export async function pictureControllerRecordShare(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.PictureControllerRecordShareParams,
  options?: { [key: string]: any }
) {
  const { pictureId: param0, ...queryParams } = params;
  return request<API.BaseResponseBoolean>(`/picture/share/${param0}`, {
    method: "POST",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /picture/update */
export async function pictureControllerUpdatePicture(
  body: API.PictureUpdateDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/picture/update", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /picture/upload */
export async function pictureControllerUpload(
  body: {
    fileDTO?: API.FileDTO;
  },
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

  return request<API.BaseResponsePictureVO>("/picture/upload", {
    method: "POST",
    data: formData,
    requestType: "form",
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /picture/upload/batch */
export async function pictureControllerPictureUploadByBatch(
  body: API.PictureUploadByBatchDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBatchTaskVO>("/picture/upload/batch", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /picture/upload/url */
export async function pictureControllerUpload1(
  body: API.FileDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePictureVO>("/picture/upload/url", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /picture/user/${param0} */
export async function pictureControllerGetPictureByIdUser(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.PictureControllerGetPictureByIdUserParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponsePictureVO>(`/picture/user/${param0}`, {
    method: "GET",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /picture/user/pending/query */
export async function pictureControllerQueryPendingPictures(
  body: API.PictureQueryDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePagePictureVO>("/picture/user/pending/query", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /picture/user/query */
export async function pictureControllerQueryPictureUser(
  body: API.PictureQueryDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePagePictureVO>("/picture/user/query", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /picture/view/${param0} */
export async function pictureControllerRecordView(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.PictureControllerRecordViewParams,
  options?: { [key: string]: any }
) {
  const { pictureId: param0, ...queryParams } = params;
  return request<API.BaseResponseBoolean>(`/picture/view/${param0}`, {
    method: "POST",
    params: { ...queryParams },
    ...(options || {}),
  });
}
