import { useCallback, useEffect, useState } from 'react'
import type { NavigationOptions } from './routing'
import { CreateTicketPage } from './pages/CreateTicketPage'
import { TicketDetailPage } from './pages/TicketDetailPage'
import { TicketListPage } from './pages/TicketListPage'
import { AppLink } from './components/AppLink'

interface BrowserLocation {
  pathname: string
  search: string
  state: Record<string, unknown> | null
}

function readLocation(): BrowserLocation {
  return {
    pathname: window.location.pathname,
    search: window.location.search,
    state: (window.history.state as Record<string, unknown> | null) ?? null,
  }
}

function listHref(search: string) {
  return `/tickets${search || '?page=0&size=20'}`
}

function App() {
  const [location, setLocation] = useState(readLocation)
  const [ticketsHref, setTicketsHref] = useState('/tickets?page=0&size=20')

  useEffect(() => {
    const handleNavigation = () => setLocation(readLocation())
    window.addEventListener('popstate', handleNavigation)
    return () => window.removeEventListener('popstate', handleNavigation)
  }, [])

  const navigate = useCallback((to: string, options?: NavigationOptions) => {
    const method = options?.replace ? 'replaceState' : 'pushState'
    window.history[method](options?.state ?? null, '', to)
    setLocation(readLocation())
  }, [])

  useEffect(() => {
    if (location.pathname === '/') {
      navigate('/tickets?page=0&size=20', { replace: true })
    }
  }, [location.pathname, navigate])

  useEffect(() => {
    if (location.pathname === '/tickets') {
      setTicketsHref(listHref(location.search))
    }
  }, [location.pathname, location.search])

  if (location.pathname === '/') return null

  let page
  if (location.pathname === '/tickets/new') {
    page = <CreateTicketPage navigate={navigate} ticketsHref={ticketsHref} />
  } else if (location.pathname === '/tickets') {
    page = <TicketListPage search={location.search} navigate={navigate} />
  } else {
    const detailMatch = location.pathname.match(/^\/tickets\/([^/]+)$/)
    page = detailMatch ? (
      <TicketDetailPage
        ticketId={decodeURIComponent(detailMatch[1])}
        navigate={navigate}
        navigationState={location.state}
        ticketsHref={ticketsHref}
      />
    ) : (
      <main className="app-shell state-page">
        <section className="state-message">
          <span className="state-code">404</span>
          <h1>Page not found</h1>
          <p>The page you requested does not exist in this workspace.</p>
          <button type="button" onClick={() => navigate(ticketsHref)}>
            Return to tickets
          </button>
        </section>
      </main>
    )
  }

  return (
    <div className="site-frame">
      <header className="site-header">
        <div className="site-header__inner">
          <AppLink to={ticketsHref} navigate={navigate} className="brand">
            <span className="brand-mark" aria-hidden="true">
              ST
            </span>
            <span>
              <strong>Support Desk</strong>
              <small>Ticket management</small>
            </span>
          </AppLink>
          <span className="workspace-label">Support workspace</span>
        </div>
      </header>
      {page}
    </div>
  )
}

export default App
