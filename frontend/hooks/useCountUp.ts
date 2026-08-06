import { useState, useEffect, useRef } from "react";
import { useInView } from "@/hooks/useInView";

interface UseCountUpOptions {
  duration?: number;
  threshold?: number;
  triggerOnce?: boolean;
  formatter?: (value: number) => string;
}

function easeOutCubic(t: number): number {
  return 1 - Math.pow(1 - t, 3);
}

export function useCountUp(end: number, options: UseCountUpOptions = {}) {
  const { duration = 2000, threshold = 0.2, triggerOnce = true, formatter } =
    options;
  const { ref, inView } = useInView<HTMLElement>({ threshold, triggerOnce });
  const [count, setCount] = useState(0);
  const rafRef = useRef<number | null>(null);
  const startTimeRef = useRef<number | null>(null);

  useEffect(() => {
    if (!inView) return;

    startTimeRef.current = null;

    const animate = (timestamp: number) => {
      if (startTimeRef.current === null) {
        startTimeRef.current = timestamp;
      }

      const elapsed = timestamp - startTimeRef.current;
      const progress = Math.min(elapsed / duration, 1);
      const easedProgress = easeOutCubic(progress);
      const currentCount = Math.round(easedProgress * end);

      setCount(currentCount);

      if (progress < 1) {
        rafRef.current = requestAnimationFrame(animate);
      }
    };

    rafRef.current = requestAnimationFrame(animate);

    return () => {
      if (rafRef.current !== null) {
        cancelAnimationFrame(rafRef.current);
      }
    };
  }, [inView, end, duration]);

  const displayValue = formatter ? formatter(count) : String(count);

  return { ref, count, displayValue };
}
