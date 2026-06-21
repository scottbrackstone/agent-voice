import type { FastifyInstance } from "fastify";
import { z } from "zod";
import { createRequestId } from "../utils/requestId.js";

const TranscriptionResponseSchema = z.object({
  text: z.string().optional()
});

export function registerTranscribeRoute(server: FastifyInstance): void {
  server.addContentTypeParser(
    /^audio\/.*/,
    { parseAs: "buffer", bodyLimit: 8 * 1024 * 1024 },
    (_request, body, done) => {
      done(null, body);
    }
  );

  server.post("/api/transcribe", async (request, reply) => {
    const requestId = createRequestId();
    const apiKey = process.env.MISTRAL_API_KEY;

    if (!apiKey) {
      return reply.status(503).send({
        error: "Voxtral transcription is not configured.",
        requestId
      });
    }

    if (!Buffer.isBuffer(request.body) || request.body.length === 0) {
      return reply.status(400).send({
        error: "Audio body is required.",
        requestId
      });
    }

    const contentType = request.headers["content-type"] ?? "audio/mp4";
    const fileName = contentType.includes("wav") ? "agentvoice.wav" : "agentvoice.m4a";
    const formData = new FormData();
    formData.set("model", process.env.VOXTRAL_TRANSCRIBE_MODEL ?? "voxtral-mini-latest");
    formData.set("language", process.env.VOXTRAL_TRANSCRIBE_LANGUAGE ?? "en");
    const audioBytes = new Uint8Array(request.body);
    formData.set("file", new Blob([audioBytes], { type: contentType }), fileName);

    const controller = new AbortController();
    const timeout = setTimeout(
      () => controller.abort(),
      Number(process.env.VOXTRAL_TRANSCRIBE_TIMEOUT_MS ?? 90_000)
    );

    try {
      const response = await fetch("https://api.mistral.ai/v1/audio/transcriptions", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${apiKey}`
        },
        body: formData,
        signal: controller.signal
      });
      const responseBody = await response.text();

      if (!response.ok) {
        request.log.warn(
          {
            requestId,
            statusCode: response.status,
            responseBody: responseBody.slice(0, 240)
          },
          "Voxtral transcription failed"
        );
        return reply.status(502).send({
          error: `Voxtral transcription failed with HTTP ${response.status}.`,
          requestId
        });
      }

      const parsed = TranscriptionResponseSchema.safeParse(JSON.parse(responseBody));
      if (!parsed.success) {
        return reply.status(502).send({
          error: "Voxtral transcription response was not understood.",
          requestId
        });
      }

      return reply.send({
        transcript: parsed.data.text?.trim() ?? "",
        provider: "voxtral",
        requestId
      });
    } catch (error) {
      request.log.warn({ requestId, error }, "Voxtral transcription request failed");
      return reply.status(502).send({
        error: "Unable to reach Voxtral transcription.",
        requestId
      });
    } finally {
      clearTimeout(timeout);
    }
  });
}
