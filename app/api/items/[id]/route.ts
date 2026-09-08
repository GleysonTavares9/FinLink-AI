import { PluggyClient } from 'pluggy-sdk';

export const dynamic = 'force-dynamic';

function getPluggyClient(): PluggyClient {
  const clientId = process.env.CLIENT_ID;
  const clientSecret = process.env.CLIENT_SECRET;

  if (!clientId || !clientSecret) {
    throw new Error('Missing Pluggy CLIENT_ID or CLIENT_SECRET in process.env');
  }

  return new PluggyClient({ clientId, clientSecret });
}

// GET /api/items/[id] - Retrieve item, its accounts, transactions, and identity
export async function GET(
  _req: Request,
  { params }: { params: { id: string } }
) {
  try {
    const { id } = params;
    if (!id) {
      return Response.json({ error: 'Item ID is required' }, { status: 400 });
    }

    const pluggy = getPluggyClient();

    // 1. Fetch Item details
    const item = await pluggy.fetchItem(id);

    // 2. Fetch Accounts
    let accounts: any[] = [];
    try {
      const accountsResponse = await pluggy.fetchAccounts(id);
      accounts = accountsResponse.results || [];
    } catch (err) {
      console.warn(`[Items API] Could not fetch accounts for item ${id}:`, err);
    }

    // 3. Fetch Transactions for each account
    const transactionsByAccount: Record<string, any[]> = {};
    for (const account of accounts) {
      try {
        const txResponse = await pluggy.fetchTransactions(account.id, { pageSize: 25 });
        transactionsByAccount[account.id] = txResponse.results || [];
      } catch (err) {
        console.warn(`[Items API] Could not fetch transactions for account ${account.id}:`, err);
        transactionsByAccount[account.id] = [];
      }
    }

    // 4. Fetch Identity (if available)
    let identity: any = null;
    try {
      identity = await pluggy.fetchIdentityByItemId(id);
    } catch (err) {
      // Identity may not be supported for all connectors
    }

    return Response.json({
      item,
      accounts,
      transactions: transactionsByAccount,
      identity,
    });
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Error fetching item data';
    console.error(`[Items API] Error fetching item ${params.id}:`, error);
    return Response.json({ error: message }, { status: 500 });
  }
}

// PATCH /api/items/[id] - Trigger an item synchronization / update
export async function PATCH(
  _req: Request,
  { params }: { params: { id: string } }
) {
  try {
    const { id } = params;
    if (!id) {
      return Response.json({ error: 'Item ID is required' }, { status: 400 });
    }

    const pluggy = getPluggyClient();

    try {
      const updatedItem = await pluggy.updateItem(id);
      return Response.json({ success: true, item: updatedItem });
    } catch (sdkErr: any) {
      const message = sdkErr?.response?.data?.message || sdkErr?.message || 'Sync not needed';
      return Response.json({ success: true, message, status: 'SKIPPED' });
    }
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Error updating item';
    console.error(`[Items API] Error updating item ${params.id}:`, error);
    return Response.json({ error: message }, { status: 500 });
  }
}

// DELETE /api/items/[id] - Disconnect and delete an item
export async function DELETE(
  _req: Request,
  { params }: { params: { id: string } }
) {
  try {
    const { id } = params;
    if (!id) {
      return Response.json({ error: 'Item ID is required' }, { status: 400 });
    }

    const pluggy = getPluggyClient();
    await pluggy.deleteItem(id);

    return Response.json({ success: true, deletedId: id });
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Error deleting item';
    console.error(`[Items API] Error deleting item ${params.id}:`, error);
    return Response.json({ error: message }, { status: 500 });
  }
}
