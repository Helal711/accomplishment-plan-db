/**
 * Pure keyboard shortcut classification helpers for TaskFormPage and AppHeader.
 * Designed to respect native browser controls, multiline textareas, IME composition,
 * repeated key events, and open confirmation dialogs/modals.
 */

export interface KeyboardShortcutEventLike {
  key: string;
  ctrlKey?: boolean;
  metaKey?: boolean;
  altKey?: boolean;
  shiftKey?: boolean;
  repeat?: boolean;
  isComposing?: boolean;
  defaultPrevented?: boolean;
}

export interface ShortcutTargetLike {
  tagName?: string;
  type?: string;
  isContentEditable?: boolean;
  role?: string | null;
}

export type TaskFormShortcutAction = 'submit' | 'escape' | 'prevent-only' | 'none';

const INTERACTIVE_INPUT_TYPES = new Set([
  'button',
  'submit',
  'reset',
  'checkbox',
  'radio',
  'date',
  'datetime-local',
  'time',
  'month',
  'week',
  'file',
  'color',
  'range',
]);

const INTERACTIVE_ARIA_ROLES = new Set([
  'button',
  'link',
  'menuitem',
  'option',
  'listbox',
  'combobox',
  'menu',
  'tab',
  'switch',
  'checkbox',
  'radio',
]);

export function extractShortcutTarget(target: EventTarget | null): ShortcutTargetLike {
  if (!target || typeof target !== 'object') return {};
  const el = target as {
    tagName?: string;
    type?: string;
    isContentEditable?: boolean;
    getAttribute?: (name: string) => string | null;
  };
  return {
    tagName: el.tagName ? String(el.tagName).toUpperCase() : undefined,
    type: el.type ? String(el.type).toLowerCase() : undefined,
    isContentEditable: Boolean(el.isContentEditable),
    role: typeof el.getAttribute === 'function' ? el.getAttribute('role') : null,
  };
}

export function isMultilineTextTarget(target: ShortcutTargetLike): boolean {
  return target.tagName === 'TEXTAREA' || Boolean(target.isContentEditable);
}

export function isNonSubmitInteractiveControl(target: ShortcutTargetLike): boolean {
  const tag = target.tagName;
  if (tag === 'BUTTON' || tag === 'A' || tag === 'SELECT' || tag === 'SUMMARY') {
    return true;
  }
  if (tag === 'INPUT' && target.type && INTERACTIVE_INPUT_TYPES.has(target.type)) {
    return true;
  }
  if (target.role && INTERACTIVE_ARIA_ROLES.has(target.role.toLowerCase())) {
    return true;
  }
  return false;
}

/**
 * Classifies keyboard shortcuts on Add New Task / Edit Task pages:
 * - Ctrl+S / Cmd+S -> Save/Update (always prevents browser Save Page dialog)
 * - Ctrl+Enter / Cmd+Enter -> Save/Update (including from textareas)
 * - Enter -> Save/Update (except inside textareas or interactive controls like buttons/selects/date pickers)
 * - Escape -> Cancel open modal/dialog or return to Task List
 */
export function resolveTaskFormShortcut(
  event: KeyboardShortcutEventLike,
  target: ShortcutTargetLike,
  hasOpenOverlay: boolean
): TaskFormShortcutAction {
  if (event.defaultPrevented || event.isComposing) {
    return 'none';
  }

  const key = event.key || '';
  const lowerKey = key.toLowerCase();
  const hasPrimaryModifier = Boolean(event.ctrlKey || event.metaKey);

  // Ctrl+S / Cmd+S: Always prevent browser's default Save Page dialog
  if (hasPrimaryModifier && !event.altKey && lowerKey === 's') {
    if (event.repeat || hasOpenOverlay) {
      return 'prevent-only';
    }
    return 'submit';
  }

  // Escape: Close active overlay first, or cancel/return from form
  if (key === 'Escape' && !hasPrimaryModifier && !event.altKey) {
    if (event.repeat) return 'none';
    return 'escape';
  }

  // Block Enter shortcuts while any confirmation popup or modal is open
  if (hasOpenOverlay) {
    return 'none';
  }

  // Ctrl+Enter / Cmd+Enter: Submit from anywhere, including multiline textareas
  if (hasPrimaryModifier && !event.altKey && key === 'Enter') {
    if (event.repeat) return 'prevent-only';
    return 'submit';
  }

  // Plain Enter: Submit unless inside a textarea or interactive control
  if (!hasPrimaryModifier && !event.altKey && !event.shiftKey && key === 'Enter') {
    if (isMultilineTextTarget(target) || isNonSubmitInteractiveControl(target)) {
      return 'none';
    }
    if (event.repeat) return 'prevent-only';
    return 'submit';
  }

  return 'none';
}

/**
 * Checks whether a keydown event is the Ctrl+K / Cmd+K quick-search shortcut.
 */
export function isQuickSearchShortcut(event: KeyboardShortcutEventLike): boolean {
  if (event.defaultPrevented || event.isComposing) return false;
  const hasPrimaryModifier = Boolean(event.ctrlKey || event.metaKey);
  return hasPrimaryModifier && !event.altKey && (event.key || '').toLowerCase() === 'k';
}

export const UNSAVED_CHANGES_CONFIRM_TITLE = 'Unsaved Changes:';
export const UNSAVED_CHANGES_CONFIRM_BODY = 'Are you sure you want to discard your changes?';
