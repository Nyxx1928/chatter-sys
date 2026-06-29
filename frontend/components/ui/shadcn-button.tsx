import * as React from 'react';
import { Slot } from '@radix-ui/react-slot';
import { cva, type VariantProps } from 'class-variance-authority';

import { cn } from '@/lib/utils';

const buttonVariants = cva(
  'inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-pill text-xs font-semibold uppercase tracking-[0.2em] transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-slack-primary focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 [&_svg]:pointer-events-none [&_svg]:size-4 [&_svg]:shrink-0',
  {
    variants: {
      variant: {
        default:
          'bg-slack-primary text-slack-text-inverse shadow-slack-lg hover:bg-slack-primary-light',
        destructive:
          'bg-slack-accent-red text-slack-text-inverse shadow-sm hover:bg-slack-accent-red/80',
        outline:
          'border border-slack-border bg-slack-surface-primary shadow-sm hover:bg-slack-surface-tertiary text-slack-text-primary',
        secondary:
          'bg-slack-surface-secondary text-slack-text-secondary shadow-sm hover:bg-slack-surface-tertiary',
        ghost: 'text-slack-text-secondary hover:bg-slack-surface-tertiary hover:text-slack-text-primary',
        link: 'text-slack-primary underline-offset-4 hover:underline',
      },
      size: {
        default: 'h-9 px-5 py-2',
        sm: 'h-8 rounded-pill px-4 text-[10px]',
        lg: 'h-10 rounded-pill px-6',
        icon: 'h-9 w-9 rounded-full',
        'icon-lg': 'h-10 w-10 rounded-full',
      },
    },
    defaultVariants: {
      variant: 'default',
      size: 'default',
    },
  }
);

export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {
  asChild?: boolean;
}

const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, asChild = false, ...props }, ref) => {
    const Comp = asChild ? Slot : 'button';
    return (
      <Comp
        className={cn(buttonVariants({ variant, size, className }))}
        ref={ref}
        {...props}
      />
    );
  }
);
Button.displayName = 'Button';

export { Button, buttonVariants };
