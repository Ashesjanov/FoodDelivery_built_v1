import { request } from './http'
import type {
  Address,
  AddressRequest,
  AdminUserUpdateRequest,
  AuthSession,
  Cart,
  CartAddRequest,
  CartItem,
  CartUpdateRequest,
  Coupon,
  CouponClaim,
  CouponClaimStatus,
  CouponCreateRequest,
  Delivery,
  Dish,
  DishDetail,
  DishPageQuery,
  DishSaveRequest,
  DishSpec,
  DishSpecRequest,
  LoginRequest,
  Merchant,
  MerchantCategory,
  MerchantCategoryRequest,
  MerchantDetail,
  MerchantPageQuery,
  MerchantSaveRequest,
  Order,
  OrderCreateRequest,
  OrderDetail,
  OrderPageQuery,
  PageQuery,
  PageResponse,
  PasswordChangeRequest,
  Payment,
  PaymentMockRequest,
  RegisterRequest,
  Review,
  ReviewCreateRequest,
  Rider,
  RiderProfileRequest,
  RiderStatus,
  User,
  UserProfileUpdateRequest,
} from '@/types/domain'

function params(input: Record<string, unknown>): Record<string, unknown> {
  return Object.fromEntries(
    Object.entries(input).filter(([, value]) => value !== undefined && value !== null && value !== ''),
  )
}

export const authApi = {
  register: (payload: RegisterRequest) => request<AuthSession>({ method: 'post', url: '/api/auth/register', data: payload }),
  login: (payload: LoginRequest) => request<AuthSession>({ method: 'post', url: '/api/auth/login', data: payload }),
  me: () => request<User>({ method: 'get', url: '/api/auth/me' }),
}

export const userApi = {
  me: () => request<User>({ method: 'get', url: '/api/users/me' }),
  updateMe: (payload: UserProfileUpdateRequest) =>
    request<User>({ method: 'put', url: '/api/users/me', data: payload }),
  changePassword: (payload: PasswordChangeRequest) =>
    request<void>({ method: 'put', url: '/api/users/me/password', data: payload }),
  deleteMe: () => request<void>({ method: 'delete', url: '/api/users/me' }),
  list: (query: PageQuery = {}) =>
    request<PageResponse<User>>({
      method: 'get',
      url: '/api/users',
      params: params({ page: query.page ?? 1, size: query.size ?? 20 }),
    }),
  get: (id: number) => request<User>({ method: 'get', url: `/api/users/${id}` }),
  adminUpdate: (id: number, payload: AdminUserUpdateRequest) =>
    request<User>({ method: 'put', url: `/api/users/${id}`, data: payload }),
  update: (id: number, payload: AdminUserUpdateRequest) =>
    request<User>({ method: 'put', url: `/api/users/${id}`, data: payload }),
  adminDelete: (id: number) => request<void>({ method: 'delete', url: `/api/users/${id}` }),
  remove: (id: number) => request<void>({ method: 'delete', url: `/api/users/${id}` }),
}

export const addressApi = {
  list: () => request<Address[]>({ method: 'get', url: '/api/users/me/addresses' }),
  create: (payload: AddressRequest) =>
    request<Address>({ method: 'post', url: '/api/users/me/addresses', data: payload }),
  get: (addressId: number) =>
    request<Address>({ method: 'get', url: `/api/users/me/addresses/${addressId}` }),
  update: (addressId: number, payload: AddressRequest) =>
    request<Address>({ method: 'put', url: `/api/users/me/addresses/${addressId}`, data: payload }),
  remove: (addressId: number) =>
    request<void>({ method: 'delete', url: `/api/users/me/addresses/${addressId}` }),
  setDefault: (addressId: number) =>
    request<Address>({ method: 'put', url: `/api/users/me/addresses/${addressId}/default` }),
}

