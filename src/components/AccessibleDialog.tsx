import React, { useLayoutEffect, useRef } from 'react';
import { createPortal } from 'react-dom';

interface AccessibleDialogProps {
  id: string;
  role?: 'dialog' | 'alertdialog';
  label: string;
  describedBy?: string;
  onClose: () => void;
  closeOnBackdrop?: boolean;
  closeOnEscape?: boolean;
  initialFocusRef?: React.RefObject<HTMLElement | null>;
  fallbackFocusId?: string;
  backdropClassName: string;
  panelClassName: string;
  children: React.ReactNode;
}

const dialogStack: HTMLElement[] = [];
const backgroundInert = new Map<HTMLElement, boolean>();
let bodyOverflow: string | null = null;

// Keep the original values so closing a dialog never unlocks an existing lock.
function syncBackground() {
  for (const [element, wasInert] of backgroundInert) element.inert = wasInert;
  const activeDialog = dialogStack.at(-1);
  if (!activeDialog) {
    backgroundInert.clear();
    if (bodyOverflow !== null) document.body.style.overflow = bodyOverflow;
    bodyOverflow = null;
    return;
  }

  if (bodyOverflow === null) bodyOverflow = document.body.style.overflow;
  document.body.style.overflow = 'hidden';
  for (const element of document.body.children) {
    if (!(element instanceof HTMLElement) || element === activeDialog) continue;
    if (!backgroundInert.has(element)) backgroundInert.set(element, element.inert);
    element.inert = true;
  }
}

function focusableElements(panel: HTMLElement): HTMLElement[] {
  return Array.from(panel.querySelectorAll<HTMLElement>(
    'a[href], button, input, select, textarea, [tabindex], [contenteditable="true"]',
  )).filter((element) => element.tabIndex >= 0 && !element.matches(':disabled') &&
    !element.closest('[inert]') && element.getClientRects().length > 0);
}

/** Modal behavior is deliberately opt-in; emergency alerts retain their own policy. */
export const AccessibleDialog: React.FC<AccessibleDialogProps> = ({
  id, role = 'dialog', label, describedBy, onClose, closeOnBackdrop = false, closeOnEscape = true,
  initialFocusRef, fallbackFocusId, backdropClassName, panelClassName, children,
}) => {
  const backdropRef = useRef<HTMLDivElement>(null);
  const panelRef = useRef<HTMLDivElement>(null);
  const closeRef = useRef(onClose);
  const escapeRef = useRef(closeOnEscape);

  useLayoutEffect(() => {
    closeRef.current = onClose;
    escapeRef.current = closeOnEscape;
  }, [onClose, closeOnEscape]);

  useLayoutEffect(() => {
    const backdrop = backdropRef.current;
    const panel = panelRef.current;
    if (!backdrop || !panel) return;
    const returnTarget = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    dialogStack.push(backdrop);
    syncBackground();

    const focusInitial = () => {
      const requested = initialFocusRef?.current;
      const target = requested && panel.contains(requested) ? requested : focusableElements(panel)[0] ?? panel;
      target.focus({ preventScroll: true });
    };
    const isActive = () => dialogStack.at(-1) === backdrop;
    const onKeyDown = (event: KeyboardEvent) => {
      if (!isActive()) return;
      if (event.key === 'Escape' && escapeRef.current) {
        event.preventDefault();
        event.stopPropagation();
        closeRef.current();
      } else if (event.key === 'Tab') {
        const focusable = focusableElements(panel);
        const first = focusable[0];
        const last = focusable.at(-1);
        const current = document.activeElement;
        if (!first || !last) {
          event.preventDefault();
          panel.focus({ preventScroll: true });
        } else if (event.shiftKey && (current === first || !focusable.includes(current as HTMLElement))) {
          event.preventDefault();
          last.focus();
        } else if (!event.shiftKey && (current === last || !focusable.includes(current as HTMLElement))) {
          event.preventDefault();
          first.focus();
        }
      }
    };
    const onFocusIn = (event: FocusEvent) => {
      if (isActive() && event.target instanceof Node && !panel.contains(event.target)) focusInitial();
    };
    document.addEventListener('keydown', onKeyDown, true);
    document.addEventListener('focusin', onFocusIn);
    focusInitial();

    return () => {
      document.removeEventListener('keydown', onKeyDown, true);
      document.removeEventListener('focusin', onFocusIn);
      const index = dialogStack.indexOf(backdrop);
      if (index !== -1) dialogStack.splice(index, 1);
      syncBackground();
      const fallback = fallbackFocusId ? document.getElementById(fallbackFocusId) : null;
      const target = returnTarget && returnTarget !== document.body && returnTarget.isConnected && !returnTarget.closest('[inert]')
        ? returnTarget : fallback;
      if (target && !target.closest('[inert]')) target.focus({ preventScroll: true });
    };
  }, [initialFocusRef, fallbackFocusId]);

  return createPortal(
    <div
      ref={backdropRef}
      className={backdropClassName}
      onClick={(event) => {
        if (closeOnBackdrop && event.target === event.currentTarget) onClose();
      }}
    >
      <div
        ref={panelRef}
        id={id}
        role={role}
        aria-modal="true"
        aria-label={label}
        aria-describedby={describedBy}
        tabIndex={-1}
        className={panelClassName}
      >
        {children}
      </div>
    </div>,
    document.body,
  );
};
