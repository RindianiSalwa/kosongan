const crypto = require("node:crypto");
const admin = require("firebase-admin");
const { onRequest } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");

admin.initializeApp();

const fonnteToken = defineSecret("FONNTE_TOKEN");
const OTP_TTL_MS = 5 * 60 * 1000;

exports.requestPasswordResetOtp = onRequest({ secrets: [fonnteToken] }, async (req, res) => {
  if (req.method !== "POST") {
    res.status(405).json({ message: "Method tidak didukung." });
    return;
  }

  try {
    const phone = String(req.body?.phone || "").trim();
    if (!phone) {
      res.status(400).json({ message: "Nomor WhatsApp wajib diisi." });
      return;
    }

    const pembeliDoc = await findPembeliByPhone(phone);

    if (!pembeliDoc) {
      res.status(404).json({ message: "Nomor WhatsApp tidak ditemukan." });
      return;
    }

    const sessionId = crypto.randomUUID();
    const otp = String(crypto.randomInt(100000, 1000000));
    const expiresAt = Date.now() + OTP_TTL_MS;

    await admin.firestore().collection("password_reset_otps").doc(sessionId).set({
      uid: pembeliDoc.id,
      phone: normalizeForWhatsApp(phone),
      otpHash: hashOtp(otp),
      expiresAt: admin.firestore.Timestamp.fromMillis(expiresAt),
      used: false,
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });

    await sendWhatsappOtp(normalizeForWhatsApp(phone), otp);

    res.status(200).json({
      sessionId,
      message: "OTP sudah dikirim ke WhatsApp."
    });
  } catch (error) {
    console.error("requestPasswordResetOtp failed", error);
    res.status(500).json({ message: "OTP gagal dikirim. Silakan coba lagi." });
  }
});

exports.confirmPasswordResetOtp = onRequest(async (req, res) => {
  if (req.method !== "POST") {
    res.status(405).json({ message: "Method tidak didukung." });
    return;
  }

  try {
    const sessionId = String(req.body?.sessionId || "").trim();
    const otp = String(req.body?.otp || "").trim();
    const newPassword = String(req.body?.newPassword || "");

    if (!sessionId || !otp || !newPassword) {
      res.status(400).json({ message: "Sesi, OTP, dan password baru wajib diisi." });
      return;
    }
    if (newPassword.length < 6) {
      res.status(400).json({ message: "Password minimal 6 karakter." });
      return;
    }

    const otpRef = admin.firestore().collection("password_reset_otps").doc(sessionId);
    const otpDoc = await otpRef.get();
    if (!otpDoc.exists) {
      res.status(400).json({ message: "Sesi reset tidak ditemukan." });
      return;
    }

    const data = otpDoc.data();
    const expiresAt = data.expiresAt?.toMillis?.() || 0;
    if (data.used || expiresAt < Date.now()) {
      res.status(400).json({ message: "OTP sudah kedaluwarsa. Kirim OTP ulang." });
      return;
    }
    if (data.otpHash !== hashOtp(otp)) {
      res.status(400).json({ message: "Kode OTP tidak sesuai." });
      return;
    }

    await admin.auth().updateUser(data.uid, { password: newPassword });
    await otpRef.update({
      used: true,
      usedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    res.status(200).json({ message: "Password berhasil diganti." });
  } catch (error) {
    console.error("confirmPasswordResetOtp failed", error);
    res.status(500).json({ message: "Password gagal diganti. Silakan coba lagi." });
  }
});

function phoneVariants(phone) {
  const digits = onlyDigits(phone);
  const withoutCountry = digits.startsWith("62") ? digits.slice(2) : digits;
  const withoutZero = withoutCountry.startsWith("0") ? withoutCountry.slice(1) : withoutCountry;
  return Array.from(new Set([
    digits,
    `0${withoutZero}`,
    `62${withoutZero}`,
    withoutZero
  ].filter(Boolean))).slice(0, 10);
}

async function findPembeliByPhone(phone) {
  const variants = phoneVariants(phone);
  const exactSnap = await admin.firestore()
    .collection("pembeli")
    .where("no_hp", "in", variants)
    .limit(1)
    .get();

  if (!exactSnap.empty) {
    return exactSnap.docs[0];
  }

  const target = normalizeComparablePhone(phone);
  const fallbackSnap = await admin.firestore()
    .collection("pembeli")
    .limit(500)
    .get();

  return fallbackSnap.docs.find((doc) => {
    const storedPhone = doc.get("no_hp");
    return normalizeComparablePhone(storedPhone) === target;
  }) || null;
}

function normalizeForWhatsApp(phone) {
  const digits = onlyDigits(phone);
  if (digits.startsWith("62")) return digits;
  if (digits.startsWith("0")) return `62${digits.slice(1)}`;
  if (digits.startsWith("8")) return `62${digits}`;
  return digits;
}

function onlyDigits(value) {
  return String(value || "").replace(/\D/g, "");
}

function normalizeComparablePhone(phone) {
  const digits = onlyDigits(phone);
  if (digits.startsWith("62")) return digits.slice(2);
  if (digits.startsWith("0")) return digits.slice(1);
  return digits;
}

function hashOtp(otp) {
  return crypto.createHash("sha256").update(String(otp)).digest("hex");
}

async function sendWhatsappOtp(target, otp) {
  const body = new URLSearchParams({
    target,
    message: `Kode OTP reset password Dapur Andia Anda: ${otp}. Jangan bagikan kode ini kepada siapa pun.`,
    countryCode: "62"
  });

  const response = await fetch("https://api.fonnte.com/send", {
    method: "POST",
    headers: {
      Authorization: fonnteToken.value(),
      "Content-Type": "application/x-www-form-urlencoded"
    },
    body
  });
  const text = await response.text();
  let json = {};
  try {
    json = JSON.parse(text);
  } catch (_) {
    json = {};
  }

  if (!response.ok || json.status !== true) {
    throw new Error(json.detail || "Fonnte tidak menerima permintaan OTP.");
  }
}
