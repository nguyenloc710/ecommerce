import Link from "next/link";
import { SiteFooter } from "@/components/site/site-footer";
import { SiteHeader } from "@/components/site/site-header";

/** Trang 404 dùng ở cấp gốc (ngoài layout (shop)) nên tự gắn header/footer. */
export default function NotFound() {
  return (
    <>
      <SiteHeader />
      <main className="pad flex flex-1 flex-col items-center justify-center gap-5 py-32 text-center">
        <span className="eyebrow text-muted">404</span>
        <h1 className="font-display text-[40px] font-medium leading-none lg:text-[56px]">Không tìm thấy trang</h1>
        <p className="m-0 max-w-[420px] text-[14px] text-muted">Sản phẩm có thể đã ngừng bán hoặc đường dẫn không đúng.</p>
        <div className="flex gap-6 pt-2">
          <Link href="/" className="eyebrow ul">
            Về trang chủ
          </Link>
          <Link href="/moi-ve" className="eyebrow ul">
            Xem hàng mới
          </Link>
        </div>
      </main>
      <SiteFooter />
    </>
  );
}
