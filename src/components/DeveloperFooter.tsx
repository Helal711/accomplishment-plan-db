import React from 'react';

export const DeveloperFooter: React.FC<{ className?: string }> = ({ className = '' }) => {
  return (
    <footer className={`fixed bottom-0 left-0 right-0 z-30 w-full py-2.5 text-center bg-white border-t border-gray-200 shadow-sm ${className}`}>
      <p className="text-[11px] sm:text-xs text-footer-gray font-medium tracking-wide">
        Developer by Md Al Helal Sarkar
      </p>
    </footer>
  );
};
