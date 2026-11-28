import path from 'node:path';
import { fileURLToPath } from 'node:url';
import express, { type NextFunction, type Request, type Response } from 'express';
import { AiGatewayService, type GatewayHttpResult } from './server/aiGateway/gatewayService.ts';
import {
  isGatewayAccessConfigured,
  requireGatewayAccess,
  resolveGatewayAccessPolicy,
  type GatewayAccessPolicy,
} from './server/accessPolicy.ts';
import {
  closeHttpServer, installWebRuntime, serveProductionWeb, unknownApiRoute, type WebRuntimeState,
} from './server/webRuntime.ts';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

function resolveClientKey(req: Request): string {
  const ip = typeof req.ip === 'string' && req.ip.trim() ? req.ip.trim() : '';
  const remote = req.socket?.remoteAddress || '';
  return ip || remote || 'anonymous';
}

function attachRequestCancellation(req: Request, res: Response): {
  signal: AbortSignal;
  cleanup: () => void;
} {
  const controller = new AbortController();
  const onAbort = () => {
    if (!controller.signal.aborted) {
      controller.abort();
    }
  };
  const onResClose = () => {
    if (!res.writableEnded) {
      onAbort();
    }
  };
  req.on('aborted', onAbort);
  res.on('close', onResClose);

  return {
    signal: controller.signal,
    cleanup: () => {
      req.off('aborted', onAbort);
      res.off('close', onResClose);
    },
  };
}

function sendGatewayResult<T>(res: Response, result: GatewayHttpResult<T>) {
  return res.status(result.httpStatus).json(result.body);
}

export interface CreateAppOptions {
  gateway?: AiGatewayService;
  serveStatic?: boolean;
  accessPolicy?: GatewayAccessPolicy;
  runtimeState?: WebRuntimeState;
}

