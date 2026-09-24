import { useEffect, useMemo, useRef, useState, type FormEvent } from 'react'
import {
  ApiError,
  actionErrorMessage,
  addComment,
  fieldErrors,
  getTicket,
  transitionTicket,
  updateTicket,
} from '../api/tickets'
import { AppLink } from '../components/AppLink'
import {
  TicketFieldsForm,
  validateTicketFields,
} from '../components/TicketFieldsForm'
import type { Navigate } from '../routing'
import type {
  TicketDetail,
  TicketFields,
  TicketStatus,
} from '../types/ticket'

const transitions: Record<
  TicketStatus,
  Array<{ target: TicketStatus; label: string; confirm?: string }>
> = {
  OPEN: [
    { target: 'IN_PROGRESS', label: 'Start progress' },
    {
      target: 'CANCELLED',
      label: 'Cancel ticket',
      confirm: 'Cancel this ticket?',
    },
  ],
  IN_PROGRESS: [
    { target: 'RESOLVED', label: 'Resolve ticket' },
    {
      target: 'CANCELLED',
      label: 'Cancel ticket',
      confirm: 'Cancel this ticket?',
    },
  ],
  RESOLVED: [
    {
      target: 'CLOSED',
      label: 'Close ticket',
      confirm: 'Close this ticket?',
    },
  ],
  CLOSED: [],
  CANCELLED: [],
}

function editableFields(ticket: TicketDetail): TicketFields {
  return {
    title: ticket.title,
    description: ticket.description,
    priority: ticket.priority,
    assignee: ticket.assignee ?? '',
  }
}

interface Props {
  ticketId: string
  navigate: Navigate
  navigationState: Record<string, unknown> | null
  ticketsHref: string
}

