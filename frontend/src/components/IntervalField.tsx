import { useState } from 'react'
import { INPUT_CLASSES, LABEL_CLASSES } from '../lib/formStyles'
import { INTERVAL_OPTIONS, isIntervalOption } from '../lib/slotInterval'

const OTHER = 'other'

interface IntervalFieldProps {
  id: string
  value: string
  onChange: (value: string) => void
  // Para los forms que se leen con FormData (excepciones). El horario semanal usa solo value y onChange.
  name?: string
}

// Campo "Turnos cada" de una franja: los valores comunes en una lista y "Otro" para cargar cualquier número de minutos.
export function IntervalField({ id, value, onChange, name }: IntervalFieldProps) {
  // Un valor que no está en la lista (por ejemplo, 25) abre directo el campo de "Otro".
  const [isCustom, setIsCustom] = useState(() => !isIntervalOption(value))

  return (
    <div>
      <label htmlFor={id} className={LABEL_CLASSES}>
        Turnos cada *
      </label>
      <div className="flex gap-2">
        <select
          id={id}
          value={isCustom ? OTHER : value}
          onChange={(event) => {
            const selected = event.target.value
            setIsCustom(selected === OTHER)
            if (selected !== OTHER) {
              onChange(selected)
            }
          }}
          className={INPUT_CLASSES}
        >
          {INTERVAL_OPTIONS.map((minutes) => (
            <option key={minutes} value={minutes}>
              {minutes} min
            </option>
          ))}
          <option value={OTHER}>Otro</option>
        </select>
        {isCustom ? (
          <input
            id={`${id}-custom`}
            name={name}
            type="number"
            aria-label="Minutos entre turnos"
            required
            min={1}
            max={1440}
            step={1}
            value={value}
            onChange={(event) => onChange(event.target.value)}
            className={INPUT_CLASSES}
          />
        ) : (
          name && <input type="hidden" name={name} value={value} />
        )}
      </div>
    </div>
  )
}
