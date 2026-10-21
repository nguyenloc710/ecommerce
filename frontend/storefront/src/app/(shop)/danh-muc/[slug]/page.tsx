import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Suspense } from "react";
import { ProductListing } from "@/components/listing/product-listing";
import { findCategory, getCategoryTree } from "@/lib/catalog";

type Props = PageProps<"/danh-muc/[slug]">;

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { slug } = await params;
  const found = findCategory(await getCategoryTree(), slug);
  return found ? { title: found.node.name, description: `Mua ${found.node.name.toLowerCase()} chính hãng, giao toàn quốc 2–4 ngày.` } : {};
}

export default function CategoryPage({ params, searchParams }: Props) {
  return (
    <Suspense fallback={<div className="pad pt-7"><div className="h-14 w-64 animate-pulse bg-stone" /></div>}>
      <CategoryListing params={params} searchParams={searchParams} />
    </Suspense>
  );
}

async function CategoryListing({ params, searchParams }: Props) {
  const { slug } = await params;
  const found = findCategory(await getCategoryTree(), slug);
  if (!found) notFound();
  const { node, trail } = found;
  return (
    <ProductListing
      title={node.name}
      breadcrumb={[...trail.map((t) => ({ name: t.name, href: `/danh-muc/${t.slug}` })), { name: node.name }]}
      fixed={{ category: node.slug }}
      basePath={`/danh-muc/${node.slug}`}
      searchParams={searchParams}
    >
      {node.children.length > 0 ? (
        <div className="flex flex-wrap gap-2 pt-2">
          <span className="inline-flex h-8 items-center border border-ink bg-ink px-3 text-[12px] text-paper">Tất cả</span>
          {node.children.map((c) => (
            <Link key={c.slug} href={`/danh-muc/${c.slug}`} className="inline-flex h-8 items-center border border-line px-3 text-[12px] hover:border-ink">
              {c.name}
            </Link>
          ))}
        </div>
      ) : null}
    </ProductListing>
  );
}