export async function createApp(options?: CreateAppOptions) {
  const gateway = options?.gateway ?? new AiGatewayService();
  const serveStatic = options?.serveStatic ?? true;
  const accessPolicy = options?.accessPolicy ?? resolveGatewayAccessPolicy();

  const app = express();
  const runtimeState = options?.runtimeState ?? { shuttingDown: false };
  installWebRuntime(app, runtimeState, { production: process.env.NODE_ENV === 'production' });
  // Guard the route families before parsing bodies, including future AI routes.
  // The deployment token must only be held by trusted server callers.
  app.use('/api/gemini', requireGatewayAccess(accessPolicy, { requireClinicalRelease: true }));
  app.use('/api/ai-gateway/audit', requireGatewayAccess(accessPolicy, { requireClinicalRelease: false }));
  app.use(express.json({ limit: '10mb' }));

  app.use((err: unknown, _req: Request, res: Response, next: NextFunction) => {
    if (!err) return next();
    const errObj = err as { type?: string; status?: number };
    if (errObj.type === 'entity.too.large' || errObj.status === 413) {
      return res.status(413).json({
        error: 'حجم درخواست ارسالی بیش از سقف مجاز است.',
        code: 'PAYLOAD_TOO_LARGE',
      });
    }
    if (err instanceof SyntaxError || errObj.status === 400) {
      return res.status(400).json({
        error: 'ساختار JSON درخواست نامعتبر است.',
        code: 'MALFORMED_REQUEST_JSON',
      });
    }
    return res.status(500).json({
      error: 'خطای غیرمنتظره در پردازش درخواست سرور.',
      code: 'INTERNAL_SERVER_ERROR',
    });
  });

  app.get('/api/health', (_req: Request, res: Response) => {
    res.setHeader('Cache-Control', 'no-store');
    res.json({
      ok: true,
      clinicalServiceEnabled: accessPolicy.clinicalServiceEnabled,
      aiServiceAvailable:
        accessPolicy.clinicalServiceEnabled &&
        isGatewayAccessConfigured(accessPolicy) &&
        gateway.getHealthStatus().hasGeminiKey,
      offlineEnginesIndependent: true,
    });
  });

  app.get('/api/ai-gateway/audit', (req: Request, res: Response) => {
    const rawLimit = typeof req.query.limit === 'string' ? Number.parseInt(req.query.limit, 10) : 25;
    const limit = Number.isFinite(rawLimit) ? Math.min(Math.max(rawLimit, 1), 100) : 25;
    res.json({
      gatewayMode: 'RESTRICTED_AUDITABLE',
      entries: gateway.auditTrail.listRecent(limit),
    });
  });

  // 1. Clinician Visit Draft (validated structured output; SSE delivers only post-validation output)
  app.post('/api/gemini/visit-draft', async (req: Request, res: Response) => {
    const body = (req.body && typeof req.body === 'object' ? req.body : {}) as Record<string, unknown>;
    const wantsStream = body.stream === true;
    const { signal, cleanup } = attachRequestCancellation(req, res);

    try {
      const result = await gateway.handleVisitDraft(body, resolveClientKey(req), signal);
      if (signal.aborted && result.httpStatus === 499) {
        if (!res.writableEnded) return sendGatewayResult(res, result);
        return;
      }

      if (wantsStream && result.httpStatus === 200) {
        res.setHeader('Content-Type', 'text/event-stream; charset=utf-8');
        res.setHeader('Cache-Control', 'no-store, no-transform');
        res.setHeader('Connection', 'keep-alive');

        const formattedText = result.body.data.formattedDraftFa;
        res.write(
          `data: ${JSON.stringify({
            delta: formattedText,
            text: formattedText,
            data: result.body.data,
            meta: result.body.meta,
            done: true,
          })}\n\n`,
        );
        return res.end();
      }

      return sendGatewayResult(res, result);
    } finally {
      cleanup();
    }
  });

  // 2. Pediatric Nutrition Education Advisor (locked <6m and allergy; strict <12m validation)
  app.post('/api/gemini/nutrition', async (req: Request, res: Response) => {
    const body = (req.body && typeof req.body === 'object' ? req.body : {}) as Record<string, unknown>;
    const { signal, cleanup } = attachRequestCancellation(req, res);
    try {
      const result = await gateway.handleNutrition(body, resolveClientKey(req), signal);
      if (!res.writableEnded) return sendGatewayResult(res, result);
    } finally {
      cleanup();
    }
  });

  // 3. Candidate Medication Label Extraction (read-only candidate extraction; mandatory parent confirmation)
  app.post('/api/gemini/syrup-scan', async (req: Request, res: Response) => {
    const body = (req.body && typeof req.body === 'object' ? req.body : {}) as Record<string, unknown>;
    const { signal, cleanup } = attachRequestCancellation(req, res);
    try {
      const result = await gateway.handleSyrupScan(body, resolveClientKey(req), signal);
      if (!res.writableEnded) return sendGatewayResult(res, result);
    } finally {
      cleanup();
    }
  });

  // 4. Deterministic Triage Explanation Checklist (URGENT_EMERGENCY bypasses AI completely)
  app.post('/api/gemini/safety-checklist', async (req: Request, res: Response) => {
    const body = (req.body && typeof req.body === 'object' ? req.body : {}) as Record<string, unknown>;
    const { signal, cleanup } = attachRequestCancellation(req, res);
    try {
      const result = await gateway.handleSafetyChecklist(body, resolveClientKey(req), signal);
      if (!res.writableEnded) return sendGatewayResult(res, result);
    } finally {
      cleanup();
    }
  });

  // 5. Documented FAQ Simplifier (server-side canonical FAQ grounding only)
  app.post('/api/gemini/documented-qa', async (req: Request, res: Response) => {
    const body = (req.body && typeof req.body === 'object' ? req.body : {}) as Record<string, unknown>;
    const { signal, cleanup } = attachRequestCancellation(req, res);
    try {
      const result = await gateway.handleDocumentedQa(body, resolveClientKey(req), signal);
      if (!res.writableEnded) return sendGatewayResult(res, result);
    } finally {
      cleanup();
    }
  });

  app.use('/api', unknownApiRoute);

  if (serveStatic) {
    if (process.env.NODE_ENV === 'production') {
      const distDir = path.resolve(__dirname, 'dist');
      serveProductionWeb(app, distDir);
    } else {
      const { createServer: createViteServer } = await import('vite');
      const vite = await createViteServer({
        server: {
          middlewareMode: true,
          hmr: false,
        },
        appType: 'spa',
      });
      app.use(vite.middlewares);
    }
  }

  return app;
}

const isMainModule =
  Boolean(process.argv[1]) && path.resolve(process.argv[1]) === __filename;

if (isMainModule && process.env.MANO_NO_LISTEN !== '1') {
  const port = Number.parseInt(process.env.PORT || '3000', 10) || 3000;
  const runtimeState: WebRuntimeState = { shuttingDown: false };
  createApp({ runtimeState })
    .then((app) => {
      const server = app.listen(port, '0.0.0.0', () => {
        console.log(`MANO Kid preview server running on http://0.0.0.0:${port}`);
      });
      const shutdown = async () => {
        if (runtimeState.shuttingDown) return;
        runtimeState.shuttingDown = true;
        const { forced } = await closeHttpServer(server);
        process.exit(forced ? 1 : 0);
      };
      process.once('SIGTERM', shutdown);
      process.once('SIGINT', shutdown);
    })
    .catch((err) => {
      console.error('Failed to start MANO Kid server:', err);
      process.exit(1);
    });
}
