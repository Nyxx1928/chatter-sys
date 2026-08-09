"use client";

import React from "react";
import { motion } from "framer-motion";
import { useInView } from "@/hooks/useInView";
import { useReducedMotion } from "@/hooks/useReducedMotion";
import { viewportVariants, transitions } from "@/lib/animations";

interface FeatureGridProps {
  children: React.ReactNode;
  className?: string;
  staggerDelay?: number;
}

export function FeatureGrid({
  children,
  className = "",
  staggerDelay = 0.1,
}: FeatureGridProps) {
  const prefersReducedMotion = useReducedMotion();
  const { ref, inView } = useInView({ threshold: 0.2, triggerOnce: true });

  if (prefersReducedMotion) {
    return <div className={className}>{children}</div>;
  }

  return (
    <motion.div
      ref={ref}
      className={className}
      initial="hidden"
      animate={inView ? "visible" : "hidden"}
      variants={{
        hidden: {},
        visible: {
          transition: {
            staggerChildren: staggerDelay,
          },
        },
      }}
    >
      {React.Children.map(children, (child) => (
        <motion.div
          variants={viewportVariants.fadeInUp}
          transition={transitions.default}
        >
          {child}
        </motion.div>
      ))}
    </motion.div>
  );
}
