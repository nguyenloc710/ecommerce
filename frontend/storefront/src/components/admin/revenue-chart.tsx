"use client";

import { useState } from "react";
import { periodLabel } from "@/lib/admin-shared";
import { formatVnd } from "@/lib/format";

type Point = { period: string; orderCount: number; netRevenue: number };

const W = 800;
const H = 260;
const PAD = { top: 12, right: 8, bottom: 28, left: 64 };
const MAX_BAR = 24;
const GAP = 2;
const RADIUS = 4;
const MAX_X_LABELS = 10;
const MAX_Y_STEPS = 5;

/** Trục y "1,5 tr" / "800 N" cho gọn; số đầy đủ nằm trong tooltip và bảng. */
const compactVnd = (v: number) =>
  v >= 1_000_000
    ? `${new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 1 }).format(v / 1_000_000)} tr`
    : v >= 1_000
      ? `${new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 0 }).format(v / 1_000)} N`
      : String(v);

/** Vạch trục y cách đều một bước tròn (1, 2, 2,5, 5 × 10^n), tối đa 5 khoảng. */
function niceTicks(value: number) {
  if (value <= 0) return [0, 1];
  const exp = 10 ** Math.floor(Math.log10(value / MAX_Y_STEPS));
  const step = [1, 2, 2.5, 5, 10].map((m) => m * exp).find((s) => Math.ceil(value / s) <= MAX_Y_STEPS) ?? 10 * exp;
  return Array.from({ length: Math.ceil(value / step) + 1 }, (_, i) => i * step);
}

/** Cột có góc bo 4px ở đầu dữ liệu, vuông ở đường gốc. */
function columnPath(x: number, y: number, w: number, h: number) {
  const r = Math.min(RADIUS, h, w / 2);
  const base = y + h;
  return `M${x},${base}V${y + r}Q${x},${y} ${x + r},${y}H${x + w - r}Q${x + w},${y} ${x + w},${y + r}V${base}Z`;
}

/** Doanh thu thuần theo kỳ (một chuỗi, không cần chú giải). Rê chuột hoặc Tab vào cột để xem số. */
export function RevenueChart({ series, monthly }: { series: Point[]; monthly: boolean }) {
  const [active, setActive] = useState<number | null>(null);
  const plotW = W - PAD.left - PAD.right;
  const plotH = H - PAD.top - PAD.bottom;
  const ticks = niceTicks(Math.max(...series.map((p) => p.netRevenue), 0));
  const max = ticks[ticks.length - 1];
  const band = plotW / Math.max(series.length, 1);
  const barW = Math.max(1, Math.min(MAX_BAR, band - GAP));
  const labelEvery = Math.ceil(series.length / MAX_X_LABELS);
  const total = series.reduce((s, p) => s + p.netRevenue, 0);
  const point = active === null ? null : series[active];

  return (
    <div className="relative">
      <svg
        viewBox={`0 0 ${W} ${H}`}
        className="h-auto w-full"
        role="img"
        aria-label={`Doanh thu thuần theo ${monthly ? "tháng" : "ngày"}, tổng ${formatVnd(total)}`}
        onPointerLeave={() => setActive(null)}
      >
        {ticks.map((t) => {
          const y = PAD.top + plotH - (t / max) * plotH;
          return (
            <g key={t}>
              <line x1={PAD.left} x2={W - PAD.right} y1={y} y2={y} stroke="var(--admin-line)" strokeWidth={1} />
              <text x={PAD.left - 8} y={y} textAnchor="end" dominantBaseline="middle" fontSize={11} fill="var(--muted)">
                {compactVnd(t)}
              </text>
            </g>
          );
        })}
        {series.map((p, i) => {
          const h = (p.netRevenue / max) * plotH;
          const x = PAD.left + i * band + (band - barW) / 2;
          const y = PAD.top + plotH - h;
          return (
            <g
              key={p.period}
              tabIndex={0}
              role="button"
              aria-label={`${periodLabel(p.period, monthly)}: ${formatVnd(p.netRevenue)}, ${p.orderCount} đơn`}
              onPointerEnter={() => setActive(i)}
              onFocus={() => setActive(i)}
              onBlur={() => setActive(null)}
              className="outline-none"
            >
              {/* Vùng bắt chuột cả dải, rộng hơn cột */}
              <rect x={PAD.left + i * band} y={PAD.top} width={band} height={plotH} fill={active === i ? "var(--admin-bg)" : "transparent"} />
              {h > 0 ? <path d={columnPath(x, y, barW, h)} fill="var(--chart-1)" /> : null}
              {i % labelEvery === 0 ? (
                <text x={PAD.left + i * band + band / 2} y={H - 8} textAnchor="middle" fontSize={11} fill="var(--muted)">
                  {periodLabel(p.period, monthly)}
                </text>
              ) : null}
            </g>
          );
        })}
        <line x1={PAD.left} x2={W - PAD.right} y1={PAD.top + plotH} y2={PAD.top + plotH} stroke="var(--muted)" strokeWidth={1} />
      </svg>
      {point && active !== null ? (
        <div
          className="pointer-events-none absolute top-2 z-10 -translate-x-1/2 rounded-lg border border-admin-line bg-paper px-3 py-2 text-[12px] shadow-sm"
          style={{ left: `${((PAD.left + active * band + band / 2) / W) * 100}%` }}
          role="status"
        >
          <div className="text-[14px] font-semibold tabular-nums">{formatVnd(point.netRevenue)}</div>
          <div className="text-muted">
            {periodLabel(point.period, monthly)} · {point.orderCount} đơn
          </div>
        </div>
      ) : null}
      {total === 0 ? <p className="m-0 mt-1 text-center text-[13px] text-muted">Chưa có doanh thu trong khoảng này.</p> : null}
    </div>
  );
}
