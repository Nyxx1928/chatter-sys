/**
 * Avatar data and layout configuration for the landing page UserAvatarDisplay component.
 * Uses DiceBear API for placeholder avatars with responsive positioning across devices.
 * 
 * Requirements: 2.3, 5.1, 5.4
 */

/**
 * Represents a single avatar's data
 */
export interface AvatarData {
  id: string;
  imageUrl: string;
  alt: string;
  blurDataUrl?: string;
}

/**
 * Position configuration for a single avatar at a specific breakpoint
 */
export interface AvatarBreakpointPosition {
  x: number;
  y: number;
  size: 'sm' | 'md' | 'lg';
}

/**
 * Responsive position configuration for an avatar across all breakpoints
 */
export interface AvatarPosition {
  desktop: AvatarBreakpointPosition;
  tablet: AvatarBreakpointPosition;
  mobile: AvatarBreakpointPosition;
}

/**
 * Represents a connection line between two avatars
 */
export interface AvatarConnection {
  from: string;
  to: string;
  color: string;
}

/**
 * Size mappings for avatar sizes (in pixels)
 * Mobile sizes are larger (min 44px) for better touch targets
 */
export const avatarSizes: Record<'sm' | 'md' | 'lg', number> = {
  sm: 44, // Minimum touch target size for mobile
  md: 56,
  lg: 72,
};

/**
 * Array of placeholder avatars using DiceBear API
 * Seeds are used to generate consistent, unique avatars
 */
const DEFAULT_AVATAR_BLUR =
  'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMiIgaGVpZ2h0PSIyIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPjxyZWN0IHdpZHRoPSIyIiBoZWlnaHQ9IjIiIGZpbGw9IiM2ZjQyYzEiLz48L3N2Zz4=';

export const placeholderAvatars: AvatarData[] = [
  {
    id: 'avatar-1',
    imageUrl: 'https://api.dicebear.com/7.x/avataaars/png?seed=Felix',
    alt: 'User avatar 1',
    blurDataUrl: DEFAULT_AVATAR_BLUR,
  },
  {
    id: 'avatar-2',
    imageUrl: 'https://api.dicebear.com/7.x/avataaars/png?seed=Luna',
    alt: 'User avatar 2',
    blurDataUrl: DEFAULT_AVATAR_BLUR,
  },
  {
    id: 'avatar-3',
    imageUrl: 'https://api.dicebear.com/7.x/avataaars/png?seed=Max',
    alt: 'User avatar 3',
    blurDataUrl: DEFAULT_AVATAR_BLUR,
  },
  {
    id: 'avatar-4',
    imageUrl: 'https://api.dicebear.com/7.x/avataaars/png?seed=Zoe',
    alt: 'User avatar 4',
    blurDataUrl: DEFAULT_AVATAR_BLUR,
  },
  {
    id: 'avatar-5',
    imageUrl: 'https://api.dicebear.com/7.x/avataaars/png?seed=Leo',
    alt: 'User avatar 5',
    blurDataUrl: DEFAULT_AVATAR_BLUR,
  },
  {
    id: 'avatar-6',
    imageUrl: 'https://api.dicebear.com/7.x/avataaars/png?seed=Mia',
    alt: 'User avatar 6',
    blurDataUrl: DEFAULT_AVATAR_BLUR,
  },
  {
    id: 'avatar-7',
    imageUrl: 'https://api.dicebear.com/7.x/avataaars/png?seed=Oscar',
    alt: 'User avatar 7',
    blurDataUrl: DEFAULT_AVATAR_BLUR,
  },
  {
    id: 'avatar-8',
    imageUrl: 'https://api.dicebear.com/7.x/avataaars/png?seed=Bella',
    alt: 'User avatar 8',
    blurDataUrl: DEFAULT_AVATAR_BLUR,
  },
];

/**
 * Responsive layout configuration for each avatar
 * Positions are percentage-based (x: 0-100, y: 0-100)
 * Arranged artistically to create a network/connected appearance
 */
