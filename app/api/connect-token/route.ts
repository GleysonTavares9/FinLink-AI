import { PluggyClient } from 'pluggy-sdk';

export const dynamic = 'force-dynamic';

function formatErrorMessage(error: unknown): string {
  if (!error) return 'Unknown error';
  if (error instanceof Error) return error.message;
  if (typeof error === 'object') {
    const err = error as Record<string, any>;
    if (err.message) return String(err.message);
    if (err.response?.data?.message) return String(err.response.data.message);
    try {
      return JSON.stringify(error);
    } catch {
      return String(error);
    }
  }
  return String(error);
}

async function generateConnectToken(itemId?: string, clientUserId?: string) {
  const clientId = process.env.CLIENT_ID;
  const clientSecret = process.env.CLIENT_SECRET;

  if (!clientId || !clientSecret) {
    return Response.json(
      {
        error:
          'Pluggy credentials missing. Please verify CLIENT_ID and CLIENT_SECRET in your .env.local file.',
      },
      { status: 500 }
    );
  }

  const pluggy = new PluggyClient({
    clientId,
    clientSecret,
  });

  // Pluggy SDK: createConnectToken(itemId?: string, options?: { clientUserId?: string })
  const options = clientUserId ? { clientUserId } : undefined;
  const connectToken = await pluggy.createConnectToken(itemId || undefined, options);

  return Response.json({ accessToken: connectToken.accessToken });
}

export async function GET() {
  try {
    return await generateConnectToken();
  } catch (error: unknown) {
    const message = formatErrorMessage(error);
    console.error('Failed to create Pluggy Connect Token (GET):', error);
    return Response.json(
      { error: `Failed to create Connect Token: ${message}` },
      { status: 500 }
    );
  }
}

export async function POST(req: Request) {
  try {
    let clientUserId: string | undefined;
    let itemId: string | undefined;

    try {
      const body = await req.json();
      clientUserId = body?.clientUserId;
      itemId = body?.itemId;
    } catch {
      // Body may be empty
    }

    return await generateConnectToken(itemId, clientUserId);
  } catch (error: unknown) {
    const message = formatErrorMessage(error);
    console.error('Failed to create Pluggy Connect Token (POST):', error);
    return Response.json(
      { error: `Failed to create Connect Token: ${message}` },
      { status: 500 }
    );
  }
}
