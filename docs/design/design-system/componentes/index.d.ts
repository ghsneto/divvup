/** divvup components. Load tokens.css, bundle.css, React 18 and bundle.js; use window.Divvup. */
import type { ReactNode, MouseEventHandler } from "react";

export interface LogoProps {
  /** "full" = símbolo + logotipo (padrão); "symbol" = só o cartão partido. */
  variant?: "full" | "symbol";
  /** Altura em px. Logo principal ≥ 20px; abaixo disso use "symbol". */
  height?: number;
  /** Uma cor: herda `color` do pai (use sobre ultramarino ou lima). */
  mono?: boolean;
  label?: string;
  className?: string;
}

export interface ButtonProps {
  variant?: "primary" | "accent" | "secondary" | "ghost";
  size?: "md" | "sm";
  /** Com href vira <a>. */
  href?: string;
  disabled?: boolean;
  onClick?: MouseEventHandler;
  children: ReactNode;
  className?: string;
}

export interface ChipProps {
  /** Com selected ou onToggle vira botão alternável (aria-pressed). */
  selected?: boolean;
  onToggle?: (next: boolean) => void;
  children: ReactNode;
  className?: string;
}

export interface PointsBadgeProps {
  value: number;
  /** Mostra "+" em valores positivos. */
  signed?: boolean;
  /** "accent" (lima, padrão) ou "quiet". */
  tone?: "accent" | "quiet";
  className?: string;
}

export interface TaskCardProps {
  title: string;
  /** Cômodo ou categoria: "Cozinha". */
  room: string;
  /** "Vence hoje · 20h". */
  due?: string;
  points?: number;
  /** Força o estado visual (para mockups); sem ele, o cartão arrasta. */
  state?: "aceitar" | "passar";
  onAceitar?: () => void;
  onPassar?: () => void;
  className?: string;
}

export interface RankingItem { name: string; points: number; me?: boolean }
export interface RankingProps { items: RankingItem[]; label?: string; className?: string }

export interface SiteHeaderProps {
  links?: { label: string; href?: string; current?: boolean }[];
  cta?: { label: string; href?: string; onClick?: MouseEventHandler };
  homeHref?: string;
  className?: string;
}
