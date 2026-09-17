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

function getWeekRange(date) {
  const start = new Date(date);
  start.setDate(start.getDate() - start.getDay());
  const end = new Date(start);
  end.setDate(end.getDate() + 6);
  return { start, end };
}

export default function Calendar() {
  const [monthCursor, setMonthCursor] = useState(() => {
    const now = new Date();
    return new Date(now.getFullYear(), now.getMonth(), 1);
  });
  const [selectedDateKey, setSelectedDateKey] = useState(null);
  const [filterMode, setFilterMode] = useState('month');
  const [events, setEvents] = useState([]);
  const [hasLoaded, setHasLoaded] = useState(false);
  const [fetching, setFetching] = useState(true);
  const [error, setError] = useState('');

  const monthStart = useMemo(
    () => new Date(monthCursor.getFullYear(), monthCursor.getMonth(), 1),
    [monthCursor]
  );
  const monthEnd = useMemo(
    () => new Date(monthCursor.getFullYear(), monthCursor.getMonth() + 1, 0),
    [monthCursor]
  );
  const monthKey = `${monthCursor.getFullYear()}-${monthCursor.getMonth()}`;

  useEffect(() => {
    let cancelled = false;
    async function load() {
      setFetching(true);
      setError('');
      try {
        const data = await getCalendar(toDateKey(monthStart), toDateKey(monthEnd));
        if (!cancelled) setEvents(data);
      } catch {
        if (!cancelled) setError('Could not load the calendar.');
      } finally {
        if (!cancelled) {
          setFetching(false);
          setHasLoaded(true);
        }
      }
    }
    load();
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [monthKey]);

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

  const monthGridDays = useMemo(() => {
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

  const weekGridDays = useMemo(() => {
    const { start } = getWeekRange(new Date());
    const cells = [];
    const cursor = new Date(start);
    for (let i = 0; i < 7; i += 1) {
      cells.push(new Date(cursor));
      cursor.setDate(cursor.getDate() + 1);
    }
    return cells;
  }, []);

  const todayKey = toDateKey(new Date());

  const agendaEntries = useMemo(() => {
    let keys;
    if (selectedDateKey) {
      keys = [selectedDateKey];
    } else if (filterMode === 'day') {
      keys = [todayKey];
    } else if (filterMode === 'week') {
      keys = weekGridDays.map(toDateKey);
    } else {
      keys = Array.from(eventsByDay.keys()).sort();
    }
    return keys
      .filter((key) => eventsByDay.has(key))
      .map((key) => ({ key, date: parseDateKey(key), items: eventsByDay.get(key) }));
  }, [selectedDateKey, filterMode, eventsByDay, todayKey, weekGridDays]);

  function goToMonth(offset) {
    setSelectedDateKey(null);
    setFilterMode('month');
    setMonthCursor((prev) => new Date(prev.getFullYear(), prev.getMonth() + offset, 1));
  }

  function jumpToCurrentMonth() {
    const now = new Date();
    setMonthCursor(new Date(now.getFullYear(), now.getMonth(), 1));
  }

  function handleFilterChange(mode) {
    setSelectedDateKey(null);
    setFilterMode(mode);
    jumpToCurrentMonth();
  }

  function handleDayClick(key) {
    setSelectedDateKey((prev) => (prev === key ? null : key));
  }

  const agendaHeading = selectedDateKey
    ? formatDayLabel(parseDateKey(selectedDateKey))
    : filterMode === 'day'
      ? 'Today'
      : filterMode === 'week'
        ? 'This week'
        : 'This month';

  const gridTitle =
    filterMode === 'week'
      ? `Week of ${formatShortDate(weekGridDays[0])} \u2013 ${formatShortDate(weekGridDays[6])}`
      : formatMonthLabel(monthCursor);

  function renderDayCell(date) {
    const key = toDateKey(date);
    const inMonth = filterMode === 'month' ? date.getMonth() === monthCursor.getMonth() : true;
    const dayEvents = eventsByDay.get(key) || [];
    const isSelected = selectedDateKey === key;
    const isToday = key === todayKey;
    return (
      <button
        key={key}
        onClick={() => handleDayClick(key)}
        className={[
          'aspect-square rounded-lg p-1.5 flex flex-col items-start text-left cursor-pointer transition-colors',
          isToday ? 'bg-surface-1' : inMonth ? 'bg-surface-2' : 'bg-surface-0',
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
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-5">
        <p className="text-[20px] font-medium">Calendar</p>
        <div className="flex items-center gap-2">
          <Button variant="secondary" onClick={() => goToMonth(-1)}>&larr;</Button>
          <select
            value={filterMode}
            onChange={(e) => handleFilterChange(e.target.value)}
            className="h-9 px-3 rounded-full text-[13px] font-medium bg-surface-2 border border-border-strong cursor-pointer"
          >
            <option value="day">Today</option>
            <option value="week">This week</option>
            <option value="month">This month</option>
          </select>
          <Button variant="secondary" onClick={() => goToMonth(1)}>&rarr;</Button>
        </div>
      </div>

      {!hasLoaded && fetching ? (
        <div className="flex flex-col gap-3.5">
          <SkeletonCard />
          <SkeletonCard />
        </div>
      ) : error ? (
        <ErrorInline message={error} />
      ) : (
        <div className={fetching ? 'opacity-50 pointer-events-none transition-opacity' : 'transition-opacity'}>
          {filterMode !== 'day' ? (
            <Card>
              <p className="text-[15px] font-medium mb-3">{gridTitle}</p>
              <div className="grid grid-cols-7 gap-1.5">
                {WEEKDAY_LABELS.map((label) => (
                  <div key={label} className="text-[11px] text-text-muted text-center py-1">
                    {label}
                  </div>
                ))}
                {(filterMode === 'week' ? weekGridDays : monthGridDays).map(renderDayCell)}
              </div>
            </Card>
          ) : null}

          <p className="text-[13px] font-medium text-text-secondary mt-5 mb-2.5">{agendaHeading}</p>

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
        </div>
      )}
    </div>
  );
}
