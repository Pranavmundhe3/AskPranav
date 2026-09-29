/** A run of text; `hl` marks a key term that is visually highlighted. */
export interface TextPart {
  text: string;
  hl: boolean;
}

/**
 * Splits text into sentences, each broken into plain and highlighted parts, for the staggered
 * sentence-card reveal used across the site (Home's summary, Projects/Publications descriptions).
 * Matching is case-sensitive and exact against `keywords` (the text itself is never altered, only
 * wrapped), and longer keywords are tried first so e.g. "Spring Boot" wins over a bare "Spring".
 */
export function toSentences(text: string, keywords: string[]): TextPart[][] {
  if (!text) {
    return [];
  }
  const terms = (keywords || []).filter(k => k && k.trim().length > 0);
  if (terms.length === 0) {
    return splitIntoSentences(text).map(sentence => [{ text: sentence, hl: false }]);
  }

  const pattern = new RegExp('(' + terms
    .slice()
    .sort((a, b) => b.length - a.length)
    .map(k => k.replace(/[.*+?^${}()|[\]\\/]/g, '\\$&'))
    .join('|') + ')', 'g');

  return splitIntoSentences(text).map(sentence => sentence
    .split(pattern)
    .filter(part => part.length > 0)
    .map(part => ({ text: part, hl: terms.indexOf(part) >= 0 })));
}

/**
 * Splits a comma-separated field (tech stack, technologies, skills) into trimmed, non-empty terms.
 * Parenthesis-aware: a comma inside "AWS (EC2, ECS, ...)" does not split that entry apart, so the
 * whole parenthetical stays as one term instead of breaking into "AWS (EC2" / "ECS" / "...)".
 */
export function splitCsv(value: string): string[] {
  const terms: string[] = [];
  let depth = 0;
  let current = '';
  for (const ch of (value || '')) {
    if (ch === '(') { depth++; }
    if (ch === ')') { depth = Math.max(0, depth - 1); }
    if (ch === ',' && depth === 0) {
      terms.push(current.trim());
      current = '';
    } else {
      current += ch;
    }
  }
  terms.push(current.trim());
  return terms.filter(t => t.length > 0);
}

function splitIntoSentences(text: string): string[] {
  return (text.match(/[^.]+\.?/g) || [text])
    .map(sentence => sentence.trim())
    .filter(sentence => sentence.length > 0);
}
