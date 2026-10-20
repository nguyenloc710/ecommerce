import Link from "next/link";

const shopName = process.env.NEXT_PUBLIC_SHOP_NAME ?? "Tên shop";

export function SiteFooter() {
  return (
    <footer className="pad mt-24 flex flex-col gap-14 pb-10 pt-18">
      <div className="grid grid-cols-1 gap-8 md:grid-cols-2 lg:grid-cols-[1.4fr_1fr_1fr_1.4fr]">
        <div className="flex flex-col gap-3.5">
          <span className="font-display text-[28px] font-semibold uppercase tracking-[0.22em]">{shopName}</span>
          <p className="m-0 text-[13px] leading-[1.7] text-muted">
            [Địa chỉ cửa hàng]
            <br />
            [Hotline] · [Email]
            <br />
            8:00 – 22:00 hằng ngày
          </p>
        </div>
        <div className="flex flex-col gap-2.5 text-[13px]">
          <span className="eyebrow">Mua sắm</span>
          <Link href="/danh-muc/thoi-trang">Thời trang</Link>
          <Link href="/danh-muc/phu-kien">Phụ kiện</Link>
          <Link href="/danh-muc/trang-suc">Trang sức</Link>
          <Link href="/sale">Sale</Link>
        </div>
        <div className="flex flex-col gap-2.5 text-[13px]">
          <span className="eyebrow">Hỗ trợ</span>
          <Link href="/huong-dan-chon-size">Hướng dẫn chọn size</Link>
          <Link href="/doi-tra-bao-hanh">Đổi trả &amp; bảo hành</Link>
          <Link href="/tra-cuu-don-hang">Tra cứu đơn hàng</Link>
          <Link href="/lien-he">Liên hệ</Link>
        </div>
        <form className="flex flex-col gap-3.5" action="/dang-ky-nhan-tin" method="post">
          <span className="eyebrow">Nhận tin ưu đãi</span>
          <label htmlFor="newsletter-email" className="text-[13px] text-muted">
            Email của bạn
          </label>
          <div className="flex border-b border-ink">
            <input
              id="newsletter-email"
              name="email"
              type="email"
              placeholder="ban@email.com"
              className="h-11 min-w-0 flex-1 bg-transparent text-[14px] outline-none"
            />
            <button type="submit" className="eyebrow px-1">
              Đăng ký
            </button>
          </div>
        </form>
      </div>
      <div className="flex flex-wrap justify-between gap-2 text-[11px] uppercase tracking-[0.06em] text-muted">
        <span>© 2026 {shopName} · GPKD số [số GPKD]</span>
        <span>Điều khoản · Bảo mật · Cookie</span>
      </div>
    </footer>
  );
}
