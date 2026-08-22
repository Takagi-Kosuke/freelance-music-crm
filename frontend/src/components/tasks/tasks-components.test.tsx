import React from 'react'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'

import { TaskStatusBadge } from '@/components/tasks/TaskStatusBadge'
import { DetailPanel } from '@/components/tasks/DetailPanel'
import { CalendarView } from '@/components/tasks/CalendarView'

vi.mock('react-big-calendar', () => {
  return {
    Views: {
      MONTH: 'month',
      WEEK: 'week',
    },
    dateFnsLocalizer: () => ({}) as unknown,
    Calendar: ({ events, eventPropGetter }: { events: any[]; eventPropGetter: (event: any) => { className: string } }) => (
      <div data-testid="mock-calendar">
        {events.map((event) => {
          const props = eventPropGetter(event)
          return (
            <div key={event.id} data-testid={`event-${event.id}`} className={props.className}>
              {event.title}
            </div>
          )
        })}
      </div>
    ),
  }
})

const sampleTask = {
  id: 1,
  orderId: 100,
  categoryId: 10,
  categoryName: '作曲',
  orderSubject: 'BGM制作',
  clientName: '田中太郎',
  clientEmail: 'client@example.com',
  desiredDeliveryDate: '2026-07-10',
  filePathUrl: 'https://example.com/file',
  comment: 'よろしくお願いします',
  folderPath: 'E:/workspace/Tasks/backend',
  status: 'NOT_STARTED' as const,
  statusUpdatedAt: '2026-06-01T10:00:00',
  createdAt: '2026-06-01T09:00:00',
}

describe('TaskStatusBadge', () => {
  it('ステータス別カラークラスを適用する', () => {
    const { rerender } = render(<TaskStatusBadge status="NOT_STARTED" />)
    expect(screen.getByText('未着手')).toHaveClass('bg-slate-100')

    rerender(<TaskStatusBadge status="IN_PROGRESS" />)
    expect(screen.getByText('進行中')).toHaveClass('bg-amber-100')

    rerender(<TaskStatusBadge status="COMPLETED" />)
    expect(screen.getByText('完了')).toHaveClass('bg-emerald-100')

    rerender(<TaskStatusBadge status="CANCELLED" />)
    expect(screen.getByText('キャンセル')).toHaveClass('bg-rose-100')
  })
})

describe('DetailPanel', () => {
  it('開閉動作を実行する', async () => {
    const user = userEvent.setup()
    const onClose = vi.fn()

    const { rerender } = render(
      <DetailPanel
        task={null}
        updating={false}
        savingFolderPath={false}
        onClose={onClose}
        onUpdateStatus={vi.fn()}
        onUpdateFolderPath={vi.fn().mockResolvedValue(undefined)}
      />
    )

    expect(screen.getByText('タスクを選択すると詳細が表示されます。')).toBeInTheDocument()

    rerender(
      <DetailPanel
        task={sampleTask}
        updating={false}
        savingFolderPath={false}
        onClose={onClose}
        onUpdateStatus={vi.fn()}
        onUpdateFolderPath={vi.fn().mockResolvedValue(undefined)}
      />
    )

    expect(screen.getByText('タスク詳細')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: '閉じる' }))
    expect(onClose).toHaveBeenCalledTimes(1)
  })
})

describe('CalendarView', () => {
  it('月/週切替と完了/未完了タスクのCSSクラス差異を反映する', async () => {
    const user = userEvent.setup()
    const onViewChange = vi.fn()

    render(
      <CalendarView
        tasks={[
          sampleTask,
          {
            ...sampleTask,
            id: 2,
            orderId: 101,
            orderSubject: 'ミックス作業',
            status: 'COMPLETED',
          },
        ]}
        currentDate={new Date('2026-07-01')}
        currentView={'month'}
        onViewChange={onViewChange}
        onDateChange={vi.fn()}
        onSelectTask={vi.fn()}
      />
    )

    await user.click(screen.getByRole('button', { name: '月表示' }))
    await user.click(screen.getByRole('button', { name: '週表示' }))

    expect(onViewChange).toHaveBeenCalledWith('month')
    expect(onViewChange).toHaveBeenCalledWith('week')

    expect(screen.getByTestId('event-1')).toHaveClass('task-event-active')
    expect(screen.getByTestId('event-2')).toHaveClass('task-event-completed')
  })
})
