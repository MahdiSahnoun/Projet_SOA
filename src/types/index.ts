export type Role = 'DONNEUR' | 'HOPITAL' | 'BANQUE'
export type BloodType = 'A_POSITIF' | 'A_NEGATIF' | 'B_POSITIF' | 'B_NEGATIF' | 'AB_POSITIF' | 'AB_NEGATIF' | 'O_POSITIF' | 'O_NEGATIF'
export type Status = 'EN_ATTENTE' | 'ACCEPTEE' | 'REFUSEE' | 'ANNULEE'
export type Urgency = 'NORMALE' | 'URGENTE' | 'VITALE'
export interface Session { token: string; expiresAt: number; id: string; role: Role }
export interface Don { id: number | string; groupeSanguin: BloodType; codeBanque?: string; dateDon?: string; statut: Status; motif?: string; numeroEnregistrement?: string }
export interface Demande { id: number | string; groupeSanguin: BloodType; quantite: number; urgence: Urgency; statut: Status; dateDemande?: string; allocations?: { codeBanque: string; quantite: number }[] }
export interface Stock { groupeSanguin: BloodType; codeBanque?: string; quantiteDisponible?: number; nombrePoches?: number }
export interface PageResult<T> { content: T[]; number: number; size: number; totalElements: number; totalPages: number; first: boolean; last: boolean; empty: boolean }
export const bloodTypes: BloodType[] = ['A_POSITIF','A_NEGATIF','B_POSITIF','B_NEGATIF','AB_POSITIF','AB_NEGATIF','O_POSITIF','O_NEGATIF']
export const bloodLabels: Record<BloodType,string> = { A_POSITIF:'A+', A_NEGATIF:'A−', B_POSITIF:'B+', B_NEGATIF:'B−', AB_POSITIF:'AB+', AB_NEGATIF:'AB−', O_POSITIF:'O+', O_NEGATIF:'O−' }
export const statusLabels: Record<Status,string> = { EN_ATTENTE:'En attente', ACCEPTEE:'Acceptée', REFUSEE:'Refusée', ANNULEE:'Annulée' }