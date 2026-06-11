declare namespace API {
  type AdminBanControllerGetBanRecordListParams = {
    current?: number;
    pageSize?: number;
    banStatus?: string;
  };

  type AdminBatchControllerCancelParams = {
    id: number;
  };

  type AdminBatchControllerEndParams = {
    id: number;
  };

  type AdminBatchControllerGenerateCodesParams = {
    id: number;
  };

  type AdminBatchControllerGetBatchDetailParams = {
    id: number;
  };

  type AdminBatchControllerListBatchesParams = {
    current?: number;
    pageSize?: number;
    status?: number;
  };

  type AdminBatchControllerRestoreParams = {
    id: number;
  };

  type AdminBatchControllerTransitionParams = {
    id: number;
    targetStatus: number;
  };

  type AdminBatchControllerUpdateBatchParams = {
    id: number;
  };

  type AdminBatchTaskControllerGetAdminDetailParams = {
    id: number;
  };

  type AdminBatchTaskControllerGetAdminListParams = {
    dto: BatchTaskQueryDTO;
  };

  type AdminBatchTaskVO = {
    taskId?: number;
    userId?: number;
    spaceId?: number;
    searchText?: string;
    searchSource?: string;
    totalCount?: number;
    successCount?: number;
    failCount?: number;
    status?: string;
    categoryId?: number;
    namePrefix?: string;
    tags?: string;
    errorMessage?: string;
    createTime?: string;
    updateTime?: string;
    finishTime?: string;
  };

  type AdminCouponControllerListCouponsParams = {
    current?: number;
    pageSize?: number;
    status?: number;
    userId?: number;
    batchId?: number;
  };

  type AdminCouponControllerRevokeParams = {
    id: number;
  };

  type AdminFeedbackControllerAddInternalNoteParams = {
    id: number;
  };

  type AdminFeedbackControllerAdminReplyFeedbackParams = {
    id: number;
  };

  type AdminFeedbackControllerBatchCloseFeedbackParams = {
    reason: string;
  };

  type AdminFeedbackControllerClaimFeedbackParams = {
    id: number;
  };

  type AdminFeedbackControllerConvertToReportParams = {
    id: number;
  };

  type AdminFeedbackControllerGetAdminFeedbackDetailParams = {
    id: number;
  };

  type AdminFeedbackControllerGetAdminFeedbackListParams = {
    dto: FeedbackQueryDTO;
  };

  type AdminFeedbackControllerRejectFeedbackParams = {
    id: number;
    reason: string;
  };

  type AdminFeedbackControllerTransferFeedbackParams = {
    id: number;
    targetHandlerId: number;
  };

  type AdminFeedbackControllerUpdatePriorityParams = {
    id: number;
    priority: string;
  };

  type AdminReportControllerGetAdminReportDetailParams = {
    reportId: number;
  };

  type AdminReportControllerGetAdminReportListParams = {
    dto: ReportQueryDTO;
  };

  type AdminSystemMessageControllerDeleteSystemMessageParams = {
    id: number;
  };

  type AdminSystemMessageControllerListSystemMessagesParams = {
    dto: SystemMessageQueryDTO;
  };

  type AdminSystemMessageControllerPublishSystemMessageParams = {
    id: number;
  };

  type AdminSystemMessageControllerRevokeSystemMessageParams = {
    id: number;
  };

  type AdminUpdateDTO = {
    userName?: string;
    userAvatar?: string;
    userProfile?: string;
    userRole?: string;
    id?: number;
  };

  type AdminVipControllerGrantVipParams = {
    userId: number;
  };

  type AdminVipControllerListVipUsersParams = {
    current?: number;
    pageSize?: number;
    vipType?: number;
  };

  type AdminVipControllerRevokeVipParams = {
    userId: number;
  };

  type AdminVipControllerSetDegradeLevelParams = {
    level: number;
  };

  type BanRecordVO = {
    id?: number;
    userId?: number;
    userName?: string;
    banType?: string;
    banTypeDesc?: string;
    banDuration?: number;
    banStartTime?: string;
    banEndTime?: string;
    banReason?: string;
    violationCount?: number;
    unbanned?: boolean;
    unbanTime?: string;
    unbanReason?: string;
    banOperatorName?: string;
    reportId?: number;
    createTime?: string;
  };

  type BanUnbanDTO = {
    userId?: number;
    unbanReason?: string;
  };

  type BaseResponseAdminBatchTaskVO = {
    code?: number;
    data?: AdminBatchTaskVO;
    message?: string;
  };

  type BaseResponseBatchTaskStatsVO = {
    code?: number;
    data?: BatchTaskStatsVO;
    message?: string;
  };

  type BaseResponseBatchTaskVO = {
    code?: number;
    data?: BatchTaskVO;
    message?: string;
  };

  type BaseResponseBoolean = {
    code?: number;
    data?: boolean;
    message?: string;
  };

  type BaseResponseCategory = {
    code?: number;
    data?: Category;
    message?: string;
  };

  type BaseResponseCodeCouponBatch = {
    code?: number;
    data?: CodeCouponBatch;
    message?: string;
  };

  type BaseResponseCouponActivateVO = {
    code?: number;
    data?: CouponActivateVO;
    message?: string;
  };

  type BaseResponseFeedbackAttachmentVO = {
    code?: number;
    data?: FeedbackAttachmentVO;
    message?: string;
  };

  type BaseResponseFeedbackStatsVO = {
    code?: number;
    data?: FeedbackStatsVO;
    message?: string;
  };

  type BaseResponseFeedbackVO = {
    code?: number;
    data?: FeedbackVO;
    message?: string;
  };

  type BaseResponseFeedTimelineVO = {
    code?: number;
    data?: FeedTimelineVO;
    message?: string;
  };

  type BaseResponseFeedUnreadVO = {
    code?: number;
    data?: FeedUnreadVO;
    message?: string;
  };

  type BaseResponseFollowCountVO = {
    code?: number;
    data?: FollowCountVO;
    message?: string;
  };

  type BaseResponseInteger = {
    code?: number;
    data?: number;
    message?: string;
  };

  type BaseResponseListBatchTaskVO = {
    code?: number;
    data?: BatchTaskVO[];
    message?: string;
  };

  type BaseResponseListCategory = {
    code?: number;
    data?: Category[];
    message?: string;
  };

  type BaseResponseListImageSearchResult = {
    code?: number;
    data?: ImageSearchResult[];
    message?: string;
  };

  type BaseResponseListSpaceLevel = {
    code?: number;
    data?: SpaceLevel[];
    message?: string;
  };

  type BaseResponseListSpaceVO = {
    code?: number;
    data?: SpaceVO[];
    message?: string;
  };

  type BaseResponseListTag = {
    code?: number;
    data?: Tag[];
    message?: string;
  };

  type BaseResponseLoginUserVO = {
    code?: number;
    data?: LoginUserVO;
    message?: string;
  };

  type BaseResponseLong = {
    code?: number;
    data?: number;
    message?: string;
  };

  type BaseResponseMapLongBoolean = {
    code?: number;
    data?: Record<string, any>;
    message?: string;
  };

  type BaseResponseMapStringObject = {
    code?: number;
    data?: Record<string, any>;
    message?: string;
  };

  type BaseResponseObject = {
    code?: number;
    data?: any;
    message?: string;
  };

  type BaseResponsePageAdminBatchTaskVO = {
    code?: number;
    data?: PageAdminBatchTaskVO;
    message?: string;
  };

  type BaseResponsePageBanRecordVO = {
    code?: number;
    data?: PageBanRecordVO;
    message?: string;
  };

  type BaseResponsePageCategory = {
    code?: number;
    data?: PageCategory;
    message?: string;
  };

  type BaseResponsePageCodeCoupon = {
    code?: number;
    data?: PageCodeCoupon;
    message?: string;
  };

  type BaseResponsePageCodeCouponBatch = {
    code?: number;
    data?: PageCodeCouponBatch;
    message?: string;
  };

  type BaseResponsePageCouponVO = {
    code?: number;
    data?: PageCouponVO;
    message?: string;
  };

  type BaseResponsePageFeedbackListItemVO = {
    code?: number;
    data?: PageFeedbackListItemVO;
    message?: string;
  };

  type BaseResponsePageFollowUserVO = {
    code?: number;
    data?: PageFollowUserVO;
    message?: string;
  };

  type BaseResponsePageNotificationVO = {
    code?: number;
    data?: PageNotificationVO;
    message?: string;
  };

  type BaseResponsePagePictureBriefVO = {
    code?: number;
    data?: PagePictureBriefVO;
    message?: string;
  };

  type BaseResponsePagePictureEntityVO = {
    code?: number;
    data?: PagePictureEntityVO;
    message?: string;
  };

  type BaseResponsePagePictureVO = {
    code?: number;
    data?: PagePictureVO;
    message?: string;
  };

  type BaseResponsePagePublicBatchVO = {
    code?: number;
    data?: PagePublicBatchVO;
    message?: string;
  };

  type BaseResponsePageReportVO = {
    code?: number;
    data?: PageReportVO;
    message?: string;
  };

  type BaseResponsePageSpace = {
    code?: number;
    data?: PageSpace;
    message?: string;
  };

  type BaseResponsePageSystemMessageVO = {
    code?: number;
    data?: PageSystemMessageVO;
    message?: string;
  };

  type BaseResponsePageTag = {
    code?: number;
    data?: PageTag;
    message?: string;
  };

  type BaseResponsePageUser = {
    code?: number;
    data?: PageUser;
    message?: string;
  };

  type BaseResponsePageUserVO = {
    code?: number;
    data?: PageUserVO;
    message?: string;
  };

  type BaseResponsePicture = {
    code?: number;
    data?: Picture;
    message?: string;
  };

  type BaseResponsePictureVO = {
    code?: number;
    data?: PictureVO;
    message?: string;
  };

  type BaseResponseRecommendVO = {
    code?: number;
    data?: RecommendVO;
    message?: string;
  };

  type BaseResponseReportStatsVO = {
    code?: number;
    data?: ReportStatsVO;
    message?: string;
  };

  type BaseResponseReportVO = {
    code?: number;
    data?: ReportVO;
    message?: string;
  };

  type BaseResponseSeckillOrder = {
    code?: number;
    data?: SeckillOrder;
    message?: string;
  };

  type BaseResponseSeckillStatsVO = {
    code?: number;
    data?: SeckillStatsVO;
    message?: string;
  };

  type BaseResponseSpace = {
    code?: number;
    data?: Space;
    message?: string;
  };

  type BaseResponseString = {
    code?: number;
    data?: string;
    message?: string;
  };

  type BaseResponseTag = {
    code?: number;
    data?: Tag;
    message?: string;
  };

  type BaseResponseToggleFavoriteVO = {
    code?: number;
    data?: ToggleFavoriteVO;
    message?: string;
  };

  type BaseResponseToggleLikeVO = {
    code?: number;
    data?: ToggleLikeVO;
    message?: string;
  };

  type BaseResponseUser = {
    code?: number;
    data?: User;
    message?: string;
  };

  type BaseResponseUserProfileVO = {
    code?: number;
    data?: UserProfileVO;
    message?: string;
  };

  type BaseResponseUserVO = {
    code?: number;
    data?: UserVO;
    message?: string;
  };

  type BaseResponseVipStatsVO = {
    code?: number;
    data?: VipStatsVO;
    message?: string;
  };

  type BatchCreateDTO = {
    name?: string;
    type?: number;
    totalStock?: number;
    startTime?: string;
    endTime?: string;
  };

  type BatchStatusQueryDTO = {
    pictureIds?: number[];
  };

  type BatchTaskControllerGetTaskParams = {
    taskId: number;
  };

  type BatchTaskQueryDTO = {
    current?: number;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
    status?: string;
    searchSource?: string;
    keyword?: string;
    startTime?: string;
    endTime?: string;
    userId?: number;
  };

  type BatchTaskStatsVO = {
    totalCount?: number;
    pendingCount?: number;
    processingCount?: number;
    completedCount?: number;
    failedCount?: number;
    todayNewCount?: number;
    successRate?: number;
  };

  type BatchTaskUpdateDTO = {
    id?: number;
    status?: string;
    errorMessage?: string;
    tags?: string;
  };

  type BatchTaskVO = {
    taskId?: number;
    status?: string;
    totalCount?: number;
    successCount?: number;
    failCount?: number;
    message?: string;
    createTime?: string;
    finishTime?: string;
    searchText?: string;
    searchSource?: string;
    categoryId?: number;
    namePrefix?: string;
    tags?: string[];
    spaceId?: number;
  };

  type BatchUpdateDTO = {
    id?: number;
    name?: string;
    type?: number;
    totalStock?: number;
    startTime?: string;
    endTime?: string;
  };

  type BenchmarkGrabRequest = {
    userId?: number;
    batchId?: number;
    token?: string;
    clientIP?: string;
  };

  type Category = {
    id?: number;
    name?: string;
    count?: number;
    createTime?: string;
    editTime?: string;
    updateTime?: string;
    isDelete?: number;
  };

  type CategoryAddDTO = {
    name?: string;
  };

  type CategoryBriefVO = {
    id?: number;
    name?: string;
  };

  type CategoryControllerGetCategoryByIdParams = {
    id: number;
  };

  type CategoryQueryDTO = {
    current?: number;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
    name?: string;
  };

  type CategoryUpdateDTO = {
    id?: number;
    name?: string;
  };

  type CodeCoupon = {
    id?: number;
    batchId?: number;
    userId?: number;
    code?: string;
    type?: number;
    status?: number;
    issuedAt?: string;
    activatedAt?: string;
    expireAt?: string;
    version?: number;
    createTime?: string;
    updateTime?: string;
    isDelete?: number;
  };

  type CodeCouponBatch = {
    id?: number;
    batchNo?: string;
    name?: string;
    type?: number;
    totalStock?: number;
    currentStock?: number;
    startTime?: string;
    endTime?: string;
    status?: number;
    version?: number;
    editTime?: string;
    createTime?: string;
    updateTime?: string;
    isDelete?: number;
  };

  type CouponActivateDTO = {
    couponId?: number;
  };

  type CouponActivateVO = {
    activated?: boolean;
    vipType?: number;
    vipTypeName?: string;
    vipExpireTime?: string;
    couponType?: number;
    couponTypeName?: string;
    message?: string;
  };

  type CouponControllerListMyCouponsParams = {
    page?: number;
    size?: number;
    status?: number;
  };

  type CouponVO = {
    id?: number;
    code?: string;
    type?: number;
    typeName?: string;
    status?: number;
    statusName?: string;
    issuedAt?: string;
    activatedAt?: string;
    expireAt?: string;
    remainingDays?: number;
  };

  type DeleteRequest = {
    id?: number;
  };

  type FeedbackAttachmentVO = {
    id?: number;
    fileUrl?: string;
    fileName?: string;
    fileSize?: number;
    fileType?: string;
  };

  type FeedbackControllerConfirmFeedbackParams = {
    id: number;
  };

  type FeedbackControllerGetFeedbackDetailParams = {
    id: number;
  };

  type FeedbackControllerGetMyFeedbackListParams = {
    dto: FeedbackQueryDTO;
  };

  type FeedbackControllerReopenFeedbackParams = {
    id: number;
  };

  type FeedbackControllerReplyFeedbackParams = {
    id: number;
  };

  type FeedbackControllerWithdrawFeedbackParams = {
    id: number;
  };

  type FeedbackConvertDTO = {
    targetType?: string;
    targetId?: number;
    reasonType?: string;
    description?: string;
  };

  type FeedbackListItemVO = {
    id?: number;
    title?: string;
    type?: string;
    typeDesc?: string;
    priority?: string;
    status?: string;
    statusDesc?: string;
    isAnonymous?: boolean;
    createTime?: string;
    updateTime?: string;
  };

  type FeedbackQueryDTO = {
    current?: number;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
    status?: string;
    type?: string;
    priority?: string;
    keyword?: string;
    startTime?: string;
    endTime?: string;
  };

  type FeedbackReplyDTO = {
    content?: string;
  };

  type FeedbackReplyVO = {
    id?: number;
    userId?: number;
    userName?: string;
    userAvatar?: string;
    replyType?: string;
    replyTypeDesc?: string;
    content?: string;
    createTime?: string;
  };

  type FeedbackStatsVO = {
    totalCount?: number;
    pendingCount?: number;
    processingCount?: number;
    resolvedCount?: number;
    closedCount?: number;
    rejectedCount?: number;
    p0Count?: number;
  };

  type FeedbackStatusLogVO = {
    id?: number;
    fromStatus?: string;
    toStatus?: string;
    operatorId?: number;
    operatorType?: string;
    remark?: string;
    createTime?: string;
  };

  type FeedbackSubmitDTO = {
    title?: string;
    content?: string;
    type?: string;
    relatedPictureId?: number;
    relatedUserId?: number;
    attachmentIds?: number[];
    isAnonymous?: boolean;
  };

  type FeedbackVO = {
    id?: number;
    userId?: number;
    userName?: string;
    userAvatar?: string;
    title?: string;
    content?: string;
    type?: string;
    typeDesc?: string;
    priority?: string;
    status?: string;
    statusDesc?: string;
    source?: string;
    relatedPictureId?: number;
    relatedUserId?: number;
    isAnonymous?: boolean;
    closeReason?: string;
    reopenCount?: number;
    convertedReportId?: number;
    replies?: FeedbackReplyVO[];
    statusLogs?: FeedbackStatusLogVO[];
    attachments?: FeedbackAttachmentVO[];
    createTime?: string;
    updateTime?: string;
  };

  type FeedControllerGetTimelineParams = {
    queryDTO: FeedQueryDTO;
  };

  type FeedQueryDTO = {
    current?: number;
    pageSize?: number;
  };

  type FeedTimelineVO = {
    records?: FeedVO[];
    total?: number;
    current?: number;
    size?: number;
    unreadCount?: number;
  };

  type FeedUnreadVO = {
    unreadCount?: number;
  };

  type FeedVO = {
    id?: number;
    url?: string;
    thumbnailUrl?: string;
    name?: string;
    introduction?: string;
    tags?: string[];
    picWidth?: number;
    picHeight?: number;
    createTime?: string;
    categoryId?: number;
    categoryName?: string;
    userId?: number;
    userName?: string;
    userAvatar?: string;
    socialInfo?: PictureSocialVO;
    isNew?: boolean;
  };

  type FileDTO = {
    id?: number;
    fileUrl?: string;
    name?: string;
    categoryId?: number;
    spaceId?: number;
    tags?: string;
    introduction?: string;
    userId?: number;
  };

  type FollowActionDTO = {
    targetUserId?: number;
  };

  type FollowControllerGetFollowCountParams = {
    userId: number;
  };

  type FollowControllerIsFollowingParams = {
    targetUserId: number;
  };

  type FollowControllerListFollowersParams = {
    userId: number;
    current?: number;
    pageSize?: number;
  };

  type FollowControllerListFollowingParams = {
    userId: number;
    current?: number;
    pageSize?: number;
  };

  type FollowCountVO = {
    followCount?: number;
    followerCount?: number;
  };

  type FollowUserVO = {
    id?: number;
    userName?: string;
    userAvatar?: string;
    isFollowing?: boolean;
    followTime?: string;
  };

  type ImageSearchResult = {
    thumbUrl?: string;
    fromUrl?: string;
  };

  type LoginUserVO = {
    id?: number;
    userAccount?: string;
    userPhone?: string;
    userEmail?: string;
    userName?: string;
    userAvatar?: string;
    userProfile?: string;
    userRole?: string;
    vipType?: number;
    vipExpireTime?: string;
    editTime?: string;
    createTime?: string;
    updateTime?: string;
  };

  type NotificationControllerDeleteNotificationParams = {
    id: number;
  };

  type NotificationControllerListNotificationsParams = {
    queryDTO: NotificationQueryDTO;
  };

  type NotificationControllerMarkAsReadParams = {
    id: number;
  };

  type NotificationQueryDTO = {
    current?: number;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
    type?: string;
    isRead?: number;
  };

  type NotificationVO = {
    id?: number;
    senderId?: number;
    senderName?: string;
    senderAvatar?: string;
    type?: string;
    title?: string;
    content?: string;
    resourceId?: number;
    resourceUrl?: string;
    isRead?: number;
    createTime?: string;
  };

  type OrderItem = {
    column?: string;
    asc?: boolean;
  };

  type PageAdminBatchTaskVO = {
    records?: AdminBatchTaskVO[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: any;
    searchCount?: any;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PageBanRecordVO = {
    records?: BanRecordVO[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: any;
    searchCount?: any;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PageCategory = {
    records?: Category[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: any;
    searchCount?: any;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PageCodeCoupon = {
    records?: CodeCoupon[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: any;
    searchCount?: any;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PageCodeCouponBatch = {
    records?: CodeCouponBatch[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: any;
    searchCount?: any;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PageCouponVO = {
    records?: CouponVO[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: any;
    searchCount?: any;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PageFeedbackListItemVO = {
    records?: FeedbackListItemVO[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: any;
    searchCount?: any;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PageFollowUserVO = {
    records?: FollowUserVO[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: any;
    searchCount?: any;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PageNotificationVO = {
    records?: NotificationVO[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: any;
    searchCount?: any;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PagePictureBriefVO = {
    records?: PictureBriefVO[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: any;
    searchCount?: any;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PagePictureEntityVO = {
    records?: PictureEntityVO[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: any;
    searchCount?: any;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PagePictureVO = {
    records?: PictureVO[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: any;
    searchCount?: any;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PagePublicBatchVO = {
    records?: PublicBatchVO[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: any;
    searchCount?: any;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PageReportVO = {
    records?: ReportVO[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: any;
    searchCount?: any;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PageSpace = {
    records?: Space[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: any;
    searchCount?: any;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PageSystemMessageVO = {
    records?: SystemMessageVO[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: any;
    searchCount?: any;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PageTag = {
    records?: Tag[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: any;
    searchCount?: any;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PageUser = {
    records?: User[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: any;
    searchCount?: any;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type PageUserVO = {
    records?: UserVO[];
    total?: number;
    size?: number;
    current?: number;
    orders?: OrderItem[];
    optimizeCountSql?: any;
    searchCount?: any;
    optimizeJoinOfCountSql?: boolean;
    maxLimit?: number;
    countId?: string;
    pages?: number;
  };

  type Picture = {
    id?: number;
    url?: string;
    thumbnailUrl?: string;
    originUrl?: string;
    name?: string;
    introduction?: string;
    categoryId?: number;
    spaceId?: number;
    tags?: string;
    picSize?: number;
    picWidth?: number;
    picHeight?: number;
    picScale?: number;
    picFormat?: string;
    userId?: number;
    reviewStatus?: number;
    reviewMessage?: string;
    reviewerId?: number;
    reviewTime?: string;
    createTime?: string;
    editTime?: string;
    updateTime?: string;
    hotScore?: number;
    isDelete?: number;
  };

  type PictureBriefVO = {
    id?: number;
    name?: string;
    url?: string;
    thumbnailUrl?: string;
    picWidth?: number;
    picHeight?: number;
    categoryName?: string;
    userId?: number;
    userVO?: UserVO;
    tags?: string[];
    createTime?: string;
    likeCount?: number;
    favoriteCount?: number;
    viewCount?: number;
    downloadCount?: number;
    likeTime?: string;
    favoriteTime?: string;
  };

  type PictureControllerDownloadParams = {
    id: number;
  };

  type PictureControllerGetPictureByIdAdminParams = {
    id: number;
  };

  type PictureControllerGetPictureByIdUserCacheParams = {
    id: number;
  };

  type PictureControllerGetPictureByIdUserParams = {
    id: number;
  };

  type PictureControllerGetUserFavoritedPicturesCacheParams = {
    userId: number;
  };

  type PictureControllerGetUserFavoritedPicturesParams = {
    userId: number;
  };

  type PictureControllerGetUserLikedPicturesCacheParams = {
    userId: number;
  };

  type PictureControllerGetUserLikedPicturesParams = {
    userId: number;
  };

  type PictureControllerGetUserUploadedPicturesCacheParams = {
    userId: number;
  };

  type PictureControllerRecordDownloadCountCacheParams = {
    pictureId: number;
  };

  type PictureControllerRecordDownloadCountParams = {
    pictureId: number;
  };

  type PictureControllerRecordShareCacheParams = {
    pictureId: number;
  };

  type PictureControllerRecordShareParams = {
    pictureId: number;
  };

  type PictureControllerRecordViewCacheParams = {
    pictureId: number;
  };

  type PictureControllerRecordViewParams = {
    pictureId: number;
  };

  type PictureControllerToggleFavoriteCacheParams = {
    pictureId: number;
  };

  type PictureControllerToggleFavoriteParams = {
    pictureId: number;
  };

  type PictureControllerToggleLikeCacheParams = {
    pictureId: number;
  };

  type PictureControllerToggleLikeParams = {
    pictureId: number;
  };

  type PictureEditDTO = {
    id?: number;
    name?: string;
    introduction?: string;
    categoryId?: number;
    tags?: string[];
  };

  type PictureEntityVO = {
    id?: number;
    url?: string;
    thumbnailUrl?: string;
    originPictureUrl?: string;
    name?: string;
    introduction?: string;
    categoryId?: number;
    categoryName?: string;
    tags?: string;
    picSize?: number;
    picWidth?: number;
    picHeight?: number;
    picScale?: number;
    picFormat?: string;
    userId?: number;
    reviewStatus?: number;
    reviewMessage?: string;
    reviewerId?: number;
    reviewTime?: string;
    createTime?: string;
    editTime?: string;
    updateTime?: string;
  };

  type PictureQueryDTO = {
    current?: number;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
    id?: number;
    name?: string;
    introduction?: string;
    categoryId?: number;
    tags?: string[];
    picSize?: number;
    picWidth?: number;
    picHeight?: number;
    picScale?: number;
    picFormat?: string;
    userId?: number;
    spaceId?: number;
    nullSpaceId?: boolean;
    reviewStatus?: number;
    searchText?: string;
    startEditTime?: string;
    endEditTime?: string;
  };

  type PictureReviewDTO = {
    id?: number;
    reviewStatus?: number;
    reviewMessage?: string;
  };

  type PictureSocialVO = {
    likeCount?: number;
    favoriteCount?: number;
    shareCount?: number;
    viewCount?: number;
    downloadCount?: number;
    isLiked?: boolean;
    isFavorited?: boolean;
  };

  type PictureUpdateDTO = {
    id?: number;
    name?: string;
    introduction?: string;
    categoryId?: number;
    tags?: string[];
  };

  type PictureUploadByBatchDTO = {
    searchText?: string;
    count?: number;
    profile?: string;
    tags?: string[];
    categoryId?: number;
    searchSource?: string;
    spaceId?: number;
  };

  type PictureVO = {
    id?: number;
    url?: string;
    thumbnailUrl?: string;
    originUrl?: string;
    name?: string;
    introduction?: string;
    categoryId?: number;
    categoryName?: string;
    tags?: string[];
    picSize?: number;
    picWidth?: number;
    picHeight?: number;
    picScale?: number;
    picFormat?: string;
    userId?: number;
    reviewStatus?: number;
    reviewMessage?: string;
    reviewerId?: number;
    reviewTime?: string;
    userVO?: UserVO;
    spaceId?: number;
    socialInfo?: PictureSocialVO;
  };

  type PublicBatchVO = {
    id?: number;
    name?: string;
    type?: number;
    totalStock?: number;
    remainStock?: number;
    startTime?: string;
    endTime?: string;
    status?: number;
    progress?: number;
  };

  type RecommendQueryDTO = {
    current?: number;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
    scene?: string;
    basePictureId?: number;
    excludeIds?: number[];
    categoryId?: number;
  };

  type RecommendVO = {
    pictures?: PictureVO[];
    reason?: string;
    scene?: string;
    hasMore?: boolean;
  };

  type ReportControllerCancelReportParams = {
    reportId: number;
  };

  type ReportControllerGetMyReportListParams = {
    dto: ReportQueryDTO;
  };

  type ReportControllerGetReportDetailParams = {
    reportId: number;
  };

  type ReportHandleDTO = {
    reportId?: number;
    handleResult?: string;
    handleReason?: string;
    banDuration?: number;
  };

  type ReportQueryDTO = {
    current?: number;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
    status?: string;
    targetType?: string;
    reasonType?: string;
    startTime?: string;
    endTime?: string;
    minReportCount?: number;
  };

  type ReportStatsVO = {
    pendingCount?: number;
    todayNewCount?: number;
    totalHandledCount?: number;
  };

  type ReportSubmitDTO = {
    targetType?: string;
    targetId?: number;
    reasonType?: string;
    description?: string;
  };

  type ReportTargetVO = {
    id?: number;
    type?: string;
    title?: string;
    thumbnailUrl?: string;
    authorId?: number;
    authorName?: string;
  };

  type ReportVO = {
    id?: number;
    reporterId?: number;
    reporterName?: string;
    reporterAvatar?: string;
    targetType?: string;
    targetId?: number;
    targetInfo?: ReportTargetVO;
    reasonType?: string;
    reasonDesc?: string;
    description?: string;
    status?: string;
    statusDesc?: string;
    handlerId?: number;
    handleResult?: string;
    handleResultDesc?: string;
    handleReason?: string;
    handleTime?: string;
    reportCount?: number;
    sourceFeedbackId?: number;
    createTime?: string;
    updateTime?: string;
  };

  type SearchPictureByPictureDTO = {
    pictureId?: number;
  };

  type SeckillBenchmarkControllerGetResultParams = {
    userId: number;
    orderNo: string;
  };

  type SeckillBenchmarkControllerGetTokenParams = {
    userId: number;
    batchId: number;
  };

  type SeckillBenchmarkControllerInitParams = {
    batchId: number;
  };

  type SeckillBenchmarkControllerStatsParams = {
    batchId: number;
  };

  type SeckillControllerGetBatchInfoParams = {
    batchId: number;
  };

  type SeckillControllerGetResultParams = {
    orderNo: string;
  };

  type SeckillControllerGetTokenParams = {
    batchId: number;
  };

  type SeckillControllerListBatchesParams = {
    status?: number;
    current?: number;
    pageSize?: number;
  };

  type SeckillGrabDTO = {
    batchId?: number;
    token?: string;
  };

  type SeckillOrder = {
    id?: number;
    userId?: number;
    batchId?: number;
    couponId?: number;
    orderNo?: string;
    status?: number;
    createTime?: string;
  };

  type SeckillStatsVO = {
    totalBatches?: number;
    totalCoupons?: number;
    claimedCount?: number;
    activatedCount?: number;
    expiredCount?: number;
    claimRate?: number;
    activationRate?: number;
  };

  type SendVerificationCodeDTO = {
    type?: number;
    account?: string;
    captchaVerifyParam?: string;
  };

  type SocialBenchmarkControllerBatchFavoriteStatusCacheParams = {
    userId: number;
  };

  type SocialBenchmarkControllerBatchFavoriteStatusDbParams = {
    userId: number;
  };

  type SocialBenchmarkControllerBatchLikeStatusCacheParams = {
    userId: number;
  };

  type SocialBenchmarkControllerBatchLikeStatusDbParams = {
    userId: number;
  };

  type SocialBenchmarkControllerGetUserFavoritedPicturesParams = {
    userId: number;
  };

  type SocialBenchmarkControllerGetUserLikedPicturesParams = {
    userId: number;
  };

  type SocialBenchmarkControllerToggleFavoriteCacheParams = {
    pictureId: number;
    userId: number;
  };

  type SocialBenchmarkControllerToggleFavoriteDbParams = {
    pictureId: number;
    userId: number;
  };

  type SocialBenchmarkControllerToggleLikeCacheParams = {
    pictureId: number;
    userId: number;
  };

  type SocialBenchmarkControllerToggleLikeDbParams = {
    pictureId: number;
    userId: number;
  };

  type Space = {
    id?: number;
    spaceName?: string;
    spaceLevel?: number;
    maxSize?: number;
    maxCount?: number;
    totalSize?: number;
    totalCount?: number;
    userId?: number;
    createTime?: string;
    editTime?: string;
    updateTime?: string;
    isDelete?: number;
  };

  type SpaceAddDTO = {
    spaceName?: string;
    spaceLevel?: number;
    maxSize?: number;
    maxCount?: number;
    userId?: number;
  };

  type SpaceControllerGetSpaceByIdParams = {
    id: number;
  };

  type SpaceControllerGetSpaceByUserIdParams = {
    id: number;
  };

  type SpaceEditDTO = {
    id?: number;
    spaceName?: string;
  };

  type SpaceLevel = {
    value?: number;
    text?: string;
    maxCount?: number;
    maxSize?: number;
  };

  type SpaceQueryDTO = {
    current?: number;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
    id?: number;
    spaceName?: string;
    spaceLevel?: number;
    maxSize?: number;
    maxCount?: number;
    totalSize?: number;
    totalCount?: number;
    userId?: number;
    createTime?: string;
    editTime?: string;
    updateTime?: string;
  };

  type SpaceUpdateDTO = {
    id?: number;
    spaceName?: string;
    spaceLevel?: number;
    maxSize?: number;
    maxCount?: number;
    totalSize?: number;
    totalCount?: number;
    userId?: number;
  };

  type SpaceVO = {
    id?: number;
    spaceName?: string;
    spaceLevel?: number;
    maxSize?: number;
    maxCount?: number;
    totalSize?: number;
    totalCount?: number;
    userId?: number;
    createTime?: string;
    editTime?: string;
    userVO?: UserVO;
  };

  type SseEmitter = {
    timeout?: number;
  };

  type SystemMessageCreateDTO = {
    id?: number;
    title?: string;
    content?: string;
    sendMode?: string;
    targetType?: string;
    filterRole?: string;
    filterSpaceLevel?: number;
    filterRegisterStart?: string;
    filterRegisterEnd?: string;
  };

  type SystemMessageQueryDTO = {
    current?: number;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
    title?: string;
    status?: number;
    sendMode?: string;
  };

  type SystemMessageVO = {
    id?: number;
    title?: string;
    content?: string;
    sendMode?: string;
    targetType?: string;
    filterRole?: string;
    filterSpaceLevel?: number;
    filterRegisterStart?: string;
    filterRegisterEnd?: string;
    status?: number;
    publisherId?: number;
    publishTime?: string;
    createTime?: string;
    updateTime?: string;
  };

  type Tag = {
    id?: number;
    name?: string;
    count?: number;
    createTime?: string;
    editTime?: string;
    updateTime?: string;
    isDelete?: number;
  };

  type TagAddDTO = {
    name?: string;
  };

  type TagControllerGetTagByIdParams = {
    id: number;
  };

  type TagQueryDTO = {
    current?: number;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
    name?: string;
  };

  type TagUpdateDTO = {
    id?: number;
    name?: string;
  };

  type ToggleFavoriteVO = {
    favorited?: boolean;
    favoriteCount?: number;
  };

  type ToggleLikeVO = {
    liked?: boolean;
    likeCount?: number;
  };

  type User = {
    id?: number;
    userAccount?: string;
    userPhone?: string;
    userEmail?: string;
    userPassword?: string;
    userName?: string;
    userAvatar?: string;
    userProfile?: string;
    userRole?: string;
    vipType?: number;
    vipExpireTime?: string;
    vipActivatedAt?: string;
    vipTotalDays?: number;
    banStatus?: string;
    banEndTime?: string;
    violationCount?: number;
    lastViolationTime?: string;
    editTime?: string;
    createTime?: string;
    updateTime?: string;
    isDelete?: number;
  };

  type UserAddDTO = {
    userAccount?: string;
    userName?: string;
    userAvatar?: string;
    userProfile?: string;
    userRole?: string;
  };

  type UserBindAccountDTO = {
    type?: number;
    account?: string;
    verificationCode?: string;
  };

  type UserControllerGetUserInfoParams = {
    id: number;
  };

  type UserControllerGetUserProfileCacheParams = {
    id: number;
  };

  type UserControllerGetUserProfileParams = {
    id: number;
  };

  type UserLoginDTO = {
    type?: number;
    account?: string;
    password?: string;
    verityCode?: string;
    isVerityCode?: number;
  };

  type UserPasswordUpdateDTO = {
    oldPassword?: string;
    newPassword?: string;
    confirmPassword?: string;
  };

  type UserPictureQueryDTO = {
    current?: number;
    pageSize?: number;
    category?: string;
    tags?: string[];
    sortBy?: string;
  };

  type UserProfileVO = {
    id?: number;
    userName?: string;
    userAvatar?: string;
    userProfile?: string;
    userRole?: string;
    vipType?: number;
    vipExpireTime?: string;
    isActiveVip?: boolean;
    createTime?: string;
    uploadCount?: number;
    totalLikes?: number;
    totalFavorites?: number;
    totalViews?: number;
    totalShares?: number;
    totalDownloads?: number;
    userLikeCount?: number;
    userFavoriteCount?: number;
    categories?: CategoryBriefVO[];
    followCount?: number;
    followerCount?: number;
    isFollowed?: boolean;
  };

  type UserQueryDTO = {
    current?: number;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
    id?: number;
    userAccount?: string;
    userPhone?: string;
    userEmail?: string;
    userName?: string;
    userRole?: string;
  };

  type UserRegisterDTO = {
    type?: number;
    account?: string;
    password?: string;
    checkPassword?: string;
    verityCode?: string;
  };

  type UserUpdateDTO = {
    userName?: string;
    userAvatar?: string;
    userProfile?: string;
  };

  type UserVO = {
    id?: number;
    userAccount?: string;
    userPhone?: string;
    userEmail?: string;
    userName?: string;
    userAvatar?: string;
    userProfile?: string;
    userRole?: string;
    vipType?: number;
    createTime?: string;
  };

  type VipGrantDTO = {
    userId?: number;
    days?: number;
    reason?: string;
  };

  type VipStatsVO = {
    totalVipUsers?: number;
    activeVipUsers?: number;
    todayNewVip?: number;
    expiringVipUsers?: number;
  };
}
