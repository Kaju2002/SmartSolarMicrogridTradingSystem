import { useEffect, useId, useRef, useState } from 'react'

const HOURS = Array.from({ length: 24 }, (_, i) =>
  String(i).padStart(2, '0'),
)
const MINUTES = Array.from({ length: 60 }, (_, i) =>
  String(i).padStart(2, '0'),
)

function parseTime(value: string): { hour: string; minute: string } {
  const match = /^(\d{1,2}):(\d{2})$/.exec(value.trim())
  if (!match) return { hour: '06', minute: '00' }
  const hour = String(Math.min(23, Math.max(0, Number(match[1])))).padStart(
    2,
    '0',
  )
  const minute = String(Math.min(59, Math.max(0, Number(match[2])))).padStart(
    2,
    '0',
  )
  return { hour, minute }
}

function formatTime(hour: string, minute: string) {
  return `${hour}:${minute}`
}

function ClockIcon({ className = '' }: { className?: string }) {
  return (
    <svg
      className={className}
      width="18"
      height="18"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden
    >
      <circle cx="12" cy="12" r="9" />
      <path d="M12 7v5l3 2" />
    </svg>
  )
}

function Column({
  items,
  selected,
  onSelect,
  label,
}: {
  items: string[]
  selected: string
  onSelect: (value: string) => void
  label: string
}) {
  const activeRef = useRef<HTMLButtonElement | null>(null)

  useEffect(() => {
    activeRef.current?.scrollIntoView({ block: 'center' })
  }, [selected])

  return (
    <div className="flex min-w-0 flex-1 flex-col">
      <p className="border-b border-line px-2 py-1.5 text-center text-[10px] font-semibold tracking-wide text-muted uppercase">
        {label}
      </p>
      <div className="max-h-40 overflow-y-auto py-1 [scrollbar-width:none] [-ms-overflow-style:none] [&::-webkit-scrollbar]:hidden">
        {items.map((item) => {
          const active = item === selected
          return (
            <button
              key={item}
              type="button"
              ref={active ? activeRef : undefined}
              onClick={() => onSelect(item)}
              className={[
                'mx-1 flex w-[calc(100%-0.5rem)] items-center justify-center rounded-lg py-1.5 text-sm transition',
                active
                  ? 'bg-primary font-semibold text-white shadow-sm'
                  : 'text-ink hover:bg-primary-light hover:text-primary',
              ].join(' ')}
            >
              {item}
            </button>
          )
        })}
      </div>
    </div>
  )
}

type SolarTimePickerProps = {
  value: string
  onChange: (value: string) => void
  label?: string
  'aria-label'?: string
  size?: 'md' | 'lg'
  className?: string
}

export function SolarTimePicker({
  value,
  onChange,
  label,
  'aria-label': ariaLabel,
  size = 'lg',
  className = '',
}: SolarTimePickerProps) {
  const panelId = useId()
  const rootRef = useRef<HTMLDivElement | null>(null)
  const [open, setOpen] = useState(false)
  const { hour, minute } = parseTime(value || '06:00')

  useEffect(() => {
    if (!open) return

    function onPointerDown(event: MouseEvent) {
      if (!rootRef.current?.contains(event.target as Node)) {
        setOpen(false)
      }
    }

    function onKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') setOpen(false)
    }

    document.addEventListener('mousedown', onPointerDown)
    document.addEventListener('keydown', onKeyDown)
    return () => {
      document.removeEventListener('mousedown', onPointerDown)
      document.removeEventListener('keydown', onKeyDown)
    }
  }, [open])

  const display = formatTime(hour, minute)
  const triggerHeight = size === 'lg' ? 'h-11' : 'h-10'

  return (
    <div ref={rootRef} className={`relative block ${className}`}>
      {label ? (
        <span className="mb-2 block text-sm font-medium text-ink">{label}</span>
      ) : null}

      <button
        type="button"
        aria-label={ariaLabel || label || 'Select time'}
        aria-expanded={open}
        aria-controls={panelId}
        onClick={() => setOpen((v) => !v)}
        className={[
          triggerHeight,
          'flex w-full items-center justify-between rounded-lg border border-line bg-panel px-4 text-left text-sm text-ink shadow-sm outline-none transition',
          open
            ? 'border-primary ring-2 ring-primary/15'
            : 'hover:border-primary/40 focus:border-primary focus:ring-2 focus:ring-primary/15',
        ].join(' ')}
      >
        <span className="font-medium tabular-nums tracking-wide">{display}</span>
        <span className={open ? 'text-primary' : 'text-muted'}>
          <ClockIcon />
        </span>
      </button>

      {open ? (
        <div
          id={panelId}
          role="dialog"
          aria-label="Time picker"
          className="absolute top-[calc(100%+0.4rem)] right-0 left-0 z-30 overflow-hidden rounded-xl border border-line bg-panel shadow-lg"
        >
          <div className="flex border-b border-line bg-surface/60">
            <Column
              label="Hour"
              items={HOURS}
              selected={hour}
              onSelect={(nextHour) => onChange(formatTime(nextHour, minute))}
            />
            <div className="w-px self-stretch bg-line" />
            <Column
              label="Min"
              items={MINUTES}
              selected={minute}
              onSelect={(nextMinute) => onChange(formatTime(hour, nextMinute))}
            />
          </div>
          <div className="flex items-center justify-between gap-2 px-3 py-2">
            <p className="text-xs text-muted">
              Selected{' '}
              <span className="font-semibold text-ink tabular-nums">
                {display}
              </span>
            </p>
            <button
              type="button"
              onClick={() => setOpen(false)}
              className="rounded-lg bg-primary px-3 py-1.5 text-xs font-medium text-white transition hover:opacity-90"
            >
              Done
            </button>
          </div>
        </div>
      ) : null}
    </div>
  )
}

export function AppTimePicker({
  label,
  value,
  onChange,
  className = '',
}: {
  label: string
  value: string
  onChange: (value: string) => void
  className?: string
}) {
  return (
    <SolarTimePicker
      label={label}
      value={value}
      onChange={onChange}
      className={className}
      size="lg"
    />
  )
}

export function CompactTimePicker({
  value,
  onChange,
  'aria-label': ariaLabel,
}: {
  value: string
  onChange: (value: string) => void
  'aria-label': string
}) {
  return (
    <SolarTimePicker
      value={value}
      onChange={onChange}
      aria-label={ariaLabel}
      size="md"
    />
  )
}
