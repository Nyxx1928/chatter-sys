import { ForceDarkMode } from '@/components/theme/ForceDarkMode';

export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <>
      <ForceDarkMode />
      {children}
    </>
  );
}
