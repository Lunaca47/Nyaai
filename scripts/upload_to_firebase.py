"""
Upload Nyaai Legal Dataset to Firebase Cloud Storage / Firestore.
Uses firebase-admin SDK or provides Firebase CLI deployment guidance.
"""
import os
import json

ROOT_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATASET_PATH = os.path.join(ROOT_DIR, "web", "data", "legal_dataset_master.json")
MANIFEST_PATH = os.path.join(ROOT_DIR, "web", "data", "manifest.json")

def upload_via_firebase_admin(cred_path=None):
    try:
        import firebase_admin
        from firebase_admin import credentials, storage, firestore

        if cred_path and os.path.exists(cred_path):
            cred = credentials.Certificate(cred_path)
            firebase_admin.initialize_app(cred, {
                'storageBucket': 'nyaai-8ce6d.firebasestorage.app'
            })
        else:
            print("[INFO] To authenticate Firebase Admin SDK, set GOOGLE_APPLICATION_CREDENTIALS or pass your service-account.json path.")
            print("[INFO] Alternatively, you can deploy using the Firebase CLI or GitHub Pages CDN.")
            return False

        bucket = storage.bucket()
        blob = bucket.blob("data/legal_dataset_master.json")
        blob.upload_from_filename(DATASET_PATH)
        blob.make_public()
        print(f"[SUCCESS] Uploaded dataset to Firebase Storage: {blob.public_url}")

        manifest_blob = bucket.blob("data/manifest.json")
        manifest_blob.upload_from_filename(MANIFEST_PATH)
        manifest_blob.make_public()
        print(f"[SUCCESS] Uploaded manifest to Firebase Storage: {manifest_blob.public_url}")
        return True
    except ImportError:
        print("[NOTICE] 'firebase-admin' Python package not installed. (Run: pip install firebase-admin)")
        return False
    except Exception as e:
        print(f"[ERROR] Firebase upload failed: {e}")
        return False

def print_cli_instructions():
    print("=" * 65)
    print("NYAAI CLOUD DISTRIBUTION OPTIONS")
    print("=" * 65)
    print("1. GITHUB PAGES CDN (Zero Configuration - Active Now):")
    print("   Data is available at:")
    print("   https://lunaca47.github.io/Nyaai/data/manifest.json")
    print("   https://lunaca47.github.io/Nyaai/data/legal_dataset_master.json")
    print()
    print("2. FIREBASE STORAGE / HOSTING (Firebase Project: nyaai-8ce6d):")
    print("   Deploy using Firebase CLI:")
    print("   > npm install -g firebase-tools")
    print("   > firebase login")
    print("   > firebase use nyaai-8ce6d")
    print("   > firebase deploy --only hosting")
    print("=" * 65)

if __name__ == "__main__":
    uploaded = upload_via_firebase_admin()
    if not uploaded:
        print_cli_instructions()
