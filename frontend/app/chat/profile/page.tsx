'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/lib/store/authStore';
import { deleteAccount } from '@/lib/api/users';
import { Modal } from '@/components/ui/Modal';
import { Button } from '@/components/ui/Button';

/**
 * Profile page — displays user info, logout, and delete account.
 *
 * Requirements: 6.1, 6.2, 6.3, 6.4
 */
export default function ProfilePage() {
  const router = useRouter();
  const { user, token, logout } = useAuthStore();

  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  if (!user) return null;

  const handleLogOut = () => {
    router.push('/');
    logout();
  };

  const handleDeleteAccount = async () => {
    if (!token) return;
    try {
      setIsDeleting(true);
      setDeleteError(null);
      await deleteAccount(token);
      // Account gone — log out and go home
      logout();
      router.push('/');
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Failed to delete account.';
      setDeleteError(msg);
      setIsDeleting(false);
    }
  };

  const initial = user.displayName?.charAt(0).toUpperCase() ?? '?';

  return (
    <div className="flex flex-col h-full bg-[#13131f] min-w-0">
      <div className="flex-1 flex flex-col items-center md:items-start justify-center px-8 py-12 gap-6">

        {/* Avatar */}
        <div
          className="w-24 h-24 rounded-full bg-gradient-to-br from-kiro-purple-500 to-kiro-purple-700
                     flex items-center justify-center shrink-0"
          aria-hidden="true"
        >
          <span className="text-4xl font-bold text-white select-none">{initial}</span>
        </div>

        {/* User info */}
        <div className="flex flex-col items-center md:items-start gap-1 min-w-0">
          <h1 className="text-xl font-semibold text-white truncate max-w-xs">{user.displayName}</h1>
          <p className="text-sm text-kiro-slate-400 truncate max-w-xs">@{user.username}</p>
        </div>

        {/* Actions */}
        <div className="flex flex-col items-center md:items-start gap-3 mt-2">
          <button
            onClick={handleLogOut}
            aria-label="Log Out"
            className="px-5 py-2.5 rounded-lg border border-red-500/40 text-red-400
                       hover:bg-red-500/10 active:bg-red-500/20 transition-colors
                       text-sm font-medium min-h-[44px] min-w-[44px]"
          >
            Log Out
          </button>

          <button
            onClick={() => { setDeleteError(null); setShowDeleteModal(true); }}
            aria-label="Delete account"
            className="px-5 py-2.5 rounded-lg border border-red-800/40 text-red-600
                       hover:bg-red-900/20 active:bg-red-900/30 transition-colors
                       text-sm font-medium min-h-[44px] min-w-[44px]"
          >
            Delete Account
          </button>
        </div>
      </div>

      {/* Delete account confirmation modal */}
      <Modal
        open={showDeleteModal}
        title="Delete account"
        onClose={() => setShowDeleteModal(false)}
        footer={
          <div className="flex flex-col gap-2 sm:flex-row sm:justify-end">
            <Button variant="secondary" onClick={() => setShowDeleteModal(false)} disabled={isDeleting}>
              Cancel
            </Button>
            <Button variant="danger" onClick={handleDeleteAccount} disabled={isDeleting}>
              {isDeleting ? 'Deleting…' : 'Yes, delete my account'}
            </Button>
          </div>
        }
      >
        <p className="text-sm text-kiro-slate-200">
          This will permanently delete your account, all your messages, and remove you from all rooms and friendships.
          <strong className="text-white"> This cannot be undone.</strong>
        </p>
        {deleteError && (
          <p className="mt-3 text-sm text-red-400" role="alert">{deleteError}</p>
        )}
      </Modal>
    </div>
  );
}