export function TicketDetailPage({
  ticketId,
  navigate,
  navigationState,
  ticketsHref,
}: Props) {
  const [ticket, setTicket] = useState<TicketDetail | null>(null)
  const [draft, setDraft] = useState<TicketFields | null>(null)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [notFound, setNotFound] = useState(false)
  const [reload, setReload] = useState(0)
  const [editErrors, setEditErrors] = useState<Record<string, string>>({})
  const [editError, setEditError] = useState('')
  const [saving, setSaving] = useState(false)
  const [commentBody, setCommentBody] = useState('')
  const [commentError, setCommentError] = useState('')
  const [commenting, setCommenting] = useState(false)
  const [transitionError, setTransitionError] = useState('')
  const [transitioning, setTransitioning] = useState(false)
  const [announcement, setAnnouncement] = useState('')
  const editFormRef = useRef<HTMLFormElement>(null)
  const ticketRef = useRef<TicketDetail | null>(null)
  ticketRef.current = ticket

  useEffect(() => {
    if (typeof navigationState?.flash !== 'string') return
    const frame = requestAnimationFrame(() => {
      setAnnouncement(navigationState.flash as string)
    })
    return () => cancelAnimationFrame(frame)
  }, [navigationState])

  useEffect(() => {
    let active = true
    setLoading(true)
    setLoadError('')
    setNotFound(false)
    getTicket(ticketId)
      .then((loaded) => {
        if (!active) return
        setTicket(loaded)
        setDraft(editableFields(loaded))
      })
      .catch((error: unknown) => {
        if (!active) return
        if (error instanceof ApiError && error.problem.code === 'TICKET_NOT_FOUND') {
          setNotFound(true)
        } else {
          setLoadError(actionErrorMessage(error))
        }
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [ticketId, reload])

  const dirty = useMemo(() => {
    if (!ticket || !draft) return false
    const current = editableFields(ticket)
    return (Object.keys(current) as Array<keyof TicketFields>).some(
      (field) => current[field] !== draft[field],
    )
  }, [draft, ticket])

  async function save(event: FormEvent) {
    event.preventDefault()
    if (!ticket || !draft || !dirty) return
    const clientErrors = validateTicketFields(draft)
    if (Object.keys(clientErrors).length > 0) {
      setEditErrors(clientErrors)
      requestAnimationFrame(() =>
        editFormRef.current
          ?.querySelector<HTMLElement>('[aria-invalid="true"]')
          ?.focus(),
      )
      return
    }

    const current = editableFields(ticket)
    const patch = Object.fromEntries(
      (Object.keys(current) as Array<keyof TicketFields>)
        .filter((field) => current[field] !== draft[field])
        .map((field) => [field, draft[field]]),
    ) as Partial<TicketFields>

    setSaving(true)
    setEditErrors({})
    setEditError('')
    try {
      const updated = await updateTicket(ticket.id, patch)
      setTicket(updated)
      setDraft(editableFields(updated))
      setAnnouncement('Ticket updated successfully.')
    } catch (error) {
      setEditErrors(fieldErrors(error))
      setEditError(actionErrorMessage(error))
    } finally {
      setSaving(false)
    }
  }

  async function submitComment(event: FormEvent) {
    event.preventDefault()
    if (!ticket) return
    const normalized = commentBody.trim()
    if (!normalized) {
      setCommentError('Comment is required.')
      return
    }
    if (normalized.length > 2000) {
      setCommentError('Comment must be at most 2000 characters.')
      return
    }

    setCommenting(true)
    setCommentError('')
    try {
      const comment = await addComment(ticket.id, commentBody)
      setTicket({ ...ticket, comments: [...ticket.comments, comment] })
      setCommentBody('')
      setAnnouncement('Comment added successfully.')
    } catch (error) {
      setCommentError(actionErrorMessage(error))
    } finally {
      setCommenting(false)
    }
  }

  async function transition(
    target: TicketStatus,
    confirmation?: string,
  ) {
    if (!ticket || (confirmation && !window.confirm(confirmation))) return
    setTransitioning(true)
    setTransitionError('')
    try {
      const updated = await transitionTicket(ticket.id, target)
      setTicket(updated)
      setDraft(editableFields(updated))
      setAnnouncement(`Ticket status changed to ${target.replace('_', ' ')}.`)
    } catch (error) {
      setTransitionError(actionErrorMessage(error))
      if (
        error instanceof ApiError &&
        error.problem.code === 'INVALID_STATUS_TRANSITION'
      ) {
        try {
          const loaded = await getTicket(ticketId)
          const previous = ticketRef.current
          setTicket(loaded)
          setDraft((currentDraft) => {
            if (!currentDraft || !previous) return editableFields(loaded)
            const baseline = editableFields(previous)
            const hasUnsavedEdits = (
              Object.keys(baseline) as Array<keyof TicketFields>
            ).some((field) => baseline[field] !== currentDraft[field])
            return hasUnsavedEdits ? currentDraft : editableFields(loaded)
          })
        } catch {
          setReload((value) => value + 1)
        }
      }
    } finally {
      setTransitioning(false)
    }
  }

  if (loading) {
    return (
      <main className="app-shell state-page">
        <section className="state-message" aria-live="polite">
          <span className="loading-indicator" aria-hidden="true" />
          <h1>Loading ticket…</h1>
          <p>Retrieving the latest ticket details.</p>
        </section>
      </main>
    )
  }

  if (notFound) {
    return (
      <main className="app-shell state-page">
        <section className="state-message">
          <span className="state-code">404</span>
          <h1>Ticket not found</h1>
          <p>This ticket may have been removed or the address may be incorrect.</p>
          <AppLink to={ticketsHref} navigate={navigate} className="button primary">
            Return to tickets
          </AppLink>
        </section>
      </main>
    )
  }

  if (loadError || !ticket || !draft) {
    return (
      <main className="app-shell state-page">
        <section className="error-banner" role="alert">
          <span className="error-icon" aria-hidden="true">!</span>
          <div>
            <h1>Ticket could not be loaded</h1>
            <p>{loadError || 'The ticket could not be loaded.'}</p>
            <div className="button-row">
              <button type="button" onClick={() => setReload((value) => value + 1)}>
                Retry
              </button>
              <AppLink to={ticketsHref} navigate={navigate} className="button">
                Return to tickets
              </AppLink>
            </div>
          </div>
        </section>
      </main>
    )
  }

  return (
    <main className="app-shell">
      <AppLink to={ticketsHref} navigate={navigate} className="back-link">
        <span>←</span> Back to tickets
      </AppLink>
      <header className="detail-header">
        <div>
          <p className="eyebrow">Ticket details</p>
          <h1>{ticket.title}</h1>
          <p className="ticket-identifier">ID {ticket.id}</p>
        </div>
        <span className={`badge status-${ticket.status.toLowerCase()}`}>
          {ticket.status.replace('_', ' ')}
        </span>
      </header>

      <div className="detail-layout">
        <div className="detail-main">
          <section className="panel form-panel">
            <div className="section-heading">
              <div>
                <p className="section-kicker">Request</p>
                <h2>Edit ticket</h2>
              </div>
              <span className="dirty-indicator">{dirty ? 'Unsaved changes' : 'Up to date'}</span>
            </div>
            <form ref={editFormRef} onSubmit={save}>
              <TicketFieldsForm
                value={draft}
                errors={editErrors}
                disabled={saving}
                onChange={setDraft}
              />
              {editError && (
                <div className="error-banner compact" role="alert">
                  <span className="error-icon" aria-hidden="true">!</span>
                  <p>{editError}</p>
                </div>
              )}
              <div className="form-actions">
                <button className="primary" disabled={!dirty || saving} type="submit">
                  {saving ? 'Saving…' : 'Save changes'}
                </button>
              </div>
            </form>
          </section>

          <section className="panel comments-panel">
            <div className="section-heading">
              <div>
                <p className="section-kicker">Conversation</p>
                <h2>Comments</h2>
              </div>
              <span className="count-badge">{ticket.comments.length}</span>
            </div>
            {ticket.comments.length === 0 ? (
              <div className="inline-empty">
                <p>No comments yet.</p>
                <span>Start the conversation with an update for the team.</span>
              </div>
            ) : (
              <ol className="comment-list">
                {ticket.comments.map((comment) => (
                  <li key={comment.id}>
                    <span className="comment-avatar" aria-hidden="true">S</span>
                    <div>
                      <p>{comment.body}</p>
                      <time dateTime={comment.createdAt}>
                        {new Date(comment.createdAt).toLocaleString()}
                      </time>
                    </div>
                  </li>
                ))}
              </ol>
            )}
            <form onSubmit={submitComment} className="comment-form">
              <label>
                Add comment
                <textarea
                  value={commentBody}
                  onChange={(event) => setCommentBody(event.target.value)}
                  maxLength={2000}
                  rows={4}
                  disabled={commenting}
                  placeholder="Share an update or add investigation notes"
                  aria-invalid={Boolean(commentError)}
                  aria-describedby={commentError ? 'comment-error' : undefined}
                />
              </label>
              {commentError && (
                <p id="comment-error" className="field-error" role="alert">
                  {commentError}
                </p>
              )}
              <div className="form-actions">
                <button className="primary" type="submit" disabled={commenting}>
                  {commenting ? 'Adding…' : 'Add comment'}
                </button>
              </div>
            </form>
          </section>
        </div>

        <aside className="detail-sidebar">
          <section className="panel">
            <p className="section-kicker">Workflow</p>
            <h2>Status actions</h2>
            {transitions[ticket.status].length === 0 ? (
              <p className="muted-copy">No further status transitions are available.</p>
            ) : (
              <div className="action-stack">
                {transitions[ticket.status].map((action) => (
                  <button
                    key={action.target}
                    type="button"
                    disabled={transitioning}
                    onClick={() => transition(action.target, action.confirm)}
                  >
                    {action.label}
                  </button>
                ))}
              </div>
            )}
            {transitionError && (
              <p className="field-error" role="alert">
                {transitionError}
              </p>
            )}
          </section>

          <section className="metadata panel" aria-label="Ticket metadata">
            <p className="section-kicker">Timeline</p>
            <dl>
              <div>
                <dt>Created</dt>
                <dd>{new Date(ticket.createdAt).toLocaleString()}</dd>
              </div>
              <div>
                <dt>Last updated</dt>
                <dd>{new Date(ticket.updatedAt).toLocaleString()}</dd>
              </div>
            </dl>
          </section>
        </aside>
      </div>

      <div className="sr-only" aria-live="polite">
        {announcement}
      </div>
    </main>
  )
}
