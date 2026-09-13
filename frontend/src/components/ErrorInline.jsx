export default function ErrorInline({ message }) {
  return (
    <div className="flex items-center gap-2 px-3 py-2 rounded-lg bg-danger-bg text-danger-text text-[12px]">
      <span>{message}</span>
    </div>
  );
}
