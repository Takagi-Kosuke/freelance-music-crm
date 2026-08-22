import Link from 'next/link'

export default function Home() {
  return (
    <main className="min-h-screen px-4 py-8 md:py-12">
      <div className="mx-auto flex max-w-5xl flex-col gap-6 rounded-[26px] border border-[#d9e3ec] bg-white/90 p-6 shadow-[0_20px_60px_rgba(15,23,42,0.06)] backdrop-blur-sm md:p-8">
        <header className="space-y-4">
          <div className="flex items-center gap-3">
            <span className="inline-flex rounded-full border border-[#cfe0ee] bg-[#edf5fb] px-2.5 py-1 text-[10px] font-black tracking-[0.22em] text-[#0f4c7a] uppercase">
              FMC
            </span>
          </div>

          <h1 className="max-w-3xl text-2xl font-semibold tracking-[-0.05em] text-[#0f172a] md:text-3xl">
            音楽制作の見積依頼と進行管理を、
            <span className="block text-[#0f4c7a]">ひとつの画面で。</span>
          </h1>

          <p className="max-w-2xl text-[11px] leading-6 text-[#475569] md:text-xs">
            クライアントの方は見積依頼フォームから、作業者の方はダッシュボードから、
            依頼から納品までをスムーズに管理できます。
          </p>
        </header>

        <div className="flex flex-wrap items-center gap-2.5">
          <Link
            href="/quote"
            className="inline-flex min-h-10 items-center rounded-xl bg-[#0f4c7a] px-4 py-2.5 text-sm font-semibold text-white shadow-[0_12px_30px_rgba(15,76,122,0.2)] transition hover:bg-[#0a3557] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#0f4c7a]"
          >
            見積依頼はこちら
          </Link>
          <Link
            href="/login"
            className="inline-flex min-h-10 items-center rounded-xl border border-[#c7d6e5] bg-[#f8fbff] px-4 py-2.5 text-sm font-semibold text-[#0f172a] transition hover:border-[#a9c2d8] hover:bg-[#edf5fb] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#0f4c7a]"
          >
            作業者ログイン
          </Link>
        </div>
      </div>
    </main>
  )
}
