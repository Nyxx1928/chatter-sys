# Authentication Components

This directory contains authentication-related form components for the real-time chat system.

## Components

### LoginForm

A mobile-first login form component with username and password inputs.

**Features:**
- Client-side validation with error messages
- Integration with Zustand auth store
- Loading states during authentication
- Accessible form controls with ARIA labels
- Mobile-optimized with 44x44px minimum touch targets
- Auto-redirect to chat on successful login

**Usage:**
```tsx
import { LoginForm } from '@/components/auth';

export default function LoginPage() {
  return (
    <div className="flex items-center justify-center min-h-screen">
      <LoginForm />
    </div>
  );
}
```

**Validation Rules:**
- Username: Required, 3-50 characters
- Password: Required, minimum 6 characters

**Requirements:** 1.1, 1.2, 15.1, 15.2, 17.3

---

### RegisterForm

A mobile-first registration form component with username, email, password, and display name inputs.

**Features:**
- Comprehensive client-side validation
- Password confirmation matching
- Email format validation
- Username format validation (alphanumeric, hyphens, underscores only)
- Integration with Zustand auth store
- Loading states during registration
- Accessible form controls with ARIA labels
- Mobile-optimized with 44x44px minimum touch targets
- Auto-redirect to login page on successful registration

**Usage:**
```tsx
import { RegisterForm } from '@/components/auth';

export default function RegisterPage() {
  return (
    <div className="flex items-center justify-center min-h-screen">
      <RegisterForm />
    </div>
  );
}
```

**Validation Rules:**
- Username: Required, 3-50 characters, alphanumeric with hyphens/underscores only
- Email: Required, valid email format, max 100 characters
- Display Name: Required, 2-100 characters
- Password: Required, minimum 6 characters, max 100 characters
- Confirm Password: Must match password

**Requirements:** 1.1, 1.2, 15.1, 15.2, 17.3

---

## Integration

Both components integrate with the Zustand auth store (`lib/store/authStore.ts`) which provides:
- `login(request: LoginRequest)` - Authenticates user and stores token
- `register(request: RegisterRequest)` - Registers new user

The auth store automatically:
- Persists authentication tokens to browser storage
- Updates authentication state
- Handles API errors

## Error Handling

Both forms provide comprehensive error handling:

**Field-level errors:**
- Displayed inline below each input field
- Cleared when user starts typing
- Accessible via ARIA attributes

**Form-level errors:**
- Displayed in a prominent alert box above the submit button
- Used for API errors (invalid credentials, duplicate username/email, etc.)
- Automatically mapped to specific fields when possible

## Accessibility

All components follow WCAG AA accessibility standards:
- Semantic HTML with proper form elements
- ARIA labels and descriptions
- Keyboard navigation support
- Focus indicators
- Error announcements via `role="alert"`
- Minimum 44x44px touch targets for mobile

## Demo

View the components in action at `/auth-demo` when running the development server.

## Dependencies

- React 19+
- Next.js 14+ (App Router)
- Zustand (state management)
- Tailwind CSS (styling)
- Base UI components (Button, Input)

## Related Files

- `lib/store/authStore.ts` - Authentication state management
- `lib/api/auth.ts` - Authentication API calls
- `types/api.ts` - Request/response type definitions
- `components/ui/Button.tsx` - Button component
- `components/ui/Input.tsx` - Input component
