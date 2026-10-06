export type Service = {
  id: string;
  name: string;
  minutes: number;
  price: string;
  detail: string;
};

export type WeekWindow = {
  dow: number;
  start: string;
  end: string;
};

export type Security = {
  requireLink: boolean;
  linkLabel: string;
  requireId: boolean;
  questions: string[];
};

export type Provider = {
  id: string;
  phone: string;
  name: string;
  slug: string;
  photo: string;
  line: string;
  timezone: string;
  address: string;
  placeLabel: string;
  services: Service[];
  hours: WeekWindow[];
  bufferMinutes: number;
  horizonDays: number;
  minNoticeMinutes: number;
  busyIcsUrl: string;
  security: Security;
  confirmLeadMinutes: number;
  autoCancelIfUnconfirmed: boolean;
  requestExpiresHours: number;
  calToken: string;
  createdAt: number;
};

export type Invite = {
  id: string;
  providerId: string;
  token: string;
  expiresAt: number;
  bookingId: string | null;
  createdAt: number;
};

export type BookingStatus = "pending" | "changes" | "confirmed" | "declined" | "canceled" | "expired";

export type ShowConfirm = "none" | "asked" | "yes" | "no";

export type Booking = {
  id: string;
  publicId: string;
  code: string;
  providerId: string;
  inviteId: string | null;
  customerName: string;
  customerPhone: string;
  serviceId: string;
  serviceName: string;
  minutes: number;
  price: string;
  options: number[];
  chosenStart: number | null;
  link: string;
  idImage: string;
  answers: { q: string; a: string }[];
  status: BookingStatus;
  changeNote: string;
  showConfirm: ShowConfirm;
  confirmAskedAt: number | null;
  directionsToken: string | null;
  directionsUsedAt: number | null;
  codeFails: number;
  codeLockedUntil: number;
  createdAt: number;
  expiresAt: number;
  sessionExpiresAt: number;
};

export type TextMessage = {
  id: string;
  providerId: string;
  bookingId: string | null;
  to: string;
  from: string;
  body: string;
  mediaUrl: string;
  direction: "out" | "in";
  delivered: boolean;
  createdAt: number;
};

export type Otp = {
  phone: string;
  code: string;
  expiresAt: number;
  attempts: number;
};

export type DB = {
  v: 1;
  providers: Provider[];
  invites: Invite[];
  bookings: Booking[];
  messages: TextMessage[];
  otps: Otp[];
};

export type Outbound = {
  to: string;
  body: string;
  mediaUrl?: string;
  providerId: string;
  bookingId: string | null;
};
