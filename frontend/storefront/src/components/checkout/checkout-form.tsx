"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useMemo, useRef, useState, useTransition } from "react";
import { useForm, useWatch, type FieldError } from "react-hook-form";
import { loadWards, placeOrder, quoteCheckout, validateVoucher, type ActionError } from "@/lib/checkout-actions";
import {
  PAYMENT_LABELS,
  PAYMENT_METHODS,
  VOUCHER_ERROR_FALLBACK,
  checkoutFormSchema,
  type CheckoutFormValues,
  type CheckoutLine,
  type CheckoutQuote,
  type Division,
  type SavedAddress,
} from "@/lib/checkout-shared";
import { useCartStore } from "@/lib/cart-store";
import { formatVnd } from "@/lib/format";

type Props = { email: string; addresses: SavedAddress[]; provinces: Division[] };

const QUOTE_DEBOUNCE_MS = 300;

/** Thanh toán một trang (UI-11): trái là form, phải là tóm tắt nền bone; mobile tóm tắt thu gọn ở đầu + nút dính đáy. */
export function CheckoutForm({ email, addresses, provinces }: Props) {
  const router = useRouter();
  const synced = useCartStore((s) => s.synced);
  const cartLines = useCartStore((s) => s.lines);
  const lines: CheckoutLine[] = useMemo(() => cartLines.map((l) => ({ skuId: l.skuId, quantity: l.qty, engraving: l.engraving })), [cartLines]);
  const linesKey = JSON.stringify(lines);

  const defaultAddress = addresses.find((a) => a.isDefault) ?? addresses[0];
  const form = useForm<CheckoutFormValues>({
    resolver: zodResolver(checkoutFormSchema),
    defaultValues: {
      addressMode: defaultAddress ? "saved" : "new",
      addressId: defaultAddress?.id,
      address: { recipientName: "", phone: "", provinceCode: "", wardCode: "", line1: "", note: "" },
      saveAddress: addresses.length === 0,
      contactEmail: email,
      paymentMethod: "COD",
      note: "",
    },
  });
  const { register, handleSubmit, setValue, control, formState } = form;
  const [addressMode, addressId, provinceCode, carrier, paymentMethod] = useWatch({
    control,
    name: ["addressMode", "addressId", "address.provinceCode", "shippingCarrier", "paymentMethod"],
  });

  // ---- Phường theo tỉnh ----
  const [wards, setWards] = useState<Division[]>([]);
  useEffect(() => {
    if (!provinceCode) return;
    let alive = true;
    void loadWards(provinceCode).then((list) => alive && setWards(list));
    return () => {
      alive = false;
    };
  }, [provinceCode]);

  // ---- Voucher ----
  const [voucherInput, setVoucherInput] = useState("");
  const [voucher, setVoucher] = useState<string | null>(null);
  const [voucherMsg, setVoucherMsg] = useState<{ ok: boolean; text: string } | null>(null);
  const [applyingVoucher, startApplyVoucher] = useTransition();

  // ---- Báo giá (chạy lại khi giỏ, địa chỉ đã lưu, hãng vận chuyển hoặc voucher đổi) ----
  const [quote, setQuote] = useState<CheckoutQuote | null>(null);
  const [quoteError, setQuoteError] = useState<ActionError | null>(null);
  const quoteSeq = useRef(0);
  useEffect(() => {
    if (!synced || lines.length === 0) return;
    const seq = ++quoteSeq.current;
    const timer = setTimeout(async () => {
      const res = await quoteCheckout(lines, {
        addressId: addressMode === "saved" ? addressId : undefined,
        shippingCarrier: carrier || undefined,
        voucherCode: voucher ?? undefined,
      });
      if (seq !== quoteSeq.current) return;
      if (!res.ok) {
        setQuoteError(res);
        return;
      }
      setQuoteError(null);
      setQuote(res.quote);
      if (res.quote.voucherError && voucher) setVoucherMsg({ ok: false, text: VOUCHER_ERROR_FALLBACK });
    }, QUOTE_DEBOUNCE_MS);
    return () => clearTimeout(timer);
    // linesKey thay cho lines: mảng mới mỗi lần store đổi nhưng nội dung có thể giữ nguyên
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [synced, linesKey, addressMode, addressId, carrier, voucher]);

  const applyVoucher = () => {
    const code = voucherInput.trim().toUpperCase();
    if (!code) return;
    startApplyVoucher(async () => {
      const res = await validateVoucher(code, quote?.subtotal ?? 0);
      if (res.ok) {
        setVoucher(code);
        setVoucherMsg({ ok: true, text: res.description ?? `Đã áp dụng mã ${code}` });
      } else {
        setVoucher(null);
        setVoucherMsg({ ok: false, text: res.message });
      }
    });
  };
  const removeVoucher = () => {
    setVoucher(null);
    setVoucherInput("");
    setVoucherMsg(null);
  };

  // ---- Đặt hàng ----
  const [placeError, setPlaceError] = useState<ActionError | null>(null);
  const [placing, startPlacing] = useTransition();
  // Giữ nguyên key khi lỗi mạng (bấm lại sẽ nhận đơn cũ nếu server đã tạo); lỗi nghiệp vụ thì sinh key mới
  const [idempotencyKey] = useState<{ current: string | null }>(() => ({ current: null }));

  const onSubmit = (v: CheckoutFormValues) =>
    startPlacing(async () => {
      setPlaceError(null);
      idempotencyKey.current ??= crypto.randomUUID();
      const res = await placeOrder(
        lines,
        {
          addressId: v.addressMode === "saved" ? v.addressId : undefined,
          address: v.addressMode === "new" ? v.address : undefined,
          saveAddress: v.addressMode === "new" && v.saveAddress,
          contactEmail: v.contactEmail,
          shippingCarrier: v.shippingCarrier || quote?.selectedShipping?.carrier,
          paymentMethod: v.paymentMethod,
          voucherCode: voucher ?? undefined,
          note: v.note,
        },
        idempotencyKey.current,
      );
      if (!res.ok) {
        if (res.code) idempotencyKey.current = null;
        setPlaceError(res);
        return;
      }
      // Server đã bỏ các SKU vừa đặt khỏi giỏ; lấy lại giỏ để header cập nhật
      await useCartStore.getState().sync();
      router.push(res.next);
    });

  if (!synced) return <div className="h-64 animate-pulse bg-stone" />;
  if (cartLines.length === 0) {
    return (
      <div className="flex flex-col items-start gap-4 py-10">
        <p className="m-0 text-[14px] text-muted">Giỏ hàng đang trống.</p>
        <Link href="/moi-ve" className="btn">
          Xem hàng mới về
        </Link>
      </div>
    );
  }

  const shippingOptions = quote?.shippingOptions ?? [];
  const selectedCarrier = carrier || quote?.selectedShipping?.carrier;
  const summary = (
    <Summary
      quote={quote}
      cartLines={cartLines}
      voucher={voucher}
      voucherInput={voucherInput}
      setVoucherInput={setVoucherInput}
      voucherMsg={voucherMsg}
      applyingVoucher={applyingVoucher}
      onApply={applyVoucher}
      onRemove={removeVoucher}
    />
  );

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="grid gap-10 pb-28 lg:grid-cols-[1fr_440px] lg:gap-16 lg:pb-0">
      {/* Mobile: tóm tắt thu gọn ở đầu */}
      <details className="border-y border-line bg-bone lg:hidden">
        <summary className="flex cursor-pointer list-none items-center justify-between px-4 py-4 text-[13px]">
          <span className="ul">Xem tóm tắt đơn hàng</span>
          <span className="font-display text-[20px]">{formatVnd(quote?.total ?? 0)}</span>
        </summary>
        <div className="px-4 pb-5">{summary}</div>
      </details>

      <div className="flex flex-col gap-10">
        {(placeError || quoteError) && <ErrorBanner error={(placeError ?? quoteError)!} />}

        <Section title="Liên hệ">
          <Field label="Email nhận thông báo đơn hàng" error={formState.errors.contactEmail}>
            <input type="email" autoComplete="email" {...register("contactEmail")} className="input" />
          </Field>
        </Section>

        <Section title="Địa chỉ giao hàng">
          {addresses.length > 0 && (
            <div className="flex flex-col">
              {addresses.map((a) => (
                <label key={a.id} className="flex cursor-pointer gap-4 border-b border-line py-4 text-[13px]">
                  <input
                    type="radio"
                    name="address-choice"
                    checked={addressMode === "saved" && addressId === a.id}
                    onChange={() => {
                      setValue("addressMode", "saved");
                      setValue("addressId", a.id, { shouldValidate: formState.isSubmitted });
                    }}
                    className="mt-1 accent-ink"
                  />
                  <span className="flex flex-col gap-0.5">
                    <span>
                      {a.recipientName} · {a.phone}
                      {a.isDefault ? <span className="eyebrow ml-2 text-[10px] text-muted">Mặc định</span> : null}
                    </span>
                    <span className="text-muted">
                      {a.line1}, {a.wardName}, {a.provinceName}
                    </span>
                  </span>
                </label>
              ))}
              <label className="flex cursor-pointer gap-4 border-b border-line py-4 text-[13px]">
                <input type="radio" name="address-choice" checked={addressMode === "new"} onChange={() => setValue("addressMode", "new")} className="mt-1 accent-ink" />
                <span>Giao đến địa chỉ khác</span>
              </label>
            </div>
          )}
          {formState.errors.addressId && <p className="m-0 text-[12px] text-sale">{formState.errors.addressId.message}</p>}

          {addressMode === "new" && (
            <div className="grid gap-4 pt-2 sm:grid-cols-2">
              <Field label="Họ tên người nhận" error={formState.errors.address?.recipientName}>
                <input autoComplete="name" {...register("address.recipientName")} className="input" />
              </Field>
              <Field label="Số điện thoại" error={formState.errors.address?.phone}>
                <input type="tel" autoComplete="tel" {...register("address.phone")} className="input" />
              </Field>
              <Field label="Tỉnh / thành phố" error={formState.errors.address?.provinceCode}>
                <select
                  {...register("address.provinceCode", {
                    onChange: () => {
                      setWards([]);
                      setValue("address.wardCode", "");
                    },
                  })}
                  className="input"
                >
                  <option value="">Chọn tỉnh / thành phố</option>
                  {provinces.map((p) => (
                    <option key={p.code} value={p.code}>
                      {p.name}
                    </option>
                  ))}
                </select>
              </Field>
              <Field label="Phường / xã" error={formState.errors.address?.wardCode}>
                <select {...register("address.wardCode")} disabled={!provinceCode} className="input disabled:opacity-50">
                  <option value="">{provinceCode && wards.length === 0 ? "Đang tải…" : "Chọn phường / xã"}</option>
                  {wards.map((w) => (
                    <option key={w.code} value={w.code}>
                      {w.name}
                    </option>
                  ))}
                </select>
              </Field>
              <div className="sm:col-span-2">
                <Field label="Số nhà, tên đường" error={formState.errors.address?.line1}>
                  <input autoComplete="street-address" {...register("address.line1")} className="input" />
                </Field>
              </div>
              <label className="flex items-center gap-3 text-[13px] sm:col-span-2">
                <input type="checkbox" {...register("saveAddress")} className="accent-ink" />
                Lưu vào sổ địa chỉ
              </label>
            </div>
          )}
        </Section>

        <Section title="Vận chuyển">
          {shippingOptions.length === 0 ? (
            <div className="h-14 animate-pulse bg-stone" />
          ) : (
            shippingOptions.map((o) => (
              <label key={o.carrier} className="flex cursor-pointer items-center gap-4 border-b border-line py-4 text-[13px]">
                <input
                  type="radio"
                  name="shipping-choice"
                  checked={selectedCarrier === o.carrier}
                  onChange={() => setValue("shippingCarrier", o.carrier)}
                  className="accent-ink"
                />
                <span className="flex flex-1 flex-col">
                  <span>{o.carrierName}</span>
                  <span className="text-muted">Dự kiến {o.etaDays} ngày</span>
                </span>
                <span>{o.fee === 0 ? "Miễn phí" : formatVnd(o.fee)}</span>
              </label>
            ))
          )}
        </Section>

        <Section title="Thanh toán">
          {PAYMENT_METHODS.map((m) => (
            <label key={m} className="flex cursor-pointer items-center gap-4 border-b border-line py-4 text-[13px]">
              <input type="radio" value={m} {...register("paymentMethod")} className="accent-ink" />
              <span className="flex flex-col">
                <span>{PAYMENT_LABELS[m].title}</span>
                <span className="text-muted">{PAYMENT_LABELS[m].hint}</span>
              </span>
            </label>
          ))}
          {formState.errors.paymentMethod && <p className="m-0 text-[12px] text-sale">{formState.errors.paymentMethod.message}</p>}
        </Section>

        <Section title="Ghi chú">
          <Field label="Ghi chú cho đơn hàng (không bắt buộc)" error={formState.errors.note}>
            <textarea rows={3} {...register("note")} className="input py-3" />
          </Field>
        </Section>
      </div>

      <aside className="hidden h-fit flex-col gap-5 bg-bone p-8 lg:sticky lg:top-24 lg:flex">
        {summary}
        <PlaceButton placing={placing} disabled={!quote} total={quote?.total} method={paymentMethod} />
      </aside>

      {/* Mobile: nút đặt hàng dính đáy */}
      <div className="fixed inset-x-0 bottom-0 z-20 border-t border-line bg-paper px-4 py-3 lg:hidden">
        <PlaceButton placing={placing} disabled={!quote} total={quote?.total} method={paymentMethod} />
      </div>
    </form>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="flex flex-col gap-3">
      <h2 className="eyebrow m-0">{title}</h2>
      {children}
    </section>
  );
}

