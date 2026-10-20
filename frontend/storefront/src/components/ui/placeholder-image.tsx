import type { ReactNode } from "react";

export type Tone = "stone" | "sand" | "taupe" | "bone" | "ink";
export type IconName = "shirt" | "dress" | "bag" | "ring" | "necklace" | "earring";

const toneClass: Record<Tone, string> = {
  stone: "bg-stone",
  sand: "bg-sand",
  taupe: "bg-taupe",
  bone: "bg-[#eeeae3]",
  ink: "bg-[#2b2a28]",
};

// Nét vẽ mảnh gợi hình sản phẩm, dùng khi chưa có ảnh thật (UI-14)
const icons: Record<IconName, ReactNode> = {
  shirt: <path d="M35 20h30l12 10-8 10-4-3v43H35V37l-4 3-8-10z" />,
  dress: (
    <>
      <path d="M38 18h24l10 60H28z" />
      <path d="M42 18q8 10 16 0" />
    </>
  ),
  bag: (
    <>
      <path d="M25 40h50l-4 40H29z" />
      <path d="M38 40q12-24 24 0" />
    </>
  ),
  ring: (
    <>
      <circle cx="50" cy="58" r="24" />
      <circle cx="50" cy="58" r="19" />
      <path d="M42 30 50 22l8 8-8 7z" />
    </>
  ),
  necklace: (
    <>
      <path d="M20 30q30 40 60 0" />
      <path d="M44 58 50 70l6-12-6-4z" />
    </>
  ),
  earring: (
    <>
      <circle cx="35" cy="36" r="4" />
      <circle cx="65" cy="36" r="4" />
      <path d="M35 40v16a8 8 0 0 0 16 0M65 40v16a8 8 0 0 1-16 0" />
    </>
  ),
};

type Props = {
  tone?: Tone;
  icon?: IconName;
  className?: string;
  iconSize?: number;
  children?: ReactNode;
};

export function PlaceholderImage({ tone = "stone", icon, className = "", iconSize = 90, children }: Props) {
  const stroke = tone === "ink" ? "#fff" : "#111";
  return (
    <div className={`img-ph ${toneClass[tone]} ${className}`}>
      {icon ? (
        <svg
          aria-hidden="true"
          width={iconSize}
          height={iconSize}
          viewBox="0 0 100 100"
          fill="none"
          stroke={stroke}
          strokeWidth="1"
          className="opacity-30"
        >
          {icons[icon]}
        </svg>
      ) : null}
      {children}
    </div>
  );
}
