import type { MouseEvent, ReactNode } from 'react'
import type { Navigate } from '../routing'

interface Props {
  to: string
  navigate: Navigate
  children: ReactNode
  className?: string
}

export function AppLink({ to, navigate, children, className }: Props) {
  function follow(event: MouseEvent<HTMLAnchorElement>) {
    if (
      event.button === 0 &&
      !event.metaKey &&
      !event.ctrlKey &&
      !event.shiftKey &&
      !event.altKey
    ) {
      event.preventDefault()
      navigate(to)
    }
  }

  return (
    <a href={to} onClick={follow} className={className}>
      {children}
    </a>
  )
}