function Field({ label, error, children }: { label: string; error?: FieldError; children: React.ReactNode }) {
  return (
    <label className="flex flex-col gap-1.5 text-[12px] text-muted">
      {label}
      {children}
      {error ? <span className="text-sale">{error.message}</span> : null}
    </label>
  );
}

function ErrorBanner({ error }: { error: ActionError }) {
  const stockProblem = error.code?.startsWith("checkout.insufficient") || error.code?.startsWith("inventory.") || error.code === "checkout.sku-not-sellable";
  return (
    <div role="alert" className="flex flex-col gap-1 border border-sale px-4 py-3 text-[13px] text-sale">
      <span>{error.message}</span>
      {stockProblem ? (
        <Link href="/gio-hang" className="ul w-fit">
          Chỉnh lại giỏ hàng
        </Link>
      ) : null}
      {error.traceId ? (
        <button type="button" onClick={() => void navigator.clipboard?.writeText(error.traceId!)} className="w-fit text-[11px] text-muted underline">
          Mã lỗi: {error.traceId} (bấm để sao chép)
        </button>
      ) : null}
    </div>
  );
}

function PlaceButton({ placing, disabled, total, method }: { placing: boolean; disabled: boolean; total?: number; method?: string }) {
  return (
    <button type="submit" disabled={placing || disabled} className="btn w-full disabled:cursor-wait disabled:opacity-60">
      {placing ? "Đang đặt hàng…" : `${method === "MOCK_ONLINE" ? "Thanh toán" : "Đặt hàng"}${total != null ? ` · ${formatVnd(total)}` : ""}`}
    </button>
  );
}

