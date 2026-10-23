/** Sao hiển thị (không tương tác); làm tròn nửa sao xuống sao đầy cho gọn, đọc to bằng aria-label. */
export function Stars({ value, size = 13 }: { value: number; size?: number }) {
  const full = Math.round(value);
  return (
    <span aria-label={`${value.toLocaleString("vi-VN")} trên 5 sao`} role="img" className="inline-flex tracking-[0.12em]" style={{ fontSize: size }}>
      {[1, 2, 3, 4, 5].map((i) => (
        <span key={i} aria-hidden="true" className={i <= full ? "text-ink" : "text-line"}>
          ★
        </span>
      ))}
    </span>
  );
}
