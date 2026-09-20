export function Loader({ full = false }: { full?: boolean }) {
  return (
    <div className={full ? 'flex items-center justify-center h-screen' : 'flex items-center justify-center py-16'}>
      <div className="w-6 h-6 border-2 border-border border-t-accent rounded-full animate-spin" />
    </div>
  )
}
