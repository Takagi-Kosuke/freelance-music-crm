import Link from 'next/link'

export default function Home() {
  return (
    <main className="min-h-screen px-4 py-10">
      <div className="mx-auto flex max-w-4xl flex-col gap-8 rounded-2xl border border-[#E5E7EB] bg-white p-8 shadow-sm md:p-10">
        <header className="space-y-3">
          <p className="inline-flex rounded-full bg-[#EAF1F6] px-3 py-1 text-xs font-semibold tracking-wide text-[#0B4F6C]">
            FMC
          </p>
          <h1 className="text-3xl font-bold tracking-tight text-[#1F271B] md:text-4xl">
            音楽制作の見積依頼と進行管理を、
            <br className="hidden md:block" />
            ひとつの画面で。
          </h1>
          <p className="text-sm leading-7 text-[#384236] md:text-base">
            クライアントの方は見積依頼フォームからすぐに依頼を送信できます。作業者の方はログインしてダッシュボードから管理してください。
          </p>
        </header>

        <div className="flex flex-wrap items-center gap-3">
          <Link
            href="/quote"
            className="inline-flex min-h-12 items-center rounded-xl bg-[#145C9E] px-5 py-3 text-sm font-semibold text-white transition hover:bg-[#0B4F6C] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#145C9E]"
          >
            見積依頼はこちら
          </Link>
          <Link
            href="/login"
            className="inline-flex min-h-12 items-center rounded-xl border border-[#CBB9A8] bg-white px-5 py-3 text-sm font-semibold text-[#1F271B] transition hover:bg-[#EFE4DB] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#145C9E]"
          >
            作業者ログイン
          </Link>
        </div>
      </div>
    </main>
  )
}
