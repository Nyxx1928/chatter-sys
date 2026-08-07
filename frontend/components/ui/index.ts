/**
 * UI Components - Base reusable components with mobile-first design
 * 
 * All components follow:
 * - Mobile-first responsive design (Requirement 13.2, 13.3, 13.4)
 * - Minimum 44x44px touch targets for mobile (Requirement 15.5)
 * - Tailwind CSS styling (Requirement 13.5)
 * - TypeScript type safety (Requirement 17)
 * - Accessibility standards (Requirement 18)
 */

export { Button } from './Button';
export type { ButtonProps } from './Button';

export { Input, TextArea } from './Input';
export type { InputProps, TextAreaProps } from './Input';

export { Card, CardHeader, CardTitle, CardContent, CardFooter } from './Card';
export type { CardProps, CardHeaderProps, CardTitleProps, CardContentProps, CardFooterProps } from './Card';

export { Modal } from './Modal';
export type { ModalProps } from './Modal';

export { AnimatedStatCounter } from './AnimatedStatCounter';

export { AnimatedCard } from './AnimatedCard';
