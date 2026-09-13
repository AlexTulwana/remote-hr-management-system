export default function SkeletonRow() {
  return (
    <div className="flex items-center justify-between px-3.5 py-2.5 border-b border-border last:border-b-0 animate-pulse">
      <div className="flex items-center gap-2.5">
        <div className="w-[26px] h-[26px] rounded-full bg-surface-1" />
        <div>
          <div className="h-3 w-28 bg-surface-1 rounded mb-1.5" />
          <div className="h-2.5 w-20 bg-surface-1 rounded" />
        </div>
      </div>
      <div className="h-5 w-14 bg-surface-1 rounded-full" />
    </div>
  );
}
