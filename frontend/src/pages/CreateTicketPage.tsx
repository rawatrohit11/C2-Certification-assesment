import { useRef, useState, type FormEvent } from 'react'
import {
  actionErrorMessage,
  createTicket,
  fieldErrors,
} from '../api/tickets'
import { AppLink } from '../components/AppLink'
import {
  TicketFieldsForm,
  validateTicketFields,
} from '../components/TicketFieldsForm'
import type { Navigate } from '../routing'
import type { TicketFields } from '../types/ticket'

const initialFields: TicketFields = {
  title: '',
  description: '',
  priority: 'MEDIUM',
  assignee: '',
}

export function CreateTicketPage({
  navigate,
  ticketsHref,
}: {
  navigate: Navigate
  ticketsHref: string
}) {
  const [fields, setFields] = useState(initialFields)
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [actionError, setActionError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const formRef = useRef<HTMLFormElement>(null)

  function focusFirstError() {
    requestAnimationFrame(() => {
      formRef.current
        ?.querySelector<HTMLElement>('[aria-invalid="true"]')
        ?.focus()
    })
  }

  async function submit(event: FormEvent) {
    event.preventDefault()
    const clientErrors = validateTicketFields(fields)
    if (Object.keys(clientErrors).length > 0) {
      setErrors(clientErrors)
      focusFirstError()
      return
    }

    setSubmitting(true)
    setErrors({})
    setActionError('')
    try {
      const created = await createTicket(fields)
      navigate(`/tickets/${created.id}`, {
        state: { flash: 'Ticket created successfully.' },
      })
    } catch (error) {
      const backendErrors = fieldErrors(error)
      setErrors(backendErrors)
      setActionError(actionErrorMessage(error))
      if (Object.keys(backendErrors).length > 0) focusFirstError()
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="app-shell narrow">
      <AppLink to={ticketsHref} navigate={navigate} className="back-link">
        <span>←</span> Back to tickets
      </AppLink>
      <header className="page-header form-page-header">
        <div>
          <p className="eyebrow">New support request</p>
          <h1>Create ticket</h1>
          <p className="page-description">
            Capture the issue clearly so the support team can act quickly.
          </p>
        </div>
      </header>
      <section className="panel form-panel">
        <div className="section-heading">
          <div>
            <h2>Ticket information</h2>
            <p>Required fields are marked below.</p>
          </div>
          <span className="required-note">All fields except assignee are required</span>
        </div>
        <form ref={formRef} onSubmit={submit}>
          <TicketFieldsForm
            value={fields}
            errors={errors}
            disabled={submitting}
            onChange={setFields}
          />
          {actionError && (
            <div className="error-banner compact" role="alert">
              <span className="error-icon" aria-hidden="true">!</span>
              <p>{actionError}</p>
            </div>
          )}
          <div className="form-actions">
            <AppLink to={ticketsHref} navigate={navigate} className="button">
              Cancel
            </AppLink>
            <button className="primary" type="submit" disabled={submitting}>
              {submitting ? 'Creating…' : 'Create ticket'}
            </button>
          </div>
        </form>
      </section>
      <div className="sr-only" aria-live="polite">
        {submitting ? 'Creating ticket' : ''}
      </div>
    </main>
  )
}
