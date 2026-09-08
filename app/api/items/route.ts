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

// GET /api/items - Retrieve an item if ?id= is passed, or guidance
export async function GET(req: Request) {
  try {
    const { searchParams } = new URL(req.url);
    const id = searchParams.get('id') || searchParams.get('itemId');

    if (id) {
      const pluggy = getPluggyClient();
      const item = await pluggy.fetchItem(id);
      return Response.json(item);
    }

    return Response.json({
      message: 'Pluggy API requires an item ID to fetch financial data. Use /api/items/[id].',
    });
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Error fetching items';
    console.error('[Items API] Error:', error);
    return Response.json({ error: message }, { status: 500 });
  }
}
