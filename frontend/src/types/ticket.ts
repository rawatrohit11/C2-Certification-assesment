export const priorities = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'] as const
export const statuses = [
  'OPEN',
  'IN_PROGRESS',
  'RESOLVED',
  'CLOSED',
  'CANCELLED',
] as const

export type TicketPriority = (typeof priorities)[number]
export type TicketStatus = (typeof statuses)[number]

export interface TicketSummary {
  id: string
  title: string
  priority: TicketPriority
  status: TicketStatus
  assignee: string | null
  createdAt: string
  updatedAt: string
}

export interface TicketComment {
  id: string
  body: string
  createdAt: string
}

export interface TicketDetail extends TicketSummary {
  description: string
  comments: TicketComment[]
}

export interface TicketPage {
  content: TicketSummary[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface TicketFields {
  title: string
  description: string
  priority: TicketPriority
  assignee: string
}

export interface FieldError {
  field: string
  message: string
}

export interface ApiProblem {
  title: string
  status: number
  detail: string
  code: string
  fieldErrors: FieldError[]
}
