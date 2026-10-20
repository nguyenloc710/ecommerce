import type { Metadata } from "next";
import { Be_Vietnam_Pro, Cormorant_Garamond } from "next/font/google";
import "./globals.css";
import { SiteHeader } from "@/components/site/site-header";
import { SiteFooter } from "@/components/site/site-footer";

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

const shopName = process.env.NEXT_PUBLIC_SHOP_NAME ?? "Tên shop";

export const metadata: Metadata = {
  // URL public của site để resolve ảnh Open Graph tuyệt đối
  metadataBase: new URL(process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000"),
  title: {
    default: `${shopName}: Thời trang, phụ kiện, trang sức`,
    template: `%s | ${shopName}`,
  },
  description: "Thời trang linen, lụa và trang sức bạc 925 chế tác thủ công. Giao hàng toàn quốc 2–4 ngày.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="vi" className={`${beVietnamPro.variable} ${cormorant.variable} h-full antialiased`}>
      <body className="min-h-full flex flex-col">
        <SiteHeader />
        <div className="flex flex-1 flex-col">{children}</div>
        <SiteFooter />
      </body>
    </html>
  );
}
