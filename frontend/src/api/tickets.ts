import type {
  ApiProblem,
  TicketComment,
  TicketDetail,
  TicketFields,
  TicketPage,
  TicketStatus,
} from '../types/ticket'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? '/api/v1'
const connectivityMessage =
  'Unable to reach the support service. Check your connection and try again.'

export class ApiError extends Error {
  constructor(public readonly problem: ApiProblem) {
    super(problem.detail)
  }
}

function isProblem(value: unknown): value is ApiProblem {
  if (!value || typeof value !== 'object') return false
  const problem = value as Record<string, unknown>
  return (
    typeof problem.title === 'string' &&
    typeof problem.status === 'number' &&
    typeof problem.detail === 'string' &&
    typeof problem.code === 'string' &&
    Array.isArray(problem.fieldErrors)
  )
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response
  try {
    response = await fetch(`${apiBaseUrl}${path}`, {
      ...init,
      headers: {
        Accept: 'application/json, application/problem+json',
        ...(init?.body ? { 'Content-Type': 'application/json' } : {}),
        ...init?.headers,
      },
    })
  } catch {
    throw new Error(connectivityMessage)
  }

  let body: unknown
  try {
    body = await response.json()
  } catch {
    throw new Error(connectivityMessage)
  }

  if (!response.ok) {
    if (isProblem(body)) throw new ApiError(body)
    throw new Error(connectivityMessage)
  }
  return body as T
}

export function listTickets(search: string): Promise<TicketPage> {
  return request(`/tickets${search}`)
}

export function getTicket(ticketId: string): Promise<TicketDetail> {
  return request(`/tickets/${ticketId}`)
}

export function createTicket(fields: TicketFields): Promise<TicketDetail> {
  return request('/tickets', {
    method: 'POST',
    body: JSON.stringify({
      ...fields,
      assignee: fields.assignee || null,
    }),
  })
}

export function updateTicket(
  ticketId: string,
  fields: Partial<TicketFields>,
): Promise<TicketDetail> {
  return request(`/tickets/${ticketId}`, {
    method: 'PATCH',
    body: JSON.stringify(fields),
  })
}

export function addComment(
  ticketId: string,
  body: string,
): Promise<TicketComment> {
  return request(`/tickets/${ticketId}/comments`, {
    method: 'POST',
    body: JSON.stringify({ body }),
  })
}

export function transitionTicket(
  ticketId: string,
  targetStatus: TicketStatus,
): Promise<TicketDetail> {
  return request(`/tickets/${ticketId}/transitions`, {
    method: 'POST',
    body: JSON.stringify({ targetStatus }),
  })
}

export function actionErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.problem.code === 'INTERNAL_ERROR') {
      return 'The support service could not complete the request. Try again.'
    }
    return error.problem.detail
  }
  return connectivityMessage
}

export function fieldErrors(error: unknown): Record<string, string> {
  if (!(error instanceof ApiError)) return {}
  return Object.fromEntries(
    error.problem.fieldErrors.map(({ field, message }) => [field, message]),
  )
}
