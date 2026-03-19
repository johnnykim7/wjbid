interface TopHeaderProps {
  title: string
  onMobileMenuClick?: () => void
}

export function TopHeader({ title, onMobileMenuClick }: TopHeaderProps) {
  return (
    <header className="h-[var(--header-height)] bg-white border-b border-gray-200 flex items-center justify-between px-6 z-10 flex-shrink-0">
      <div className="flex items-center">
        {onMobileMenuClick && (
          <button
            className="lg:hidden text-gray-500 hover:text-gray-700 mr-4"
            onClick={onMobileMenuClick}
          >
            <i className="fa-solid fa-bars text-xl" />
          </button>
        )}
        <h1 className="text-xl font-bold text-gray-800">{title}</h1>
      </div>

      <div className="flex items-center space-x-4">
        <div className="text-sm text-gray-500 hidden sm:block">
          마지막 동기화:{' '}
          <span className="font-medium text-gray-700">방금 전</span>
        </div>
        <button className="relative p-2 text-gray-400 hover:text-gray-600 transition-colors">
          <i className="fa-regular fa-bell text-xl" />
          <span className="absolute top-1.5 right-1.5 h-2 w-2 rounded-full bg-red-500" />
        </button>
      </div>
    </header>
  )
}