type SummaryProps = {
  quote: CheckoutQuote | null;
  cartLines: { skuId: string; name: string; variantLabel: string; imageUrl?: string; qty: number; price: number; engraving?: string }[];
  voucher: string | null;
  voucherInput: string;
  setVoucherInput: (v: string) => void;
  voucherMsg: { ok: boolean; text: string } | null;
  applyingVoucher: boolean;
  onApply: () => void;
  onRemove: () => void;
};

/** Tóm tắt đơn: dòng hàng theo giá server (quote), voucher, phí ship, tổng. Chưa có quote thì tạm dùng giỏ. */
function Summary({ quote, cartLines, voucher, voucherInput, setVoucherInput, voucherMsg, applyingVoucher, onApply, onRemove }: SummaryProps) {
  const rows = quote
    ? quote.lines.map((l) => ({ key: `${l.skuId}:${l.engraving ?? ""}`, name: l.productName, variant: l.variantLabel, image: l.imageUrl, qty: l.quantity, total: l.lineTotal, engraving: l.engraving }))
    : cartLines.map((l) => ({ key: `${l.skuId}:${l.engraving ?? ""}`, name: l.name, variant: l.variantLabel, image: l.imageUrl ?? null, qty: l.qty, total: l.price * l.qty, engraving: l.engraving ?? null }));
  return (
    <div className="flex flex-col gap-5">
      <ul className="m-0 flex list-none flex-col gap-4 p-0">
        {rows.map((r) => (
          <li key={r.key} className="flex gap-4 text-[13px]">
            <span className="relative h-[80px] w-[60px] flex-none bg-stone">
              {r.image ? <Image src={r.image} alt={r.name} fill sizes="60px" className="object-cover" /> : null}
              <span className="absolute -right-2 -top-2 inline-flex h-5 min-w-5 items-center justify-center rounded-full bg-ink px-1 text-[10px] text-paper">{r.qty}</span>
            </span>
            <span className="flex flex-1 flex-col">
              <span>{r.name}</span>
              <span className="text-[12px] text-muted">
                {r.variant}
                {r.engraving ? ` · Khắc "${r.engraving}"` : ""}
              </span>
            </span>
            <span>{formatVnd(r.total)}</span>
          </li>
        ))}
      </ul>

      <div className="flex flex-col gap-2 border-t border-line pt-5">
        {voucher ? (
          <div className="flex items-center justify-between text-[13px]">
            <span>
              Mã <strong>{voucher}</strong>
            </span>
            <button type="button" onClick={onRemove} className="text-[12px] text-muted underline">
              Bỏ mã
            </button>
          </div>
        ) : (
          <div className="flex gap-2">
            <input
              value={voucherInput}
              onChange={(e) => setVoucherInput(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") {
                  e.preventDefault();
                  onApply();
                }
              }}
              placeholder="Mã giảm giá"
              aria-label="Mã giảm giá"
              className="input flex-1 uppercase"
            />
            <button type="button" onClick={onApply} disabled={applyingVoucher || !voucherInput.trim()} className="btn btn-light min-h-11 px-5 disabled:opacity-50">
              Áp dụng
            </button>
          </div>
        )}
        {voucherMsg ? <span className={`text-[12px] ${voucherMsg.ok ? "text-ok" : "text-sale"}`}>{voucherMsg.text}</span> : null}
      </div>

      <dl className="m-0 flex flex-col gap-2 border-t border-line pt-5 text-[13px]">
        <Row label="Tạm tính" value={quote ? formatVnd(quote.subtotal) : "…"} />
        {quote && quote.discount > 0 ? <Row label="Giảm giá" value={`−${formatVnd(quote.discount)}`} /> : null}
        <Row label="Vận chuyển" value={quote ? (quote.shippingFee === 0 ? "Miễn phí" : formatVnd(quote.shippingFee)) : "…"} />
        <div className="mt-2 flex items-baseline justify-between border-t border-line pt-4">
          <dt>Tổng cộng</dt>
          <dd className="font-display m-0 text-[28px] leading-none">{quote ? formatVnd(quote.total) : "…"}</dd>
        </div>
      </dl>
    </div>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between">
      <dt className="text-muted">{label}</dt>
      <dd className="m-0">{value}</dd>
    </div>
  );
}
