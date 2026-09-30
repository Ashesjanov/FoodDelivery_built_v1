import type {
  Address,
  AuthSession,
  CartItem,
  Coupon,
  CouponClaim,
  Delivery,
  Dish,
  DishSpec,
  Merchant,
  MerchantCategory,
  Order,
  OrderItem,
  Payment,
  Review,
  Rider,
  User,
} from '@/types/domain'

const createdAt = '2026-09-23T08:00:00Z'
const updatedAt = '2026-09-23T09:30:00Z'

const demoUsers: User[] = [
  {
    id: 1,
    username: 'demo_customer',
    phone: '13800138000',
    nickname: '林晓',
    role: 'CUSTOMER',
    status: 'ACTIVE',
    createdAt,
    updatedAt,
  },
  {
    id: 2,
    username: 'demo_merchant',
    phone: '13800138001',
    nickname: '禾下食堂',
    role: 'MERCHANT',
    status: 'ACTIVE',
    createdAt,
    updatedAt,
  },
  {
    id: 3,
    username: 'demo_rider',
    phone: '13800138002',
    nickname: '陈师傅',
    role: 'RIDER',
    status: 'ACTIVE',
    createdAt,
    updatedAt,
  },
]

const demoAddresses: Address[] = [
  {
    id: 1,
    userId: 1,
    contactName: '林晓',
    phone: '13800138000',
    province: '上海市',
    city: '上海市',
    district: '徐汇区',
    detail: '虹桥路 888 号 3 栋 502',
    longitude: 121.436,
    latitude: 31.192,
    isDefault: true,
    createdAt,
    updatedAt,
  },
]

const demoCategories: MerchantCategory[] = [
  { id: 1, name: '中式快餐', iconUrl: null, sortOrder: 1, enabled: true },
  { id: 2, name: '轻食', iconUrl: null, sortOrder: 2, enabled: true },
]

const demoMerchants: Merchant[] = [
  {
    id: 10,
    ownerId: 2,
    name: '禾下食堂',
    description: '现炒家常菜与营养套餐',
    logoUrl: '/demo/merchant.svg',
    contactName: '周禾',
    contactPhone: '13800138001',
    province: '上海市',
    city: '上海市',
    district: '徐汇区',
    address: '宜山路 700 号',
    longitude: 121.427,
    latitude: 31.181,
    businessStatus: 'OPEN',
    businessHours: '10:00-22:00',
    minOrderAmount: 20,
    deliveryFee: 4,
    packagingFee: 2,
    rating: 4.8,
    monthlySales: 862,
    createdAt,
    updatedAt,
  },
]

const demoDishes: Dish[] = [
  {
    id: 100,
    merchantId: 10,
    categoryId: 1,
    name: '黑椒牛肉饭',
    description: '嫩煎牛肉配时蔬与溏心蛋',
    imageUrl: '/demo/beef-rice.svg',
    price: 32,
    originalPrice: 38,
    status: 'ON_SALE',
    stock: 40,
    sales: 218,
    sortOrder: 1,
    createdAt,
    updatedAt,
  },
  {
    id: 101,
    merchantId: 10,
    categoryId: 2,
    name: '牛油果鸡胸沙拉',
    description: '低脂鸡胸、牛油果与油醋汁',
    imageUrl: '/demo/salad.svg',
    price: 28,
    originalPrice: 32,
    status: 'ON_SALE',
    stock: 24,
    sales: 146,
    sortOrder: 2,
    createdAt,
    updatedAt,
  },
]

const demoDishSpecs: DishSpec[] = [
  { id: 1000, dishId: 100, groupName: '份量', name: '标准份', priceOffset: 0, sortOrder: 1, isDefault: true },
  { id: 1001, dishId: 100, groupName: '份量', name: '大份', priceOffset: 5, sortOrder: 2, isDefault: false },
]

const demoCartItems: CartItem[] = [
  {
    id: 200,
    merchantId: 10,
    dishId: 100,
    dishSpecId: 1000,
    dishName: '黑椒牛肉饭',
    specName: '标准份',
    imageUrl: '/demo/beef-rice.svg',
    quantity: 1,
    unitPrice: 32,
    subtotal: 32,
    selected: true,
    note: '少辣',
    updatedAt,
  },
]

