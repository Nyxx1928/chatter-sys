export const easings = {
  smooth: [0.21, 0.47, 0.32, 0.98] as const,
  snappy: [0.4, 0, 0.2, 1] as const,
};

export const durations = {
  fast: 0.1,
  normal: 0.3,
  slow: 0.6,
} as const;

export const transitions = {
  default: { duration: durations.normal, ease: easings.smooth },
  fast: { duration: durations.fast, ease: easings.smooth },
  slow: { duration: durations.slow, ease: easings.smooth },
  snappy: { duration: durations.normal, ease: easings.snappy },
};

export const viewportVariants = {
  fadeInUp: {
    hidden: { opacity: 0, y: 24 },
    visible: {
      opacity: 1,
      y: 0,
      transition: transitions.default,
    },
  },
  fadeIn: {
    hidden: { opacity: 0 },
    visible: {
      opacity: 1,
      transition: transitions.default,
    },
  },
  stagger: {
    hidden: { opacity: 0 },
    visible: {
      opacity: 1,
      transition: {
        staggerChildren: 0.1,
        ...transitions.default,
      },
    },
  },
};
