import type { ChangeEvent, RefObject } from 'react'
import { priorities, type TicketFields } from '../types/ticket'

interface Props {
  value: TicketFields
  errors: Record<string, string>
  disabled: boolean
  onChange: (fields: TicketFields) => void
  titleRef?: RefObject<HTMLInputElement | null>
}

export function TicketFieldsForm({
  value,
  errors,
  disabled,
  onChange,
  titleRef,
}: Props) {
  const change =
    (field: keyof TicketFields) =>
    (
      event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>,
    ) =>
      onChange({ ...value, [field]: event.target.value })

  return (
    <div className="form-grid">
      <label>
        <span className="field-label">
          Title <span aria-hidden="true">*</span>
        </span>
        <input
          ref={titleRef}
          value={value.title}
          onChange={change('title')}
          maxLength={200}
          disabled={disabled}
          placeholder="Briefly describe the issue"
          aria-label="Title"
          aria-invalid={Boolean(errors.title)}
          aria-describedby={errors.title ? 'title-error' : undefined}
        />
        {errors.title && (
          <span id="title-error" className="field-error">
            {errors.title}
          </span>
        )}
      </label>

      <label>
        <span className="field-label">
          Description <span aria-hidden="true">*</span>
        </span>
        <textarea
          value={value.description}
          onChange={change('description')}
          maxLength={5000}
          rows={6}
          disabled={disabled}
          placeholder="Add the context and impact needed to investigate"
          aria-label="Description"
          aria-invalid={Boolean(errors.description)}
          aria-describedby={errors.description ? 'description-error' : undefined}
        />
        {errors.description && (
          <span id="description-error" className="field-error">
            {errors.description}
          </span>
        )}
      </label>

      <div className="form-row">
        <label>
          <span className="field-label">
            Priority <span aria-hidden="true">*</span>
          </span>
          <select
            value={value.priority}
            onChange={change('priority')}
            disabled={disabled}
            aria-label="Priority"
            aria-invalid={Boolean(errors.priority)}
            aria-describedby={errors.priority ? 'priority-error' : undefined}
          >
            {priorities.map((priority) => (
              <option key={priority} value={priority}>
                {priority}
              </option>
            ))}
          </select>
          {errors.priority && (
            <span id="priority-error" className="field-error">
              {errors.priority}
            </span>
          )}
        </label>

        <label>
          <span className="field-label">
            Assignee <span className="optional-label">Optional</span>
          </span>
          <input
            value={value.assignee}
            onChange={change('assignee')}
            maxLength={100}
            disabled={disabled}
            placeholder="Team member name"
            aria-label="Assignee"
            aria-invalid={Boolean(errors.assignee)}
            aria-describedby={errors.assignee ? 'assignee-error' : undefined}
          />
          {errors.assignee && (
            <span id="assignee-error" className="field-error">
              {errors.assignee}
            </span>
          )}
        </label>
      </div>
    </div>
  )
}

export function validateTicketFields(
  fields: TicketFields,
): Record<string, string> {
  const errors: Record<string, string> = {}
  const title = fields.title.trim()
  const description = fields.description.trim()
  const assignee = fields.assignee.trim()

  if (!title) errors.title = 'Title is required.'
  else if (title.length > 200) errors.title = 'Title must be at most 200 characters.'
  if (!description) errors.description = 'Description is required.'
  else if (description.length > 5000) {
    errors.description = 'Description must be at most 5000 characters.'
  }
  if (assignee.length > 100) {
    errors.assignee = 'Assignee must be at most 100 characters.'
  }
  return errors
}
