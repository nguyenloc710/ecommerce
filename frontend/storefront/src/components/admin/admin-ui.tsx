import Link from "next/link";
import type { ReactNode } from "react";

/** Phần đầu trang quản trị: tiêu đề + thao tác bên phải. */
export function AdminHeader({ title, back, actions }: { title: string; back?: { href: string; label: string }; actions?: ReactNode }) {
  return (
    <div className="mb-6 flex flex-col gap-2">
      {back ? (
        <Link href={back.href} className="w-fit text-[13px] text-muted hover:text-ink">
          ← {back.label}
        </Link>
      ) : null}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="m-0 text-[22px] font-semibold">{title}</h1>
        {actions}
      </div>
    </div>
  );
}

type Tone = "neutral" | "info" | "success" | "warning" | "critical";
const toneClass: Record<Tone, string> = {
  neutral: "bg-[#e3e3e3] text-[#303030]",
  info: "bg-[#e0f0ff] text-[#00527c]",
  success: "bg-[#cdfee1] text-[#0c5132]",
  warning: "bg-[#fff1e3] text-[#5e4200]",
  critical: "bg-[#fee9e8] text-[#8e1f0b]",
};

/** Nhãn trạng thái bo tròn kiểu Shopify. */
export function Badge({ tone = "neutral", children }: { tone?: Tone; children: ReactNode }) {
  return <span className={`inline-flex items-center whitespace-nowrap rounded-full px-2 py-0.5 text-[12px] font-medium ${toneClass[tone]}`}>{children}</span>;
}

export function orderTone(status: string): Tone {
  switch (status) {
    case "CONFIRMED":
    case "PENDING":
      return "warning";
    case "PACKING":
    case "SHIPPING":
    case "AWAITING_PAYMENT":
      return "info";
    case "DELIVERED":
    case "COMPLETED":
      return "success";
    case "CANCELLED":
    case "RETURN_REQUESTED":
      return "critical";
    default:
      return "neutral";
  }
}

/** Trang trước/sau giữ nguyên các tham số lọc. */
export function Pager({ basePath, params, page, hasNext, total }: { basePath: string; params: Record<string, string | undefined>; page: number; hasNext: boolean; total: number }) {
  const href = (p: number) => {
    const sp = new URLSearchParams();
    for (const [k, v] of Object.entries(params)) if (v) sp.set(k, v);
    if (p > 0) sp.set("trang", String(p));
    const s = sp.toString();
    return s ? `${basePath}?${s}` : basePath;
  };
  return (
    <div className="flex items-center justify-between px-4 py-3 text-[13px] text-muted">
      <span>{total} kết quả</span>
      <div className="flex gap-2">
        {page > 0 ? (
          <Link href={href(page - 1)} className="btn-admin btn-admin-light">
            ← Trước
          </Link>
        ) : null}
        {hasNext ? (
          <Link href={href(page + 1)} className="btn-admin btn-admin-light">
            Sau →
          </Link>
        ) : null}
      </div>
    </div>
  );
}

export function ErrorNote({ message, traceId }: { message: string; traceId?: string }) {
  return (
    <p role="alert" className="m-0 rounded-lg bg-[#fee9e8] px-3 py-2 text-[13px] text-[#8e1f0b]">
      {message}
      {traceId ? <span className="ml-1 text-[12px] opacity-80">(mã lỗi {traceId})</span> : null}
    </p>
  );
}

export const pageParam = (v: string | string[] | undefined) => Math.max(0, Number(typeof v === "string" ? v : 0) || 0);
export const textParam = (v: string | string[] | undefined) => (typeof v === "string" ? v.trim() : "");
