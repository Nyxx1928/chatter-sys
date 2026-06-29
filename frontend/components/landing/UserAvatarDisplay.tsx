'use client';

import Image from 'next/image';
import { useCallback, useEffect, useMemo, useState } from 'react';

import {
  avatarLayout,
  avatarSizes,
  placeholderAvatars,
} from '@/lib/data/avatars';
import type { AvatarData, AvatarBreakpointPosition } from '@/lib/data/avatars';

export interface UserAvatarDisplayProps {
  className?: string;
  avatarCount?: number;
}

type ViewportSize = 'mobile' | 'tablet' | 'desktop';

interface FallbackAvatarProps {
  avatar: AvatarData;
  size: number;
}

function FallbackAvatar({ avatar, size }: FallbackAvatarProps) {
  const colors = [
    'bg-slack-primary',
    'bg-slack-primary',
    'bg-slack-primary-light',
  ];

  const colorIndex = Math.abs(
    avatar.id.charCodeAt(avatar.id.length - 1) - 48
  ) % colors.length;
  const bgColor = colors[colorIndex] ?? 'bg-slack-primary';

  return (
    <div
      className={`${bgColor} flex items-center justify-center rounded-full border-2 border-slack-primary/30 shadow-lg shadow-slack-primary/20`}
      style={{ width: size, height: size }}
    >
      <svg
        className="h-1/2 w-1/2 text-slack-text-inverse"
        fill="currentColor"
        viewBox="0 0 24 24"
        aria-hidden="true"
      >
        <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" />
      </svg>
    </div>
  );
}

interface AvatarItemProps {
  avatar: AvatarData;
  position: AvatarBreakpointPosition;
  onImageError: (id: string) => void;
  hasError: boolean;
}

function AvatarItem({
  avatar,
  position,
  onImageError,
  hasError,
}: AvatarItemProps) {
  const size = avatarSizes[position.size];

  const handleError = useCallback(() => {
    onImageError(avatar.id);
  }, [avatar.id, onImageError]);

  return (
    <div
      className="absolute transition-transform duration-100 will-change-transform hover:scale-110 hover:z-10 hover:drop-shadow-lg"
      style={{
        left: `${position.x}%`,
        top: `${position.y}%`,
        width: size,
        height: size,
      }}
    >
      {hasError ? (
        <FallbackAvatar avatar={avatar} size={size} />
      ) : (
        <div className="relative h-full w-full overflow-hidden rounded-full border-2 border-slack-primary/30 bg-slack-surface-primary shadow-lg shadow-slack-primary/10">
          <Image
            src={avatar.imageUrl}
            alt={avatar.alt}
            fill
            className="object-cover"
            sizes={`${size}px`}
            placeholder={avatar.blurDataUrl ? 'blur' : 'empty'}
            blurDataURL={avatar.blurDataUrl}
            loading="lazy"
            onError={handleError}
          />
        </div>
      )}
    </div>
  );
}

function AmbientField() {
  return (
    <div aria-hidden className="pointer-events-none absolute inset-0 z-0">
      <div className="absolute inset-0 opacity-40 [background-image:radial-gradient(circle,rgba(74,21,75,0.2)_1px,transparent_1px)] [background-size:32px_32px]" />
      <div className="absolute -left-8 top-10 h-28 w-28 rounded-full bg-slack-primary/25 blur-3xl" />
      <div className="absolute right-8 top-14 h-20 w-20 rounded-full bg-slack-primary/20 blur-3xl" />
      <div className="absolute bottom-8 left-1/2 h-32 w-32 -translate-x-1/2 rounded-full bg-slack-primary/20 blur-[90px]" />
    </div>
  );
}

export function UserAvatarDisplay({
  className = '',
  avatarCount = 8,
}: UserAvatarDisplayProps) {
  const [viewportSize, setViewportSize] = useState<ViewportSize>('desktop');
  const [imageErrors, setImageErrors] = useState<Set<string>>(new Set());

  const displayAvatars = useMemo(
    () => placeholderAvatars.slice(0, avatarCount),
    [avatarCount]
  );

  useEffect(() => {
    const updateViewportSize = () => {
      const width = window.innerWidth;
      if (width < 640) {
        setViewportSize('mobile');
      } else if (width < 1024) {
        setViewportSize('tablet');
      } else {
        setViewportSize('desktop');
      }
    };

    updateViewportSize();
    window.addEventListener('resize', updateViewportSize);

    return () => window.removeEventListener('resize', updateViewportSize);
  }, []);

  const handleImageError = useCallback((id: string) => {
    setImageErrors((prev) => new Set(prev).add(id));
  }, []);

  return (
    <div
      className={`relative h-full min-h-[320px] w-full overflow-hidden sm:min-h-[360px] lg:min-h-[400px] ${className}`.trim()}
      role="img"
      aria-label="Network of connected users"
    >
      <AmbientField />
      <div className="relative z-10">
        {displayAvatars.map((avatar) => {
          const position = avatarLayout[avatar.id]?.[viewportSize];
          if (!position) return null;

          return (
            <AvatarItem
              key={avatar.id}
              avatar={avatar}
              position={position}
              onImageError={handleImageError}
              hasError={imageErrors.has(avatar.id)}
            />
          );
        })}
      </div>
    </div>
  );
}
