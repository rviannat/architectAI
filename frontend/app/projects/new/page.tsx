'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { projectsApi } from '@/lib/api'

export default function NewProjectPage() {
  const router = useRouter()
  const [repoUrl, setRepoUrl] = useState('')
  const [defaultBranch, setDefaultBranch] = useState('main')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const project = await projectsApi.create(repoUrl.trim(), defaultBranch.trim())
      router.push(`/projects/${project.id}`)
    } catch (err: unknown) {
      setError('Erro ao criar projeto. Verifique a URL e tente novamente.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-xl mx-auto">
      <h1 className="text-2xl font-bold text-slate-900 mb-6">Novo Projeto</h1>
      <div className="bg-white rounded-xl border p-6 shadow-sm">
        <form onSubmit={handleSubmit} className="space-y-5">
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">URL do Repositório</label>
            <input
              type="url"
              required
              placeholder="https://github.com/org/repo.git"
              value={repoUrl}
              onChange={e => setRepoUrl(e.target.value)}
              className="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Branch padrão</label>
            <input
              type="text"
              required
              placeholder="main"
              value={defaultBranch}
              onChange={e => setDefaultBranch(e.target.value)}
              className="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
          </div>
          {error && <p className="text-red-600 text-sm">{error}</p>}
          <button
            type="submit"
            disabled={loading}
            className="w-full bg-brand-600 hover:bg-brand-700 disabled:opacity-50 text-white py-2 rounded-lg text-sm font-medium transition-colors"
          >
            {loading ? 'Criando...' : 'Criar Projeto'}
          </button>
        </form>
      </div>
    </div>
  )
}

