import Link from "next/link";
import { SiteHeader } from "@/components/site/site-header";
import { SiteFooter } from "@/components/site/site-footer";
import { ProductCard } from "@/components/product/product-card";
import { PlaceholderImage } from "@/components/ui/placeholder-image";
import { getCategoryTree, getNewArrivals, iconFor, leafCategories, toneFor } from "@/lib/catalog";

export default async function HomePage() {
  const [newArrivals, tree] = await Promise.all([getNewArrivals(4), getCategoryTree()]);
  const categories = leafCategories(tree, 6);
  return (
    <>
      <SiteHeader cartCount={0} />
      <main className="flex flex-col">
        {/* Hero tràn màn hình, chữ đè góc dưới trái */}
        <section className="relative flex h-[520px] items-end bg-[#2b2a28] text-paper lg:h-[820px]">
          <svg
            aria-hidden="true"
            viewBox="0 0 100 100"
            fill="none"
            stroke="#fff"
            strokeWidth="0.8"
            className="absolute right-6 top-28 h-[150px] w-[150px] lg:right-[12%] lg:top-1/2 lg:h-[260px] lg:w-[260px] lg:-translate-y-1/2"
          >
            <circle cx="50" cy="58" r="26" />
            <circle cx="50" cy="58" r="20" />
            <path d="M40 32 50 22l10 10-10 8z" />
          </svg>
          <div className="pad relative flex max-w-[720px] flex-col gap-3 pb-7 lg:gap-5 lg:pb-18">
            <span className="eyebrow">Bộ sưu tập Thu Đông 2026</span>
            <h1 className="font-display text-[44px] font-medium leading-[0.98] lg:text-[84px]">
              Lặng lẽ,
              <br />
              <em className="font-normal">và sang trọng.</em>
            </h1>
            <div className="flex gap-5 pt-2 lg:gap-8">
              <Link href="/danh-muc/thoi-trang" className="eyebrow ul">
                Khám phá thời trang
              </Link>
              <Link href="/danh-muc/trang-suc" className="eyebrow ul">
                Khám phá trang sức
              </Link>
            </div>
          </div>
        </section>

        {/* Hai ô chiến dịch */}
        <section className="pad grid grid-cols-2 gap-0.5 pt-0.5 lg:gap-4 lg:pt-4">
          <Link href="/danh-muc/thoi-trang" className="block">
            <PlaceholderImage tone="sand" icon="shirt" iconSize={140} className="aspect-[4/5] !items-end !justify-start">
              <div className="relative flex flex-col gap-2.5 p-3.5 lg:p-9">
                <span className="eyebrow">Thời trang</span>
                <span className="hidden font-display text-[40px] leading-[1.05] lg:block">Linen &amp; lụa cho ngày se lạnh</span>
                <span className="eyebrow ul mt-1.5 self-start text-[9px] lg:text-[11px]">Xem bộ sưu tập</span>
              </div>
            </PlaceholderImage>
          </Link>
          <Link href="/danh-muc/trang-suc" className="block">
            <PlaceholderImage tone="stone" icon="necklace" iconSize={140} className="aspect-[4/5] !items-end !justify-start bg-[#e9e4dc]">
              <div className="relative flex flex-col gap-2.5 p-3.5 lg:p-9">
                <span className="eyebrow">Trang sức</span>
                <span className="hidden font-display text-[40px] leading-[1.05] lg:block">Bạc 925 chế tác thủ công</span>
                <span className="eyebrow ul mt-1.5 self-start text-[9px] lg:text-[11px]">Xem bộ sưu tập</span>
              </div>
            </PlaceholderImage>
          </Link>
        </section>

        {/* Mới về */}
        <section className="pad pt-8 lg:pt-24">
          <div className="mb-3 flex items-baseline justify-between gap-4 lg:mb-7">
            <h2 className="font-display text-[28px] font-medium lg:text-[44px]">Mới về</h2>
            <Link href="/moi-ve" className="eyebrow ul">
              Xem tất cả
            </Link>
          </div>
          <div className="grid grid-cols-2 gap-3 lg:grid-cols-4 lg:gap-6">
            {newArrivals.map((p) => (
              <ProductCard key={p.slug} product={p} />
            ))}
          </div>
        </section>

        {/* Câu chuyện chế tác */}
        <section className="pad grid grid-cols-1 items-center gap-8 pt-16 lg:grid-cols-2 lg:gap-16 lg:pt-24">
          <PlaceholderImage tone="ink" icon="necklace" iconSize={200} className="aspect-[4/5]">
            <span className="absolute bottom-3 left-3.5 text-[10px] uppercase tracking-[0.14em] text-white/50">Ảnh xưởng chế tác</span>
          </PlaceholderImage>
          <div className="flex max-w-[480px] flex-col gap-5 lg:gap-6">
            <span className="eyebrow text-muted">Câu chuyện</span>
            <h2 className="font-display text-[34px] font-medium leading-[1.02] lg:text-[52px]">Mỗi chiếc nhẫn đi qua 14 bàn tay thợ</h2>
            <p className="m-0 text-[15px] leading-[1.7] text-[#3a3a3a]">
              Bạc 925 được đúc, giũa và đánh bóng thủ công tại xưởng. Đá chủ đi kèm giấy kiểm định, khắc tên miễn phí, bảo
              hành đánh bóng trọn đời. Giá vàng 18K cập nhật theo ngày và hiển thị rõ cách tính.
            </p>
            <Link href="/che-tac" className="eyebrow ul self-start">
              Về quy trình chế tác
            </Link>
          </div>
        </section>

        {/* Mua theo danh mục */}
        <section className="pad pt-16 lg:pt-24">
          <h2 className="mb-4 font-display text-[28px] font-medium lg:mb-7 lg:text-[44px]">Mua theo danh mục</h2>
          <div className="grid grid-cols-3 gap-3 lg:grid-cols-6 lg:gap-4">
            {categories.map((c) => (
              <Link key={c.slug} href={`/danh-muc/${c.slug}`} className="flex flex-col gap-2">
                <PlaceholderImage tone={toneFor(c.slug)} icon={iconFor(c.slug)} iconSize={56} className="aspect-square" />
                <span className="eyebrow text-[11px]">{c.name}</span>
              </Link>
            ))}
          </div>
        </section>

        {/* Dải ưu đãi */}
        <section className="mt-16 bg-bone lg:mt-24">
          <div className="pad flex flex-wrap items-center justify-between gap-6 py-12 lg:gap-8 lg:py-18">
            <div className="flex max-w-[640px] flex-col gap-3">
              <span className="eyebrow text-sale">Ưu đãi cuối mùa</span>
              <h2 className="font-display text-[36px] font-medium leading-none lg:text-[56px]">Giảm đến 30% cho áo quần hè</h2>
              <p className="m-0 text-[14px] text-muted">Áp dụng đến hết 31/10. Không áp dụng cùng voucher khác.</p>
            </div>
            <Link href="/sale" className="btn">
              Mua ngay
            </Link>
          </div>
        </section>

        {/* Cam kết */}
        <section className="pad grid grid-cols-2 gap-6 border-b border-line py-10 lg:grid-cols-4 lg:py-14">
          {[
            ["Giao 2–4 ngày", "GHN, GHTK toàn quốc"],
            ["Đổi trả 7 ngày", "Miễn phí nếu lỗi từ shop"],
            ["Kiểm định đi kèm", "Với mọi sản phẩm có đá"],
            ["COD · VNPay · MoMo", "Kiểm hàng trước khi trả tiền"],
          ].map(([title, desc]) => (
            <div key={title} className="flex flex-col gap-1.5">
              <span className="eyebrow">{title}</span>
              <span className="text-[13px] text-muted">{desc}</span>
            </div>
          ))}
        </section>
      </main>
      <SiteFooter />
    </>
  );
}
