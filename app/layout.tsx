import React from 'react';

export const metadata = {
  title: 'FinLink AI — Open Finance Aggregator',
  description: 'Pluggy Open Finance integration with real-time accounts and transaction syncing.',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="pt-BR">
      <head>
        <script src="https://cdn.tailwindcss.com"></script>
      </head>
      <body className="bg-slate-950 text-slate-100 min-h-screen">
        {children}
      </body>
    </html>
  );
}