export const merchantApi = {
  page: (query: MerchantPageQuery = {}) =>
    request<PageResponse<Merchant>>({
      method: 'get',
      url: '/api/merchants',
      params: params({
        page: query.page ?? 1,
        size: query.size ?? 20,
        keyword: query.keyword,
        categoryId: query.categoryId,
        openOnly: query.openOnly,
      }),
    }),
  list: (query: MerchantPageQuery = {}) =>
    request<PageResponse<Merchant>>({
      method: 'get',
      url: '/api/merchants',
      params: params({
        page: query.page ?? 1,
        size: query.size ?? 20,
        keyword: query.keyword,
        categoryId: query.categoryId,
        openOnly: query.openOnly,
      }),
    }),
  categories: () => request<MerchantCategory[]>({ method: 'get', url: '/api/merchants/categories' }),
  createCategory: (payload: MerchantCategoryRequest) =>
    request<MerchantCategory>({ method: 'post', url: '/api/merchants/categories', data: payload }),
  updateCategory: (id: number, payload: MerchantCategoryRequest) =>
    request<MerchantCategory>({ method: 'put', url: `/api/merchants/categories/${id}`, data: payload }),
  deleteCategory: (id: number) => request<void>({ method: 'delete', url: `/api/merchants/categories/${id}` }),
  removeCategory: (id: number) => request<void>({ method: 'delete', url: `/api/merchants/categories/${id}` }),
  detail: (id: number) => request<MerchantDetail>({ method: 'get', url: `/api/merchants/${id}` }),
  create: (payload: MerchantSaveRequest) =>
    request<Merchant>({ method: 'post', url: '/api/merchants', data: payload }),
  update: (id: number, payload: MerchantSaveRequest) =>
    request<Merchant>({ method: 'put', url: `/api/merchants/${id}`, data: payload }),
  updateBusinessStatus: (id: number, payload: { status: Merchant['businessStatus'] }) =>
    request<Merchant>({ method: 'patch', url: `/api/merchants/${id}/business-status`, data: payload }),
  setBusinessStatus: (id: number, payload: { status: Merchant['businessStatus'] }) =>
    request<Merchant>({ method: 'patch', url: `/api/merchants/${id}/business-status`, data: payload }),
  addCategory: (id: number, categoryId: number) =>
    request<void>({ method: 'post', url: `/api/merchants/${id}/categories/${categoryId}` }),
  removeMerchantCategory: (id: number, categoryId: number) =>
    request<void>({ method: 'delete', url: `/api/merchants/${id}/categories/${categoryId}` }),
}

export const dishApi = {
  page: (query: DishPageQuery) =>
    request<PageResponse<Dish>>({
      method: 'get',
      url: '/api/dishes',
      params: params({
        merchantId: query.merchantId,
        categoryId: query.categoryId,
        keyword: query.keyword,
        onSaleOnly: query.onSaleOnly,
        page: query.page ?? 1,
        size: query.size ?? 20,
      }),
    }),
  list: (query: DishPageQuery) =>
    request<PageResponse<Dish>>({
      method: 'get',
      url: '/api/dishes',
      params: params({
        merchantId: query.merchantId,
        categoryId: query.categoryId,
        keyword: query.keyword,
        onSaleOnly: query.onSaleOnly,
        page: query.page ?? 1,
        size: query.size ?? 20,
      }),
    }),
  detail: (id: number) => request<DishDetail>({ method: 'get', url: `/api/dishes/${id}` }),
  specs: (id: number) => request<DishSpec[]>({ method: 'get', url: `/api/dishes/${id}/specs` }),
  create: (merchantId: number, payload: DishSaveRequest) =>
    request<Dish>({ method: 'post', url: '/api/dishes', params: { merchantId }, data: payload }),
  update: (id: number, payload: DishSaveRequest) =>
    request<Dish>({ method: 'put', url: `/api/dishes/${id}`, data: payload }),
  updateStatus: (id: number, payload: { status: Dish['status'] }) =>
    request<Dish>({ method: 'patch', url: `/api/dishes/${id}/status`, data: payload }),
  setStatus: (id: number, payload: { status: Dish['status'] }) =>
    request<Dish>({ method: 'patch', url: `/api/dishes/${id}/status`, data: payload }),
  remove: (id: number) => request<void>({ method: 'delete', url: `/api/dishes/${id}` }),
  addSpec: (dishId: number, payload: DishSpecRequest) =>
    request<DishSpec>({ method: 'post', url: `/api/dishes/${dishId}/specs`, data: payload }),
  updateSpec: (dishId: number, id: number, payload: DishSpecRequest) =>
    request<DishSpec>({ method: 'put', url: `/api/dishes/${dishId}/specs/${id}`, data: payload }),
  removeSpec: (dishId: number, id: number) =>
    request<void>({ method: 'delete', url: `/api/dishes/${dishId}/specs/${id}` }),
}

