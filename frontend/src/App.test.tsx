import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'
import type { TicketDetail, TicketPage } from './types/ticket'

const ticket: TicketDetail = {
  id: 'c06571ea-3e71-4ec7-b210-10803a04e73a',
  title: 'Billing failure',
  description: 'The billing page is unavailable.',
  priority: 'HIGH',
  status: 'OPEN',
  assignee: null,
  createdAt: '2026-09-23T06:30:00Z',
  updatedAt: '2026-09-23T06:30:00Z',
  comments: [],
}

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      'Content-Type':
        status >= 400 ? 'application/problem+json' : 'application/json',
    },
  })
}

function page(content: TicketDetail[] = []): TicketPage {
  return {
    content,
    page: 0,
    size: 20,
    totalElements: content.length,
    totalPages: content.length ? 1 : 0,
  }
}

describe('support ticket application', () => {
  beforeEach(() => {
    window.history.replaceState(null, '', '/tickets?size=20')
    vi.restoreAllMocks()
  })

  it('shows the database-empty list state', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(page())))
    render(<App />)

    expect(screen.getByText('Loading tickets…')).toBeInTheDocument()
    expect(
      await screen.findByText('No tickets have been created.'),
    ).toBeInTheDocument()
  })

  it('distinguishes no matches and normalizes a manually edited page size', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(page()))
    vi.stubGlobal('fetch', fetchMock)
    window.history.replaceState(
      null,
      '',
      '/tickets?keyword=missing&page=0&size=100',
    )
    render(<App />)

    expect(
      await screen.findByText('No tickets match the current search and filter.'),
    ).toBeInTheDocument()
    expect(window.location.search).toContain('size=20')
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('size=20'),
      expect.anything(),
    )
  })

  it('searches, filters, and retains fixed pagination in the URL', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(page([ticket])))
    vi.stubGlobal('fetch', fetchMock)
    render(<App />)

    expect(await screen.findByText('Billing failure')).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Search tickets'), {
      target: { value: ' billing ' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Search' }))
    fireEvent.change(screen.getByLabelText('Status'), {
      target: { value: 'OPEN' },
    })

    await waitFor(() => {
      expect(window.location.search).toContain('keyword=billing')
      expect(window.location.search).toContain('status=OPEN')
      expect(window.location.search).toContain('size=20')
    })
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('keyword=billing'),
      expect.anything(),
    )

    fireEvent.click(screen.getByRole('link', { name: 'Create ticket' }))
    expect(screen.getByRole('link', { name: '← Back to tickets' })).toHaveAttribute(
      'href',
      expect.stringContaining('keyword=billing'),
    )
  })

  it('validates create fields before submission', async () => {
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
    window.history.replaceState(null, '', '/tickets/new')
    render(<App />)

    fireEvent.click(screen.getByRole('button', { name: 'Create ticket' }))

    expect(await screen.findByText('Title is required.')).toBeInTheDocument()
    expect(screen.getByText('Description is required.')).toBeInTheDocument()
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('creates a ticket and navigates to server-issued details', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const path = String(input)
      if (path.endsWith('/api/v1/tickets') && init?.method === 'POST') {
        return jsonResponse(ticket, 201)
      }
      return jsonResponse(ticket)
    })
    vi.stubGlobal('fetch', fetchMock)
    window.history.replaceState(null, '', '/tickets/new')
    render(<App />)

    fireEvent.change(screen.getByLabelText('Title'), {
      target: { value: 'Billing failure' },
    })
    fireEvent.change(screen.getByLabelText('Description'), {
      target: { value: 'The billing page is unavailable.' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Create ticket' }))

    await waitFor(() =>
      expect(window.location.pathname).toBe(`/tickets/${ticket.id}`),
    )
    expect(await screen.findByText('Ticket created successfully.')).toBeInTheDocument()
  })

  it('preserves create input when backend validation fails', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        jsonResponse(
          {
            title: 'Request validation failed',
            status: 422,
            detail: 'One or more request fields are invalid.',
            code: 'VALIDATION_FAILED',
            fieldErrors: [{ field: 'title', message: 'already exists' }],
          },
          422,
        ),
      ),
    )
    window.history.replaceState(null, '', '/tickets/new')
    render(<App />)

    const title = screen.getByLabelText('Title')
    fireEvent.change(title, { target: { value: 'Retained title' } })
    fireEvent.change(screen.getByLabelText('Description'), {
      target: { value: 'Description' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Create ticket' }))

    expect(await screen.findByText('already exists')).toBeInTheDocument()
    expect(title).toHaveValue('Retained title')
  })

  it('updates fields, adds comments, and shows valid transition actions', async () => {
    const updated = { ...ticket, title: 'Updated title' }
    const progressed = { ...updated, status: 'IN_PROGRESS' as const }
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      if (init?.method === 'PATCH') return jsonResponse(updated)
      if (String(input).endsWith('/comments') && init?.method === 'POST') {
        return jsonResponse(
          {
            id: 'ed2eddb4-6531-4876-b8fe-713bac644ebb',
            body: 'Investigating',
            createdAt: '2026-09-23T06:45:00Z',
          },
          201,
        )
      }
      if (String(input).endsWith('/transitions') && init?.method === 'POST') {
        return jsonResponse(progressed)
      }
      return jsonResponse(ticket)
    })
    vi.stubGlobal('fetch', fetchMock)
    window.history.replaceState(null, '', `/tickets/${ticket.id}`)
    render(<App />)

    expect(await screen.findByRole('button', { name: 'Start progress' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Cancel ticket' })).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Title'), {
      target: { value: 'Updated title' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Save changes' }))
    expect(await screen.findByText('Ticket updated successfully.')).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Add comment'), {
      target: { value: 'Investigating' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Add comment' }))
    expect(await screen.findByText('Investigating')).toBeInTheDocument()
    expect(screen.getByLabelText('Add comment')).toHaveValue('')

    fireEvent.click(screen.getByRole('button', { name: 'Start progress' }))
    expect(
      await screen.findByText('Ticket status changed to IN PROGRESS.'),
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Resolve ticket' })).toBeInTheDocument()
  })

  it('shows invalid-transition details and reloads authoritative status', async () => {
    let detailLoads = 0
    const progressed = { ...ticket, status: 'IN_PROGRESS' as const }
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      if (String(input).endsWith('/transitions') && init?.method === 'POST') {
        return jsonResponse(
          {
            title: 'Invalid ticket status transition',
            status: 409,
            detail: 'Ticket cannot transition from OPEN to IN_PROGRESS.',
            code: 'INVALID_STATUS_TRANSITION',
            fieldErrors: [],
          },
          409,
        )
      }
      detailLoads += 1
      return jsonResponse(detailLoads > 1 ? progressed : ticket)
    })
    vi.stubGlobal('fetch', fetchMock)
    window.history.replaceState(null, '', `/tickets/${ticket.id}`)
    render(<App />)

    const title = await screen.findByLabelText('Title')
    fireEvent.change(title, { target: { value: 'Unsaved draft title' } })
    fireEvent.click(screen.getByRole('button', { name: 'Start progress' }))

    expect(
      await screen.findByText('Ticket cannot transition from OPEN to IN_PROGRESS.'),
    ).toBeInTheDocument()
    expect(title).toHaveValue('Unsaved draft title')
    expect(await screen.findByRole('button', { name: 'Resolve ticket' })).toBeInTheDocument()
    expect(detailLoads).toBeGreaterThan(1)
  })

  it('renders not-found and connectivity failures distinctly', async () => {
    const problem = {
      title: 'Ticket not found',
      status: 404,
      detail: 'Ticket was not found.',
      code: 'TICKET_NOT_FOUND',
      fieldErrors: [],
    }
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(problem, 404)))
    window.history.replaceState(null, '', `/tickets/${ticket.id}`)
    const view = render(<App />)
    expect(
      await screen.findByRole('heading', { name: 'Ticket not found' }),
    ).toBeInTheDocument()

    view.unmount()
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('offline')))
    window.history.replaceState(null, '', '/tickets?size=20')
    render(<App />)
    expect(
      await screen.findByText(
        'Unable to reach the support service. Check your connection and try again.',
      ),
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Retry' })).toBeInTheDocument()
  })
})
