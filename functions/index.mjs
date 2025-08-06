// index.mjs
import { onCall, HttpsError } from "firebase-functions/v2/https";
import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { onSchedule } from "firebase-functions/v2/scheduler";
import admin from "firebase-admin";
import jwt from "jsonwebtoken";
import axios from "axios";
import { createRequire } from "module";
const require = createRequire(import.meta.url);
const { StreamChat } = require("stream-chat");

admin.initializeApp();

const db = admin.firestore();

const STREAM_API_KEY = process.env.STREAM_API_KEY || process.env.FUNCTIONS_CONFIG?.stream?.api_key;
const STREAM_API_SECRET = process.env.STREAM_API_SECRET || process.env.FUNCTIONS_CONFIG?.stream?.api_secret;


if (!STREAM_API_KEY || !STREAM_API_SECRET) {
  console.warn("⚠️ STREAM_API_KEY or STREAM_API_SECRET is not set in environment variables.");
}

// Create Notification
export const createNotification = onCall(async (request) => {
  const { toUserId, title, message, type, eventId } = request.data;

  if (!request.auth) {
    throw new HttpsError("unauthenticated", "User must be logged in.");
  }

  if (!toUserId || !title || !message || !type || !eventId) {
      throw new HttpsError("invalid-argument", "Missing one or more parameters.");
    }

  const nowMillis = Date.now();
  const expiryMillis = nowMillis + 24 * 60 * 60 * 1000; // 1 day

  const notification = {
    title,
    message,
    type,
    eventId,
    timestamp: nowMillis,
    expiry: admin.firestore.Timestamp.fromMillis(expiryMillis),
    isRead: false
  };

  await db.collection("notifications")
      .doc(toUserId)
      .collection("userNotifications")
      .add(notification);

  console.log(`✅ Notification created for ${toUserId}: ${title}`);

  return { success: true };
});


// 1️⃣ Push Notifications via FCM
export const sendPushNotification = onDocumentCreated(
  {
    region: "us-central1",
    document: "notifications/{userId}/userNotifications/{notificationId}",
  },
  async (event) => {
    if (!event.data) {
      console.log("No data found.");
      return;
    }

    const notification = event.data.data();
    const userId = event.params.userId;

    const userDoc = await db.collection("users").doc(userId).get();
    const fcmToken = userDoc.get("fcmToken");

    if (!fcmToken) {
      console.log(`No FCM token for user ${userId}`);
      return;
    }

    const message = {
      token: fcmToken,
      notification: {
        title: notification.title || "New Notification",
        body: notification.message || "",
      },
      data: {
        type: notification.type || "",
        eventId: notification.eventId || "",
      },
    };

    try {
      await admin.messaging().send(message);
      console.log(`Notification sent to ${userId}`);
    } catch (err) {
      console.error("Error sending notification:", err.message);
    }
  }
);

// 2️⃣ Cleanup old notifications
export const cleanupOldNotifications = onSchedule(
  {
    schedule: "every 24 hours",
    timeZone: "Asia/Kuala_Lumpur",
    region: "us-central1",
  },
  async () => {
    const expirationTime = Date.now() - 1 * 24 * 60 * 60 * 1000; // 10 days

    const users = await db.collection("notifications").listDocuments();

    for (const userDoc of users) {
      const userId = userDoc.id;
      const notifSnap = await db
        .collection("notifications")
        .doc(userId)
        .collection("userNotifications")
        .where("timestamp", "<", expirationTime)
        .get();

      if (!notifSnap.empty) {
        const batch = db.batch();
        notifSnap.forEach((doc) => batch.delete(doc.ref));
        await batch.commit();
        console.log(`Deleted ${notifSnap.size} old notifications for user ${userId}`);
      }
    }

    console.log("Cleanup complete.");
  }
);

// 3️⃣ Generate Stream Chat Token
export const generateStreamToken = onCall(async (request) => {
  console.log("generateStreamToken called");
  console.log("User auth:", request.auth);
  console.log("STREAM_API_SECRET:", STREAM_API_SECRET ? "[SET]" : "[NOT SET]");

  if (!request.auth) {
    throw new HttpsError("unauthenticated", "User must be logged in.");
  }

  if (!STREAM_API_SECRET) {
    throw new HttpsError("internal", "STREAM_API_SECRET is not configured.");
  }

  try {
    const userId = request.auth.uid;
    console.log("Generating token for userId:", userId);

    const token = jwt.sign(
      { user_id: userId },
      STREAM_API_SECRET,
      { algorithm: "HS256" }
    );

    console.log("Token generated successfully");
    return { token };
  } catch (err) {
    console.error("Token generation failed:", err);
    throw new HttpsError("internal", err.message);
  }
});


