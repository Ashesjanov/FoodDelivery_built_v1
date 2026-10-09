import { readdir, readFile } from 'node:fs/promises'
import { extname, join, relative, resolve } from 'node:path'

const projectRoot = resolve(import.meta.dirname, '..')
const sourceRoot = join(projectRoot, 'frontend', 'src')
const extensions = new Set(['.vue', '.ts', '.tsx', '.css', '.scss'])

const forbidden = [
  { pattern: /\brounded-none\b/, reason: '禁止使用无圆角' },
  { pattern: /\brounded-sm\b/, reason: '禁止使用小圆角' },
  { pattern: /\bgap-1\b/, reason: 'Bento 间隙只允许 gap-4 或 gap-6' },
  { pattern: /\bgap-2\b/, reason: 'Bento 间隙只允许 gap-4 或 gap-6' },
  { pattern: /\bgap-10\b/, reason: 'Bento 间隙只允许 gap-4 或 gap-6' },
  { pattern: /\bgap-12\b/, reason: 'Bento 间隙只允许 gap-4 或 gap-6' },
  { pattern: /\bgap-(?:0|3|5|7|8|9|11|13|14|15|16|17|18|19|20)\b/, reason: 'Bento 间隙只允许 gap-4 或 gap-6' },
  { pattern: /\bgap-[xy]-(?:0|1|2|3|5|7|8|9|10|11|12|13|14|15|16|17|18|19|20)\b/, reason: 'Bento 间隙只允许 gap-4 或 gap-6' },
  { pattern: /(?:^|[;{\s])gap\s*:(?!\s*(?:16px|24px|1rem|1\.5rem))/i, reason: 'CSS 间隙只允许 16px/24px（gap-4/gap-6）' },
  { pattern: /\bfont-family\s*:[^;]*(?:Inter|Roboto|Geist)/i, reason: '禁止使用 Inter / Roboto / Geist' },
  { pattern: /fonts\.(?:googleapis|gstatic)\.com/i, reason: '禁止引用外部 Google Fonts' },
  { pattern: /\b(?:animate-bounce|animate-elastic|cubic-bezier\([^)]*(?:1\.7|1\.8|2\.5))/i, reason: '禁止 bounce / elastic 缓动' },
  { pattern: /background-clip\s*:\s*text/i, reason: '禁止渐变文字' },
  { pattern: /backdrop-filter\s*:/i, reason: '禁止把玻璃态当作默认风格' },
  { pattern: /\bborder-(?:left|right)(?:-\d+)?\b/, reason: '禁止单侧粗边框装饰' },
]

const requiredGlobalTokens = [
  'grid grid-cols-4',
  'col-span-2',
  'row-span-2',
  'gap-4 md:gap-6',
  'rounded-2xl',
  'hover:-translate-y-1 hover:scale-[1.01]',
  'group-hover:scale-110',
  'rounded-xl font-medium transition-colors',
  'focus:ring-blue-500/20 focus:border-blue-500',
  'prefers-reduced-motion',
]

async function collectFiles(directory) {
  const entries = await readdir(directory, { withFileTypes: true })
  const files = await Promise.all(
    entries.map(async (entry) => {
      const path = join(directory, entry.name)
      if (entry.isDirectory()) return collectFiles(path)
      return extensions.has(extname(entry.name)) ? [path] : []
    }),
  )
  return files.flat()
}

const files = await collectFiles(sourceRoot)
const errors = []
const combined = []

for (const file of files) {
  const source = await readFile(file, 'utf8')
  combined.push(source)
  const displayPath = relative(projectRoot, file)

  for (const rule of forbidden) {
    if (rule.pattern.test(source)) errors.push(`${displayPath}: ${rule.reason}`)
  }

  if (/from-(?:blue|indigo|violet|purple)-\d+/.test(source) && /to-(?:blue|indigo|violet|purple)-\d+/.test(source)) {
    errors.push(`${displayPath}: 禁止蓝色到紫色渐变`)
  }
}

const allSource = combined.join('\n')
for (const token of requiredGlobalTokens) {
  if (!allSource.includes(token)) errors.push(`frontend/src: 缺少必需 Bento token “${token}”`)
}

if (errors.length > 0) {
  console.error(`Bento 风格检查失败（${errors.length} 项）：\n${errors.map((error) => `- ${error}`).join('\n')}`)
  process.exitCode = 1
} else {
  console.log(`Bento 风格检查通过，共扫描 ${files.length} 个源文件。`)
}
