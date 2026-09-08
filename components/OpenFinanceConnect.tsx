'use client';

import React, { useState, useEffect } from 'react';
import { PluggyConnect } from 'react-pluggy-connect';

interface ItemData {
  item: {
    id: string;
    connector?: {
      id: number;
      name: string;
      imageUrl?: string;
    };
    status?: string;
    executionStatus?: string;
  };
}

interface OpenFinanceConnectProps {
  onSuccess?: (itemData: ItemData) => void;
  onError?: (error: unknown) => void;
  onClose?: () => void;
  clientUserId?: string;
}

export function OpenFinanceConnect({
  onSuccess,
  onError,
  onClose,
  clientUserId,
}: OpenFinanceConnectProps) {
  const [connectToken, setConnectToken] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;

    async function fetchToken() {
      try {
        setLoading(true);
        setErrorMessage(null);

        // Fetch connect token from our secure server-side endpoint
        const res = await fetch('/api/connect-token', {
          method: clientUserId ? 'POST' : 'GET',
          headers: clientUserId ? { 'Content-Type': 'application/json' } : undefined,
          body: clientUserId ? JSON.stringify({ clientUserId }) : undefined,
        });

        const data = await res.json();

        if (!res.ok || !data.accessToken) {
          throw new Error(data.error || 'Failed to retrieve Pluggy Connect token');
        }

        if (isMounted) {
          setConnectToken(data.accessToken);
        }
      } catch (err: unknown) {
        const msg = err instanceof Error ? err.message : 'Error fetching connect token';
        if (isMounted) {
          setErrorMessage(msg);
        }
        if (onError) onError(err);
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    }

    fetchToken();

    return () => {
      isMounted = false;
    };
  }, [clientUserId, onError]);

  if (loading) {
    return (
      <div className="flex items-center justify-center p-6 text-sm text-slate-400 animate-pulse">
        Initializing Open Finance secure consent flow...
      </div>
    );
  }

  if (errorMessage) {
    return (
      <div className="p-4 rounded-xl bg-red-950/40 border border-red-800 text-red-300 text-sm">
        <p className="font-semibold">Unable to connect to Pluggy</p>
        <p className="mt-1 text-xs opacity-90">{errorMessage}</p>
        <p className="mt-2 text-xs text-slate-400">
          Make sure your <code className="text-amber-300">CLIENT_ID</code> and{' '}
          <code className="text-amber-300">CLIENT_SECRET</code> are set in{' '}
          <code className="text-white">.env.local</code>.
        </p>
      </div>
    );
  }

  if (!connectToken) {
    return null;
  }

  return (
    <div className="w-full flex justify-center">
      <PluggyConnect
        connectToken={connectToken}
        includeSandbox={true}
        onSuccess={(itemData) => {
          console.log('Open Finance connection established:', itemData);
          if (onSuccess) onSuccess(itemData);
        }}
        onError={(error) => {
          console.error('Pluggy Connect error:', error);
          if (onError) onError(error);
        }}
        onClose={() => {
          console.log('Pluggy Connect modal closed');
          if (onClose) onClose();
        }}
      />
    </div>
  );
}

export default OpenFinanceConnect;
