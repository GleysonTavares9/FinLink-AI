'use client';

import React, { useState } from 'react';

declare global {
  interface Window {
    PluggyConnect?: any;
  }
}

interface PluggyConnectButtonProps {
  clientUserId?: string;
  onSuccess?: (itemData: { item: { id: string; connector: { name: string } } }) => void;
  onError?: (error: unknown) => void;
  className?: string;
}

export function PluggyConnectButton({
  clientUserId,
  onSuccess,
  onError,
  className = '',
}: PluggyConnectButtonProps) {
  const [isLoading, setIsLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const handleOpenConnect = async () => {
    setIsLoading(true);
    setErrorMsg(null);

    try {
      // 1. Request connect token from our secure server-side endpoint
      const response = await fetch('/api/connect-token', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ clientUserId }),
      });

      const data = await response.json();

      if (!response.ok || !data.accessToken) {
        throw new Error(data.error || 'Failed to obtain Pluggy connect token');
      }

      // 2. Open Pluggy Connect widget using the retrieved connect token
      // NOTE: Ensure the Pluggy Connect CDN script is included or use package 'react-pluggy-connect'
      if (typeof window !== 'undefined' && window.PluggyConnect) {
        const pluggyConnect = new window.PluggyConnect({
          connectToken: data.accessToken,
          includeSandbox: true, // Set to false in production
          onSuccess: (itemData: any) => {
            console.log('Successfully connected institution:', itemData);
            if (onSuccess) onSuccess(itemData);
          },
          onError: (err: any) => {
            console.error('Pluggy Connect error:', err);
            setErrorMsg(err?.message || 'Connection error');
            if (onError) onError(err);
          },
          onClose: () => {
            console.log('Pluggy Connect modal closed');
          },
        });

        pluggyConnect.init();
      } else {
        console.warn(
          'PluggyConnect widget script not found on window. Consider loading https://cdn.pluggy.ai/pluggy-connect/v1/pluggy-connect.js or using react-pluggy-connect.'
        );
        alert(`Pluggy Connect Token generated: ${data.accessToken.substring(0, 10)}... (Ready for PluggyConnect widget)`);
      }
    } catch (err: any) {
      console.error('Connection initialization failed:', err);
      setErrorMsg(err.message || 'Error initiating connection');
      if (onError) onError(err);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="flex flex-col items-start gap-2">
      <button
        onClick={handleOpenConnect}
        disabled={isLoading}
        className={`px-5 py-2.5 rounded-xl font-semibold transition-all shadow-md active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed ${
          className || 'bg-emerald-600 hover:bg-emerald-500 text-white'
        }`}
      >
        {isLoading ? 'Securing Connection...' : 'Connect Bank Account (Pluggy)'}
      </button>

      {errorMsg && (
        <span className="text-sm text-red-500 font-medium">
          {errorMsg}
        </span>
      )}
    </div>
  );
}