const demoCoupons: Coupon[] = [
  {
    id: 300,
    name: '秋日满减券',
    code: 'AUTUMN8',
    couponType: 'FIXED',
    status: 'ACTIVE',
    thresholdAmount: 50,
    discountAmount: 8,
    discountRate: null,
    totalQuantity: 200,
    claimedQuantity: 86,
    remainingQuantity: 114,
    perUserLimit: 1,
    startTime: '2026-09-01T00:00:00Z',
    endTime: '2026-10-31T23:59:59Z',
  },
]

const demoCouponClaims: CouponClaim[] = [
  {
    id: 301,
    couponId: 300,
    orderId: 500,
    status: 'UNUSED',
    couponName: '秋日满减券',
    couponType: 'FIXED',
    thresholdAmount: 50,
    discountAmount: 8,
    discountRate: null,
    claimedAt: createdAt,
    usedAt: null,
    expiresAt: '2026-10-31T23:59:59Z',
  },
]

const demoOrders: Order[] = [
  {
    id: 500,
    orderNo: 'FD202609230001',
    userId: 1,
    merchantId: 10,
    merchantName: '禾下食堂',
    addressId: 1,
    couponClaimId: 301,
    status: 'PAID',
    totalAmount: 32,
    deliveryFee: 4,
    packagingFee: 2,
    discountAmount: 8,
    payableAmount: 30,
    userNote: '少辣',
    merchantNote: null,
    cancelReason: null,
    contactPhone: '13800138000',
    deliveryAddressSnapshot: '上海市徐汇区虹桥路 888 号 3 栋 502',
    createdAt,
    acceptedAt: null,
    readyAt: null,
    pickedAt: null,
    deliveredAt: null,
    cancelledAt: null,
    updatedAt,
  },
]

const demoOrderItems: OrderItem[] = [
  {
    id: 501,
    dishId: 100,
    dishSpecId: 1000,
    dishName: '黑椒牛肉饭',
    specName: '标准份',
    note: '少辣',
    unitPrice: 32,
    quantity: 1,
    subtotal: 32,
  },
]

const demoPayments: Payment[] = [
  {
    id: 600,
    paymentNo: 'PAY202609230001',
    orderId: 500,
    userId: 1,
    amount: 30,
    paymentMethod: 'MOCK_BALANCE',
    status: 'SUCCESS',
    transactionNo: 'DEMO-PAY-0001',
    failureReason: null,
    refundedAmount: 0,
    paidAt: createdAt,
    createdAt,
    updatedAt,
  },
]

const demoRiders: Rider[] = [
  {
    id: 700,
    userId: 3,
    name: '陈师傅',
    phone: '13800138002',
    vehicleType: '电动车',
    status: 'ONLINE',
    currentLongitude: 121.43,
    currentLatitude: 31.19,
    rating: 4.9,
    completedCount: 428,
    activeOrderCount: 1,
  },
]

const demoDeliveries: Delivery[] = [
  {
    id: 800,
    orderId: 500,
    riderId: 700,
    status: 'ASSIGNED',
    pickupCode: '4821',
    deliveryNote: null,
    distanceKm: 2.4,
    acceptedAt: createdAt,
    pickedUpAt: null,
    deliveredAt: null,
    createdAt,
    updatedAt,
  },
]

const demoReviews: Review[] = [
  {
    id: 900,
    orderId: 500,
    userId: 1,
    merchantId: 10,
    rating: 5,
    content: '牛肉很嫩，配送也及时。',
    imageUrl: null,
    replyContent: '谢谢您的支持。',
    repliedAt: updatedAt,
    createdAt,
  },
]

export const demoFixtures = {
  users: demoUsers,
  addresses: demoAddresses,
  categories: demoCategories,
  merchants: demoMerchants,
  dishes: demoDishes,
  dishSpecs: demoDishSpecs,
  cartItems: demoCartItems,
  coupons: demoCoupons,
  couponClaims: demoCouponClaims,
  orders: demoOrders,
  orderItems: demoOrderItems,
  payments: demoPayments,
  riders: demoRiders,
  deliveries: demoDeliveries,
  reviews: demoReviews,
}

export const demoSession: AuthSession = {
  token: 'demo-token',
  tokenType: 'Bearer',
  expiresIn: 7_200,
  user: demoUsers[0],
}

export function resetDemoStorage(): void {
  if (typeof window === 'undefined') return
  for (const key of ['food-delivery-token', 'food-delivery-demo', 'food-delivery-cart', 'food-delivery-session']) {
    window.localStorage.removeItem(key)
  }
}
