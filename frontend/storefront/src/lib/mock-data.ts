// Dữ liệu giả cho trang chủ tĩnh (E0.5). Thay bằng API catalog ở E5.2.
import type { ProductCardData } from "@/components/product/product-card";

export const newArrivals: ProductCardData[] = [
  {
    slug: "ao-so-mi-linen-oversize",
    name: "Áo sơ mi linen oversize",
    price: 590000,
    label: "Mới",
    tone: "stone",
    icon: "shirt",
    colors: ["#f3efe8", "#cfc6ba", "#7d7466", "#111111"],
  },
  {
    slug: "vay-midi-lua-co-vuong",
    name: "Váy midi lụa cổ vuông",
    price: 1290000,
    tone: "sand",
    icon: "dress",
    colors: ["#2f3a2f", "#d8c3b8"],
  },
  {
    slug: "nhan-bac-dinh-da-moissanite",
    name: "Nhẫn bạc đính đá moissanite",
    price: 890000,
    label: "Bạc 925",
    tone: "bone",
    icon: "ring",
    colors: ["#d7d7d7", "#d9b65c", "#d9a08f"],
  },
  {
    slug: "tui-deo-cheo-da-bo",
    name: "Túi đeo chéo da bò",
    price: 1650000,
    tone: "taupe",
    icon: "bag",
    colors: ["#6b4a2b", "#111111", "#c9b9a4"],
  },
];

export const categories = [
  { slug: "ao", name: "Áo", icon: "shirt", tone: "stone" },
  { slug: "vay-quan", name: "Váy & Quần", icon: "dress", tone: "sand" },
  { slug: "tui", name: "Túi", icon: "bag", tone: "taupe" },
  { slug: "nhan", name: "Nhẫn", icon: "ring", tone: "bone" },
  { slug: "day-chuyen", name: "Dây chuyền", icon: "necklace", tone: "stone" },
  { slug: "khuyen-tai", name: "Khuyên tai", icon: "earring", tone: "sand" },
] as const;
