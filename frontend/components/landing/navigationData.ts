import {
  Activity,
  Users,
  Sparkles,
  type LucideIcon,
} from 'lucide-react';

export interface MenuItem {
  label: string;
  id: string;
}

export const MENU_ITEMS: MenuItem[] = [
  { label: 'Platform', id: 'system' },
  { label: 'Community', id: 'community' },
  { label: 'Stories', id: 'stories' },
];

export interface SystemModule {
  title: string;
  role: string;
  icon: LucideIcon;
  stats: { value: string; label: string }[];
}

export const SYSTEM_MODULES: SystemModule[] = [
  {
    title: 'Pulse',
    role: 'AI business planner',
    icon: Activity,
    stats: [
      { value: '2500+', label: 'Launch labs' },
      { value: '7+ regions', label: 'Market scans' },
      { value: '92%', label: 'Signal clarity' },
    ],
  },
  {
    title: 'Relay',
    role: 'Community dashboard',
    icon: Users,
    stats: [
      { value: '3400+', label: 'Signal loops' },
      { value: '100+', label: 'Founder circles' },
      { value: '94%', label: 'Feedback lift' },
    ],
  },
  {
    title: 'Aurora',
    role: 'Smart founder hub',
    icon: Sparkles,
    stats: [
      { value: '120+', label: 'Mentor tracks' },
      { value: '90%', label: 'Milestones hit' },
      { value: '8x', label: 'Momentum' },
    ],
  },
];
