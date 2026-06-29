'use client';

import { NavigationMenuContent } from '@/components/ui/navigation-menu';
import { SYSTEM_MODULES } from './navigationData';
import { ArrowUpRight } from 'lucide-react';

export function SystemMegaMenu() {
  return (
    <NavigationMenuContent className="p-0">
      <div className="grid w-[700px] grid-cols-3 gap-6 divide-x divide-slack-border px-8 py-8">
        {SYSTEM_MODULES.map((module) => (
          <div key={module.title} className="flex flex-col px-3">
            <div className="mb-3 inline-flex h-10 w-10 items-center justify-center rounded-xl bg-slack-surface-tertiary">
              <module.icon className="h-5 w-5 text-slack-primary" />
            </div>
            <h4 className="mb-1 text-sm font-semibold text-slack-text-primary">
              {module.title}
            </h4>
            <p className="mb-3 text-sm tracking-tight text-slack-text-secondary">
              {module.role}
            </p>
            <div className="flex flex-wrap gap-2">
              {module.stats.map((stat) => (
                  <span
                    key={stat.label}
                    className="inline-flex items-center rounded-full border border-slack-border bg-slack-surface-secondary px-2.5 py-0.5 text-[10px] font-semibold uppercase tracking-[0.15em] text-slack-text-secondary"
                  >
                    {stat.value}
                  </span>
                ))}
            </div>
            <a
              href="#system"
              className="group mt-4 flex items-center text-xs font-semibold text-slack-primary transition-colors hover:text-slack-primary-light"
            >
              Explore {module.title}{' '}
              <ArrowUpRight className="ml-1 size-3.5 transition-transform group-hover:translate-x-0.5" />
            </a>
          </div>
        ))}
      </div>
    </NavigationMenuContent>
  );
}
