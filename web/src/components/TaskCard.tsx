import React, { useState, useRef, useEffect } from 'react';
import { Building2, ChevronDown, Trash2, Edit3 } from 'lucide-react';
import { TaskItem, Priority } from '../types';

interface TaskCardProps {
  task: TaskItem;
  availableStatuses: string[];
  onCardClick: () => void;
  onStatusChange: (newStatus: string) => void;
  onDeleteClick?: () => void;
}

export const TaskCard: React.FC<TaskCardProps> = ({
  task,
  availableStatuses,
  onCardClick,
  onStatusChange,
  onDeleteClick,
}) => {
  const [statusDropdownOpen, setStatusDropdownOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setStatusDropdownOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const formattedAmount = task.amount
    ? task.amount.toLowerCase().endsWith('tk')
      ? task.amount
      : `${task.amount} TK`
    : '0 TK';

  const statusColors = getStatusColor(task.status);

  return (
    <div
      onClick={onCardClick}
      className="group relative bg-white rounded-lg border border-app-border hover:border-blue-400 hover:shadow-sm transition-all cursor-pointer p-3 sm:p-3.5 select-none"
    >
      {/* Row 1: Task Title & Priority Badge */}
      <div className="flex items-center justify-between gap-2">
        <h4 className="text-[14.5px] font-bold text-gray-900 truncate flex-1 leading-snug">
          {task.taskNumber}. {task.description}
        </h4>

        <div className="flex items-center space-x-2 flex-shrink-0">
          <PriorityBadge priority={task.priority} />
        </div>
      </div>

      {/* Row 2: Amount | Payment Mode & Expected Date */}
      <div className="flex items-center justify-between text-[13px] text-gray-600 my-1.5">
        <div>
          <span>Amount: </span>
          <span className="font-semibold text-gray-800">{formattedAmount}</span>
          <span className="text-gray-400"> | </span>
          <span>{task.paymentMode || 'Cash'}</span>
        </div>

        {task.expectedDate && (
          <span className="text-[11px] text-gray-500 hidden sm:inline-block">
            📅 {task.expectedDate}
          </span>
        )}
      </div>

      {/* Row 3: Unit Chip, Account info & Status Pill */}
      <div className="flex items-center justify-between gap-2 mt-2 pt-1 border-t border-gray-100">
        <div className="flex items-center space-x-1.5 flex-wrap gap-y-1">
          {/* Unit chip */}
          <div className="inline-flex items-center space-x-1 bg-unit-bg text-unit-text text-[11.5px] font-medium px-2 py-0.5 rounded">
            <Building2 className="w-3.5 h-3.5" />
            <span>{task.category ? `${task.unit || 'Unit-1'} • ${task.category}` : (task.unit || 'Unit-1')}</span>
          </div>

          {/* Account Code mini badge */}
          {task.accountCode && (
            <span className="text-[10.5px] text-gray-500 font-mono hidden md:inline-block">
              {task.accountCode}
            </span>
          )}
        </div>

        {/* Status Pill with interactive quick dropdown */}
        <div className="flex items-center space-x-2">
          <div className="relative" ref={dropdownRef}>
            <button
              type="button"
              onClick={(e) => {
                e.stopPropagation();
                setStatusDropdownOpen(!statusDropdownOpen);
              }}
              style={{
                backgroundColor: statusColors.bg,
                color: statusColors.text,
                borderColor: statusColors.border,
              }}
              className="inline-flex items-center space-x-1 px-2.5 py-1 rounded-full text-xs font-semibold border transition-transform hover:scale-[1.02]"
            >
              <span>{task.status || 'Draft Ready'}</span>
              <ChevronDown className="w-3 h-3 opacity-75" />
            </button>

            {statusDropdownOpen && (
              <div
                className="absolute right-0 bottom-full mb-1 sm:bottom-auto sm:top-full sm:mt-1 w-40 bg-white rounded-lg shadow-xl border border-gray-200 py-1 z-30 animate-fadeIn"
                onClick={(e) => e.stopPropagation()}
              >
                <div className="px-2.5 py-1 text-[10px] font-bold text-gray-400 uppercase tracking-wider">
                  Update Status
                </div>
                {availableStatuses.map((opt) => (
                  <button
                    key={opt}
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      onStatusChange(opt);
                      setStatusDropdownOpen(false);
                    }}
                    className={`w-full text-left px-3 py-1.5 text-xs transition-colors flex items-center justify-between ${
                      opt === task.status
                        ? 'font-bold bg-blue-50 text-app-primary'
                        : 'text-gray-700 hover:bg-gray-50'
                    }`}
                  >
                    <span>{opt}</span>
                    {opt === task.status && <span className="w-1.5 h-1.5 rounded-full bg-app-primary"></span>}
                  </button>
                ))}
              </div>
            )}
          </div>

          {/* Quick Edit & Delete icons on hover/mobile */}
          <div className="flex items-center space-x-0.5 opacity-80 group-hover:opacity-100 transition-opacity">
            <button
              type="button"
              onClick={(e) => {
                e.stopPropagation();
                onCardClick();
              }}
              title="Edit Task"
              className="p-1 text-gray-400 hover:text-app-primary hover:bg-blue-50 rounded"
            >
              <Edit3 className="w-3.5 h-3.5" />
            </button>
            {onDeleteClick && (
              <button
                type="button"
                onClick={(e) => {
                  e.stopPropagation();
                  onDeleteClick();
                }}
                title="Delete Task"
                className="p-1 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded"
              >
                <Trash2 className="w-3.5 h-3.5" />
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export const PriorityBadge: React.FC<{ priority: Priority | string }> = ({ priority }) => {
  const norm = (priority || '').toLowerCase();
  let dotColor = 'bg-priority-med';
  let label = 'Med';

  if (norm === 'high') {
    dotColor = 'bg-priority-high';
    label = 'High';
  } else if (norm === 'low') {
    dotColor = 'bg-priority-low';
    label = 'Low';
  }

  return (
    <div className="inline-flex items-center space-x-1.5">
      <span className={`w-2 h-2 rounded-full ${dotColor}`}></span>
      <span className="text-xs font-semibold text-gray-700">{label}</span>
    </div>
  );
};

export function getStatusColor(status: string) {
  const s = (status || '').toLowerCase();
  if (s === 'done') {
    return { bg: '#E8F5E9', text: '#2E7D32', border: '#A5D6A7' };
  }
  if (s === 'advance ready') {
    return { bg: '#FEF3C7', text: '#B45309', border: '#FDE68A' };
  }
  if (s === 'draft ready') {
    return { bg: '#FEF9C3', text: '#854D0E', border: '#FDE047' };
  }
  if (s === 'in progress') {
    return { bg: '#EFF6FF', text: '#1D4ED8', border: '#93C5FD' };
  }
  if (s === 'cancelled') {
    return { bg: '#FEE2E2', text: '#B91C1C', border: '#FCA5A5' };
  }
  return { bg: '#F3F4F6', text: '#4B5563', border: '#D1D5DB' };
}
