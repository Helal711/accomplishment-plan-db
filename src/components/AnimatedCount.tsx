import React, { useEffect, useRef, useState } from 'react';

interface AnimatedCountProps {
  value: number;
  durationMs?: number;
  className?: string;
}

function prefersReducedMotion(): boolean {
  if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
    return false;
  }
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
}

/**
 * Lightweight zero-dependency animated integer counter.
 * Smoothly interpolates from the previous count to the updated count (200-350ms)
 * and immediately snaps to the target value when prefers-reduced-motion is enabled.
 */
export const AnimatedCount: React.FC<AnimatedCountProps> = ({
  value,
  durationMs = 280,
  className = '',
}) => {
  const safeTarget = Number.isFinite(value) ? Math.max(0, Math.round(value)) : 0;
  const [displayValue, setDisplayValue] = useState<number>(() =>
    prefersReducedMotion() ? safeTarget : 0
  );
  const currentDisplayedRef = useRef<number>(prefersReducedMotion() ? safeTarget : 0);

  useEffect(() => {
    const startValue = currentDisplayedRef.current;

    if (prefersReducedMotion() || startValue === safeTarget || durationMs <= 0) {
      currentDisplayedRef.current = safeTarget;
      setDisplayValue(safeTarget);
      return;
    }

    let frameId = 0;
    let startTime: number | null = null;
    const delta = safeTarget - startValue;

    const tick = (timestamp: number) => {
      if (startTime === null) {
        startTime = timestamp;
      }
      const elapsed = timestamp - startTime;
      const progress = Math.min(elapsed / durationMs, 1);
      // Ease-out cubic for crisp deceleration
      const eased = 1 - Math.pow(1 - progress, 3);
      const nextVal = Math.round(startValue + delta * eased);

      if (nextVal !== currentDisplayedRef.current) {
        currentDisplayedRef.current = nextVal;
        setDisplayValue(nextVal);
      }

      if (progress < 1) {
        frameId = window.requestAnimationFrame(tick);
      } else if (currentDisplayedRef.current !== safeTarget) {
        currentDisplayedRef.current = safeTarget;
        setDisplayValue(safeTarget);
      }
    };

    frameId = window.requestAnimationFrame(tick);
    return () => {
      if (frameId) {
        window.cancelAnimationFrame(frameId);
      }
    };
  }, [safeTarget, durationMs]);

  return <span className={`tabular-nums ${className}`.trim()}>{displayValue}</span>;
};
