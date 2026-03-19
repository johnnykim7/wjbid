import type { Opportunity } from '../types'

export const MOCK_BIDS: Opportunity[] = [
  {
    id: '1',
    solicitationNumber: 'FA8732-24-R-0001',
    title: 'Cloud Computing and Cyber Security Services Support',
    agencyName: 'Dept. of Air Force',
    naicsCode: '541512',
    setAside: 'Small Business',
    responseDeadline: '2026-03-01',
    postedDate: '2026-01-15',
    placeOfPerformance: 'Washington, DC',
    description: 'The Air Force requires comprehensive cloud computing and cybersecurity services to support its enterprise IT infrastructure. This includes migration to a multi-cloud environment, Zero-Trust Architecture implementation, and 24/7 security operations center support.',
    status: 'active',
  },
  {
    id: '2',
    solicitationNumber: 'W9128F-24-R-0015',
    title: 'Information Technology Enterprise Solutions - 3 Services (ITES-3S)',
    agencyName: 'Dept. of the Army',
    naicsCode: '541512',
    setAside: 'Unrestricted',
    responseDeadline: '2026-03-15',
    postedDate: '2026-01-20',
    placeOfPerformance: 'Fort Belvoir, VA',
    description: 'This is a presolicitation notice for the Information Technology Enterprise Solutions. The Contractor shall provide a full range of IT equipment and software, along with comprehensive maintenance, cloud migration, and cybersecurity support tailored to the Department\'s infrastructure. Key requirements include compliance with NIST SP 800-53 and the deployment of a Zero-Trust Architecture (ZTA).',
    status: 'active',
  },
  {
    id: '3',
    solicitationNumber: '36C245-24-Q-0120',
    title: 'Medical Equipment Maintenance Service for VA Hospital',
    agencyName: 'Dept. of Veterans Affairs',
    naicsCode: '811219',
    setAside: 'SDVOSB',
    responseDeadline: '2026-02-28',
    postedDate: '2026-01-10',
    placeOfPerformance: 'Multiple Locations',
    description: 'The Department of Veterans Affairs requires maintenance and repair services for biomedical and medical equipment at VA Medical Centers across the Southeast region.',
    status: 'active',
  },
  {
    id: '4',
    solicitationNumber: 'N00039-24-R-0011',
    title: 'AI & Machine Learning Data Analytics Platform Development',
    agencyName: 'Dept. of the Navy',
    naicsCode: '541511',
    setAside: 'Small Business',
    responseDeadline: '2026-04-10',
    postedDate: '2026-02-01',
    placeOfPerformance: 'San Diego, CA',
    description: 'The Navy seeks a contractor to develop and deploy an advanced AI/ML analytics platform to process and analyze large-scale operational data sets.',
    status: 'active',
  },
  {
    id: '5',
    solicitationNumber: 'HT0011-24-R-0022',
    title: 'Defense Health Agency Enterprise IT Services',
    agencyName: 'Defense Health Agency',
    naicsCode: '541519',
    setAside: 'Unrestricted',
    responseDeadline: '2026-03-30',
    postedDate: '2026-01-25',
    placeOfPerformance: 'Falls Church, VA',
    description: 'The Defense Health Agency requires enterprise IT services including helpdesk support, network management, and cybersecurity for medical facilities across the DoD.',
    status: 'active',
  },
  {
    id: '6',
    solicitationNumber: 'GS-35F-0024X',
    title: 'Federal Cybersecurity Operations Center (CSOC) Services',
    agencyName: 'General Services Administration',
    naicsCode: '541512',
    setAside: '8(a)',
    responseDeadline: '2026-05-01',
    postedDate: '2026-02-05',
    placeOfPerformance: 'Washington, DC',
    description: 'GSA seeks a qualified contractor to provide 24/7 cybersecurity operations center services including threat monitoring, incident response, and vulnerability management.',
    status: 'active',
  },
  {
    id: '7',
    solicitationNumber: 'DISA-D-2024-0033',
    title: 'Network Infrastructure Modernization Services',
    agencyName: 'Defense Information Systems Agency',
    naicsCode: '517312',
    setAside: 'Unrestricted',
    responseDeadline: '2026-04-20',
    postedDate: '2026-02-10',
    placeOfPerformance: 'Fort Meade, MD',
    description: 'DISA requires modernization of its global network infrastructure including SD-WAN deployment, optical fiber upgrades, and network automation capabilities.',
    status: 'active',
  },
]

export const MOCK_BOOKMARKS: Opportunity[] = [
  MOCK_BIDS[3],
  MOCK_BIDS[4],
]

export const MOCK_STATS = {
  total: 12408,
  myKeywordMatches: 42,
  proposalsThisMonth: 3,
  aiCreditsUsed: 5,
  aiCreditsTotal: 10,
}

export const MOCK_PROFILE = {
  companyName: 'WJ Global IT Solutions',
  uei: 'QWX8Y7Z65432',
  cageCode: '9A8B7',
  naicsCodes: '541511, 541512, 541519',
  capabilities: 'We specialize in providing secure, scalable cloud infrastructure and cybersecurity solutions tailored for federal agencies. With over 10 years of experience, we ensure 100% compliance with NIST frameworks and Zero-Trust Architectures.',
  contactName: 'Gildong Hong',
  contactTitle: 'CEO / President',
  contactEmail: 'gildong@wjglobal.com',
}

export const MOCK_PROPOSAL_CONTENT = (noticeId: string, title: string) => `
REFERENCE: ${noticeId}

────────────────────────────────────────
VOLUME I: TECHNICAL PROPOSAL
────────────────────────────────────────

1.0 Executive Summary

This proposal is submitted by [Your Company Name] in response to the solicitation for ${title}. We have thoroughly reviewed the Statement of Work (SOW) and are fully prepared to provide the required services as specified.

2.0 Technical Approach

Our technical approach leverages industry best practices and NIST compliant frameworks. To meet the requirements of section 3.1 of the PWS, we will deploy a scalable cloud architecture combined with 24/7 proactive monitoring.

  • Implementation of Zero-Trust Architecture (ZTA)
  • Automated threat detection and incident response protocols
  • Seamless integration with existing DoD legacy systems
  • NIST SP 800-53 Rev. 5 compliance framework

3.0 Past Performance & Corporate Capability

[Your Company Name] has a proven track record of delivering similar IT services to federal agencies. Our UEI is QWX8Y7Z65432 and our CAGE Code is 9A8B7. Over the last 5 years, we have successfully managed contracts exceeding $5M in the IT services sector.

4.0 Management Approach

Our program management office will provide dedicated oversight with a certified PMP as Program Manager. We commit to maintaining all milestones with bi-weekly status reports.

────────────────────────────────────────
Confidential - Not for Public Release
Page 1 of 12
────────────────────────────────────────
`
