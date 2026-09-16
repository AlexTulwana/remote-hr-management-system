import { useEffect, useMemo, useState } from 'react';
import { getCalendar } from '../api/calendar';
import Card from '../components/Card';
import Pill from '../components/Pill';
import Button from '../components/Button';
import SkeletonCard from '../components/SkeletonCard';
import EmptyState from '../components/EmptyState';
import ErrorInline from '../components/ErrorInline';

const TYPE_META = {
  CALENDAR_EVENT: { label: 'Event', variant: 'neutral' },
  LEAVE: { label: 'Leave', variant: 'warning' },
  INTERVIEW: { label: 'Interview', variant: 'info' },
  HEARING: { label: 'Hearing', variant: 'urgent' },
  JOB_POSTING_OPEN: { label: 'Job opens', variant: 'success' },
  JOB_POSTING_CLOSE: { label: 'Job closes', variant: 'neutral' },
};

const DOT_COLORS = {
  CALENDAR_EVENT: 'var(--color-text-muted)',
  LEAVE: 'var(--color-warning-text)',
  INTERVIEW: 'var(--color-info-text)',
  HEARING: 'var(--color-danger-text)',
  JOB_POSTING_OPEN: 'var(--color-success-text)',
  JOB_POSTING_CLOSE: 'var(--color-text-muted)',
};

const WEEKDAY_LABELS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

