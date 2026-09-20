function initials(name: string): string {
  return name
    .split(' ')
    .map((p) => p[0])
    .join('')
    .slice(0, 2)
    .toUpperCase()
}

export function Avatar({ name, color, size = 18 }: { name: string; color?: string | null; size?: number }) {
  return (
    <span
      className="rounded-full text-white font-bold flex items-center justify-center flex-shrink-0 font-sans"
      style={{
        width: size,
        height: size,
        background: color ?? '#93989F',
        fontSize: Math.round(size * 0.5),
      }}
    >
      {initials(name)}
    </span>
  )
}
