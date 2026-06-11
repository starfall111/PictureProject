// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 POST /benchmark/social/db/favorite/${param0} */
export async function socialBenchmarkControllerToggleFavoriteDb(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.SocialBenchmarkControllerToggleFavoriteDbParams,
  options?: { [key: string]: any }
) {
  const { pictureId: param0, ...queryParams } = params;
  return request<API.BaseResponseToggleFavoriteVO>(
    `/benchmark/social/db/favorite/${param0}`,
    {
      method: "POST",
      params: {
        ...queryParams,
      },
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 POST /benchmark/social/db/favorite/status */
export async function socialBenchmarkControllerBatchFavoriteStatusDb(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.SocialBenchmarkControllerBatchFavoriteStatusDbParams,
  body: API.BatchStatusQueryDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseMapLongBoolean>(
    "/benchmark/social/db/favorite/status",
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      params: {
        ...params,
      },
      data: body,
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 POST /benchmark/social/db/like/${param0} */
export async function socialBenchmarkControllerToggleLikeDb(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.SocialBenchmarkControllerToggleLikeDbParams,
  options?: { [key: string]: any }
) {
  const { pictureId: param0, ...queryParams } = params;
  return request<API.BaseResponseToggleLikeVO>(
    `/benchmark/social/db/like/${param0}`,
    {
      method: "POST",
      params: {
        ...queryParams,
      },
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 POST /benchmark/social/db/like/status */
export async function socialBenchmarkControllerBatchLikeStatusDb(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.SocialBenchmarkControllerBatchLikeStatusDbParams,
  body: API.BatchStatusQueryDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseMapLongBoolean>(
    "/benchmark/social/db/like/status",
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      params: {
        ...params,
      },
      data: body,
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 POST /benchmark/social/favorite/${param0} */
export async function socialBenchmarkControllerToggleFavoriteCache(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.SocialBenchmarkControllerToggleFavoriteCacheParams,
  options?: { [key: string]: any }
) {
  const { pictureId: param0, ...queryParams } = params;
  return request<API.BaseResponseToggleFavoriteVO>(
    `/benchmark/social/favorite/${param0}`,
    {
      method: "POST",
      params: {
        ...queryParams,
      },
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 POST /benchmark/social/favorite/status */
export async function socialBenchmarkControllerBatchFavoriteStatusCache(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.SocialBenchmarkControllerBatchFavoriteStatusCacheParams,
  body: API.BatchStatusQueryDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseMapLongBoolean>(
    "/benchmark/social/favorite/status",
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      params: {
        ...params,
      },
      data: body,
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 POST /benchmark/social/favorited/user/${param0}/query */
export async function socialBenchmarkControllerGetUserFavoritedPictures(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.SocialBenchmarkControllerGetUserFavoritedPicturesParams,
  body: API.UserPictureQueryDTO,
  options?: { [key: string]: any }
) {
  const { userId: param0, ...queryParams } = params;
  return request<API.BaseResponsePagePictureBriefVO>(
    `/benchmark/social/favorited/user/${param0}/query`,
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

/** 此处后端没有提供注释 POST /benchmark/social/like/${param0} */
export async function socialBenchmarkControllerToggleLikeCache(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.SocialBenchmarkControllerToggleLikeCacheParams,
  options?: { [key: string]: any }
) {
  const { pictureId: param0, ...queryParams } = params;
  return request<API.BaseResponseToggleLikeVO>(
    `/benchmark/social/like/${param0}`,
    {
      method: "POST",
      params: {
        ...queryParams,
      },
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 POST /benchmark/social/like/status */
export async function socialBenchmarkControllerBatchLikeStatusCache(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.SocialBenchmarkControllerBatchLikeStatusCacheParams,
  body: API.BatchStatusQueryDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseMapLongBoolean>(
    "/benchmark/social/like/status",
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      params: {
        ...params,
      },
      data: body,
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 POST /benchmark/social/liked/user/${param0}/query */
export async function socialBenchmarkControllerGetUserLikedPictures(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.SocialBenchmarkControllerGetUserLikedPicturesParams,
  body: API.UserPictureQueryDTO,
  options?: { [key: string]: any }
) {
  const { userId: param0, ...queryParams } = params;
  return request<API.BaseResponsePagePictureBriefVO>(
    `/benchmark/social/liked/user/${param0}/query`,
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
