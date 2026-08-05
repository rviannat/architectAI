'use client'

import { useEffect, useState } from 'react'
import { projectsApi, analysesApi, type Project, type Analysis } from '@/lib/api'
import Link from 'next/link'
import { useRouter } from 'next/navigation'

const STATUS_COLOR: Record<string, string> = {
  PENDING:   'bg-yellow-100 text-yellow-800',
  CLONING:   'bg-blue-100 text-blue-800',
  ANALYZING: 'bg-purple-100 text-purple-800',
  COMPLETED: 'bg-green-100 text-green-800',
  FAILED:    'bg-red-100 text-red-800',
}

const ANALYSIS_TYPES = [
  'CODE_REVIEW',
  'ARCHITECTURE',
  'PERFORMANCE',
  'SECURITY',
  'CLOUD',
  'DATABASE',
  'DUE_DILIGENCE',
]

export default function ProjectPage({ params }: { params: { id: string } }) {
  const router = useRouter()
  const [project, setProject] = useState<Project | null>(null)
  const [analyses, setAnalyses] = useState<Analysis[]>([])
  const [type, setType] = useState('CODE_REVIEW')
  const [starting, setStarting] = useState(false)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    async function load() {
      try {
        const [p, as] = await Promise.all([
          projectsApi.get(params.id),
          analysesApi.listByProject(params.id).catch(() => [] as Analysis[]),
        ])
        setProject(p)
        setAnalyses(as)
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [params.id])

  async function startAnalysis() {
    setStarting(true)
    try {
      const a = await analysesApi.create(params.id, type)
      router.push(`/analyses/${a.id}`)
    } finally {
      setStarting(false)
    }
  }

  if (loading) return <div className="text-center py-16 text-slate-400">Carregando...</div>
  if (!project) return <div className="text-center py-16 text-red-500">Projeto não encontrado.</div>

  return (
    <div className="space-y-8">
      <div>
        <Link href="/" className="text-brand-600 text-sm hover:underline">← Dashboard</Link>
        <h1 className="text-2xl font-bold text-slate-900 mt-2 break-all">
          {project.repoUrl.replace('https://github.com/', '')}
        </h1>
        <p className="text-slate-500 text-sm mt-1">Branch: {project.defaultBranch}</p>
      </div>

      <div className="bg-white rounded-xl border p-6 shadow-sm">
        <h2 className="font-semibold text-slate-800 mb-4">Iniciar Nova Análise</h2>
        <div className="flex gap-3 items-end">
          <div className="flex-1">
            <label className="block text-xs text-slate-500 mb-1">Tipo</label>
            <select
              value={type}
              onChange={e => setType(e.target.value)}
              className="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
            >
              {ANALYSIS_TYPES.map(t => <option key={t}>{t}</option>)}
            </select>
          </div>
          <button
            onClick={startAnalysis}
            disabled={starting}
            className="bg-brand-600 hover:bg-brand-700 disabled:opacity-50 text-white px-5 py-2 rounded-lg text-sm font-medium transition-colors"
          >
            {starting ? 'Iniciando...' : '▶ Analisar'}
          </button>
        </div>
      </div>

      <section>
        <h2 className="text-lg font-semibold text-slate-800 mb-3">Análises ({analyses.length})</h2>
        {analyses.length === 0 ? (
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
                {analyses.map(a => (
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
    </div>
  )
}

