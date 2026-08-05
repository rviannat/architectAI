'use client'

import { useEffect, useState } from 'react'
import { projectsApi, analysesApi, type Project, type Analysis } from '@/lib/api'
import Link from 'next/link'

const STATUS_COLOR: Record<string, string> = {
  PENDING:   'bg-yellow-100 text-yellow-800',
  CLONING:   'bg-blue-100 text-blue-800',
  ANALYZING: 'bg-purple-100 text-purple-800',
  COMPLETED: 'bg-green-100 text-green-800',
  FAILED:    'bg-red-100 text-red-800',
}

export default function Dashboard() {
  const [projects, setProjects] = useState<Project[]>([])
  const [recentAnalyses, setRecentAnalyses] = useState<Analysis[]>([])
  const [loading, setLoading] = useState(true)

  async function load() {
    try {
      const ps = await projectsApi.list().catch(() => [] as Project[])
      setProjects(ps)
      const all: Analysis[] = []
      for (const p of ps.slice(0, 5)) {
        const as = await analysesApi.listByProject(p.id).catch(() => [] as Analysis[])
        all.push(...as)
      }
      all.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
      setRecentAnalyses(all.slice(0, 10))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  async function handleDelete(id: string) {
    if (!confirm('Deletar este projeto e todas suas análises?')) return
    await projectsApi.delete(id)
    setProjects(prev => prev.filter(p => p.id !== id))
    setRecentAnalyses(prev => prev.filter(a => a.projectId !== id))
  }

  return (
    <div className="space-y-8">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Dashboard</h1>
          <p className="text-slate-500 mt-1">Análises de repositórios com IA especializada</p>
        </div>
        <Link
          href="/projects/new"
          className="bg-brand-600 hover:bg-brand-700 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors"
        >
          + Novo Projeto
        </Link>
      </div>

      {loading ? (
        <div className="text-center py-16 text-slate-400">Carregando...</div>
      ) : (
        <>
          <section>
            <h2 className="text-lg font-semibold text-slate-800 mb-3">Projetos ({projects.length})</h2>
            {projects.length === 0 ? (
              <div className="border-2 border-dashed rounded-xl p-12 text-center text-slate-400">
                Nenhum projeto ainda.{' '}
                <Link href="/projects/new" className="text-brand-600 hover:underline">Crie o primeiro</Link>.
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {projects.map(p => (
                  <Link
                    key={p.id}
                    href={`/projects/${p.id}`}
                    className="bg-white rounded-xl border p-4 hover:shadow-md transition-shadow relative group"
                  >
                    <button
                      onClick={e => { e.preventDefault(); handleDelete(p.id) }}
                      className="absolute top-2 right-2 text-slate-300 hover:text-red-500 opacity-0 group-hover:opacity-100 transition-opacity text-xs px-1"
                      title="Deletar projeto"
                    >✕</button>
                    <div className="font-medium text-slate-900 truncate pr-4">{p.repoUrl.replace('https://github.com/', '')}</div>
                    <div className="text-xs text-slate-500 mt-1">Branch: {p.defaultBranch}</div>
                    <div className="text-xs text-slate-400 mt-1">{new Date(p.createdAt).toLocaleDateString('pt-BR')}</div>
                  </Link>
                ))}
              </div>
            )}
          </section>

          <section>
            <h2 className="text-lg font-semibold text-slate-800 mb-3">Análises Recentes</h2>
            {recentAnalyses.length === 0 ? (
              <p className="text-slate-400 text-sm">Nenhuma análise ainda.</p>
            ) : (
              <div className="bg-white rounded-xl border overflow-hidden">
                <table className="w-full text-sm">
                  <thead className="bg-slate-50 text-slate-600">
                    <tr>
                      <th className="text-left px-4 py-3">ID</th>
                      <th className="text-left px-4 py-3">Tipo</th>
                      <th className="text-left px-4 py-3">Status</th>
                      <th className="text-left px-4 py-3">Findings</th>
                      <th className="text-left px-4 py-3">Data</th>
                      <th className="px-4 py-3">Ações</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y">
                    {recentAnalyses.map(a => (
                      <tr key={a.id} className="hover:bg-slate-50">
                        <td className="px-4 py-3 font-mono text-xs text-slate-500">{a.id.slice(0, 8)}…</td>
                        <td className="px-4 py-3">{a.type}</td>
                        <td className="px-4 py-3">
                          <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${STATUS_COLOR[a.status]}`}>
                            {a.status}
                          </span>
                        </td>
                        <td className="px-4 py-3">{a.findingsCount ?? '—'}</td>
                        <td className="px-4 py-3 text-slate-500">{new Date(a.createdAt).toLocaleDateString('pt-BR')}</td>
                        <td className="px-4 py-3">
                          <div className="flex items-center gap-3 justify-end">
                            <Link href={`/analyses/${a.id}`} className="text-brand-600 hover:underline text-xs">Ver</Link>
                            {a.status === 'COMPLETED' && (
                              <a
                                href={analysesApi.downloadUrl(a.id)}
                                target="_blank"
                                rel="noreferrer"
                                className="text-emerald-700 hover:underline text-xs font-medium"
                              >
                                PDF
                              </a>
                            )}
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        </>
      )}
    </div>
  )
}

