"use client";

import React from "react";
import { motion } from "framer-motion";
import { useReducedMotion } from "@/hooks/useReducedMotion";

interface AnimatedCardProps {
  children: React.ReactNode;
  className?: string;
  enableHover?: boolean;
}

const hoverShadow = "0 12px 24px rgba(74, 21, 75, 0.15)";

export function AnimatedCard({
  children,
  className = "",
  enableHover = true,
}: AnimatedCardProps) {
  const prefersReducedMotion = useReducedMotion();
  const shouldAnimate = enableHover && !prefersReducedMotion;

  return (
    <motion.div
      className={className}
      whileHover={
        shouldAnimate
          ? {
              y: -4,
              boxShadow: hoverShadow,
            }
          : undefined
      }
      transition={{ duration: 0.2 }}
    >
      {children}
    </motion.div>
  );
}
