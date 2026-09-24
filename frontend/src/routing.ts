export interface NavigationOptions {
  replace?: boolean
  state?: Record<string, unknown>
}

export type Navigate = (to: string, options?: NavigationOptions) => void
