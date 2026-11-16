/**
 * Índice de Equilíbrio (glossário, R-45): 0–100, quão uniformemente os pontos da temporada estão
 * divididos entre os moradores ativos. 100 menos o coeficiente de Gini normalizado (× n/(n−1)),
 * em pontos percentuais. Casa de 1 morador ou sem pontos no mês: 100 (nada desequilibrado).
 * Mesma conta de `core/domain` (`indiceDeEquilibrio`).
 */
export function indiceDeEquilibrio(pontos: number[]): number {
  const n = pontos.length;
  const total = pontos.reduce((a, b) => a + b, 0);
  if (n < 2 || total <= 0) return 100;
  let diferencas = 0;
  for (const a of pontos) for (const b of pontos) diferencas += Math.abs(a - b);
  const gini = diferencas / (2 * n * total);
  return Math.round(100 * (1 - (gini * n) / (n - 1)));
}