export const cartApi = {
  cart: () => request<Cart>({ method: 'get', url: '/api/cart' }),
  get: () => request<Cart>({ method: 'get', url: '/api/cart' }),
  addItem: (payload: CartAddRequest) =>
    request<CartItem>({ method: 'post', url: '/api/cart/items', data: payload }),
  add: (payload: CartAddRequest) =>
    request<CartItem>({ method: 'post', url: '/api/cart/items', data: payload }),
  updateItem: (id: number, payload: CartUpdateRequest) =>
    request<CartItem>({ method: 'put', url: `/api/cart/items/${id}`, data: payload }),
  update: (id: number, payload: CartUpdateRequest) =>
    request<CartItem>({ method: 'put', url: `/api/cart/items/${id}`, data: payload }),
  selectAll: (selected: boolean) =>
    request<void>({ method: 'post', url: '/api/cart/select-all', data: { selected } }),
  removeItem: (id: number) => request<void>({ method: 'delete', url: `/api/cart/items/${id}` }),
  remove: (id: number) => request<void>({ method: 'delete', url: `/api/cart/items/${id}` }),
  clear: () => request<void>({ method: 'delete', url: '/api/cart' }),
}

export const orderApi = {
  create: (payload: OrderCreateRequest) =>
    request<OrderDetail>({ method: 'post', url: '/api/orders', data: payload }),
  page: (query: OrderPageQuery = {}) =>
    request<PageResponse<Order>>({
      method: 'get',
      url: '/api/orders',
      params: params({
        merchantId: query.merchantId,
        userId: query.userId,
        status: query.status,
        page: query.page ?? 1,
        size: query.size ?? 20,
      }),
    }),
  list: (query: OrderPageQuery = {}) =>
    request<PageResponse<Order>>({
      method: 'get',
      url: '/api/orders',
      params: params({
        merchantId: query.merchantId,
        userId: query.userId,
        status: query.status,
        page: query.page ?? 1,
        size: query.size ?? 20,
      }),
    }),
  get: (id: number) => request<OrderDetail>({ method: 'get', url: `/api/orders/${id}` }),
  detail: (id: number) => request<OrderDetail>({ method: 'get', url: `/api/orders/${id}` }),
  cancel: (id: number, reason?: string) =>
    request<OrderDetail>({ method: 'post', url: `/api/orders/${id}/cancel`, data: { reason } }),
  userCancel: (id: number, reason?: string) =>
    request<OrderDetail>({ method: 'post', url: `/api/orders/${id}/user-cancel`, data: { reason } }),
  merchantCancel: (id: number, reason?: string) =>
    request<OrderDetail>({ method: 'post', url: `/api/orders/${id}/merchant-cancel`, data: { reason } }),
  accept: (id: number) => request<OrderDetail>({ method: 'post', url: `/api/orders/${id}/accept` }),
  ready: (id: number) => request<OrderDetail>({ method: 'post', url: `/api/orders/${id}/ready` }),
  pickup: (id: number) => request<OrderDetail>({ method: 'post', url: `/api/orders/${id}/pickup` }),
  deliver: (id: number) => request<OrderDetail>({ method: 'post', url: `/api/orders/${id}/deliver` }),
  complete: (id: number) => request<OrderDetail>({ method: 'post', url: `/api/orders/${id}/complete` }),
}

export const paymentApi = {
  mock: (payload: PaymentMockRequest) =>
    request<Payment>({ method: 'post', url: '/api/payments/mock', data: payload }),
  mockPay: (orderId: number, payload?: string | { transactionNo?: string }) =>
    request<Payment>({
      method: 'post',
      url: `/api/payments/orders/${orderId}/mock-pay`,
      data: typeof payload === 'string' ? { transactionNo: payload } : (payload ?? {}),
    }),
  get: (paymentId: number) => request<Payment>({ method: 'get', url: `/api/payments/${paymentId}` }),
  latestForOrder: (orderId: number) =>
    request<Payment>({ method: 'get', url: `/api/payments/orders/${orderId}` }),
  byOrder: (orderId: number) =>
    request<Payment>({ method: 'get', url: `/api/payments/orders/${orderId}` }),
}

export const couponApi = {
  available: () => request<Coupon[]>({ method: 'get', url: '/api/coupons' }),
  create: (payload: CouponCreateRequest) =>
    request<Coupon>({ method: 'post', url: '/api/coupons', data: payload }),
  claim: (id: number) => request<CouponClaim>({ method: 'post', url: `/api/coupons/${id}/claim` }),
  my: (status?: CouponClaimStatus) =>
    request<CouponClaim[]>({ method: 'get', url: '/api/coupons/my', params: params({ status }) }),
}

