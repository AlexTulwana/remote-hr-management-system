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
  ANNOUNCEMENT: { label: 'Announcement', variant: 'info' },
};

const BAR_COLORS = {
  CALENDAR_EVENT: 'var(--color-text-muted)',
  LEAVE: 'var(--color-warning-text)',
  INTERVIEW: 'var(--color-info-text)',
  HEARING: 'var(--color-danger-text)',
  JOB_POSTING_OPEN: 'var(--color-success-text)',
  JOB_POSTING_CLOSE: 'var(--color-text-muted)',
  ANNOUNCEMENT: 'var(--color-info-text)',
};

const WEEKDAY_LABELS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
const DAY_MS = 24 * 60 * 60 * 1000;
const BAR_ROW_HEIGHT = 20;

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

// Assigns each event a horizontal "lane" within a week so multi-day events
// render as a single spanning bar (Google Calendar style) instead of per-day dots.
function computeWeekBars(weekDays, events) {
  const weekStart = weekDays[0];
  const weekEnd = weekDays[6];

  const overlapping = events
    .map((item) => {
      const evStart = parseDateKey(item.date);
      const evEnd = item.endDate ? parseDateKey(item.endDate) : evStart;
      return { item, evStart, evEnd };
    })
    .filter(({ evStart, evEnd }) => evEnd >= weekStart && evStart <= weekEnd)
    .sort((a, b) => {
      const startDiff = a.evStart - b.evStart;
      if (startDiff !== 0) return startDiff;
      return (b.evEnd - b.evStart) - (a.evEnd - a.evStart);
    });

  const laneEnds = [];
  const placed = overlapping.map(({ item, evStart, evEnd }) => {
    const clampedStart = evStart < weekStart ? weekStart : evStart;
    const clampedEnd = evEnd > weekEnd ? weekEnd : evEnd;
    const startCol = Math.round((clampedStart - weekStart) / DAY_MS);
    const endCol = Math.round((clampedEnd - weekStart) / DAY_MS);

    let lane = laneEnds.findIndex((end) => end < startCol);
    if (lane === -1) {
      lane = laneEnds.length;
      laneEnds.push(endCol);
    } else {
      laneEnds[lane] = endCol;
    }

    return {
      item,
      startCol,
      endCol,
      lane,
      continuesBefore: evStart < weekStart,
      continuesAfter: evEnd > weekEnd,
    };
  });

  return { placed, laneCount: laneEnds.length };
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

  const weeks = useMemo(() => {
    if (filterMode === 'week') return [weekGridDays];
    const rows = [];
    for (let i = 0; i < monthGridDays.length; i += 7) {
      rows.push(monthGridDays.slice(i, i + 7));
    }
    return rows;
  }, [filterMode, monthGridDays, weekGridDays]);

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

  function handleToday() {
    setSelectedDateKey(null);
    jumpToCurrentMonth();
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

  function renderWeek(weekDays, weekIndex) {
    const { placed, laneCount } = computeWeekBars(weekDays, events);
    const barsAreaHeight = Math.max(laneCount, 1) * BAR_ROW_HEIGHT;

    return (
      <div key={`week-${weekIndex}`} className="mb-2">
        <div className="grid grid-cols-7 gap-1.5">
          {weekDays.map((date) => {
            const key = toDateKey(date);
            const inMonth = filterMode === 'month' ? date.getMonth() === monthCursor.getMonth() : true;
            const isSelected = selectedDateKey === key;
            const isToday = key === todayKey;
            return (
              <button
                key={key}
                onClick={() => handleDayClick(key)}
                className={[
                  'rounded-t-lg pt-1 px-1.5 flex justify-start text-left cursor-pointer transition-colors',
                  isToday ? 'bg-surface-1' : inMonth ? 'bg-surface-2' : 'bg-surface-0',
                  isSelected ? 'border border-text-primary border-b-0' : 'border border-transparent',
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
              </button>
            );
          })}
        </div>

        <div
          className="grid grid-cols-7 gap-x-1.5 gap-y-0.5 px-0"
          style={{ minHeight: barsAreaHeight, gridAutoRows: `${BAR_ROW_HEIGHT - 2}px` }}
        >
          {placed.map(({ item, startCol, endCol, lane, continuesBefore, continuesAfter }, i) => {
            return (
              <button
                key={`${item.sourceType}-${item.sourceId}-${weekIndex}-${i}`}
                onClick={() => handleDayClick(item.date)}
                title={item.title}
                className="text-[10px] text-left text-white truncate px-1.5 flex items-center"
                style={{
                  gridColumn: `${startCol + 1} / ${endCol + 2}`,
                  gridRow: lane + 1,
                  height: `${BAR_ROW_HEIGHT - 2}px`,
                  background: BAR_COLORS[item.sourceType] || 'var(--color-text-muted)',
                  borderTopLeftRadius: continuesBefore ? 0 : 4,
                  borderBottomLeftRadius: continuesBefore ? 0 : 4,
                  borderTopRightRadius: continuesAfter ? 0 : 4,
                  borderBottomRightRadius: continuesAfter ? 0 : 4,
                }}
              >
                {continuesBefore ? '\u2039 ' : ''}
                {item.title}
                {continuesAfter ? ' \u203a' : ''}
              </button>
            );
          })}
        </div>
      </div>
    );
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-5">
        <p className="text-[20px] font-medium">Calendar</p>
        <div className="flex items-center gap-2">
          <Button variant="secondary" onClick={handleToday}>Today</Button>
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
          <Card>
            <p className="text-[15px] font-medium mb-3">{gridTitle}</p>
            <div className="grid grid-cols-7 gap-1.5 mb-1">
              {WEEKDAY_LABELS.map((label) => (
                <div key={label} className="text-[11px] text-text-muted text-center py-1">
                  {label}
                </div>
              ))}
            </div>
            {weeks.map((weekDays, i) => renderWeek(weekDays, i))}
          </Card>

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
