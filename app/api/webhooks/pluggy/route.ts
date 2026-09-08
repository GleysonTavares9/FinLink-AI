import { PluggyClient } from 'pluggy-sdk';

export const dynamic = 'force-dynamic';

interface PluggyWebhookEvent {
  event: 'item/created' | 'item/updated' | 'item/error' | 'item/deleted' | string;
  eventId?: string;
  itemId?: string;
  error?: {
    code: string;
    message: string;
  };
  triggeredBy?: string;
  data?: Record<string, unknown>;
}

function getPluggyClient(): PluggyClient | null {
  const clientId = process.env.CLIENT_ID;
  const clientSecret = process.env.CLIENT_SECRET;

  if (!clientId || !clientSecret) {
    console.warn(
      '[Webhook] CLIENT_ID or CLIENT_SECRET missing in process.env. Cannot fetch item details.'
    );
    return null;
  }

  return new PluggyClient({ clientId, clientSecret });
}

async function handleItemCreated(itemId: string) {
  console.log(`[Webhook] Item created: ${itemId}`);
  const pluggy = getPluggyClient();
  if (!pluggy) return;

  try {
    const item = await pluggy.fetchItem(itemId);
    console.log(
      `[Webhook] Fetched item info for ${itemId}: Connector ${item.connector?.name} | Status: ${item.status}`
    );
  } catch (err) {
    console.error(`[Webhook] Error processing item/created for ${itemId}:`, err);
  }
}

async function handleItemUpdated(itemId: string) {
  console.log(`[Webhook] Item updated: ${itemId}`);
  const pluggy = getPluggyClient();
  if (!pluggy) return;

  try {
    const item = await pluggy.fetchItem(itemId);
    console.log(
      `[Webhook] Item ${itemId} status: ${item.status}, executionStatus: ${item.executionStatus}`
    );

    if (item.status === 'UPDATED' || item.executionStatus === 'SUCCESS') {
      const accounts = await pluggy.fetchAccounts(itemId);
      console.log(`[Webhook] Synchronized ${accounts.results.length} accounts for item ${itemId}`);
    }
  } catch (err) {
    console.error(`[Webhook] Error processing item/updated for ${itemId}:`, err);
  }
}

async function handleItemError(itemId: string, error?: { code: string; message: string }) {
  console.error(`[Webhook] Item ${itemId} reported error:`, error);
}

export async function POST(req: Request) {
  try {
    let rawBody = '';
    let event: any = null;

    try {
      rawBody = await req.text();
      event = rawBody ? JSON.parse(rawBody) : {};
    } catch (parseErr) {
      console.error('[Webhook] JSON parse error:', parseErr);
      return Response.json(
        { error: 'Invalid JSON payload', details: String(parseErr) },
        { status: 400 }
      );
    }

    console.log(`[Webhook] Received event: ${event?.event} | Item ID: ${event?.itemId}`);

    if (event?.itemId) {
      switch (event.event) {
        case 'item/created':
          handleItemCreated(event.itemId).catch((err) =>
            console.error('[Webhook] Background handleItemCreated failed:', err)
          );
          break;

        case 'item/updated':
          handleItemUpdated(event.itemId).catch((err) =>
            console.error('[Webhook] Background handleItemUpdated failed:', err)
          );
          break;

        case 'item/error':
          handleItemError(event.itemId, event.error).catch((err) =>
            console.error('[Webhook] Background handleItemError failed:', err)
          );
          break;

        default:
          console.log(`[Webhook] Unhandled event type: ${event.event}`);
          break;
      }
    }

    // Return 200 within 5 seconds
    return Response.json({ received: true, event: event?.event || null }, { status: 200 });
  } catch (error: unknown) {
    console.error('[Webhook] Failed to process request:', error);
    const message = error instanceof Error ? error.message : String(error);
    return Response.json({ error: 'Internal webhook error', details: message }, { status: 500 });
  }
}
