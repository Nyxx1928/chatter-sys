'use client';

import { useState } from 'react';
import { Button, Input, Modal, TextArea } from '@/components/ui';

export interface RoomCreateModalProps {
  open: boolean;
  isSubmitting?: boolean;
  errorMessage?: string | null;
  onClose: () => void;
  onCreate: (name: string, description: string) => Promise<void>;
}

/**
 * Modal for creating a new chat room.
 */
export function RoomCreateModal({
  open,
  isSubmitting = false,
  errorMessage,
  onClose,
  onCreate
}: RoomCreateModalProps) {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    await onCreate(name.trim(), description.trim());
  };

  return (
    <Modal open={open} title="Create a new room" onClose={onClose}
      footer={
        <div className="flex flex-col gap-2 sm:flex-row sm:justify-end">
          <Button variant="secondary" onClick={onClose} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button type="submit" form="room-create-form" disabled={isSubmitting || !name.trim()}>
            {isSubmitting ? 'Creating...' : 'Create room'}
          </Button>
        </div>
      }
    >
      <form id="room-create-form" className="space-y-4" onSubmit={handleSubmit}>
        <Input
          label="Room name"
          placeholder="Enter a unique room name"
          value={name}
          onChange={(event) => setName(event.target.value)}
          fullWidth
          required
          autoFocus
        />
        <TextArea
          label="Description (optional)"
          placeholder="Add a short description"
          value={description}
          onChange={(event) => setDescription(event.target.value)}
          fullWidth
          rows={4}
        />
        {errorMessage && (
          <p className="text-sm text-red-600" role="alert">
            {errorMessage}
          </p>
        )}
      </form>
    </Modal>
  );
}
