import axios from 'axios'

const BASE = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'

const api = axios.create({ baseURL: BASE })

export interface Project {
  id: string
  repoUrl: string
  defaultBranch: string
  createdAt: string
}

export interface Finding {
  tool: string
  id: string
  type: string
  severity: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW'
  file: string
  line: number
  message: string
  evidence: string
  tags: string[]
}

export interface Analysis {
  id: string
  projectId: string
  type: string
  status: 'PENDING' | 'CLONING' | 'ANALYZING' | 'COMPLETED' | 'FAILED'
  createdAt: string
  startedAt?: string
  finishedAt?: string
  findingsCount?: number
  reportUrl?: string
  errorMessage?: string
  findings: Finding[]
  staticAnalysisFindingsByTool: Record<string, number>
  estimatedCostRs?: number
}

export interface ApiResponse<T> {
  status: number
  message: string
  data: T
}

export const projectsApi = {
  create: (repoUrl: string, defaultBranch: string) =>
    api.post<ApiResponse<Project>>('/api/v1/projects', { repoUrl, defaultBranch }).then(r => r.data.data),

  get: (id: string) =>
    api.get<ApiResponse<Project>>(`/api/v1/projects/${id}`).then(r => r.data.data),

  list: () =>
    api.get<ApiResponse<Project[]>>('/api/v1/projects').then(r => r.data.data),
}

export const analysesApi = {
  create: (projectId: string, type: string) =>
    api.post<ApiResponse<Analysis>>(`/api/v1/projects/${projectId}/analyses`, { type }).then(r => r.data.data),

  get: (id: string) =>
    api.get<ApiResponse<Analysis>>(`/api/v1/analyses/${id}`).then(r => r.data.data),

  listByProject: (projectId: string) =>
    api.get<ApiResponse<Analysis[]>>(`/api/v1/projects/${projectId}/analyses`).then(r => r.data.data),

  downloadUrl: (id: string) =>
    `${BASE}/api/v1/analyses/${id}/report/download`,
}

