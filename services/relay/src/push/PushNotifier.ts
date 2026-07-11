import type { RelayInbox } from "../db/inbox.js";
import type { Messaging, MulticastMessage } from "firebase-admin/messaging";

export type PushMessage = {
  threadId: string;
  messageId: string;
  title: string;
  body: string;
  source: string;
};

export interface PushNotifier {
  notify(message: PushMessage): Promise<void>;
}

export class DisabledPushNotifier implements PushNotifier {
  async notify(): Promise<void> {}
}

export class FirebasePushNotifier implements PushNotifier {
  private messagingPromise?: Promise<Messaging | null>;

  constructor(private readonly store: RelayInbox) {}

  async notify(message: PushMessage): Promise<void> {
    const tokens = this.store.listDeviceTokens();
    if (tokens.length === 0) return;
    const messaging = await this.messaging();
    if (!messaging) return;
    const payload: MulticastMessage = {
      tokens,
      notification: {
        title: message.title,
        body: message.body
      },
      data: {
        type: "message_ready",
        threadId: message.threadId,
        messageId: message.messageId,
        source: message.source
      },
      android: {
        priority: "high",
        notification: {
          channelId: "agentvoice_messages",
          tag: `thread:${message.threadId}`
        }
      }
    };
    await messaging.sendEachForMulticast(payload);
  }

  private messaging(): Promise<Messaging | null> {
    this.messagingPromise ??= this.loadMessaging();
    return this.messagingPromise;
  }

  private async loadMessaging(): Promise<Messaging | null> {
    if (process.env.AGENTVOICE_FIREBASE_ENABLED !== "true") return null;
    const appModule = await import("firebase-admin/app");
    const messagingModule = await import("firebase-admin/messaging");
    const app = appModule.getApps()[0] ?? appModule.initializeApp({
      credential: appModule.applicationDefault(),
      projectId: process.env.FIREBASE_PROJECT_ID
    });
    return messagingModule.getMessaging(app);
  }
}
