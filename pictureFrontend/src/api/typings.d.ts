declare namespace API {
  type AdminUpdateDTO = {
    id?: number;
    userAvatar?: string;
    userName?: string;
    userProfile?: string;
    userRole?: string;
  };

  type BaseResponseBoolean_ = {
    code?: number;
    data?: boolean;
    message?: string;
  };

  type BaseResponseCategory_ = {
    code?: number;
    data?: Category;
    message?: string;
  };

  type BaseResponseInt_ = {
    code?: number;
    data?: number;
    message?: string;
  };

  type BaseResponseListCategory_ = {
    code?: number;
    data?: Category[];
    message?: string;
  };

  type BaseResponseListSpaceLevel_ = {
    code?: number;
    data?: SpaceLevel[];
    message?: string;
  };

  type BaseResponseListSpaceVO_ = {
    code?: number;
    data?: SpaceVO[];
    message?: string;
  };

  type BaseResponseListTag_ = {
    code?: number;
    data?: Tag[];
    message?: string;
  };

  type BaseResponseLoginUserVO_ = {
    code?: number;
    data?: LoginUserVO;
    message?: string;
  };

  type BaseResponseLong_ = {
    code?: number;
    data?: number;
    message?: string;
  };

  type BaseResponsePageCategory_ = {
    code?: number;
    data?: PageCategory_;
    message?: string;
  };

  type BaseResponsePagePictureEntityVO_ = {
    code?: number;
    data?: PagePictureEntityVO_;
    message?: string;
  };

  type BaseResponsePagePictureVO_ = {
    code?: number;
    data?: PagePictureVO_;
    message?: string;
  };

  type BaseResponsePageSpace_ = {
    code?: number;
    data?: PageSpace_;
    message?: string;
  };

  type BaseResponsePageTag_ = {
    code?: number;
    data?: PageTag_;
    message?: string;
  };

  type BaseResponsePageUserVO_ = {
    code?: number;
    data?: PageUserVO_;
    message?: string;
  };

  type BaseResponsePicture_ = {
    code?: number;
    data?: Picture;
    message?: string;
  };

  type BaseResponsePictureVO_ = {
    code?: number;
    data?: PictureVO;
    message?: string;
  };

  type BaseResponseSpace_ = {
    code?: number;
    data?: Space;
    message?: string;
  };

  type BaseResponseString_ = {
    code?: number;
    data?: string;
    message?: string;
  };

  type BaseResponseTag_ = {
    code?: number;
    data?: Tag;
    message?: string;
  };

  type BaseResponseUser_ = {
    code?: number;
    data?: User;
    message?: string;
  };

  type BaseResponseUserVO_ = {
    code?: number;
    data?: UserVO;
    message?: string;
  };

  type Category = {
    count?: number;
    createTime?: string;
    editTime?: string;
    id?: number;
    isDelete?: number;
    name?: string;
    updateTime?: string;
  };

  type CategoryAddDTO = {
    name?: string;
  };

  type CategoryQueryDTO = {
    current?: number;
    name?: string;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
  };

  type CategoryUpdateDTO = {
    id?: number;
    name?: string;
  };

  type DeleteRequest = {
    id?: number;
  };

  type downloadUsingGETParams = {
    /** id */
    id?: number;
  };

  type FileDTO = {
    categoryId?: number;
    fileUrl?: string;
    id?: number;
    name?: string;
    spaceId?: number;
    tags?: string;
  };

  type getCategoryByIdUsingGETParams = {
    /** id */
    id: number;
  };

  type getPictureByIdAdminUsingGETParams = {
    /** id */
    id: number;
  };

  type getPictureByIdUserUsingGETParams = {
    /** id */
    id: number;
  };

  type getSpaceByIdUsingGETParams = {
    /** id */
    id: number;
  };

  type getSpaceByUserIdUsingGETParams = {
    /** id */
    id: number;
  };

  type getTagByIdUsingGETParams = {
    /** id */
    id: number;
  };

  type getUserInfoUsingGETParams = {
    /** id */
    id: number;
  };

  type LoginUserVO = {
    createTime?: string;
    editTime?: string;
    id?: number;
    updateTime?: string;
    userAccount?: string;
    userAvatar?: string;
    userEmail?: string;
    userName?: string;
    userPhone?: string;
    userProfile?: string;
    userRole?: string;
  };

  type PageCategory_ = {
    current?: number;
    pages?: number;
    records?: Category[];
    size?: number;
    total?: number;
  };

  type PagePictureEntityVO_ = {
    current?: number;
    pages?: number;
    records?: PictureEntityVO[];
    size?: number;
    total?: number;
  };

  type PagePictureVO_ = {
    current?: number;
    pages?: number;
    records?: PictureVO[];
    size?: number;
    total?: number;
  };

  type PageSpace_ = {
    current?: number;
    pages?: number;
    records?: Space[];
    size?: number;
    total?: number;
  };

  type PageTag_ = {
    current?: number;
    pages?: number;
    records?: Tag[];
    size?: number;
    total?: number;
  };

  type PageUserVO_ = {
    current?: number;
    pages?: number;
    records?: UserVO[];
    size?: number;
    total?: number;
  };

  type Picture = {
    categoryId?: number;
    createTime?: string;
    editTime?: string;
    id?: number;
    introduction?: string;
    isDelete?: number;
    name?: string;
    originUrl?: string;
    picFormat?: string;
    picHeight?: number;
    picScale?: number;
    picSize?: number;
    picWidth?: number;
    reviewMessage?: string;
    reviewStatus?: number;
    reviewTime?: string;
    reviewerId?: number;
    spaceId?: number;
    tags?: string;
    thumbnailUrl?: string;
    updateTime?: string;
    url?: string;
    userId?: number;
  };

  type PictureEditDTO = {
    categoryId?: number;
    id?: number;
    introduction?: string;
    name?: string;
    tags?: string[];
  };

  type PictureEntityVO = {
    categoryId?: number;
    categoryName?: string;
    createTime?: string;
    editTime?: string;
    id?: number;
    introduction?: string;
    name?: string;
    originPictureUrl?: string;
    picFormat?: string;
    picHeight?: number;
    picScale?: number;
    picSize?: number;
    picWidth?: number;
    reviewMessage?: string;
    reviewStatus?: number;
    reviewTime?: string;
    reviewerId?: number;
    tags?: string;
    thumbnailUrl?: string;
    updateTime?: string;
    url?: string;
    userId?: number;
  };

  type PictureQueryDTO = {
    categoryId?: number;
    current?: number;
    id?: number;
    introduction?: string;
    name?: string;
    nullSpaceId?: boolean;
    pageSize?: number;
    picFormat?: string;
    picHeight?: number;
    picScale?: number;
    picSize?: number;
    picWidth?: number;
    reviewStatus?: number;
    searchText?: string;
    sortField?: string;
    sortOrder?: string;
    spaceId?: number;
    tags?: string[];
    userId?: number;
  };

  type PictureReviewDTO = {
    id?: number;
    reviewMessage?: string;
    reviewStatus?: number;
  };

  type PictureUpdateDTO = {
    categoryId?: number;
    id?: number;
    introduction?: string;
    name?: string;
    tags?: string[];
  };

  type PictureUploadByBatchDTO = {
    categoryId?: number;
    count?: number;
    profile?: string;
    searchText?: string;
    tags?: string[];
  };

  type PictureVO = {
    categoryId?: number;
    categoryName?: string;
    id?: number;
    introduction?: string;
    name?: string;
    originUrl?: string;
    picFormat?: string;
    picHeight?: number;
    picScale?: number;
    picSize?: number;
    picWidth?: number;
    reviewMessage?: string;
    reviewStatus?: number;
    reviewTime?: string;
    reviewerId?: number;
    tags?: string[];
    thumbnailUrl?: string;
    url?: string;
    userId?: number;
    userVO?: UserVO;
  };

  type SendVerificationCodeDTO = {
    account?: string;
    captchaVerifyParam?: string;
    type?: number;
  };

  type Space = {
    createTime?: string;
    editTime?: string;
    id?: number;
    isDelete?: number;
    maxCount?: number;
    maxSize?: number;
    spaceLevel?: number;
    spaceName?: string;
    totalCount?: number;
    totalSize?: number;
    updateTime?: string;
    userId?: number;
  };

  type SpaceAddDTO = {
    maxCount?: number;
    maxSize?: number;
    spaceLevel?: number;
    spaceName?: string;
    userId?: number;
  };

  type SpaceEditDTO = {
    id?: number;
    spaceName?: string;
  };

  type SpaceLevel = {
    maxCount?: number;
    maxSize?: number;
    text?: string;
    value?: number;
  };

  type SpaceQueryDTO = {
    createTime?: string;
    current?: number;
    editTime?: string;
    id?: number;
    maxCount?: number;
    maxSize?: number;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
    spaceLevel?: number;
    spaceName?: string;
    totalCount?: number;
    totalSize?: number;
    updateTime?: string;
    userId?: number;
  };

  type SpaceUpdateDTO = {
    id?: number;
    maxCount?: number;
    maxSize?: number;
    spaceLevel?: number;
    spaceName?: string;
    totalCount?: number;
    totalSize?: number;
    userId?: number;
  };

  type SpaceVO = {
    createTime?: string;
    editTime?: string;
    id?: number;
    maxCount?: number;
    maxSize?: number;
    spaceLevel?: number;
    spaceName?: string;
    totalCount?: number;
    totalSize?: number;
    userId?: number;
    userVO?: UserVO;
  };

  type Tag = {
    count?: number;
    createTime?: string;
    editTime?: string;
    id?: number;
    isDelete?: number;
    name?: string;
    updateTime?: string;
  };

  type TagAddDTO = {
    name?: string;
  };

  type TagQueryDTO = {
    current?: number;
    name?: string;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
  };

  type TagUpdateDTO = {
    id?: number;
    name?: string;
  };

  type uploadUsingPOST1Params = {
    categoryId?: number;
    fileUrl?: string;
    id?: number;
    name?: string;
    spaceId?: number;
    tags?: string;
  };

  type User = {
    createTime?: string;
    editTime?: string;
    id?: number;
    isDelete?: number;
    updateTime?: string;
    userAccount?: string;
    userAvatar?: string;
    userEmail?: string;
    userName?: string;
    userPassword?: string;
    userPhone?: string;
    userProfile?: string;
    userRole?: string;
  };

  type UserAddDTO = {
    userAccount?: string;
    userAvatar?: string;
    userName?: string;
    userProfile?: string;
    userRole?: string;
  };

  type UserBindAccountDTO = {
    account?: string;
    type?: number;
    verificationCode?: string;
  };

  type UserLoginDTO = {
    account?: string;
    isVerityCode?: number;
    password?: string;
    type?: number;
    verityCode?: string;
  };

  type UserQueryDTO = {
    current?: number;
    id?: number;
    pageSize?: number;
    sortField?: string;
    sortOrder?: string;
    userAccount?: string;
    userEmail?: string;
    userName?: string;
    userPhone?: string;
    userRole?: string;
  };

  type UserRegisterDTO = {
    account?: string;
    checkPassword?: string;
    password?: string;
    type?: number;
    verityCode?: string;
  };

  type UserUpdateDTO = {
    userAvatar?: string;
    userName?: string;
    userProfile?: string;
  };

  type UserVO = {
    createTime?: string;
    id?: number;
    userAccount?: string;
    userAvatar?: string;
    userEmail?: string;
    userName?: string;
    userPhone?: string;
    userProfile?: string;
    userRole?: string;
  };
}
