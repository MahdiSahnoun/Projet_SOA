import axios from 'axios'
import type { Demande, Don, PageResult, Session, Stock } from '../types'

const apiOrigin = (import.meta.env.VITE_API_URL || '').replace(/\/$/, '')
export const api = axios.create({ baseURL: apiOrigin ? `${apiOrigin}/api/v1` : '/api/v1', timeout: 12000, headers: { 'Content-Type': 'application/json' } })
api.interceptors.request.use((config) => {
  const session = readSession()
  if (session?.token) config.headers.Authorization = `Bearer ${session.token}`
  return config
})
api.interceptors.response.use((response) => response, (error) => {
  if (error.response?.status === 401) { localStorage.removeItem('hema-session'); window.location.assign('/login') }
  const detail = error.response?.data?.detail
  error.userMessage = detail || (error.response?.status === 409 ? 'Cette action entre en conflit avec l’état actuel.' : error.response?.status === 503 ? 'Le service est temporairement indisponible.' : error.response ? 'Une erreur est survenue. Vérifiez les informations saisies.' : 'Connexion impossible. Vérifiez que le serveur est accessible.')
  return Promise.reject(error)
})
export function readSession(): Session | null {
  try { const session = JSON.parse(localStorage.getItem('hema-session') || 'null'); if (session?.expiresAt && session.expiresAt <= Date.now()) { localStorage.removeItem('hema-session'); return null } return session } catch { return null }
}
export const authService = {
  async login(payload: { email: string; motDePasse: string }) { const { data } = await api.post('/auth/login', payload); const parts = data.accessToken.split('.')[1]; const claims = JSON.parse(atob(parts.replace(/-/g, '+').replace(/_/g, '/'))); const role = claims.role as 'DONNEUR'|'HOPITAL'|'BANQUE'; if (!['DONNEUR','HOPITAL','BANQUE'].includes(role)) throw new Error('Le rôle du compte est invalide.'); const session = { token: data.accessToken, expiresAt: Date.now() + data.expiresIn * 1000, id: claims.sub, role }; localStorage.setItem('hema-session', JSON.stringify(session)); return session },
  async signup(payload: Record<string, unknown>) { return (await api.post('/auth/signup', payload)).data },
}
export const donneurService = { get: async (id: string) => (await api.get(`/donneurs/${encodeURIComponent(id)}`)).data }
export const donService = { list: async () => { const { data } = await api.get<Don[] | PageResult<Don>>('/dons'); return Array.isArray(data) ? data : data.content }, get: async (id: string) => (await api.get<Don>(`/dons/${id}`)).data, create: async (codeBanque: string) => (await api.post<Don>('/dons', { codeBanque })).data }
export const demandeService = { list: async (page = 0, statut = '', urgence = '') => { const { data } = await api.get<PageResult<Demande>>('/demandes', { params: { page, size: 20, sort: 'dateDemande,desc', ...(statut && { statut }), ...(urgence && { urgence }) } }); return data }, get: async (id: string) => (await api.get<Demande>(`/demandes/${id}`)).data, create: async (payload: { groupeSanguin: string; quantite: number; urgence: string }) => (await api.post<Demande>('/demandes', payload)).data, cancel: async (id: string) => (await api.patch(`/demandes/${id}/annulation`)).data }
export const stockService = {
  list: async (groupeSanguin?: string, codeBanque?: string) => {
    const groupes = groupeSanguin
      ? [groupeSanguin]
      : ['A_POSITIF','A_NEGATIF','B_POSITIF','B_NEGATIF','AB_POSITIF','AB_NEGATIF','O_POSITIF','O_NEGATIF']

    const reponses = await Promise.all(
      groupes.map(async (groupe) => {
        const { data } = await api.get<{
          groupeSanguin: Stock['groupeSanguin']
          codeBanque?: string
          disponibles: number
        }>('/stocks', {
          params: {
            groupeSanguin: groupe,
            ...(codeBanque && { codeBanque }),
          },
        })

        return {
          groupeSanguin: data.groupeSanguin,
          codeBanque: data.codeBanque,
          quantiteDisponible: data.disponibles,
          nombrePoches: data.disponibles,
        } satisfies Stock
      })
    )

    return reponses
  },
}
export const pocheService = { create: async (payload: { groupeSanguin: string; volumeMl: number; dateCollecte: string; datePeremption: string }) => (await api.post('/poches', payload)).data }