'use client';

import Image from 'next/image';
import { useCallback, useEffect, useMemo, useState } from 'react';

import {
  avatarConnections,
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
    'bg-kiro-purple-500',
    'bg-kiro-purple-600',
    'bg-kiro-purple-700',
  ];

  const colorIndex = Math.abs(
    avatar.id.charCodeAt(avatar.id.length - 1) - 48
  ) % colors.length;
  const bgColor = colors[colorIndex] ?? 'bg-kiro-purple-500';

  return (
    <div
      className={`${bgColor} flex items-center justify-center rounded-full border-2 border-kiro-purple-400/30 shadow-lg shadow-kiro-purple-500/20`}
      style={{ width: size, height: size }}
    >
      <svg
        className="h-1/2 w-1/2 text-kiro-slate-100"
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
      className="absolute transition-all duration-100 hover:scale-110 hover:z-10 hover:drop-shadow-lg"
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
        <div className="relative h-full w-full overflow-hidden rounded-full border-2 border-kiro-purple-400/30 bg-kiro-ink-900 shadow-lg shadow-kiro-purple-500/10">
          <Image
            src={avatar.imageUrl}
            alt={avatar.alt}
            fill
            className="object-cover"
            onError={handleError}
            unoptimized
          />
        </div>
      )}
    </div>
  );
}

interface ConnectionLinesProps {
  viewportSize: ViewportSize;
  containerWidth: number;
  containerHeight: number;
}

function ConnectionLines({
  viewportSize,
  containerWidth,
  containerHeight,
}: ConnectionLinesProps) {
  const lines = useMemo(() => {
    if (containerWidth === 0 || containerHeight === 0) {
      return [];
    }

    return avatarConnections
      .map((connection) => {
        const fromPosition = avatarLayout[connection.from]?.[viewportSize];
        const toPosition = avatarLayout[connection.to]?.[viewportSize];

        if (!fromPosition || !toPosition) {
          return null;
        }

        const fromSize = avatarSizes[fromPosition.size];
        const toSize = avatarSizes[toPosition.size];

        const x1 = (fromPosition.x / 100) * containerWidth + fromSize / 2;
        const y1 = (fromPosition.y / 100) * containerHeight + fromSize / 2;
        const x2 = (toPosition.x / 100) * containerWidth + toSize / 2;
        const y2 = (toPosition.y / 100) * containerHeight + toSize / 2;

        return {
          id: `${connection.from}-${connection.to}`,
          x1,
          y1,
          x2,
          y2,
          color: connection.color,
        };
      })
      .filter((line): line is NonNullable<typeof line> => line !== null);
  }, [viewportSize, containerWidth, containerHeight]);

  if (lines.length === 0) {
    return null;
  }

  return (
    <svg
      className="pointer-events-none absolute inset-0 h-full w-full"
      aria-hidden="true"
    >
      <defs>
        <linearGradient id="lineGradient" x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%" stopColor="#7c5cff" stopOpacity="0.4" />
          <stop offset="50%" stopColor="#9b7cff" stopOpacity="0.6" />
          <stop offset="100%" stopColor="#7c5cff" stopOpacity="0.4" />
        </linearGradient>
      </defs>
      {lines.map((line) => (
        <line
          key={line.id}
          x1={line.x1}
          y1={line.y1}
          x2={line.x2}
          y2={line.y2}
          stroke="url(#lineGradient)"
          strokeWidth="2"
          strokeLinecap="round"
          opacity={0.8}
        />
      ))}
    </svg>
  );
}

export function UserAvatarDisplay({
  className = '',
  avatarCount = 8,
}: UserAvatarDisplayProps) {
  const [viewportSize, setViewportSize] = useState<ViewportSize>('desktop');
  const [containerSize, setContainerSize] = useState({ width: 0, height: 0 });
  const [imageErrors, setImageErrors] = useState<Set<string>>(new Set());
  const [containerRef, setContainerRef] = useState<HTMLDivElement | null>(null);

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

  useEffect(() => {
    if (!containerRef) return;

    const updateContainerSize = () => {
      const rect = containerRef.getBoundingClientRect();
      setContainerSize({ width: rect.width, height: rect.height });
    };

    updateContainerSize();

    const resizeObserver = new ResizeObserver(updateContainerSize);
    resizeObserver.observe(containerRef);

    return () => resizeObserver.disconnect();
  }, [containerRef]);

  const handleImageError = useCallback((id: string) => {
    setImageErrors((prev) => new Set(prev).add(id));
  }, []);

  return (
    <div
      ref={setContainerRef}
      className={`relative h-full min-h-[400px] w-full overflow-hidden ${className}`.trim()}
      role="img"
      aria-label="Network of connected users"
    >
      <ConnectionLines
        viewportSize={viewportSize}
        containerWidth={containerSize.width}
        containerHeight={containerSize.height}
      />

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
  );
}