export const deliveryApi = {
  rider: () => request<Rider>({ method: 'get', url: '/api/delivery/riders/me' }),
  me: () => request<Rider>({ method: 'get', url: '/api/delivery/riders/me' }),
  updateRider: (payload: RiderProfileRequest) =>
    request<Rider>({ method: 'put', url: '/api/delivery/riders/me', data: payload }),
  updateProfile: (payload: RiderProfileRequest) =>
    request<Rider>({ method: 'put', url: '/api/delivery/riders/me', data: payload }),
  updateStatus: (payload: { status: RiderStatus }) =>
    request<Rider>({ method: 'put', url: '/api/delivery/riders/me/status', data: payload }),
  setStatus: (payload: { status: RiderStatus }) =>
    request<Rider>({ method: 'put', url: '/api/delivery/riders/me/status', data: payload }),
  updateLocation: (payload: { longitude: number; latitude: number }) =>
    request<Rider>({ method: 'put', url: '/api/delivery/riders/me/location', data: payload }),
  availableOrders: (query: PageQuery = {}) =>
    request<PageResponse<Delivery>>({
      method: 'get',
      url: '/api/delivery/orders/available',
      params: params({ page: query.page ?? 1, size: query.size ?? 20 }),
    }),
  available: (query: PageQuery = {}) =>
    request<PageResponse<Delivery>>({
      method: 'get',
      url: '/api/delivery/orders/available',
      params: params({ page: query.page ?? 1, size: query.size ?? 20 }),
    }),
  accept: (orderId: number) =>
    request<Delivery>({ method: 'post', url: `/api/delivery/orders/${orderId}/accept` }),
  pickup: (orderId: number, deliveryNote?: string) =>
    request<Delivery>({
      method: 'post',
      url: `/api/delivery/orders/${orderId}/pickup`,
      data: { deliveryNote },
    }),
  deliver: (orderId: number, deliveryNote?: string) =>
    request<Delivery>({
      method: 'post',
      url: `/api/delivery/orders/${orderId}/deliver`,
      data: { deliveryNote },
    }),
  active: () => request<Delivery[]>({ method: 'get', url: '/api/delivery/orders/active' }),
  delivery: (orderId: number) =>
    request<Delivery>({ method: 'get', url: `/api/delivery/orders/${orderId}/delivery` }),
  records: (query: PageQuery = {}) =>
    request<PageResponse<Delivery>>({
      method: 'get',
      url: '/api/delivery/records',
      params: params({ page: query.page ?? 1, size: query.size ?? 20 }),
    }),
}

export const reviewApi = {
  create: (payload: ReviewCreateRequest) =>
    request<Review>({ method: 'post', url: '/api/reviews', data: payload }),
  merchantReviews: (merchantId: number, query: PageQuery = {}) =>
    request<PageResponse<Review>>({
      method: 'get',
      url: `/api/reviews/merchants/${merchantId}`,
      params: params({ page: query.page ?? 1, size: query.size ?? 20 }),
    }),
  byMerchant: (merchantId: number, query: PageQuery = {}) =>
    request<PageResponse<Review>>({
      method: 'get',
      url: `/api/reviews/merchants/${merchantId}`,
      params: params({ page: query.page ?? 1, size: query.size ?? 20 }),
    }),
  reply: (id: number, replyContent: string) =>
    request<Review>({ method: 'put', url: `/api/reviews/${id}/reply`, data: { replyContent } }),
}

export { ApiError, api, getToken, setToken } from './http'

export type {
  Address,
  AdminUserUpdateRequest,
  AuthSession,
  Cart,
  CartAddRequest,
  CartItem,
  CartUpdateRequest,
  Coupon,
  CouponClaim,
  CouponCreateRequest,
  Delivery,
  Dish,
  DishDetail,
  DishPageQuery,
  DishSaveRequest,
  DishSpec,
  DishSpecRequest,
  LoginRequest,
  Merchant,
  MerchantCategory,
  MerchantCategoryRequest,
  MerchantDetail,
  MerchantPageQuery,
  MerchantSaveRequest,
  Order,
  OrderCreateRequest,
  OrderDetail,
  OrderPageQuery,
  PageQuery,
  PageResponse,
  PasswordChangeRequest,
  Payment,
  PaymentMockRequest,
  RegisterRequest,
  Review,
  ReviewCreateRequest,
  Rider,
  RiderProfileRequest,
  User,
  UserProfileUpdateRequest,
} from '@/types/domain'
