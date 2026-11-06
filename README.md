# Teen Patti 3

Do APK, ek Telegram group = DB.

## Telegram setup (ek baar)
1. @BotFather → `/newbot` **do baar** → `ADMIN_BOT_TOKEN`, `PUBLIC_BOT_TOKEN`
2. Dono bot pe `/setprivacy` → **Disable**
3. Dono bot ko ek private group me add karo, dono ko admin banao
4. Group id: `-100xxxxxxxxxx` (link se ya @userinfobot se)
5. Group me koi bhi message bhejo warna getUpdates khali rahega

## Build
Push to `main` → Actions → `teenpatti-apks` artifact me dono APK.

## Pehli baar
- Admin APK: token + chat id + password → Ping (dono OK aana chahiye) → Save → Login
- Settings → Table config bharo → "COPY CODE"
- User APK: wahi code paste → register

## Scale
~500 users tak theek. Uske baad Firebase Realtime DB pe shift karo.
