'use client'

import { useEffect, useRef, useState } from 'react'
import { analysesApi, type Analysis } from '@/lib/api'
import Link from 'next/link'

const STATUS_COLOR: Record<string, string> = {
  PENDING:   'bg-yellow-100 text-yellow-800',
  CLONING:   'bg-blue-100 text-blue-800',
  ANALYZING: 'bg-purple-100 text-purple-800',
  COMPLETED: 'bg-green-100 text-green-800',
  FAILED:    'bg-red-100 text-red-800',
}

const SEV_COLOR: Record<string, string> = {
  CRITICAL: 'bg-red-100 text-red-800',
  HIGH:     'bg-orange-100 text-orange-800',
  MEDIUM:   'bg-yellow-100 text-yellow-800',
  LOW:      'bg-blue-100 text-blue-800',
}

const IN_PROGRESS = new Set(['PENDING', 'CLONING', 'ANALYZING'])

export default function AnalysisPage({ params }: { params: { id: string } }) {
  const [analysis, setAnalysis] = useState<Analysis | null>(null)
  const [loading, setLoading] = useState(true)
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null)

  async function fetchAnalysis() {
    const a = await analysesApi.get(params.id).catch(() => null)
    if (a) setAnalysis(a)
    if (a && !IN_PROGRESS.has(a.status)) {
      if (intervalRef.current) clearInterval(intervalRef.current)
    }
  }

  useEffect(() => {
    fetchAnalysis().finally(() => setLoading(false))
    intervalRef.current = setInterval(fetchAnalysis, 5000)
    return () => { if (intervalRef.current) clearInterval(intervalRef.current) }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [params.id])

  if (loading) return <div className="text-center py-16 text-slate-400">Carregando...</div>
  if (!analysis) return <div className="text-center py-16 text-red-500">Análise não encontrada.</div>

  const inProgress = IN_PROGRESS.has(analysis.status)

  return (
    <div className="space-y-8">
      <div>
        <Link href={`/projects/${analysis.projectId}`} className="text-brand-600 text-sm hover:underline">
          ← Projeto
        </Link>
        <div className="flex items-center gap-4 mt-2">
          <h1 className="text-2xl font-bold text-slate-900">Análise</h1>
          <span className={`px-3 py-1 rounded-full text-sm font-medium ${STATUS_COLOR[analysis.status]}`}>
            {analysis.status}
          </span>
        </div>
        <p className="text-slate-500 text-sm mt-1 font-mono">{analysis.id}</p>
      </div>

      {/* Status card */}
      <div className="bg-white rounded-xl border p-6 shadow-sm grid grid-cols-2 md:grid-cols-4 gap-4">
        <Stat label="Tipo" value={analysis.type} />
        <Stat label="Findings" value={analysis.findingsCount?.toString() ?? (inProgress ? '…' : '0')} />
        <Stat label="Custo estimado" value={analysis.estimatedCostRs ? `R$ ${(analysis.estimatedCostRs / 100).toFixed(2)}` : '—'} />
        <Stat label="Duração" value={analysis.finishedAt && analysis.startedAt
          ? `${Math.round((new Date(analysis.finishedAt).getTime() - new Date(analysis.startedAt).getTime()) / 1000)}s`
          : inProgress ? '⏳' : '—'} />
      </div>

      {/* Progresso */}
      {inProgress && (
        <div className="bg-blue-50 rounded-xl border border-blue-100 p-6 text-center">
          <div className="animate-pulse text-blue-600 font-medium text-lg">Pipeline em execução…</div>
          <p className="text-blue-400 text-sm mt-1">A página atualiza automaticamente a cada 5 segundos.</p>
        </div>
      )}

      {/* Erro */}
      {analysis.status === 'FAILED' && (
        <div className="bg-red-50 rounded-xl border border-red-200 p-6">
          <p className="text-red-700 font-medium">Falha na análise</p>
          <p className="text-red-500 text-sm mt-1">{analysis.errorMessage}</p>
        </div>
      )}

      {/* Download PDF */}
      {analysis.status === 'COMPLETED' && (
        <div className="flex gap-4">
          <a
            href={analysesApi.downloadUrl(analysis.id)}
            download
            className="bg-brand-600 hover:bg-brand-700 text-white px-5 py-2.5 rounded-lg text-sm font-medium transition-colors flex items-center gap-2"
          >
            📄 Download PDF
          </a>
        </div>
      )}

      {/* Static analysis por ferramenta */}
      {Object.keys(analysis.staticAnalysisFindingsByTool || {}).length > 0 && (
        <section>
          <h2 className="text-lg font-semibold text-slate-800 mb-3">Análise Estática por Ferramenta</h2>
          <div className="flex flex-wrap gap-3">
            {Object.entries(analysis.staticAnalysisFindingsByTool).map(([tool, count]) => (
              <div key={tool} className="bg-white border rounded-lg px-4 py-3 text-center min-w-[100px]">
                <div className="text-2xl font-bold text-slate-900">{count}</div>
                <div className="text-xs text-slate-500 mt-1 uppercase">{tool}</div>
              </div>
            ))}
          </div>
        </section>
      )}

      {/* Findings */}
      {(analysis.findings || []).length > 0 && (
        <section>
          <h2 className="text-lg font-semibold text-slate-800 mb-3">
            Findings ({analysis.findings.length})
          </h2>
          <div className="space-y-2">
            {analysis.findings.map((f, i) => (
              <div key={i} className="bg-white border rounded-lg p-4 text-sm">
                <div className="flex items-center gap-3 flex-wrap">
                  <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${SEV_COLOR[f.severity]}`}>
                    {f.severity}
                  </span>
                  <span className="text-slate-500 text-xs uppercase">{f.tool}</span>
                  <span className="font-medium text-slate-800">{f.type}</span>
                </div>
                <p className="text-slate-700 mt-2">{f.message}</p>
                {f.file && (
                  <p className="text-slate-400 text-xs mt-1 font-mono">
                    {f.file}{f.line ? `:${f.line}` : ''}
                  </p>
                )}
              </div>
            ))}
          </div>
        </section>
      )}
    </div>
  )
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="text-center">
      <div className="text-2xl font-bold text-slate-900">{value}</div>
      <div className="text-xs text-slate-500 mt-1">{label}</div>
    </div>
  )
}

