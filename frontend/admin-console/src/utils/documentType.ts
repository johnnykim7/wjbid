// 입찰 건 상세와 문서 편집이 같은 문서 이름을 보이도록 한곳에 둔다
export const DOC_TYPE_LABELS: Record<string, string> = {
  COVER_LETTER: 'Cover Letter',
  TECHNICAL_PROPOSAL: 'Technical Proposal',
  PAST_PERFORMANCE: 'Past Performance',
  COMPANY_PROFILE: 'Company Profile',
  COMPLIANCE_MATRIX: 'Compliance Matrix',
  PRICING_SUMMARY: 'Pricing Summary',
  OTHER: 'Other',
}

export function docTypeLabel(type: string): string {
  return DOC_TYPE_LABELS[type] ?? type
}
