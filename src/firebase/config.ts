import { initializeApp, getApps, getApp } from 'firebase/app';
import { getAuth, GoogleAuthProvider } from 'firebase/auth';
import { getFirestore } from 'firebase/firestore';

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY || "AIzaSyCb46rloSBnJqmMuDl2p5cNHKLxJdMHXIA",
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN || "accomplishment-plan-db.firebaseapp.com",
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID || "accomplishment-plan-db",
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET || "accomplishment-plan-db.firebasestorage.app",
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID || "724771876461",
  appId: import.meta.env.VITE_FIREBASE_APP_ID || "1:724771876461:web:accomplishmentplanweb01",
};

// Initialize Firebase App singleton
const app = getApps().length > 0 ? getApp() : initializeApp(firebaseConfig);

export const auth = getAuth(app);
export const db = getFirestore(app);
export const googleProvider = new GoogleAuthProvider();
googleProvider.setCustomParameters({ prompt: 'select_account' });

export default app;
