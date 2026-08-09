import { useRef, useState, useEffect, useCallback } from "react";

interface UseInViewOptions {
  threshold?: number;
  rootMargin?: string;
  triggerOnce?: boolean;
}

export function useInView<T extends HTMLElement = HTMLElement>(
  options: UseInViewOptions = {}
) {
  const { threshold = 0.2, rootMargin = "0px", triggerOnce = false } = options;
  const [inView, setInView] = useState(false);
  const elementRef = useRef<T | null>(null);
  const triggered = useRef(false);

  useEffect(() => {
    const element = elementRef.current;
    if (!element) return;

    const observer = new IntersectionObserver(
      ([entry]) => {
        const isIntersecting = entry.isIntersecting;
        if (triggerOnce && triggered.current && !isIntersecting) return;
        if (isIntersecting) {
          triggered.current = true;
        }
        setInView(isIntersecting);
      },
      { threshold, rootMargin }
    );

    observer.observe(element);

    return () => observer.disconnect();
  }, [threshold, rootMargin, triggerOnce]);

  const ref = useCallback((node: T | null) => {
    elementRef.current = node;
  }, []);

  return { ref, inView };
}
