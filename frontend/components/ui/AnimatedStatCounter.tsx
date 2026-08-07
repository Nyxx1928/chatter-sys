"use client";

import { useCountUp } from "@/hooks/useCountUp";

interface AnimatedStatCounterProps {
  value: number;
  suffix?: string;
  prefix?: string;
  duration?: number;
  decimals?: number;
  className?: string;
}

export function AnimatedStatCounter({
  value,
  suffix = "",
  prefix = "",
  duration = 2000,
  decimals,
  className = "",
}: AnimatedStatCounterProps) {
  const { ref, count } = useCountUp(value, { duration });

  const formattedValue =
    decimals !== undefined ? count.toFixed(decimals) : Math.round(count).toString();

  return (
    <span ref={ref} className={className}>
      {prefix}
      {formattedValue}
      {suffix}
    </span>
  );
}
