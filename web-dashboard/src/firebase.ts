import { initializeApp } from 'firebase/app';
import { getAuth } from 'firebase/auth';
import { getFirestore } from 'firebase/firestore';

// Replace with your Firebase Web Project Configuration
const firebaseConfig = {
  apiKey: process.env.VITE_FIREBASE_API_KEY || "AIzaSyDemoPlaceholderKeyForBuildSafety",
  authDomain: process.env.VITE_FIREBASE_AUTH_DOMAIN || "mydevice-control.firebaseapp.com",
  projectId: process.env.VITE_FIREBASE_PROJECT_ID || "mydevice-control",
  storageBucket: process.env.VITE_FIREBASE_STORAGE_BUCKET || "mydevice-control.appspot.com",
  messagingSenderId: process.env.VITE_FIREBASE_MESSAGING_SENDER_ID || "729862144893",
  appId: process.env.VITE_FIREBASE_APP_ID || "1:729862144893:web:a1b2c3d4e5f6g7"
};

const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const db = getFirestore(app);
