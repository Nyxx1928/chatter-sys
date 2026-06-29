export const formatRelativeTime = (timestamp: string): string => {
  const now = Date.now();
  const date = new Date(timestamp).getTime();
  const diffMs = now - date;
  const diffSecs = Math.floor(diffMs / 1000);
  const diffMins = Math.floor(diffSecs / 60);
  const diffHours = Math.floor(diffMins / 60);
  const diffDays = Math.floor(diffHours / 24);

  if (diffSecs < 60) return 'now';
  if (diffMins < 60) return `${diffMins}m ago`;
  if (diffHours < 24) return `${diffHours}h ago`;
  if (diffDays === 1) return 'Yesterday';
  if (diffDays < 7) return `${diffDays}d ago`;

  const d = new Date(timestamp);
  const month = d.toLocaleString('default', { month: 'short' });
  return `${month} ${d.getDate()}`;
};

export const formatMessageTime = (timestamp: string): string => {
  const d = new Date(timestamp);
  return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
};

export const isSameDay = (a: string, b: string): boolean => {
  const da = new Date(a);
  const db = new Date(b);
  return da.getFullYear() === db.getFullYear()
    && da.getMonth() === db.getMonth()
    && da.getDate() === db.getDate();
};

export const formatDateSeparator = (timestamp: string): string => {
  const now = new Date();
  const date = new Date(timestamp);
  if (isSameDay(timestamp, now.toISOString())) return 'Today';
  const yesterday = new Date(now);
  yesterday.setDate(yesterday.getDate() - 1);
  if (isSameDay(timestamp, yesterday.toISOString())) return 'Yesterday';
  return date.toLocaleDateString(undefined, { weekday: 'long', month: 'long', day: 'numeric' });
};
