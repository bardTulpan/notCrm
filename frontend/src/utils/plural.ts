export function pluralRu(n: number, forms: [string, string, string]): string {
  const n100 = n % 100
  const n10 = n % 10
  if (n10 === 1 && n100 !== 11) return forms[0]
  if (n10 >= 2 && n10 <= 4 && !(n100 >= 12 && n100 <= 14)) return forms[1]
  return forms[2]
}

export function pluralPeople(n: number): string {
  return pluralRu(n, ['человек', 'человека', 'человек'])
}
