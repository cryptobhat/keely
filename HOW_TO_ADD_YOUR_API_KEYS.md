# 🔑 How to Add Your Bhashini API Keys

You mentioned you have your API key and inference key. Here's how to configure them:

## ✅ Option 1: Add to `local.properties` (Recommended)

1. **Open** `local.properties` file in your project root
   - If it doesn't exist, create it next to `gradle.properties`

2. **Add these lines:**

```properties
# Bhashini API Configuration
BHASHINI_API_KEY=YOUR_ULCA_API_KEY_HERE
BHASHINI_USER_ID=YOUR_USER_ID_HERE
BHASHINI_INFERENCE_KEY=YOUR_INFERENCE_KEY_HERE
```

3. **Replace** the placeholders with your actual keys:
   - `YOUR_ULCA_API_KEY_HERE` → Your Bhashini ulcaApiKey
   - `YOUR_USER_ID_HERE` → Your Bhashini user ID
   - `YOUR_INFERENCE_KEY_HERE` → Your inference API key

4. **Save** the file

## ✅ Option 2: Configure via Settings UI (Runtime)

You can also set the keys programmatically or via settings UI:

```kotlin
val apiKeyManager = ApiKeyManager(context)
apiKeyManager.setBhashiniApiKey("your_ulca_api_key")
apiKeyManager.setBhashiniUserId("your_user_id")
apiKeyManager.setBhashiniInferenceKey("your_inference_key")
```

## 📋 What Each Key Is For

### 1. **BHASHINI_API_KEY** (ulcaApiKey)
- This is your main API key from Bhashini
- Used in the **pipeline configuration** request
- Required for getting service IDs

### 2. **BHASHINI_USER_ID**
- Your Bhashini user ID
- Also used in the pipeline configuration request

### 3. **BHASHINI_INFERENCE_KEY** (Optional)
- Sometimes provided by the pipeline config response
- Used for the actual TTS/ASR inference calls
- If not provided, the main API key will be used

## 🧪 Test Your Configuration

After adding the keys, build the project:

```bash
./gradlew clean assembleDebug
```

Then check the logs when you try TTS:
- ✅ Success: You should see pipeline config response in logs
- ❌ Error: Check if keys are correct

## 🔒 Security Note

`local.properties` is already in `.gitignore`, so your keys won't be committed to git. Keep them safe!

## 🚀 Next Steps

After configuring the keys, the remaining fixes will be applied to TTSManager to use the correct API flow. Once that's done, TTS should work perfectly!
