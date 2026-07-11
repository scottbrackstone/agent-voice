import type { FastifyInstance } from "fastify";
import { z } from "zod";
import type { RelayInbox } from "../db/inbox.js";

const DeviceBody = z.object({
  installationId: z.string().trim().min(1).max(200),
  fcmToken: z.string().trim().min(1).max(4096),
  platform: z.literal("android").default("android")
});
const DeviceParams = z.object({ installationId: z.string().trim().min(1) });

export function registerDevicesRoute(server: FastifyInstance, store: RelayInbox): void {
  server.post("/api/devices/register", async (request, reply) => {
    const parsed = DeviceBody.safeParse(request.body);
    if (!parsed.success) return reply.status(400).send({ error: "Invalid device registration" });
    store.upsertDeviceToken(parsed.data.installationId, parsed.data.fcmToken);
    return reply.status(201).send({ installationId: parsed.data.installationId, status: "registered" });
  });

  server.delete("/api/devices/:installationId", async (request, reply) => {
    const parsed = DeviceParams.safeParse(request.params);
    if (!parsed.success) return reply.status(400).send({ error: "installationId is required" });
    const removed = store.removeDeviceToken(parsed.data.installationId);
    return removed ? reply.status(204).send() : reply.status(404).send({ error: "Device not found" });
  });
}