export const avatarLayout: Record<string, AvatarPosition> = {
  'avatar-1': {
    desktop: { x: 8, y: 25, size: 'md' },
    tablet: { x: 5, y: 20, size: 'md' },
    mobile: { x: 8, y: 12, size: 'sm' },
  },
  'avatar-2': {
    desktop: { x: 22, y: 15, size: 'lg' },
    tablet: { x: 18, y: 10, size: 'md' },
    mobile: { x: 30, y: 5, size: 'sm' },
  },
  'avatar-3': {
    desktop: { x: 18, y: 55, size: 'sm' },
    tablet: { x: 10, y: 42, size: 'sm' },
    mobile: { x: 5, y: 28, size: 'sm' },
  },
  'avatar-4': {
    desktop: { x: 35, y: 35, size: 'lg' },
    tablet: { x: 32, y: 28, size: 'md' },
    mobile: { x: 40, y: 18, size: 'md' },
  },
  'avatar-5': {
    desktop: { x: 55, y: 18, size: 'md' },
    tablet: { x: 55, y: 14, size: 'sm' },
    mobile: { x: 65, y: 8, size: 'sm' },
  },
  'avatar-6': {
    desktop: { x: 70, y: 45, size: 'lg' },
    tablet: { x: 70, y: 38, size: 'md' },
    mobile: { x: 75, y: 28, size: 'md' },
  },
  'avatar-7': {
    desktop: { x: 85, y: 25, size: 'md' },
    tablet: { x: 85, y: 22, size: 'sm' },
    mobile: { x: 88, y: 15, size: 'sm' },
  },
  'avatar-8': {
    desktop: { x: 78, y: 65, size: 'sm' },
    tablet: { x: 78, y: 55, size: 'sm' },
    mobile: { x: 85, y: 42, size: 'sm' },
  },
};

/**
 * Connections between avatars for drawing SVG lines
 * Creates a network pattern that emphasizes the social/connected nature of the app
 */
export const avatarConnections: AvatarConnection[] = [
  { from: 'avatar-1', to: 'avatar-2', color: 'rgba(168, 85, 247, 0.4)' },
  { from: 'avatar-1', to: 'avatar-3', color: 'rgba(168, 85, 247, 0.3)' },
  { from: 'avatar-2', to: 'avatar-4', color: 'rgba(168, 85, 247, 0.5)' },
  { from: 'avatar-3', to: 'avatar-4', color: 'rgba(168, 85, 247, 0.35)' },
  { from: 'avatar-4', to: 'avatar-5', color: 'rgba(168, 85, 247, 0.4)' },
  { from: 'avatar-5', to: 'avatar-6', color: 'rgba(168, 85, 247, 0.45)' },
  { from: 'avatar-5', to: 'avatar-7', color: 'rgba(168, 85, 247, 0.3)' },
  { from: 'avatar-6', to: 'avatar-7', color: 'rgba(168, 85, 247, 0.35)' },
  { from: 'avatar-6', to: 'avatar-8', color: 'rgba(168, 85, 247, 0.4)' },
  { from: 'avatar-2', to: 'avatar-5', color: 'rgba(168, 85, 247, 0.25)' },
  { from: 'avatar-4', to: 'avatar-6', color: 'rgba(168, 85, 247, 0.3)' },
];

/**
 * Helper function to get avatar data by ID
 */
export function getAvatarById(id: string): AvatarData | undefined {
  return placeholderAvatars.find((avatar) => avatar.id === id);
}

/**
 * Helper function to get avatar position for a specific breakpoint
 */
export function getAvatarPosition(
  avatarId: string,
  breakpoint: 'desktop' | 'tablet' | 'mobile'
): AvatarBreakpointPosition | undefined {
  const layout = avatarLayout[avatarId];
  return layout?.[breakpoint];
}

/**
 * Helper function to calculate the center point of an avatar
 * Used for drawing connection lines between avatars
 */
export function getAvatarCenter(
  avatarId: string,
  breakpoint: 'desktop' | 'tablet' | 'mobile',
  containerWidth: number,
  containerHeight: number
): { x: number; y: number } | undefined {
  const position = getAvatarPosition(avatarId, breakpoint);
  if (!position) return undefined;

  const size = avatarSizes[position.size];
  return {
    x: (position.x / 100) * containerWidth + size / 2,
    y: (position.y / 100) * containerHeight + size / 2,
  };
}
