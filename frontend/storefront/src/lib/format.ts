const vnd = new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 0 });

/** Định dạng tiền VND theo UI-15: 1.290.000₫ */
export function formatVnd(amount: number): string {
  return `${vnd.format(amount)}₫`;
}