// 4️⃣ Freeze Chat After Event Ends
export const freezeChatAfterEvent = onSchedule(
  {
    schedule: "every 1 hours",
    timeZone: "Asia/Kuala_Lumpur",
    region: "us-central1",
  },
  async () => {
    const now = Date.now();
    const twoHoursMillis = 2 * 60 * 60 * 1000;

    const snapshot = await db
      .collection("events")
      .where("endTimeMillis", "<", now - twoHoursMillis)
      .where("chatFrozen", "==", false)
      .get();

    for (const doc of snapshot.docs) {
      const event = doc.data();
      const eventId = doc.id;
      const channelId = `event_${eventId}`;

      try {
        const channel = streamServerClient.channel("messaging", channelId);
        await channel.update({ frozen: true });

        await doc.ref.update({ chatFrozen: true });

        console.log(`Chat for event ${eventId} frozen.`);
      } catch (err) {
        console.error(`Failed to freeze chat for ${eventId}:`, err.response?.data || err.message);
      }
    }
  }
);

// Helper: Generate Server-Side JWT
function generateServerToken() {
  if (!STREAM_API_SECRET) {
    throw new Error("STREAM_API_SECRET is not set");
  }

  return jwt.sign(
    { server: true },
    STREAM_API_SECRET,
    { algorithm: "HS256" }
  );
}


// Initialize Stream Server Client
const streamServerClient = StreamChat.getInstance(STREAM_API_KEY, STREAM_API_SECRET);

// 5️⃣ Add User to Event Chat
export const addUserToEventChat = onCall(async (request) => {
  const { eventId, userId } = request.data;

  if (!request.auth) {
    throw new HttpsError("unauthenticated", "User must be logged in.");
  }

  if (!eventId || !userId) {
    throw new HttpsError("invalid-argument", "Missing eventId or userId.");
  }

  try {
    // Fetch user profile from Firestore
    const userDoc = await db.collection("users").doc(userId).get();

    if (!userDoc.exists) {
      throw new HttpsError("not-found", `User ${userId} does not exist in Firestore.`);
    }

    const userData = userDoc.data();
    const name = userData.name || "Unknown";
    const image = userData.profileImageUrl || "";

    // Upsert global user profile for Stream Chat
    await streamServerClient.upsertUser({
      id: userId,
      name: name,
      image: image,
    });

    console.log(`✅ Upserted global user ${userId} with name: ${name}`);

    // Add to event channel with custom member fields
    const channel = streamServerClient.channel("messaging", `event_${eventId}`);

    await channel.addMembers([
      {
        user_id: userId,
        role: "member",
        custom: {
          name: name,
          image: image,
        }
      }
    ]);

    console.log(`✅ Added user ${userId} to event_${eventId} with custom fields.`);

    return { success: true };

  } catch (err) {
    console.error("❌ Failed to add user to event chat:", err);
    throw new HttpsError("internal", err.message);
  }
});




// 6️⃣ Remove User from Event Chat
export const removeUserFromEventChat = onCall(async (request) => {
  const { eventId, userId } = request.data;

  if (!request.auth) {
    throw new HttpsError("unauthenticated", "User must be logged in.");
  }

  if (!eventId || !userId) {
    throw new HttpsError("invalid-argument", "Missing eventId or userId.");
  }

  try {
    const channel = streamServerClient.channel("messaging", `event_${eventId}`);

    // Remove the user from the channel
    await channel.removeMembers([userId]);

    console.log(`✅ Removed user ${userId} from chat event_${eventId}`);
    return { success: true };
  } catch (err) {
    console.error("❌ Failed to remove user from chat:", err);
    throw new HttpsError("internal", err.message);
  }
});


// 7️⃣ Sync Stream User Profile
export const syncStreamUserProfile = onCall(async (request) => {
    const { userId } = request.data;

    if (!userId) {
        throw new HttpsError("invalid-argument", "Missing userId.");
    }

    try {
        const userDoc = await db.collection("users").doc(userId).get();

        if (!userDoc.exists) {
            throw new HttpsError("not-found", `User ${userId} not found.`);
        }

        const userData = userDoc.data();
        const streamUser = {
            id: userId,
            name: userData?.name || "Unknown",
            image: userData?.profileImageUrl || null,
        };

        await streamServerClient.upsertUser(streamUser);
        console.log(`✅ Synced user ${userId} to Stream`);

        return { success: true };
    } catch (err) {
        console.error(`❌ Failed to sync user ${userId}:`, err);
        throw new HttpsError("internal", err.message || "Unknown error");
    }
});
