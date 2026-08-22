'use client'

import React from 'react'
import { dateFnsLocalizer, Calendar, type View, Views } from 'react-big-calendar'
import { format, parse, startOfWeek, getDay } from 'date-fns'
import { ja } from 'date-fns/locale'
import type { TaskItem } from '@/components/tasks/DetailPanel'

type CalendarViewProps = {
  tasks: TaskItem[]
  currentDate: Date
  currentView: View
  onViewChange: (view: View) => void
  onDateChange: (date: Date) => void
  onSelectTask: (taskId: number) => void
}

type CalendarEvent = {
  id: number
  title: string
  start: Date
  end: Date
  allDay: boolean
  resource: TaskItem
}

const localizer = dateFnsLocalizer({
  format,
  parse,
  startOfWeek: (date: Date) => startOfWeek(date, { weekStartsOn: 1 }),
  getDay,
  locales: { ja },
})

export function CalendarView({
  tasks,
  currentDate,
  currentView,
  onViewChange,
  onDateChange,
  onSelectTask,
}: CalendarViewProps) {
  const events: CalendarEvent[] = tasks.map((task) => {
    const due = parse(task.desiredDeliveryDate, 'yyyy-MM-dd', new Date())
    return {
      id: task.id,
      title: `${task.orderSubject} (${task.clientName})`,
      start: due,
      end: due,
      allDay: true,
      resource: task,
    }
  })

  return (
    <div className="rounded-xl border border-gray-200 bg-white p-4">
      <div className="mb-3 flex items-center gap-2">
        <button
          type="button"
          onClick={() => onViewChange(Views.MONTH)}
          className={`min-h-[44px] rounded-md border px-4 py-2 text-sm font-medium ${
            currentView === Views.MONTH
              ? 'border-[#145C9E] bg-[#EAF1F6] text-[#0B4F6C]'
              : 'border-gray-300 bg-white text-gray-700 hover:bg-gray-50'
          }`}
        >
          月表示
        </button>
        <button
          type="button"
          onClick={() => onViewChange(Views.WEEK)}
          className={`min-h-[44px] rounded-md border px-4 py-2 text-sm font-medium ${
            currentView === Views.WEEK
              ? 'border-[#145C9E] bg-[#EAF1F6] text-[#0B4F6C]'
              : 'border-gray-300 bg-white text-gray-700 hover:bg-gray-50'
          }`}
        >
          週表示
        </button>
      </div>

      <div className="max-h-[70vh] overflow-y-auto">
        <Calendar
          localizer={localizer}
          events={events}
          startAccessor="start"
          endAccessor="end"
          date={currentDate}
          view={currentView}
          views={[Views.MONTH, Views.WEEK]}
          style={{ height: 700, minWidth: 680 }}
          onView={onViewChange}
          onNavigate={onDateChange}
          onSelectEvent={(event) => onSelectTask(event.resource.id)}
          eventPropGetter={(event) => {
            const isCompleted = event.resource.status === 'COMPLETED'
            return {
              className: isCompleted ? 'task-event-completed' : 'task-event-active',
            }
          }}
          messages={{
            next: '次へ',
            previous: '前へ',
            today: '今日',
            month: '月',
            week: '週',
            day: '日',
            agenda: '予定',
            date: '日付',
            time: '時間',
            event: 'タスク',
            noEventsInRange: 'この期間のタスクはありません',
          }}
        />
      </div>
    </div>
  )
}
