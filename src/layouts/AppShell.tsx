import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { Activity, Bell, ChevronDown, CircleHelp, Droplet, LayoutDashboard, LogOut, Menu, Package, Plus, UserRound, X, HeartPulse, ClipboardList } from 'lucide-react'
import { useState } from 'react'
import { readSession } from '../services/api'
import type { Role } from '../types'

const links = {
  DONNEUR: [{ label:'Vue d’ensemble', path:'/donneur/dashboard', icon:LayoutDashboard },{ label:'Mon profil',path:'/donneur/profil',icon:UserRound },{ label:'Historique des dons',path:'/donneur/dons',icon:HeartPulse },{ label:'Proposer un don',path:'/donneur/dons/nouveau',icon:Plus }],
  HOPITAL: [{ label:'Vue d’ensemble',path:'/hopital/dashboard',icon:LayoutDashboard },{ label:'Demandes de sang',path:'/hopital/demandes',icon:ClipboardList },{ label:'Créer une demande',path:'/hopital/demandes/nouvelle',icon:Plus },{ label:'Stocks disponibles',path:'/hopital/stocks',icon:Package }],
  BANQUE: [{ label:'Vue d’ensemble',path:'/banque/dashboard',icon:LayoutDashboard },{ label:'Gestion des stocks',path:'/banque/stocks',icon:Package },{ label:'Ajouter une poche',path:'/banque/poches/nouvelle',icon:Plus }],
}
const roleNames = { DONNEUR:'Espace donneur', HOPITAL:'Espace hospitalier', BANQUE:'Espace banque de sang' }
export function AppShell() {
  const session = readSession(); const role: Role = session?.role || 'DONNEUR'; const [mobileOpen,setMobileOpen] = useState(false); const navigate = useNavigate()
  const logout = () => { localStorage.removeItem('hema-session'); navigate('/login') }
  return <div className="app-shell">
    {mobileOpen && <button className="mobile-scrim" aria-label="Fermer le menu" onClick={() => setMobileOpen(false)} />}
    <aside className={`sidebar ${mobileOpen ? 'sidebar-open' : ''}`}>
      <div className="brand"><div className="brand-mark"><Droplet fill="currentColor" /></div><span>hé<strong>ma</strong></span><button className="icon-button sidebar-close" onClick={() => setMobileOpen(false)} aria-label="Fermer"><X /></button></div>
      <div className="sidebar-caption">ESPACE DE TRAVAIL</div><div className="role-switch"><span className="role-dot" />{roleNames[role]}<ChevronDown /></div>
      <nav className="side-nav" aria-label="Navigation principale">{links[role].map(({label,path,icon:Icon}) => <NavLink key={path} to={path} onClick={() => setMobileOpen(false)} className={({isActive}) => `nav-link ${isActive ? 'active' : ''}`}><Icon />{label}</NavLink>)}</nav>
      <div className="sidebar-bottom"><div className="help-card"><div className="help-icon"><CircleHelp /></div><div><strong>Besoin d’aide ?</strong><span>Notre équipe vous accompagne</span></div><ChevronDown /></div><button className="profile-menu" onClick={logout}><div className="avatar">{role === 'HOPITAL' ? 'CH' : role === 'BANQUE' ? 'BS' : 'JD'}</div><div className="profile-copy"><strong>{role === 'HOPITAL' ? 'Centre hospitalier' : role === 'BANQUE' ? 'Banque de sang' : 'Mon espace'}</strong><span>{roleNames[role]}</span></div><LogOut className="logout-icon" /></button></div>
    </aside>
    <section className="main-column"><header className="topbar"><button className="icon-button menu-toggle" onClick={() => setMobileOpen(true)} aria-label="Ouvrir le menu"><Menu /></button><div className="breadcrumbs"><span>Héma</span><span className="crumb-slash">/</span><strong>{roleNames[role]}</strong></div><div className="top-actions"><span className="live-status"><i /> Service actif</span><button className="icon-button notification" aria-label="Notifications"><Bell /><i /></button><div className="top-avatar">{role === 'HOPITAL' ? 'CH' : role === 'BANQUE' ? 'BS' : 'JD'}</div></div></header><main className="page-content"><Outlet /></main><footer className="app-footer"><span>© 2026 Héma · La vie, au cœur du lien.</span><span><Activity /> Coordination médicale sécurisée</span></footer></section>
  </div>
}