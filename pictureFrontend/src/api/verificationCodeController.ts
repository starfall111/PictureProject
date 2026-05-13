// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** sendVerificationCode POST /api/verification/send */
export async function sendVerificationCodeUsingPost(
  body: API.SendVerificationCodeDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseString_>("/api/verification/send", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
