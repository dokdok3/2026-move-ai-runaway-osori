export const parseCsv = (raw: string): Record<string, string>[] => {
  const rows = raw
    .trim()
    .split('\n')
    .map((line) => parseCsvLine(line))

  const [header, ...body] = rows
  if (!header) return []

  return body.map((fields) =>
    Object.fromEntries(header.map((key, index) => [key, fields[index] ?? ''])),
  )
}

const parseCsvLine = (line: string): string[] => {
  const fields: string[] = []
  let current = ''
  let insideQuotes = false

  for (let i = 0; i < line.length; i += 1) {
    const char = line[i]

    if (char === '"') {
      insideQuotes = !insideQuotes
      continue
    }

    if (char === ',' && !insideQuotes) {
      fields.push(current)
      current = ''
      continue
    }

    current += char
  }

  fields.push(current)
  return fields
}
