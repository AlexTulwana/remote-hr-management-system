export default function SkeletonCard() {
  return (
    <div className="bg-surface-1 rounded-lg p-3.5 animate-pulse">
      <div className="h-3 w-16 bg-surface-2 rounded mb-2.5" />
      <div className="h-5 w-10 bg-surface-2 rounded" />
    </div>
  );
}
