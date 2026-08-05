import type { Metadata } from 'next'
import './globals.css'

export const metadata: Metadata = {
  title: 'ArchitectAI',
  description: 'Plataforma de análise de repositórios com IA',
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="pt-BR">
      <body>
        <header className="border-b bg-white shadow-sm">
          <div className="max-w-6xl mx-auto px-4 py-3 flex items-center gap-3">
            <span className="text-brand-700 font-bold text-xl tracking-tight">🏗 ArchitectAI</span>
            <nav className="ml-8 flex gap-6 text-sm text-slate-600">
              <a href="/" className="hover:text-brand-600 transition-colors">Dashboard</a>
              <a href="/projects/new" className="hover:text-brand-600 transition-colors">+ Analisar Repositório</a>
            </nav>
          </div>
        </header>
        <main className="max-w-6xl mx-auto px-4 py-8">{children}</main>
      </body>
    </html>
  )
}

