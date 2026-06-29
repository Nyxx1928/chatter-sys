import * as React from 'react';
import { cva, type VariantProps } from 'class-variance-authority';

import { cn } from '@/lib/utils';

const badgeVariants = cva(
  'inline-flex items-center rounded-pill border border-slack-border px-2.5 py-0.5 text-[10px] font-semibold uppercase tracking-[0.2em] transition-colors focus:outline-none focus:ring-2 focus:ring-slack-primary focus:ring-offset-2',
  {
    variants: {
      variant: {
        default:
          'border-transparent bg-slack-surface-tertiary text-slack-text-primary hover:bg-slack-surface-tertiary',
        secondary:
          'border-transparent bg-slack-surface-secondary text-slack-text-secondary hover:bg-slack-surface-secondary/80',
        destructive:
          'border-transparent bg-slack-accent-red text-slack-text-inverse hover:bg-slack-accent-red/80',
        outline: 'text-slack-text-secondary',
      },
    },
    defaultVariants: {
      variant: 'default',
    },
  }
);

export interface BadgeProps
  extends React.HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof badgeVariants> {}

function Badge({ className, variant, ...props }: BadgeProps) {
  return (
    <div className={cn(badgeVariants({ variant }), className)} {...props} />
  );
}

export { Badge, badgeVariants };
