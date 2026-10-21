import type { Metadata } from "next";
import { Be_Vietnam_Pro, Cormorant_Garamond } from "next/font/google";
import "./globals.css";
import { SHOP_NAME as shopName, SITE_URL } from "@/lib/site";

// Font theo UI-03: tiêu đề Cormorant Garamond, nội dung Be Vietnam Pro, đều có subset tiếng Việt
const beVietnamPro = Be_Vietnam_Pro({
  variable: "--font-be-vietnam-pro",
  subsets: ["latin", "vietnamese"],
  weight: ["300", "400", "500"],
});

const cormorant = Cormorant_Garamond({
  variable: "--font-cormorant",
  subsets: ["latin", "vietnamese"],
  weight: ["400", "500", "600"],
  style: ["normal", "italic"],
});


export const metadata: Metadata = {
  // URL public của site để resolve ảnh Open Graph tuyệt đối
  metadataBase: new URL(SITE_URL),
  title: {
    default: `${shopName}: Thời trang, phụ kiện, trang sức`,
    template: `%s | ${shopName}`,
  },
  description: "Thời trang linen, lụa và trang sức bạc 925 chế tác thủ công. Giao hàng toàn quốc 2–4 ngày.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="vi" className={`${beVietnamPro.variable} ${cormorant.variable} h-full antialiased`}>
      {/* Header/footer cửa hàng nằm ở (shop)/layout.tsx; /admin có khung riêng (UI-12) */}
      <body className="min-h-full flex flex-col">{children}</body>
    </html>
  );
}
