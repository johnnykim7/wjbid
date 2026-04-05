import { useState, useEffect } from 'react'
import { getPricing } from '../api/client'

interface PricingPlan {
  id: string
  name: string
  price: number
  currency: string
  period: string
  features: string[]
  recommended: boolean
}

export default function PricingPage() {
  const [plans, setPlans] = useState<PricingPlan[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getPricing()
      .then((res) => setPlans(res.data))
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-secondary" />
      </div>
    )
  }

  return (
    <div className="p-6">
      <div className="max-w-5xl mx-auto">
        <div className="text-center mb-10">
          <h2 className="text-2xl font-bold text-gray-900">Service Plans & Pricing</h2>
          <p className="mt-2 text-gray-500">Choose the plan that fits your bidding needs</p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {plans.map((plan) => (
            <div
              key={plan.id}
              className={`bg-white rounded-2xl shadow-sm border-2 p-8 relative ${
                plan.recommended ? 'border-secondary' : 'border-gray-200'
              }`}
            >
              {plan.recommended && (
                <div className="absolute top-0 right-0 bg-secondary text-white px-4 py-1 rounded-bl-xl rounded-tr-xl text-xs font-bold">
                  Recommended
                </div>
              )}
              <h3 className="text-xl font-bold text-gray-900 mb-2">{plan.name}</h3>
              <div className="my-4">
                <span className="text-3xl font-extrabold text-gray-900">
                  ${plan.price}
                </span>
                <span className="text-gray-500 ml-1">/ {plan.period}</span>
              </div>
              <ul className="space-y-3 mb-8 text-sm text-gray-600">
                {plan.features.map((feature, i) => (
                  <li key={i} className="flex items-start">
                    <i className="fa-solid fa-check text-accent mr-2 mt-0.5" />
                    <span>{feature}</span>
                  </li>
                ))}
              </ul>
              <button
                className={`w-full py-2.5 rounded-lg font-bold transition ${
                  plan.recommended
                    ? 'bg-secondary text-white hover:bg-blue-600'
                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                }`}
              >
                {plan.id === 'enterprise' ? 'Contact Sales' : 'Get Started'}
              </button>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
