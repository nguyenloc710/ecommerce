import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";
import { AdminHeader, textParam } from "@/components/admin/admin-ui";
import { RevenueChart } from "@/components/admin/revenue-chart";
import { getRevenueReport } from "@/lib/admin-data";
import { PAYMENT_METHOD_LABELS, periodLabel } from "@/lib/admin-shared";
import { formatVnd } from "@/lib/format";
import { isAdmin, requireStaff } from "@/lib/session";

export const metadata: Metadata = { title: "Báo cáo doanh thu" };

const DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

/** Ngày hôm nay theo giờ Việt Nam dạng yyyy-MM-dd (báo cáo tính theo ngày VN). */
function todayVn() {
  return new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Ho_Chi_Minh" }).format(new Date());
}

function shift(date: string, days: number) {
  const d = new Date(`${date}T00:00:00Z`);
  d.setUTCDate(d.getUTCDate() + days);
  return d.toISOString().slice(0, 10);
}

export default function Page({ searchParams }: PageProps<"/admin/bao-cao">) {
  return (
    <>
      <AdminHeader title="Báo cáo doanh thu" />
      <Suspense fallback={<div className="card h-64 animate-pulse" />}>
        <Report searchParams={searchParams} />
      </Suspense>
    </>
  );
}

/** Doanh thu theo khoảng ngày (B-11: chỉ chủ shop). Mặc định 30 ngày gần nhất, theo ngày. */
async function Report({ searchParams }: Pick<PageProps<"/admin/bao-cao">, "searchParams">) {
  const sp = await searchParams;
  const session = await requireStaff("/admin/bao-cao");
  if (!isAdmin(session)) {
    return <p className="card m-0 p-6 text-[14px] text-muted">Chỉ chủ shop (quyền ADMIN) xem được báo cáo doanh thu.</p>;
  }
  const today = todayVn();
  const to = DATE_RE.test(textParam(sp.den)) ? textParam(sp.den) : today;
  const from = DATE_RE.test(textParam(sp.tu)) ? textParam(sp.tu) : shift(to, -29);
  const monthly = textParam(sp.theo) === "thang";
  const report = await getRevenueReport({ from, to, groupBy: monthly ? "MONTH" : "DAY" });
  const s = report.summary;
  const presets = [
    { label: "7 ngày", href: `?tu=${shift(today, -6)}&den=${today}` },
    { label: "30 ngày", href: `?tu=${shift(today, -29)}&den=${today}` },
    { label: "Tháng này", href: `?tu=${today.slice(0, 8)}01&den=${today}` },
    { label: "Năm nay", href: `?tu=${today.slice(0, 4)}-01-01&den=${today}&theo=thang` },
  ];
  const tiles = [
    { label: "Doanh thu thuần", value: formatVnd(s.netRevenue), note: `Tiền hàng ${formatVnd(s.grossSales)} − giảm ${formatVnd(s.discount)} + ship ${formatVnd(s.shippingFee)}` },
    { label: "Số đơn", value: String(s.orderCount), note: "Đã xác nhận hoặc đã thanh toán" },
    { label: "Giá trị trung bình / đơn", value: formatVnd(s.averageOrderValue), note: null },
    { label: "Trả hàng / hoàn tiền", value: formatVnd(s.returnedAmount), note: `${s.returnedCount} đơn` },
  ];

  return (
    <div className="flex flex-col gap-4">
      <form action="/admin/bao-cao" className="card flex flex-wrap items-end gap-3 p-3">
        <label className="flex flex-col gap-1 text-[12px] text-muted">
          Từ ngày
          <input type="date" name="tu" defaultValue={from} max={to} className="input-admin" />
        </label>
        <label className="flex flex-col gap-1 text-[12px] text-muted">
          Đến ngày
          <input type="date" name="den" defaultValue={to} className="input-admin" />
        </label>
        <label className="flex flex-col gap-1 text-[12px] text-muted">
          Theo
          <select name="theo" defaultValue={monthly ? "thang" : "ngay"} className="input-admin">
            <option value="ngay">Ngày</option>
            <option value="thang">Tháng</option>
          </select>
        </label>
        <button type="submit" className="btn-admin">
          Xem
        </button>
        <nav aria-label="Khoảng nhanh" className="flex flex-wrap gap-1 sm:ml-auto">
          {presets.map((p) => (
            <Link key={p.label} href={`/admin/bao-cao${p.href}`} className="rounded-lg px-2.5 py-1.5 text-[13px] text-muted hover:bg-admin-bg hover:text-ink">
              {p.label}
            </Link>
          ))}
        </nav>
      </form>

      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        {tiles.map((t) => (
          <div key={t.label} className="card flex flex-col gap-1 p-4">
            <span className="text-[12px] text-muted">{t.label}</span>
            <span className="text-[22px] font-semibold">{t.value}</span>
            {t.note ? <span className="text-[12px] text-muted">{t.note}</span> : null}
          </div>
        ))}
      </div>
      <p className="m-0 text-[12px] text-muted">
        Không tính: {s.awaitingPaymentCount} đơn chờ thanh toán, {s.cancelledCount} đơn đã hủy. Khoảng {periodLabel(report.from, false)}/{report.from.slice(0, 4)} –{" "}
        {periodLabel(report.to, false)}/{report.to.slice(0, 4)}, giờ Việt Nam.
      </p>

      <section className="card flex flex-col gap-3 p-4">
        <h2 className="m-0 text-[14px] font-semibold">Doanh thu thuần theo {monthly ? "tháng" : "ngày"}</h2>
        <RevenueChart series={report.series} monthly={monthly} />
        <details className="text-[13px]">
          <summary className="cursor-pointer text-muted">Xem dạng bảng</summary>
          <table className="mt-2 w-full border-collapse">
            <thead>
              <tr className="bg-admin-bg text-left text-[12px] text-muted">
                <th className="px-3 py-1.5 font-medium">Kỳ</th>
                <th className="px-3 py-1.5 text-right font-medium">Số đơn</th>
                <th className="px-3 py-1.5 text-right font-medium">Doanh thu thuần</th>
              </tr>
            </thead>
            <tbody>
              {report.series.map((p) => (
                <tr key={p.period} className="border-t border-admin-line">
                  <td className="px-3 py-1.5">{periodLabel(p.period, monthly)}</td>
                  <td className="px-3 py-1.5 text-right tabular-nums">{p.orderCount}</td>
                  <td className="px-3 py-1.5 text-right tabular-nums">{formatVnd(p.netRevenue)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </details>
      </section>

      <div className="grid gap-4 lg:grid-cols-[2fr_1fr]">
        <section className="card overflow-hidden">
          <h2 className="m-0 border-b border-admin-line px-4 py-3 text-[14px] font-semibold">Sản phẩm bán chạy</h2>
          {report.topProducts.length === 0 ? (
            <p className="m-0 px-4 py-8 text-center text-[13px] text-muted">Chưa có đơn nào.</p>
          ) : (
            <table className="w-full border-collapse text-[13px]">
              <thead>
                <tr className="bg-admin-bg text-left text-[12px] text-muted">
                  <th className="px-4 py-2 font-medium">Sản phẩm</th>
                  <th className="px-4 py-2 text-right font-medium">Đã bán</th>
                  <th className="px-4 py-2 text-right font-medium">Tiền hàng</th>
                </tr>
              </thead>
              <tbody>
                {report.topProducts.map((p) => (
                  <tr key={p.productId} className="border-t border-admin-line">
                    <td className="px-4 py-2.5">
                      <Link href={`/san-pham/${p.productSlug}`} target="_blank" className="hover:underline">
                        {p.productName}
                      </Link>
                    </td>
                    <td className="px-4 py-2.5 text-right tabular-nums">{p.quantity}</td>
                    <td className="px-4 py-2.5 text-right tabular-nums">{formatVnd(p.revenue)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          <p className="m-0 px-4 py-2 text-[12px] text-muted">Tiền hàng theo giá bán từng dòng, trước giảm giá của đơn.</p>
        </section>
        <section className="card overflow-hidden">
          <h2 className="m-0 border-b border-admin-line px-4 py-3 text-[14px] font-semibold">Theo phương thức thanh toán</h2>
          <ul className="m-0 flex list-none flex-col p-0 text-[13px]">
            {report.byPaymentMethod.length === 0 ? <li className="px-4 py-8 text-center text-muted">Chưa có đơn nào.</li> : null}
            {report.byPaymentMethod.map((m) => (
              <li key={m.method} className="flex items-baseline justify-between gap-3 border-t border-admin-line px-4 py-2.5 first:border-t-0">
                <span>
                  {PAYMENT_METHOD_LABELS[m.method] ?? m.method}
                  <span className="ml-1 text-muted">· {m.orderCount} đơn</span>
                </span>
                <span className="tabular-nums">{formatVnd(m.netRevenue)}</span>
              </li>
            ))}
          </ul>
        </section>
      </div>
    </div>
  );
}
