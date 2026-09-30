/** 与后端 DTO、数据库实体和 REST 枚举保持一致的前端领域契约。 */

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: string
}

export interface PageResponse<T> {
  records: T[]
  total: number
  page: number
  size: number
  pages: number
}

export type UserRole = 'CUSTOMER' | 'ADMIN' | 'MERCHANT' | 'RIDER'
export type UserStatus = 'ACTIVE' | 'LOCKED' | 'DISABLED'
export type CartItemStatus = 'ACTIVE' | 'CHECKED_OUT' | 'REMOVED'
export type MerchantBusinessStatus = 'PREPARING' | 'OPEN' | 'PAUSED' | 'CLOSED'
export type DishStatus = 'ON_SALE' | 'OFF_SALE' | 'SOLD_OUT' | 'DELETED'
export type OrderStatus =
  | 'PENDING_PAYMENT'
  | 'PAID'
  | 'ACCEPTED'
  | 'PREPARING'
  | 'READY'
  | 'PICKED_UP'
  | 'DELIVERED'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'REFUNDED'
export type PaymentMethod = 'MOCK_BALANCE' | 'ALIPAY' | 'WECHAT'
export type PaymentStatus = 'PENDING' | 'SUCCESS' | 'FAILED' | 'CLOSED' | 'REFUNDED'
export type CouponType = 'FIXED' | 'PERCENT'
export type CouponStatus = 'UPCOMING' | 'ACTIVE' | 'PAUSED' | 'EXPIRED' | 'EXHAUSTED'
export type CouponClaimStatus = 'UNUSED' | 'USED' | 'EXPIRED' | 'CANCELLED'
export type DeliveryStatus = 'WAITING_RIDER' | 'ASSIGNED' | 'PICKED_UP' | 'DELIVERED' | 'CANCELLED'
export type RiderStatus = 'OFFLINE' | 'ONLINE' | 'BUSY' | 'SUSPENDED'
export type ReviewStatus = 'VISIBLE' | 'HIDDEN' | 'DELETED'

export interface User {
  id: number
  username: string
  phone: string | null
  nickname: string | null
  role: UserRole
  status: UserStatus
  createdAt: string
  updatedAt: string
}

export interface Address {
  id: number
  userId: number
  contactName: string
  phone: string
  province: string
  city: string
  district: string | null
  detail: string
  longitude: number | null
  latitude: number | null
  label?: string | null
  isDefault: boolean
  createdAt: string
  updatedAt: string
}

export interface MerchantCategory {
  id: number
  name: string
  iconUrl: string | null
  sortOrder: number
  enabled: boolean
}

export interface Merchant {
  id: number
  ownerId: number
  name: string
  description: string | null
  logoUrl: string | null
  contactName: string | null
  contactPhone: string
  province: string | null
  city: string | null
  district: string | null
  address: string
  longitude: number | null
  latitude: number | null
  businessStatus: MerchantBusinessStatus
  businessHours: string | null
  minOrderAmount: number
  deliveryFee: number
  packagingFee: number
  rating: number
  monthlySales: number
  createdAt: string
  updatedAt: string
}

export interface MerchantDetail {
  merchant: Merchant
  categoryIds: number[]
}

export interface Dish {
  id: number
  merchantId: number
  categoryId: number
  name: string
  description: string | null
  imageUrl: string | null
  price: number
  originalPrice: number | null
  status: DishStatus
  stock: number
  sales: number
  sortOrder: number
  createdAt: string
  updatedAt: string
}

export interface DishSpec {
  id: number
  dishId: number
  groupName: string | null
  name: string
  priceOffset: number
  sortOrder: number
  isDefault: boolean
}

export interface DishDetail {
  dish: Dish
  specs: DishSpec[]
}

export interface CartItem {
  id: number
  merchantId: number
  dishId: number
  dishSpecId: number | null
  dishName: string
  specName: string | null
  imageUrl: string | null
  quantity: number
  unitPrice: number
  subtotal: number
  selected: boolean
  note: string | null
  updatedAt: string
}

export interface Cart {
  items: CartItem[]
  totalAmount: number
  itemCount: number
}

export interface Coupon {
  id: number
  name: string
  code: string
  couponType: CouponType
  status: CouponStatus
  thresholdAmount: number
  discountAmount: number | null
  discountRate: number | null
  totalQuantity: number
  claimedQuantity: number
  remainingQuantity: number
  perUserLimit: number
  startTime: string
  endTime: string
}

export interface CouponClaim {
  id: number
  couponId: number
  orderId: number | null
  status: CouponClaimStatus
  couponName: string
  couponType: CouponType
  thresholdAmount: number
  discountAmount: number | null
  discountRate: number | null
  claimedAt: string
  usedAt: string | null
  expiresAt: string
}

export interface OrderItem {
  id: number
  dishId: number
  dishSpecId: number | null
  dishName: string
  specName: string | null
  note: string | null
  unitPrice: number
  quantity: number
  subtotal: number
}

