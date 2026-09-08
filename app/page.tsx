'use client';

import React, { useState } from 'react';
import { OpenFinanceConnect } from '../components/OpenFinanceConnect';

interface AvailableAmount {
  amount: number;
  currencyCode: string;
  name?: string;
}

interface Account {
  id: string;
  name: string;
  type: string;
  subtype: string;
  number: string;
  balance: number;
  currencyCode: string;
  bankData?: {
    availableAmounts?: AvailableAmount[];
  } | null;
  creditData?: {
    brand?: string;
    creditLimit?: number;
    availableCreditLimit?: number;
    balanceDueDate?: string;
  } | null;
}

interface Transaction {
  id: string;
  description: string;
  amount: number;
  date: string;
  category?: string;
  type?: string;
}

export default function HomePage() {
  const [showConnect, setShowConnect] = useState(false);
  const [loadingDetails, setLoadingDetails] = useState(false);
  const [itemData, setItemData] = useState<any>(null);
  const [selectedAccountId, setSelectedAccountId] = useState<string | null>(null);
  const [feedbackMsg, setFeedbackMsg] = useState<string | null>(null);

  const item = itemData?.item || (itemData?.id ? itemData : null);
  const accounts: Account[] = Array.isArray(itemData?.accounts)
    ? itemData.accounts
    : [];
  const transactions: Record<string, Transaction[]> = itemData?.transactions || {};
  const identity = itemData?.identity || null;

  async function fetchItemFinancialData(itemId: string) {
    try {
      setLoadingDetails(true);
      setFeedbackMsg(null);
      const res = await fetch(`/api/items/${itemId}`);
      const data = await res.json();

      if (!res.ok) {
        throw new Error(data.error || 'Erro ao carregar dados financeiros');
      }

      setItemData(data);
      if (data.accounts && data.accounts.length > 0) {
        setSelectedAccountId(data.accounts[0].id);
      }
    } catch (err: any) {
      console.error('Erro ao buscar dados:', err);
      setFeedbackMsg(`Erro ao buscar dados: ${err.message}`);
    } finally {
      setLoadingDetails(false);
    }
  }

  async function handleSync(itemId: string) {
    try {
      setFeedbackMsg('Atualizando dados financeiros...');
      await fetch(`/api/items/${itemId}`, { method: 'PATCH' });
      await fetchItemFinancialData(itemId);
      setFeedbackMsg('Dados consolidados e atualizados com sucesso!');
    } catch (err: any) {
      setFeedbackMsg(`Erro ao sincronizar: ${err.message}`);
    }
  }

  async function handleDisconnect(itemId: string) {
    if (!confirm('Deseja realmente desconectar esta instituição?')) return;
    try {
      setFeedbackMsg('Desconectando...');
      const res = await fetch(`/api/items/${itemId}`, { method: 'DELETE' });
      if (res.ok) {
        setItemData(null);
        setSelectedAccountId(null);
        setFeedbackMsg('Instituição desconectada com sucesso.');
      }
    } catch (err: any) {
      setFeedbackMsg(`Erro ao desconectar: ${err.message}`);
    }
  }

  const formatCurrency = (val: number | undefined | null) => {
    if (val === undefined || val === null || isNaN(val)) return 'R$ 0,00';
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    }).format(val);
  };

  const formatDate = (dateStr: string) => {
    try {
      return new Date(dateStr).toLocaleDateString('pt-BR');
    } catch {
      return dateStr;
    }
  };

  const selectedAccount = accounts.find((a) => a.id === selectedAccountId);

  return (
    <main className="min-h-screen bg-slate-950 text-slate-100 flex flex-col items-center justify-start p-4 sm:p-6 font-sans">
      <div className="max-w-4xl w-full bg-slate-900/80 border border-slate-800 backdrop-blur-xl rounded-2xl p-6 sm:p-8 shadow-2xl space-y-6 my-auto">
        {/* Header */}
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 border-b border-slate-800/80 pb-6">
          <div>
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 mb-2">
              Pluggy Open Finance
            </div>
            <h1 className="text-2xl sm:text-3xl font-bold text-white tracking-tight">
              FinLink AI — Extrato & Saldos
            </h1>
            <p className="text-sm text-slate-400 mt-1">
              Agregador inteligente de contas bancárias e cartões com sincronização em tempo real.
            </p>
          </div>

          {!item && !showConnect && (
            <button
              onClick={() => setShowConnect(true)}
              className="py-2.5 px-5 rounded-xl bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white font-semibold text-sm shadow-lg shadow-emerald-950 transition-all active:scale-[0.98] whitespace-nowrap"
            >
              + Conectar Banco
            </button>
          )}
        </div>

        {/* Feedback Message */}
        {feedbackMsg && (
          <div className="p-3 text-xs rounded-lg bg-emerald-950/60 border border-emerald-800 text-emerald-300">
            {feedbackMsg}
          </div>
        )}

        {/* Loading details */}
        {loadingDetails && (
          <div className="p-8 text-center space-y-3">
            <div className="inline-block w-8 h-8 border-3 border-emerald-500 border-t-transparent rounded-full animate-spin"></div>
            <p className="text-sm text-slate-300 font-medium">Buscando contas e transações da instituição...</p>
            <p className="text-xs text-slate-500">Consolidando saldos e limites do Open Finance...</p>
          </div>
        )}

        {/* Pluggy Connect Modal/Widget */}
        {showConnect && !loadingDetails && (
          <div className="pt-2 space-y-4">
            <OpenFinanceConnect
              onSuccess={(result) => {
                setShowConnect(false);
                const receivedItemId = result?.item?.id || (result as any)?.id;
                if (receivedItemId) {
                  fetchItemFinancialData(receivedItemId);
                } else if (result) {
                  setItemData(result);
                }
              }}
              onError={(error) => {
                console.error('Falha na conexão:', error);
              }}
              onClose={() => {
                setShowConnect(false);
              }}
            />
            <button
              onClick={() => setShowConnect(false)}
              className="w-full text-xs text-slate-400 hover:text-slate-200 text-center underline"
            >
              Cancelar
            </button>
          </div>
        )}

        {/* Connected Item Details View */}
        {item && !loadingDetails && (
          <div className="space-y-6">
            {/* Institution Card */}
            <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 p-4 rounded-xl bg-slate-800/40 border border-slate-700/60">
              <div className="flex items-center gap-3">
                {item?.connector?.imageUrl ? (
                  <img
                    src={item.connector.imageUrl}
                    alt={item.connector.name || 'Banco'}
                    className="w-12 h-12 rounded-xl object-contain bg-white p-1"
                  />
                ) : (
                  <div className="w-12 h-12 rounded-xl bg-emerald-600/20 text-emerald-400 flex items-center justify-center font-bold text-lg">
                    🏛️
                  </div>
                )}
                <div>
                  <h3 className="font-semibold text-white text-base">
                    {item?.connector?.name || 'Mercado Pago'}
                  </h3>
                  <div className="flex items-center gap-2 text-xs text-slate-400 mt-0.5">
                    <span>
                      Status:{' '}
                      <span className="text-emerald-400 font-medium">
                        {item?.status || 'UPDATED'}
                      </span>
                    </span>
                    <span>•</span>
                    <span className="font-mono text-slate-500">
                      ID: {item?.id ? `${String(item.id).slice(0, 8)}...` : 'N/A'}
                    </span>
                  </div>
                </div>
              </div>

              <div className="flex items-center gap-2 w-full sm:w-auto">
                <button
                  onClick={() => item?.id && handleSync(item.id)}
                  className="flex-1 sm:flex-none px-3 py-1.5 text-xs font-medium rounded-lg bg-slate-700/60 hover:bg-slate-700 text-slate-200 border border-slate-600 transition-colors"
                >
                  🔄 Sincronizar
                </button>
                <button
                  onClick={() => item?.id && handleDisconnect(item.id)}
                  className="flex-1 sm:flex-none px-3 py-1.5 text-xs font-medium rounded-lg bg-red-950/40 hover:bg-red-900/60 text-red-300 border border-red-800 transition-colors"
                >
                  Desconectar
                </button>
              </div>
            </div>

            {/* Titular Identity Card (if available) */}
            {identity?.fullName && (
              <div className="p-3.5 rounded-xl bg-slate-800/30 border border-slate-800 text-xs flex flex-wrap items-center justify-between gap-3 text-slate-400">
                <div>
                  <span className="text-slate-500">Titular: </span>
                  <span className="text-white font-medium">{identity.fullName}</span>
                </div>
                {identity.document && (
                  <div>
                    <span className="text-slate-500">{identity.documentType || 'Documento'}: </span>
                    <span className="font-mono text-slate-300">{identity.document}</span>
                  </div>
                )}
                {identity.emails?.[0]?.value && (
                  <div>
                    <span className="text-slate-500">Email: </span>
                    <span className="text-slate-300">{identity.emails[0].value}</span>
                  </div>
                )}
              </div>
            )}

            {/* Accounts & Cards Grid */}
            <div>
              <h4 className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-3">
                Contas & Cartões ({accounts.length})
              </h4>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {accounts.map((acc) => {
                  const isSelected = selectedAccountId === acc.id;
                  const isCredit = acc.type === 'CREDIT';

                  return (
                    <div
                      key={acc.id}
                      onClick={() => setSelectedAccountId(acc.id)}
                      className={`p-4 rounded-xl border cursor-pointer transition-all space-y-2 ${
                        isSelected
                          ? 'bg-emerald-950/30 border-emerald-500/60 shadow-md shadow-emerald-950/20'
                          : 'bg-slate-800/30 border-slate-700/50 hover:border-slate-600'
                      }`}
                    >
                      <div className="flex justify-between items-start">
                        <div>
                          <span
                            className={`inline-block px-2 py-0.5 rounded text-[10px] font-semibold uppercase ${
                              isCredit
                                ? 'bg-purple-950 text-purple-300 border border-purple-800/50'
                                : 'bg-emerald-950 text-emerald-300 border border-emerald-800/50'
                            }`}
                          >
                            {isCredit ? 'Cartão de Crédito' : 'Conta Pagamento'}
                          </span>
                          <p className="text-sm font-semibold text-white mt-1">
                            {acc.name || (isCredit ? 'Cartão' : 'Conta')}
                          </p>
                          <p className="text-xs font-mono text-slate-500">
                            Final {acc.number || '***'}
                          </p>
                        </div>

                        <div className="text-right">
                          <p className="text-xs text-slate-400">
                            {isCredit ? 'Fatura / Usado' : 'Saldo em Conta'}
                          </p>
                          <p
                            className={`text-lg font-bold mt-0.5 ${
                              isCredit ? 'text-amber-400' : 'text-emerald-400'
                            }`}
                          >
                            {formatCurrency(acc.balance)}
                          </p>
                        </div>
                      </div>

                      {/* Credit card extra details */}
                      {isCredit && acc.creditData?.creditLimit && (
                        <div className="pt-2 border-t border-slate-800/60 flex justify-between text-xs text-slate-400">
                          <span>
                            Limite: <b className="text-slate-200">{formatCurrency(acc.creditData.creditLimit)}</b>
                          </span>
                          <span>
                            Disponível:{' '}
                            <b className="text-emerald-400">
                              {formatCurrency(acc.creditData.availableCreditLimit)}
                            </b>
                          </span>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Caixinhas / Reservas do Mercado Pago (se disponíveis na conta selecionada) */}
            {selectedAccount?.bankData?.availableAmounts &&
              selectedAccount.bankData.availableAmounts.length > 0 && (
                <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 space-y-3">
                  <h4 className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
                    Caixinhas & Reservas ({selectedAccount.bankData.availableAmounts.length})
                  </h4>
                  <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
                    {selectedAccount.bankData.availableAmounts.map((box, idx) => (
                      <div key={idx} className="p-2.5 rounded-lg bg-slate-800/40 border border-slate-700/50">
                        <p className="text-xs text-slate-400 font-medium truncate">{box.name || 'Reserva'}</p>
                        <p className="text-sm font-bold text-teal-400 mt-0.5">
                          {formatCurrency(box.amount)}
                        </p>
                      </div>
                    ))}
                  </div>
                </div>
              )}

            {/* Transactions Section */}
            {selectedAccountId && (
              <div>
                <div className="flex items-center justify-between mb-3">
                  <h4 className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
                    Últimas Transações ({selectedAccount?.name || 'Conta'})
                  </h4>
                  <span className="text-xs text-slate-500">
                    {(transactions[selectedAccountId] || []).length} registros
                  </span>
                </div>

                {(!transactions[selectedAccountId] ||
                  transactions[selectedAccountId].length === 0) ? (
                  <div className="p-4 rounded-xl bg-slate-800/20 border border-slate-800 text-center text-xs text-slate-400">
                    Nenhuma transação encontrada nesta conta até o momento.
                  </div>
                ) : (
                  <div className="rounded-xl border border-slate-800 overflow-hidden bg-slate-900/50">
                    <div className="max-h-80 overflow-y-auto divide-y divide-slate-800/60">
                      {transactions[selectedAccountId].map((tx) => (
                        <div
                          key={tx.id}
                          className="p-3.5 flex items-center justify-between hover:bg-slate-800/30 transition-colors text-xs"
                        >
                          <div className="space-y-0.5">
                            <p className="text-white font-medium text-sm">{tx.description}</p>
                            <div className="flex items-center gap-2 text-slate-500">
                              <span>{formatDate(tx.date)}</span>
                              {tx.category && (
                                <>
                                  <span>•</span>
                                  <span className="px-1.5 py-0.5 rounded bg-slate-800 text-slate-400">
                                    {tx.category}
                                  </span>
                                </>
                              )}
                            </div>
                          </div>

                          <div className="text-right">
                            <span
                              className={`font-semibold font-mono text-sm ${
                                tx.amount < 0 ? 'text-rose-400' : 'text-emerald-400'
                              }`}
                            >
                              {formatCurrency(tx.amount)}
                            </span>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>
        )}

        {/* Empty state */}
        {!item && !showConnect && !loadingDetails && (
          <div className="p-8 text-center border border-dashed border-slate-800 rounded-xl space-y-3">
            <div className="w-12 h-12 rounded-full bg-slate-800/60 flex items-center justify-center mx-auto text-xl">
              💳
            </div>
            <p className="text-sm font-medium text-slate-300">Nenhuma conta bancária conectada</p>
            <p className="text-xs text-slate-500 max-w-sm mx-auto">
              Clique em "+ Conectar Banco" para abrir o Pluggy Connect e autenticar uma conta.
            </p>
          </div>
        )}

        {/* Footer info */}
        <div className="text-xs text-slate-500 border-t border-slate-800/80 pt-4 flex flex-col sm:flex-row items-center justify-between gap-2">
          <span>Open Finance Ativo</span>
          <span className="text-emerald-400/80 font-mono">Pluggy SDK v0.70</span>
        </div>
      </div>
    </main>
  );
}
