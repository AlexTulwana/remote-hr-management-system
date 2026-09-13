import Button from './Button';

export default function EmptyState({ icon: Icon, message, actionLabel, onAction }) {
  return (
    <div className="flex flex-col items-center justify-center text-center py-10 px-4">
      {Icon ? <Icon size={28} className="text-text-muted mb-3" /> : null}
      <p className="text-[13px] text-text-secondary mb-4">{message}</p>
      {actionLabel ? (
        <Button variant="secondary" onClick={onAction}>
          {actionLabel}
        </Button>
      ) : null}
    </div>
  );
}
