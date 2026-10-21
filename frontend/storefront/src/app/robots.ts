import type { MetadataRoute } from "next";
import { SITE_URL } from "@/lib/site";

/** /robots.txt: chặn trang cá nhân, thanh toán, quản trị và API; trỏ tới sitemap. */
export default function robots(): MetadataRoute.Robots {
  return {
    rules: {
      userAgent: "*",
      allow: "/",
      disallow: ["/admin", "/tai-khoan", "/thanh-toan", "/gio-hang", "/dang-nhap", "/api/", "/tim-kiem"],
    },
    sitemap: `${SITE_URL}/sitemap.xml`,
  };
}
