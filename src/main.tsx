import React, { useEffect } from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter, Navigate, Outlet, Route, Routes, useLocation } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { Toaster } from 'sonner'
import { Activity, ArrowLeft, LockKeyhole } from 'lucide-react'
import { AppShell } from './layouts/AppShell'
import { AuthPage } from './pages/AuthPage'
import { WorkspacePage } from './pages/WorkspacePage'
import { readSession } from './services/api'
import './styles.css'

const queryClient = new QueryClient({ defaultOptions: { queries: { staleTime: 30_000, retry: 1, refetchOnWindowFocus: false } } })
function Guard({ role }: { role?: 'DONNEUR'|'HOPITAL'|'BANQUE' }) { const session=readSession(); const location=useLocation(); useEffect(()=>{if(!session)return;const timeout=window.setTimeout(()=>{localStorage.removeItem('hema-session');window.location.assign('/login')},Math.max(session.expiresAt-Date.now(),0));return()=>window.clearTimeout(timeout)},[session?.expiresAt]); if(!session)return <Navigate to="/login" replace state={{from:location.pathname}}/>; if(role&&session.role!==role)return <Navigate to="/403" replace/>; return <Outlet/> }
function ErrorPage({ forbidden=false }: { forbidden?: boolean }) { return <main className="error-page"><div className="error-brand"><div className="brand-mark"><Activity/></div>hé<strong>ma</strong></div><div className="error-card"><span className="error-icon"><LockKeyhole/></span><span className="eyebrow">{forbidden?'ACCÈS RESTREINT':'PAGE INTROUVABLE'}</span><h1>{forbidden?'Vous n’avez pas accès à cet espace':'Cette page est introuvable'}</h1><p>{forbidden?'Votre compte ne dispose pas du rôle nécessaire pour consulter cette page.':'Le lien est peut-être incorrect ou la page a été déplacée.'}</p><a className="primary-button" href="/login"><ArrowLeft/>Retour à la connexion</a></div></main> }
function PublicAuth({signup=false}:{signup?:boolean}){return readSession()?<Navigate to={`/${readSession()!.role.toLowerCase()}/dashboard`} replace/>:<AuthPage signup={signup}/>}
function App(){return <QueryClientProvider client={queryClient}><BrowserRouter><Routes><Route path="/login" element={<PublicAuth/>}/><Route path="/inscription" element={<PublicAuth signup/>}/><Route path="/403" element={<ErrorPage forbidden/>}/><Route element={<Guard/>}><Route element={<AppShell/>}><Route element={<Guard role="DONNEUR"/>}><Route path="/donneur/*" element={<WorkspacePage/>}/></Route><Route element={<Guard role="HOPITAL"/>}><Route path="/hopital/*" element={<WorkspacePage/>}/></Route><Route element={<Guard role="BANQUE"/>}><Route path="/banque/*" element={<WorkspacePage/>}/></Route></Route></Route><Route path="/" element={<Navigate to={readSession()?`/${readSession()!.role.toLowerCase()}/dashboard`:'/login'} replace/>}/><Route path="*" element={<ErrorPage/>}/></Routes><Toaster position="top-right" richColors closeButton/></BrowserRouter></QueryClientProvider>}

ReactDOM.createRoot(document.getElementById('root')!).render(<React.StrictMode><App/></React.StrictMode>)