function toDateKey(date) {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

function parseDateKey(key) {
  const [y, m, d] = key.split('-').map(Number);
  return new Date(y, m - 1, d);
}

function formatMonthLabel(date) {
  return date.toLocaleDateString([], { month: 'long', year: 'numeric' });
}

function formatDayLabel(date) {
  return date.toLocaleDateString([], { weekday: 'long', month: 'short', day: 'numeric' });
}

function formatShortDate(date) {
  return date.toLocaleDateString([], { month: 'short', day: 'numeric' });
}

function expandEventDays(item, monthStart, monthEnd) {
  const start = parseDateKey(item.date);
  const end = item.endDate ? parseDateKey(item.endDate) : start;
  const from = start < monthStart ? monthStart : start;
  const to = end > monthEnd ? monthEnd : end;
  const days = [];
  const cursor = new Date(from);
  while (cursor <= to) {
    days.push(toDateKey(cursor));
    cursor.setDate(cursor.getDate() + 1);
  }
  return days;
}

export default function Calendar() {
  const [monthCursor, setMonthCursor] = useState(() => {
    const now = new Date();
    return new Date(now.getFullYear(), now.getMonth(), 1);
  });
  const [selectedDateKey, setSelectedDateKey] = useState(null);
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const monthStart = useMemo(
    () => new Date(monthCursor.getFullYear(), monthCursor.getMonth(), 1),
    [monthCursor]
  );
  const monthEnd = useMemo(
    () => new Date(monthCursor.getFullYear(), monthCursor.getMonth() + 1, 0),
    [monthCursor]
  );

  useEffect(() => {
    async function load() {
      setLoading(true);
      setError('');
      setSelectedDateKey(null);
      try {
        const data = await getCalendar(toDateKey(monthStart), toDateKey(monthEnd));
        setEvents(data);
      } catch {
        setError('Could not load the calendar.');
      } finally {
        setLoading(false);
      }
    }
    load();
  }, [monthStart, monthEnd]);

  const eventsByDay = useMemo(() => {
    const map = new Map();
    events.forEach((item) => {
      expandEventDays(item, monthStart, monthEnd).forEach((key) => {
        if (!map.has(key)) map.set(key, []);
        map.get(key).push(item);
      });
    });
    return map;
  }, [events, monthStart, monthEnd]);

  const gridDays = useMemo(() => {
    const cells = [];
    const leading = monthStart.getDay();
    const cursor = new Date(monthStart);
    cursor.setDate(cursor.getDate() - leading);
    for (let i = 0; i < 42; i += 1) {
      cells.push(new Date(cursor));
      cursor.setDate(cursor.getDate() + 1);
    }
    return cells;
  }, [monthStart]);

  const agendaEntries = useMemo(() => {
    const keys = selectedDateKey
      ? [selectedDateKey]
      : Array.from(eventsByDay.keys()).sort();
    return keys
      .filter((key) => eventsByDay.has(key))
      .map((key) => ({ key, date: parseDateKey(key), items: eventsByDay.get(key) }));
  }, [selectedDateKey, eventsByDay]);

  const todayKey = toDateKey(new Date());

  function goToMonth(offset) {
    setMonthCursor((prev) => new Date(prev.getFullYear(), prev.getMonth() + offset, 1));
  }

  function goToToday() {
    const now = new Date();
    setMonthCursor(new Date(now.getFullYear(), now.getMonth(), 1));
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-5">
        <p className="text-[20px] font-medium">Calendar</p>
        <div className="flex items-center gap-2">
          <Button variant="secondary" onClick={() => goToMonth(-1)}>&larr;</Button>
          <Button variant="secondary" onClick={goToToday}>Today</Button>
          <Button variant="secondary" onClick={() => goToMonth(1)}>&rarr;</Button>
        </div>
      </div>

      {loading ? (
        <div className="flex flex-col gap-3.5">
          <SkeletonCard />
          <SkeletonCard />
        </div>
      ) : error ? (
        <ErrorInline message={error} />
      ) : (
        <>
          <Card>
            <p className="text-[15px] font-medium mb-3">{formatMonthLabel(monthCursor)}</p>
            <div className="grid grid-cols-7 gap-1.5">
              {WEEKDAY_LABELS.map((label) => (
                <div key={label} className="text-[11px] text-text-muted text-center py-1">
                  {label}
                </div>
              ))}
              {gridDays.map((date) => {
                const key = toDateKey(date);
                const inMonth = date.getMonth() === monthCursor.getMonth();
                const dayEvents = eventsByDay.get(key) || [];
                const isSelected = selectedDateKey === key;
                const isToday = key === todayKey;
                return (
                  <button
                    key={key}
                    onClick={() => setSelectedDateKey(isSelected ? null : key)}
                    className={[
                      'aspect-square rounded-lg p-1.5 flex flex-col items-start text-left cursor-pointer transition-colors',
                      inMonth ? 'bg-surface-2' : 'bg-surface-0',
                      isSelected ? 'border border-text-primary' : 'border border-transparent',
                    ].join(' ')}
                  >
                    <span
                      className={[
                        'text-[12px] w-5 h-5 flex items-center justify-center rounded-full',
                        isToday
                          ? 'bg-text-primary text-surface-2 font-semibold'
                          : inMonth ? 'text-text-primary' : 'text-text-muted',
                      ].join(' ')}
                    >
                      {date.getDate()}
                    </span>
                    <div className="flex flex-wrap gap-0.5 mt-auto">
                      {dayEvents.slice(0, 3).map((item, i) => (
                        <span
                          key={`${item.sourceType}-${item.sourceId}-${i}`}
                          className="w-1.5 h-1.5 rounded-full"
                          style={{ background: DOT_COLORS[item.sourceType] || 'var(--color-text-muted)' }}
                        />
                      ))}
                      {dayEvents.length > 3 ? (
                        <span className="text-[9px] text-text-muted leading-none">+{dayEvents.length - 3}</span>
                      ) : null}
                    </div>
                  </button>
                );
              })}
            </div>
          </Card>

          <p className="text-[13px] font-medium text-text-secondary mt-5 mb-2.5">
            {selectedDateKey ? formatDayLabel(parseDateKey(selectedDateKey)) : 'This month'}
          </p>

          {agendaEntries.length === 0 ? (
            <Card>
              <EmptyState message="No events for this period." />
            </Card>
          ) : (
            <div className="flex flex-col gap-3.5">
              {agendaEntries.map((entry) => (
                <Card key={entry.key}>
                  {!selectedDateKey ? (
                    <p className="text-[12px] text-text-muted mb-2">{formatDayLabel(entry.date)}</p>
                  ) : null}
                  <div className="flex flex-col gap-2.5">
                    {entry.items.map((item, i) => {
                      const meta = TYPE_META[item.sourceType] || { label: item.sourceType, variant: 'neutral' };
                      return (
                        <div key={`${item.sourceType}-${item.sourceId}-${i}`}>
                          <div className="flex items-center justify-between mb-1">
                            <p className="text-[14px] font-medium">{item.title}</p>
                            <Pill variant={meta.variant}>{meta.label}</Pill>
                          </div>
                          {item.description ? (
                            <p className="text-[13px] text-text-secondary mb-1">{item.description}</p>
                          ) : null}
                          <p className="text-[11px] text-text-muted">
                            {item.endDate && item.endDate !== item.date
                              ? `${formatShortDate(parseDateKey(item.date))} \u2013 ${formatShortDate(parseDateKey(item.endDate))}`
                              : formatShortDate(parseDateKey(item.date))}
                          </p>
                        </div>
                      );
                    })}
                  </div>
                </Card>
              ))}
            </div>
          )}
        </>
      )}
    </div>
  );
}