export interface Order {
  id: number
  orderNo: string
  userId: number
  merchantId: number
  merchantName: string
  addressId: number
  couponClaimId: number | null
  status: OrderStatus
  totalAmount: number
  deliveryFee: number
  packagingFee: number
  discountAmount: number
  payableAmount: number
  userNote: string | null
  merchantNote: string | null
  cancelReason: string | null
  contactPhone: string | null
  deliveryAddressSnapshot: string
  createdAt: string
  acceptedAt: string | null
  readyAt: string | null
  pickedAt: string | null
  deliveredAt: string | null
  cancelledAt: string | null
  updatedAt: string
}

export interface OrderDetail {
  order: Order
  items: OrderItem[]
}

export interface Payment {
  id: number
  paymentNo: string
  orderId: number
  userId: number
  amount: number
  paymentMethod: PaymentMethod
  status: PaymentStatus
  transactionNo: string | null
  failureReason: string | null
  refundedAmount: number
  paidAt: string | null
  createdAt: string
  updatedAt: string
}

export interface Rider {
  id: number
  userId: number
  name: string
  phone: string
  vehicleType: string
  status: RiderStatus
  currentLongitude: number | null
  currentLatitude: number | null
  rating: number
  completedCount: number
  activeOrderCount: number
}

export interface Delivery {
  id: number
  orderId: number
  riderId: number | null
  status: DeliveryStatus
  pickupCode: string | null
  deliveryNote: string | null
  distanceKm: number | null
  acceptedAt: string | null
  pickedUpAt: string | null
  deliveredAt: string | null
  createdAt: string
  updatedAt: string
}

export interface Review {
  id: number
  orderId: number
  userId: number
  merchantId: number
  rating: number
  content: string | null
  imageUrl: string | null
  replyContent: string | null
  repliedAt: string | null
  createdAt: string
}

export interface AuthSession {
  token: string
  tokenType: string
  expiresIn: number
  user: User
}

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  phone?: string
  nickname?: string
}

export interface UserProfileUpdateRequest {
  phone?: string
  nickname?: string
}

export interface AdminUserUpdateRequest extends UserProfileUpdateRequest {
  role?: UserRole
  status?: UserStatus
}

export interface PasswordChangeRequest {
  currentPassword: string
  newPassword: string
}

export interface AddressRequest {
  contactName: string
  phone: string
  province: string
  city: string
  district?: string
  detail: string
  longitude?: number | null
  latitude?: number | null
  label?: string | null
  isDefault?: boolean
}

export interface MerchantSaveRequest {
  name: string
  description?: string
  logoUrl?: string
  contactName?: string
  contactPhone: string
  province?: string
  city?: string
  district?: string
  address: string
  longitude?: number | null
  latitude?: number | null
  businessHours?: string
  minOrderAmount?: number
  deliveryFee?: number
  packagingFee?: number
  categoryIds?: number[]
}

export interface MerchantCategoryRequest {
  name: string
  iconUrl?: string
  sortOrder?: number
  enabled?: boolean
}

export interface DishSaveRequest {
  categoryId: number
  name: string
  description?: string
  imageUrl?: string
  price: number
  originalPrice?: number
  stock: number
  sortOrder?: number
}

export interface DishSpecRequest {
  groupName?: string
  name: string
  priceOffset?: number
  sortOrder?: number
  isDefault?: boolean
}

export interface CartAddRequest {
  dishId: number
  dishSpecId?: number | null
  quantity: number
  note?: string
}

export interface CartUpdateRequest {
  quantity?: number
  selected?: boolean
  note?: string
}

export interface OrderCreateRequest {
  cartItemIds?: number[]
  addressId: number
  couponClaimId?: number | null
  contactPhone?: string
  userNote?: string
}

export interface CouponCreateRequest {
  name: string
  code: string
  couponType: CouponType
  thresholdAmount: number
  discountAmount?: number
  discountRate?: number
  totalQuantity: number
  perUserLimit?: number
  startTime: string
  endTime: string
}

export interface PaymentMockRequest {
  orderId: number
  paymentMethod: PaymentMethod
  transactionNo?: string
}

export interface ReviewCreateRequest {
  orderId: number
  rating: number
  content?: string
  imageUrl?: string
}

export interface RiderProfileRequest {
  name: string
  phone: string
  vehicleType: string
}

export interface PageQuery {
  page?: number
  size?: number
}

export interface MerchantPageQuery extends PageQuery {
  keyword?: string
  categoryId?: number
  openOnly?: boolean
}

export interface DishPageQuery extends PageQuery {
  merchantId: number
  categoryId?: number
  keyword?: string
  onSaleOnly?: boolean
}

export interface OrderPageQuery extends PageQuery {
  merchantId?: number
  userId?: number
  status?: OrderStatus
}
