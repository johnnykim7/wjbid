export type PriceTableStatus = 'NOT_READY' | 'NOT_APPLICABLE' | 'INCOMPLETE' | 'COMPLETE'

export interface PriceLine {
  clin: string
  description: string | null
  quantity: number | null
  unit: string | null
  sourceRef: string | null
  unitPrice: number | null
  amount: number | null
}

export interface PricePeriod {
  period: string
  lines: PriceLine[]
  subtotal: number | null
}

export interface PriceTable {
  status: PriceTableStatus
  editable: boolean
  periods: PricePeriod[]
  total: number | null
}

export interface UnitPriceInput {
  clin: string
  period: string
  unitPrice: string | null
}
