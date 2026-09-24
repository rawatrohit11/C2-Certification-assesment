import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { actionErrorMessage, listTickets } from '../api/tickets'
import { AppLink } from '../components/AppLink'
import type { Navigate } from '../routing'
import { statuses, type TicketPage } from '../types/ticket'

interface Props {
  search: string
  navigate: Navigate
}

export function TicketListPage({ search, navigate }: Props) {
  const parameters = useMemo(() => new URLSearchParams(search), [search])
  const [keyword, setKeyword] = useState(parameters.get('keyword') ?? '')
  const [page, setPage] = useState<TicketPage | null>(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [retry, setRetry] = useState(0)

  const activeKeyword = parameters.get('keyword') ?? ''
  const activeStatus = parameters.get('status') ?? ''

  useEffect(() => {
    setKeyword(activeKeyword)
  }, [activeKeyword])

  useEffect(() => {
    if (parameters.get('size') !== '20' || parameters.get('page') === null) {
      const canonical = new URLSearchParams(parameters)
      canonical.set('size', '20')
      if (canonical.get('page') === null) canonical.set('page', '0')
      navigate(`/tickets?${canonical.toString()}`, { replace: true })
      return
    }

    let active = true
    setLoading(true)
    setError('')
    listTickets(`?${parameters.toString()}`)
      .then((result) => {
        if (active) setPage(result)
      })
      .catch((requestError: unknown) => {
        if (active) {
          setPage(null)
          setError(actionErrorMessage(requestError))
        }
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [navigate, parameters, retry])

  function updateQuery(changes: Record<string, string | null>) {
    const next = new URLSearchParams(parameters)
    for (const [name, value] of Object.entries(changes)) {
      if (value) next.set(name, value)
      else next.delete(name)
    }
    next.set('size', '20')
    navigate(`/tickets?${next.toString()}`)
  }

  function submitSearch(event: FormEvent) {
    event.preventDefault()
    updateQuery({ keyword: keyword.trim() || null, page: '0' })
  }

  return (
    <main className="app-shell">
      <header className="page-header">
        <div>
          <p className="eyebrow">Ticket overview</p>
          <h1>Tickets</h1>
          <p className="page-description">
            Review, prioritize, and move support requests forward.
          </p>
        </div>
        <AppLink to="/tickets/new" navigate={navigate} className="button primary">
          <span aria-hidden="true">＋</span> Create ticket
        </AppLink>
      </header>

      <section className="panel filters" aria-label="Ticket filters">
        <div className="filter-heading">
          <div>
            <h2>Find tickets</h2>
            <p>Search by subject or narrow the queue by status.</p>
          </div>
          {(activeKeyword || activeStatus) && (
            <button
              className="button-link"
              type="button"
              onClick={() => updateQuery({ keyword: null, status: null, page: '0' })}
            >
              Clear filters
            </button>
          )}
        </div>
        <div className="filter-controls">
          <form onSubmit={submitSearch} className="search-form">
            <label>
              Search tickets
              <input
                value={keyword}
                onChange={(event) => setKeyword(event.target.value)}
                placeholder="Search title or description"
              />
            </label>
            <button type="submit">Search</button>
          </form>
          <label className="status-filter">
            Status
            <select
              value={activeStatus}
              onChange={(event) =>
                updateQuery({ status: event.target.value || null, page: '0' })
              }
            >
              <option value="">All statuses</option>
              {statuses.map((status) => (
                <option key={status} value={status}>
                  {status.replace('_', ' ')}
                </option>
              ))}
            </select>
          </label>
        </div>
      </section>

      {loading && (
        <section className="state-message" aria-live="polite">
          <span className="loading-indicator" aria-hidden="true" />
          <h2>Loading tickets…</h2>
          <p>Fetching the latest support queue.</p>
        </section>
      )}
      {!loading && error && (
        <section className="error-banner" role="alert">
          <span className="error-icon" aria-hidden="true">!</span>
          <div>
            <h2>Tickets could not be loaded</h2>
          <p>{error}</p>
          <button type="button" onClick={() => setRetry((value) => value + 1)}>
            Retry
          </button>
          </div>
        </section>
      )}
      {!loading && page && page.content.length === 0 && (
        <section className="state-message">
          <span className="empty-icon" aria-hidden="true">◎</span>
          <h2>
            {activeKeyword || activeStatus
              ? 'No tickets match the current search and filter.'
              : 'No tickets have been created.'}
          </h2>
          <p>
            {activeKeyword || activeStatus
              ? 'Adjust the filters above to broaden your results.'
              : 'Create the first request to start your support queue.'}
          </p>
        </section>
      )}

      {!loading && page && page.content.length > 0 && (
        <>
          <div className="list-summary">
            <h2>Support queue</h2>
            <p className="result-count">
              {page.totalElements} ticket{page.totalElements === 1 ? '' : 's'}
            </p>
          </div>
          <div className="ticket-list">
            {page.content.map((ticket) => (
              <article className="ticket-card" key={ticket.id}>
                <div className="ticket-card__content">
                  <AppLink
                    to={`/tickets/${ticket.id}`}
                    navigate={navigate}
                    className="ticket-title"
                  >
                    {ticket.title}
                  </AppLink>
                  <div className="ticket-meta">
                    <span>{ticket.assignee || 'Unassigned'}</span>
                    <span>Created {new Date(ticket.createdAt).toLocaleString()}</span>
                  </div>
                </div>
                <div className="badges">
                  <span className={`badge priority-${ticket.priority.toLowerCase()}`}>
                    {ticket.priority}
                  </span>
                  <span className={`badge status-${ticket.status.toLowerCase()}`}>
                    {ticket.status.replace('_', ' ')}
                  </span>
                </div>
              </article>
            ))}
          </div>
          <nav className="pagination" aria-label="Ticket pages">
            <button
              type="button"
              disabled={page.page === 0}
              onClick={() => updateQuery({ page: String(page.page - 1) })}
            >
              Previous
            </button>
            <span>
              Page {page.page + 1} of {Math.max(page.totalPages, 1)}
            </span>
            <button
              type="button"
              disabled={page.page + 1 >= page.totalPages}
              onClick={() => updateQuery({ page: String(page.page + 1) })}
            >
              Next
            </button>
          </nav>
        </>
      )}
    </main>
  )
}
