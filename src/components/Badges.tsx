import type { BloodType, Status } from '../types'
import { bloodLabels, statusLabels } from '../types'

export function BloodTypeBadge({ value }: { value: BloodType }) { return <span className="blood-badge"><span aria-hidden="true">✚</span>{bloodLabels[value] || value}</span> }
export function StatusBadge({ value }: { value: Status }) { return <span className={`status-badge status-${value?.toLowerCase()}`}>{statusLabels[value] || value || '—'}</span> }